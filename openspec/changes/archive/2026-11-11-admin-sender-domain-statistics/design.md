# Design: Admin Sender Domain Statistics and Asynchronous Transaction Processing

## Context

See `proposal.md` for background and motivation regarding missing stored mail metrics, the `aggregateAndCleanTransactions` double-counting regression, and the missing sender domain tables on `/admin/statistics`.

During inbound SMTP processing, `MailDeliveryService` handles message acceptance, mailbox lookup, disk storage, forwarding dispatch, and transaction audit recording. In the legacy architecture, transactions were buffered in a volatile lock-free in-memory queue (`mtxQueue`) and periodically aggregated into `MAIL_STATISTICS` while batch-saving to the transaction table. In the Spring Boot modernization, transactions were switched to synchronous per-message `mailTransactionRepository.save()` calls, and stored messages without forwarding bypassed transaction logging entirely. Furthermore, the periodic scheduled job was modified to query `findAll()` over all historical transactions on every execution, causing metric counts in `MAIL_STATISTICS` to multiply infinitely.

## Goals / Non-Goals

**Goals:**
- Decouple the inbound SMTP delivery path from database transaction log latency by re-introducing an asynchronous in-memory queue buffer (`MailTransactionQueueService`).
- Log transaction status code `250` (*"Mail received and stored in inbox"*, RFC 5321 compliant) for messages accepted and stored locally when forwarding is disabled (`!mbox.isForwardEmails()`).
- Guarantee exactly-once aggregation of transactions into `MAIL_STATISTICS` by draining the in-memory buffer on each scheduled run and flushing remaining entries on shutdown via `@PreDestroy`.
- Batch-persist drained transactions to `mailtransactions` via JPA `saveAll()` to eliminate single-row insert overhead.
- Extend `MAIL_STATISTICS` with `STORE_COUNT` via an idempotent Flyway migration (`V5__Add_Store_Count_To_Mail_Statistics.sql`) using `ADD COLUMN IF NOT EXISTS`, and map `storeCount` in `MailStatistics`.
- Enhance the 24-hour and 7-day activity charts in `EmailStatisticsService` and `statistics.html` with a three-series breakdown (Forwarded: 300, Stored: 250, Dropped: other).
- Replace the raw status code summary table ("Transaktions-Zustellungsübersicht") on `/admin/statistics` with two localized, responsive sender domain breakdown tables (*Todays sender domains* and *Last 7 days sender domains*) backed by `MAIL_STATISTICS`.
- Enforce clean testing pyramid coverage with explicit happy path, error case, and special case tests.
- Enforce code hygiene: `final` keywords on parameters and local variables, and comprehensive Javadoc and code comments throughout.

**Non-Goals:**
- Modifying or adding indexes on `mailtransactions` or `MAIL_STATISTICS` (explicitly excluded to eliminate insert/rebalancing overhead during high-concurrency burst tests).
- Altering the existing 30-day retention pruning of `MAIL_STATISTICS` or the configurable `maxAgeHours` retention pruning of `mailtransactions`.
- Introducing distributed messaging brokers (e.g., Kafka, RabbitMQ) or distributed caching; the lightweight in-memory queue meets all single-instance application requirements.

## Architecture Overview

```
 [Inbound SMTP Client]
          |
          v
 [MailDeliveryService] 
   |                 |
   | (MBox / Mail)   | (status 250, 300, 100...)
   v                 v
[Database]     [MailTransactionQueueService]
(mail table)    (ConcurrentLinkedQueue<MailTransaction>)
                     |
                     | Periodic (every 60s) OR @PreDestroy
                     v
           [ExpirationScheduledService]
             |                     |
             | drainAll()          |
             v                     v
   [mailtransactions]      [MAIL_STATISTICS]
   (batch saveAll())       (increment drop, forward, store)
                                   |
                                   v
                         [AdminWebController]
                                   |
                      +------------+------------+
                      |                         |
                      v                         v
           [Activity Line Charts]     [Sender Domain Tables]
           - 24h & 7d time-series     - Todays sender domains
           - 3 series: 300, 250, drop - Last 7 days sender domains
```

## Decisions

### 1. Status Code 250: Rationale and Standard Selection

**Decision:** Define status `250` (*"Mail received and stored in inbox"*) in `MailTransaction`. In `MailDeliveryService`, when an incoming email is persisted for an active mailbox with `forwardEmails == false`, call `recordTransaction(250, from, recipient, forwardTarget)`.

