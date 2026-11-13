# Proposal: Restore Admin Sender Domain Statistics and Asynchronous Transaction Processing

## Why

During high-load testing and everyday administration, the `/admin/statistics` dashboard provides incomplete and misleading metrics. Incoming emails delivered to local disposable inboxes (with forwarding disabled) increment stored message counts but produce zero transaction audit records and leave 24-hour and 7-day activity charts flatlined at zero. Furthermore, the statistics page previously replaced the original sender domain breakdown tables with a raw, time-unaware status code summary ("Transaktions-Zustellungsübersicht") that lacks sender domain insight. Additionally, a regression introduced during the Spring Boot modernization causes `aggregateAndCleanTransactions` to repeatedly query `findAll()` over the entire transaction table, multiplying statistics counts upon every scheduled execution and risking database contention under high load.

Restoring the proven asynchronous in-memory queue architecture from legacy XCMailr, introducing a dedicated transaction status code for inbox-stored emails, and reviving the 24-hour and 7-day sender domain tables backed by robust `MAIL_STATISTICS` aggregation resolves these discrepancies, ensures high-throughput lock-free SMTP delivery, and provides administrators with accurate, durable traffic visibility.

## Status Code 250 Rationale

Transaction status code `250` (*"Mail received and stored in inbox"*) is selected based on the following technical rationale:
- **SMTP RFC 5321 Standard Compliance**: In SMTP (RFC 5321 Section 4.2.2), `250` is the canonical reply code for *"Requested mail action okay, completed"*. When an MTA accepts and completes delivery to a local mailbox, the SMTP session responds with `250 OK`.
- **Conflict Avoidance with Existing XCMailr Status Codes**: In XCMailr's legacy design (dating back to 2013), status `200` was assigned to *"Mail exists but is inactive"* (suppression drop), while status `300` was assigned to *"Mail forwarded successfully"*. Reusing `200` would cause collisions and corrupt drop statistics, while `300` would falsely indicate an outbound forwarding dispatch.
- **Semantic Precision**: Status `250` unambiguously signals successful inbound delivery and persistence to local storage without forwarding, adhering to the 2xx success category while maintaining clean orthogonality across all transaction status badges.

## What Changes

- **Inbound Stored Email Transaction Logging**: In `MailDeliveryService`, record transaction status `250` (*"Mail received and stored in inbox"*) when an incoming email is persisted to an active mailbox with forwarding disabled (`!mbox.isForwardEmails()`).
- **Asynchronous In-Memory Transaction Buffer**: Introduce an asynchronous transaction queue service (`MailTransactionQueueService`) backed by a non-blocking `ConcurrentLinkedQueue<MailTransaction>`, restoring lock-free, nano-second transaction enqueuing during inbound SMTP reception and eliminating synchronous JDBC roundtrips from the hot delivery path.
- **Robust Scheduled Aggregation and Batch Persistence**: In `ExpirationScheduledService`, periodically drain the transaction queue, aggregate counts into `MAIL_STATISTICS` exactly once per transaction (including a new `storeCount` metric alongside `dropCount` and `forwardCount`), batch-persist drained transactions to `mailtransactions` via `saveAll()`, and ensure graceful queue flushing upon application shutdown via `@PreDestroy`.
- **Idempotent Database Schema Migration**: Add a `STORE_COUNT` column to `MAIL_STATISTICS` via an idempotent Flyway migration (`V5__Add_Store_Count_To_Mail_Statistics.sql`) using `ADD COLUMN IF NOT EXISTS`, and map `storeCount` on `MailStatistics`.
- **Activity Chart Enhancement**: Update `EmailStatisticsService` and the 24-hour and 7-day activity line charts to plot three distinct series: Forwarded (Status `300`), Stored (Status `250`), and Dropped (Status `100`, `200`, `400`, `500`, `600`, `0`).
- **Sender Domain Statistics Tables**: Replace the raw "Transaktions-Zustellungsübersicht" table on `/admin/statistics` with two restored, localized, responsive tables:
  1. *Todays (last 24 hours) sender domains*
  2. *Last 7 days sender domains*
  Displaying `Sender Domain`, `Stored Mails`, `Forwarded Mails`, and `Dropped Mails`, queried reliably from `MAIL_STATISTICS` so domain metrics persist even when transaction audit logs are pruned or purged.
- **Code Quality & Testing Standards**:
  - Comprehensive test suites adhering strictly to the **Testing Pyramid**: Unit tests at the base (covering 15-minute slot calculations, boundary timestamps `00:00:00`, `00:14:59`, `23:59:59`, and midnight rollovers), integration tests in the middle (verifying same-slot UPDATE, consecutive-slot INSERT, multi-domain partitioning, multi-batch consecutive runs, idempotency without double counting, and defensive malformed domain handling), and MVC/delivery end-to-end tests at the top, systematically covering **Happy Path**, **Error Cases**, and **Special Cases**.
  - Code hygiene: Apply `final` modifiers on all parameters, local variables, and appropriate class fields, and provide complete Javadoc and descriptive inline code comments.

## Capabilities

### Modified Capabilities

- `inbound-smtp-engine`: Record transaction status `250` for locally stored messages, decouple delivery from database transaction logging using an in-memory queue buffer, periodically batch-persist transactions and aggregate `MAIL_STATISTICS` with shutdown flush guarantees, and apply idempotent schema migration `V5`.
- `web-console-ui`: Enhance `/admin/statistics` activity charts with a three-series breakdown (Forwarded, Stored, Dropped) and replace the raw status code overview with 24-hour and 7-day sender domain statistics tables backed by `MAIL_STATISTICS`.

## Impact

- **Affected Classes**:
  - `MailDeliveryService.java`: Enqueue status 250 for stored mails without forwarding; use `MailTransactionQueueService`.
  - `MailTransaction.java`: Document status code 250.
  - `MailStatistics.java`: Add `storeCount` property and getters/setters.
  - `MailStatisticsRepository.java`: Add queries for 24h (`CURRENT_DATE`) and 7d domain aggregations.
  - `ExpirationScheduledService.java`: Drain in-memory transaction queue, batch aggregate into `MAIL_STATISTICS`, batch-save to `mailtransactions`, and flush on `@PreDestroy`.
  - `EmailStatisticsService.java`: Support Stored (250) series in 24h and 7d chart DTOs.
  - `AdminWebController.java`: Provide 24h and 7d sender domain summary lists to the statistics model.
  - `admin/statistics.html`: Update charts for three series and render the two sender domain breakdown tables.
  - `conf/messages.properties` and `conf/messages_de.properties`: Utilize existing sender domain localization keys and add stored mail headers.
- **Database Schema**:
  - Idempotent Flyway migration `V5__Add_Store_Count_To_Mail_Statistics.sql` adding column `STORE_COUNT INT DEFAULT 0` to `MAIL_STATISTICS` using `IF NOT EXISTS`.
- **API & Backward Compatibility**:
  - Fully backward compatible with existing mailbox APIs and external clients. No breaking API changes.