**Why was Status 250 chosen?**
1. **RFC 5321 Standard Alignment**: In the SMTP specification (RFC 5321 Section 4.2.2), `250` is the primary success completion code (*"250 Requested mail action okay, completed"*). When a message is successfully received and stored in a mailbox, the SMTP protocol responds with `250 OK`.
2. **Conflict Avoidance with Existing Codes**: In XCMailr's status code taxonomy (established in 2013):
   - `0` = Syntax / Regex parsing failure
   - `100` = Unknown mailbox (dropped)
   - `200` = Inactive mailbox (dropped / suppressed)
   - `300` = Forwarded successfully (outbound relay dispatch)
   - `400` = Forwarding unreachable / failed (dropped)
   - `500` = Relay denied (dropped)
   - `600` = Inactive owning user (dropped)
   Status `200` is already taken for inactive mailbox drops. Reusing `200` would corrupt drop statistics.
3. **Semantic Distinction from Status 300**: Status `300` signifies outbound relay forwarding (analogous to HTTP 3xx redirection). An email stored in the local mailbox without forwarding never triggers outbound relaying; giving it status `250` makes the distinction crystal clear in audit logs and charts.
4. **Intuitive for Mail Administrators**: Any systems engineer or administrator inspecting transaction logs recognizes `250` as successful inbox delivery.

**Alternatives Considered:**
- *Status 200*: Rejected. Already allocated to inactive mailbox suppression drops.
- *Status 201 (HTTP Created)*: Rejected. XCMailr's core inbound protocol is SMTP, not HTTP REST.
- *Reusing Status 300*: Rejected. Obscures whether a message was forwarded to an external mail server or kept in the local mailbox.

### 2. In-Memory Transaction Buffer via `ConcurrentLinkedQueue`

**Decision:** Create `MailTransactionQueueService` encapsulating a `ConcurrentLinkedQueue<MailTransaction>`. `MailDeliveryService` enqueues transaction records via `queue.offer()` without touching the database during SMTP receipt.

**Rationale:**
- Nanosecond, non-blocking lock-free enqueueing under high concurrency (e.g. 10,000+ load-test emails).
- Completely isolates the active SMTP delivery connection pool and listener threads from JDBC write latency and row locks.

**Alternatives Considered:**
- *Synchronous `mailTransactionRepository.save()` (Current State)*: Rejected. Creates database bottlenecks and connection pool starvation during load bursts.
- *`ArrayBlockingQueue` with fixed capacity*: Rejected. A fixed bounded buffer risks either blocking SMTP threads or silently dropping transaction audit logs when bursts exceed capacity.
- *Spring `@Async` method execution*: Rejected. Creates excessive thread context-switching overhead for lightweight audit records and does not provide batch aggregation.

### 3. Scheduled Drain and Aggregation Engine (`ExpirationScheduledService`)

**Decision:** During scheduled maintenance runs (`aggregateAndCleanTransactions`), drain all pending transactions from `MailTransactionQueueService.drainAll()`. Batch-insert the drained list into `mailtransactions` via `mailTransactionRepository.saveAll()` and aggregate counts into `MAIL_STATISTICS` in memory before persisting the summary entities.

**15-Minute Slot Determination and UPDATE vs. INSERT Logic:**
1. **Slot Calculation Formula**:
   For each drained transaction, extract timestamp `ts`. Compute the calendar date and the 15-minute quarter-hour interval:
   $$\text{quarterHour} = (\text{hourOfDay} \times 4) + \lfloor \text{minute} / 15 \rfloor$$
   This strictly maps every timestamp into an integer index between `0` (00:00 - 00:14:59) and `95` (23:45 - 23:59:59).
2. **Composite Key Construction**:
   Construct `MailStatisticsKey(date, quarterHour, fromDomain, targetDomain)` where `fromDomain` and `targetDomain` are normalized to lowercase. If either address lacks a valid domain, the transaction is safely bypassed for statistics aggregation (while still persisted to `mailtransactions` for auditing).
3. **In-Memory Batch Aggregation**:
   Transactions in the drained batch are aggregated into a `Map<MailStatisticsKey, int[]> statsMap` where the array tracks `[dropCount, forwardCount, storeCount]`. Multiple transactions falling into the same 15-minute slot for identical domains are consolidated into a single map entry.
4. **Database UPDATE vs. INSERT Resolution**:
   For each key in `statsMap`:
   - Query `mailStatisticsRepository.findById(key)`.
   - **If present (UPDATE)**: Increment the existing entity's `dropCount`, `forwardCount`, and `storeCount` by the batch delta and save.
   - **If absent (INSERT)**: Instantiate a new `MailStatistics` entity with the key and the batch counts, and save.
   This ensures that whether transactions arrive across separate scheduled runs or within the same run, pre-existing database rows for that 15-minute slot are cleanly updated without primary key conflicts.

**Rationale:**
- Polling/draining guarantees each transaction is processed and aggregated **exactly once**.
- Resolves the critical regression where `mailTransactionRepository.findAll()` repeatedly aggregated all 7-day historical transactions every minute.
- Batch saving minimizes JDBC roundtrips.

**Alternatives Considered:**
- *Tracking aggregated state with a `boolean aggregated` column on `MailTransaction`*: Rejected. Requires an extra `UPDATE` statement on every transaction row and increases write amplification.
- *Real-time incrementing of `MAIL_STATISTICS` directly in `MailDeliveryService`*: Rejected. Causes extreme row-level lock contention on `(date, quarterHour, fromDomain, targetDomain)` when multiple concurrent delivery threads receive emails from the same external domain.

### 4. Graceful Queue Flushing on Shutdown (`@PreDestroy`)

**Decision:** Register a `@PreDestroy` lifecycle hook on `ExpirationScheduledService` that invokes the drain-and-aggregate process upon application context termination.

**Rationale:**
- Prevents transaction audit log or statistics data loss when the application is stopped or restarted between scheduled execution intervals.

### 5. Idempotent Schema Migration `V5__Add_Store_Count_To_Mail_Statistics.sql`

**Decision:** Add column `STORE_COUNT INT DEFAULT 0` to `MAIL_STATISTICS` using an idempotent DDL statement:
```sql
ALTER TABLE MAIL_STATISTICS ADD COLUMN IF NOT EXISTS STORE_COUNT INT DEFAULT 0;
```
Map `private int storeCount` with `@Column(name = "STORE_COUNT")` on `MailStatistics`.

**Rationale:**
- Conforms to the idempotent pattern established in `V4__Add_Mailbox_Comment.sql`.
- Safe against repeated executions, test fixture reloads, and multi-node application boots without failing on duplicate column creation.

### 6. Sender Domain Breakdown Tables Backed by `MAIL_STATISTICS`

**Decision:** Add a JPQL aggregation query to `MailStatisticsRepository`:
```java
@Query("SELECT new com.xceptance.xcmailr.dto.DomainStatisticsDto(" +
       "ms.key.fromDomain, " +
       "COALESCE(SUM(ms.dropCount), 0L), " +
       "COALESCE(SUM(ms.forwardCount), 0L), " +
       "COALESCE(SUM(ms.storeCount), 0L)) " +
       "FROM MailStatistics ms " +
       "WHERE ms.key.date >= :startDate " +
       "GROUP BY ms.key.fromDomain " +
       "ORDER BY (COALESCE(SUM(ms.dropCount), 0L) + COALESCE(SUM(ms.forwardCount), 0L) + COALESCE(SUM(ms.storeCount), 0L)) DESC")
List<DomainStatisticsDto> findDomainStatisticsSince(@Param("startDate") java.sql.Date startDate);
```
In `AdminWebController.showStatistics`, invoke this query with `startDate = today` (for 24-hour summary) and `startDate = today - 6 days` (for 7-day summary) and pass `todaySenderDomains` and `weekSenderDomains` to the model.

**Rationale:**
- Domain metrics remain available for the full 30-day statistics retention window even when raw transactions in `mailtransactions` are purged by `maxAgeHours` retention limits.
- Fully database agnostic (standard JPQL, avoids vendor-specific date functions).
- Replaces the raw, domain-unaware "Transaktions-Zustellungsübersicht" table with the two restored legacy tables (*Todays sender domains* and *Last 7 days sender domains*).

### 7. Three-Series Chart.js Activity Visualization

**Decision:** Update `ChartDataDto` to include `List<Long> stored`, update `EmailStatisticsService` to bucket status 250 into the `stored` series, and configure `statistics.html` with three datasets:
- Forwarded (Status 300): Green (`#198754`, `rgba(25, 135, 84, 0.15)`)
- Stored (Status 250): Primary Blue (`#0d6efd`, `rgba(13, 110, 253, 0.15)`)
- Dropped (Statuses 100, 200, 400, 500, 600, 0): Danger Red (`#dc3545`, `rgba(220, 53, 69, 0.15)`)

## Testing Strategy (Testing Pyramid)

The implementation must strictly adhere to the testing pyramid with dedicated test cases across three distinct categories:
1. **Happy Path Tests**: Verifying intended behavior under normal operating conditions.
2. **Error Cases**: Verifying defensive error handling, invalid inputs, and failure recovery.
3. **Special Cases**: Verifying concurrency, empty datasets, zero-filling, idempotency, and boundary conditions.

| Layer | Component | Test Class | Happy Path | Error Cases | Special Cases |
|---|---|---|---|---|---|
| **Unit** (Base) | Quarter-Hour Slot Calculation | `ExpirationScheduledServiceTest` / `QuarterHourCalculationTest` | Mathematical mapping of timestamp to quarter-hour bucket `0..95` (`(hour * 4) + (minute / 15)`) | Null/negative timestamps handled safely | Boundary timestamps (`00:00:00`, `00:14:59.999`, `00:15:00`, `23:59:59.999`); midnight date rollover across consecutive calendar dates |
| **Unit** (Base) | Queue Buffer | `MailTransactionQueueServiceTest` | Enqueue & FIFO drain of single/multiple items | Handling null transactions gracefully (ignored/no NPE) | Multi-threaded concurrent enqueueing & draining; drain on empty queue |
| **Unit** (Base) | Time-Series Bucketing | `EmailStatisticsServiceTest` | 24h & 7d bucketing into forwarded (300), stored (250), dropped | Timestamps outside window, negative timestamps | Continuous zero-fill on empty hours/days; boundary clamping |
| **Unit** (Base) | Model Entity | `MailStatisticsTest` | `storeCount` getter/setter, `hashCode`/`equals` | Null keys, mismatched domain casing | Formatting large values, equality with unequal counts |
| **Integration** (Middle) | Scheduled Aggregator (Slots & Persistence) | `ExpirationScheduledServiceTest` | **Same-Slot Aggregation**: Multiple transactions in identical 15-min window increment single row (`storeCount`, `dropCount`, `forwardCount`);<br>**Consecutive-Slot Insert**: Transactions in consecutive 15-min windows create distinct rows with sequential `quarterHour` | **Malformed Domains**: Transactions with unparseable or null email addresses safely persisted to `mailtransactions` but bypassed for `MAIL_STATISTICS` without NPE | **Multi-Domain Partitioning**: Transactions from different domains in identical slot produce distinct rows;<br>**Multi-Batch Runs**: 2nd scheduled run in same slot updates existing database row;<br>**Idempotency**: Execution with empty queue leaves counts unchanged;<br>**Shutdown Flush**: `@PreDestroy` flushes buffer |
| **Integration** (Middle) | Repository JPQL | `MailStatisticsRepositoryTest` | Aggregating drop, forward, store counts by domain since start date | Empty table, dates before any data | Null/zero sums via `COALESCE`; sorting by sum descending |
| **E2E / Controller** (Top) | SMTP Delivery | `MailDeliveryServiceTest` / `SmtpDeliveryIT` | Stored email (`forwardEmails=false`) enqueues status 250 | Inactive mailbox (200), unknown box (100), user inactive (600) | High-volume burst delivery without DB connection pool exhaustion; multi-slot deliveries across time windows |
| **E2E / Controller** (Top) | Admin Console | `AdminWebControllerTest` | `/admin/statistics` populates `todaySenderDomains`, `weekSenderDomains`, 3-series charts | Unauthorized access rejection | Empty domain statistics render localized empty state banner |

### 15-Minute Slot Aggregation & Update vs. Insert Testing Matrix

The quarter-hour time-slot engine in `ExpirationScheduledService` requires exhaustive verification across the testing pyramid:

1. **Unit Layer (Slot Index & Boundaries)**:
   - **Happy Path**: Timestamps within hours 0 to 23 and minutes 0 to 59 map precisely to indices 0 through 95. E.g., `00:07` -> slot 0; `01:15` -> slot 5; `12:30` -> slot 50; `23:59` -> slot 95.
   - **Boundary Special Cases**:
     - `00:00:00.000` evaluates to slot 0.
     - `00:14:59.999` evaluates to slot 0.
     - `00:15:00.000` evaluates to slot 1.
     - `23:59:59.999` evaluates to slot 95.
     - Midnight Rollover: A transaction at `23:59:59` on Day 1 (slot 95) and a transaction at `00:00:01` on Day 2 (slot 0) generate keys with distinct `date` values (`Date.valueOf(Day 1)` vs `Date.valueOf(Day 2)`).

2. **Integration Layer (Database Update vs. Insert in `ExpirationScheduledServiceTest`)**:
   - **Same-Slot Aggregation (Happy Path)**: When 3 stored transactions (status 250), 2 drop transactions (status 100), and 1 forwarded transaction (status 300) for `sender.test` -> `xcmailr.test` arrive within the same 15-minute slot (e.g. at 10:02, 10:07, and 10:14), draining produces exactly **one** row in `MAIL_STATISTICS` with `storeCount = 3`, `dropCount = 2`, `forwardCount = 1`.
   - **Consecutive-Slot Row Insertion (Happy Path)**: When transaction A arrives at 10:14 (slot 40) and transaction B arrives at 10:16 (slot 41), draining produces exactly **two** distinct rows in `MAIL_STATISTICS` with `quarterHour = 40` and `quarterHour = 41`.
   - **Multi-Domain Partitioning (Special Case)**: When transaction A (`alice@alpha.test` -> `box@xcmailr.test`) and transaction B (`bob@beta.test` -> `box@xcmailr.test`) arrive in the same 15-minute slot, the engine creates **two** distinct rows partitioned by `fromDomain`.
   - **Multi-Batch Consecutive Run Slot Update (Special Case)**:
     - Run 1 drains a batch containing 1 transaction at 14:05 -> inserts row for slot 56 with `storeCount = 1`.
     - Run 2 drains a batch containing 1 transaction at 14:10 (same slot 56) -> invokes `findById(key)`, discovers the pre-existing row, and updates it to `storeCount = 2` without constraint violations.
   - **Aggregation Idempotency (Special Case)**:
     - Run 3 executes with an empty queue -> no database updates or inserts occur, and existing counts remain exactly 2.
   - **Malformed Domain Defensive Handling (Error Case)**:
     - Transactions with null, empty, or unparseable sender/target addresses (e.g. `"bad-address-no-at"`, `""`, `null`) are persisted to `mailtransactions` for administrator audit logging, but are safely bypassed during `MAIL_STATISTICS` key creation, avoiding `NullPointerException` or transaction rollback.

## Implementation & Code Quality Standards

1. **Strict `final` Usage**:
   - All method and constructor parameters MUST be declared `final`.
   - All local variables inside methods MUST be declared `final`.
   - All class fields MUST be declared `final` unless mutable entity state requires otherwise.
2. **Comprehensive Javadoc & Comments**:
   - Every class, interface, enum, and record MUST have class-level Javadoc with author and purpose.
   - Every public and package-private method and constructor MUST have Javadoc explaining parameters, return values, and behavior.
   - Non-obvious algorithmic decisions (e.g. quarter-hour calculation, COALESCE logic, lock-free queue draining) MUST have clear inline comments.

## Risks / Trade-offs

- **[Risk] Unsaved in-memory transactions if JVM process crashes abruptly (`SIGKILL`)**
  → *Mitigation:* The scheduled job runs frequently (every 60s) and `@PreDestroy` handles normal shutdown/restart signals (`SIGTERM`, `SIGINT`). In-memory transaction buffering for non-critical audit logs is the standard industry pattern (and legacy XCMailr's proven design) to preserve SMTP availability and throughput.
- **[Risk] Memory growth if background aggregation job stalls during a continuous high-volume attack**
  → *Mitigation:* `ConcurrentLinkedQueue` elements are small Java objects (~80 bytes each). 100,000 pending transactions consume ~8MB of heap. Even in severe spikes, memory footprint remains well within normal heap bounds.
- **[Risk] Schema migration on existing production databases with pre-existing `MAIL_STATISTICS` rows**
  → *Mitigation:* `ALTER TABLE ... ADD COLUMN IF NOT EXISTS STORE_COUNT INT DEFAULT 0` provides seamless, idempotent, zero-downtime column addition with default 0 on existing rows.

## Migration Plan

1. Flyway runs `V5__Add_Store_Count_To_Mail_Statistics.sql` on startup, idempotently adding the `STORE_COUNT` column.
2. New `MailTransactionQueueService` bean initializes with an empty `ConcurrentLinkedQueue`.
3. Inbound SMTP delivery immediately records status `250` for stored messages and pushes to the queue.
4. `ExpirationScheduledService` drains the queue, populates `MAIL_STATISTICS`, and batch-saves transactions.
5. If rollback is necessary, standard Git revert of commits; existing tables and data remain undamaged.
