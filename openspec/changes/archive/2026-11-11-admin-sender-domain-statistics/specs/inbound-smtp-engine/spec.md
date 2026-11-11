# Spec Delta

## MODIFIED Requirements

### Requirement: Inbound Mailbox Routing and Disposition
The system SHALL evaluate the existence and activation status of the recipient mailbox and owning user upon email delivery, dropping messages destined for invalid recipients without bouncing, recording transaction audit entries asynchronously via an in-memory queue, updating delivery counters atomically to prevent concurrency contention, rejecting oversized messages, recording status 250 for emails stored locally when outbound forwarding is disabled, accurately calculating 15-minute slot buckets (`0..95`) based on transaction timestamps, determining whether to update existing slot rows or insert new slot rows in `MAIL_STATISTICS`, and guaranteeing idempotent database schema migrations and code documentation standards.

#### Scenario: Non-Existent Mailbox Black-Hole Disposition
- **WHEN** an incoming email is accepted for a whitelisted domain but the target mailbox does not exist
- **THEN** the system completes the SMTP transaction successfully (`250 OK`), drops the message content without persisting it to the mail table, and records transaction status 100

#### Scenario: Inactive or Expired Mailbox Disposition
- **WHEN** an incoming email is delivered to an inactive or expired mailbox
- **THEN** the system completes the SMTP transaction, drops the message content, increments mailbox suppressions atomically in the database, and records transaction status 200

#### Scenario: Inactive Owning User Disposition
- **WHEN** an incoming email is delivered to a mailbox owned by an inactive user
- **THEN** the system completes the SMTP transaction, drops the message content, increments mailbox suppressions atomically in the database, and records transaction status 600

#### Scenario: Stored Inbound Email Disposition (No Forwarding)
- **WHEN** an incoming email is accepted and persisted for an active mailbox configured with forwarding disabled (`forwardEmails = false`)
- **THEN** the system persists the message content into the `mail` table, increments the stored message count, and records transaction status 250 (conforming to SMTP RFC 5321 "250 Requested mail action okay, completed") indicating successful local inbox storage

#### Scenario: Asynchronous Transaction Enqueueing and Batch Persistence
- **WHEN** incoming email deliveries produce transaction audit events
- **THEN** the system enqueues transactions into a non-blocking in-memory queue without synchronous database roundtrips during SMTP delivery
- **AND** a scheduled background worker periodically drains the queue, updates aggregated metrics in `MAIL_STATISTICS` (tracking drops, forwards, and stores), and batch-persists transaction records into `mailtransactions`

#### Scenario: Graceful Transaction Queue Flush on Shutdown
- **WHEN** the application context receives a shutdown or termination signal
- **THEN** the system drains any remaining queued transactions, updates `MAIL_STATISTICS`, and batch-persists them to the database before the application context stops

#### Scenario: 15-Minute Quarter-Hour Slot Calculation (Happy Path)
- **WHEN** transaction records are aggregated for a given timestamp
- **THEN** the system computes the zero-indexed 15-minute quarter-hour interval of the day (`(hour * 4) + (minute / 15)`) ranging strictly from `0` to `95`
- **AND** associates the record with the SQL date of the transaction timestamp

#### Scenario: Same-Slot Aggregation and Update (Happy Path)
- **WHEN** multiple transactions occur within the same 15-minute window for identical source and target domains
- **THEN** the system updates the existing row in `MAIL_STATISTICS` matching `(date, quarterHour, fromDomain, targetDomain)` by adding the incremental drop, forward, and store counts without creating duplicate rows

#### Scenario: Consecutive-Slot Row Insertion (Happy Path)
- **WHEN** a transaction occurs in a subsequent 15-minute window (e.g. minute 16 vs minute 14) or for a new date
- **THEN** the system computes the new quarter-hour index and inserts a brand new row into `MAIL_STATISTICS` initialized with the transaction's counts

#### Scenario: Multi-Domain Partitioning in Identical Slot (Special Case)
- **WHEN** transactions from different sender domains or to different recipient domains arrive in the exact same 15-minute interval
- **THEN** the system creates and maintains separate, distinct rows in `MAIL_STATISTICS` partitioned by `(fromDomain, targetDomain)` for that same `(date, quarterHour)`

#### Scenario: Multi-Batch Consecutive Run Slot Update (Special Case)
- **WHEN** a scheduled aggregation run persists a row for a 15-minute slot, and a subsequent scheduled aggregation run processes additional transactions arriving within that same 15-minute slot
- **THEN** the second run detects the pre-existing row in `MAIL_STATISTICS` and safely updates its counts rather than failing on primary key conflict or creating duplicate records

#### Scenario: Slot Boundary Clamping and Midnight Transition (Edge Case)
- **WHEN** a transaction arrives at `00:00:00.000` (slot 0), `00:14:59.999` (slot 0), `00:15:00.000` (slot 1), or `23:59:59.999` (slot 95)
- **THEN** the slot index is calculated accurately without off-by-one errors, and transactions crossing midnight are partitioned into their respective dates

#### Scenario: Malformed Domain Handling during Aggregation (Error Case)
- **WHEN** a transaction record has a missing, unparseable, or null source or target email address
- **THEN** the system safely persists the raw transaction to `mailtransactions` for administrative inspection, but safely bypasses `MAIL_STATISTICS` aggregation without throwing a NullPointerException or aborting the batch

#### Scenario: Idempotent Schema Migration for Store Statistics
- **WHEN** database schema migrations execute during application startup
- **THEN** Flyway executes `V5__Add_Store_Count_To_Mail_Statistics.sql` adding column `STORE_COUNT INT DEFAULT 0` to `MAIL_STATISTICS` using idempotent DDL (`ADD COLUMN IF NOT EXISTS`)
- **AND** repeated executions on existing databases succeed without error or duplicate column exceptions

#### Scenario: Idempotent Transaction Aggregation (No Double Counting)
- **WHEN** the scheduled aggregation worker executes consecutively with no newly arrived transactions
- **THEN** the in-memory queue drains zero items and existing counts in `MAIL_STATISTICS` remain unchanged without multiplying or re-aggregating previously processed transactions

#### Scenario: Concurrent Inbound Transaction Buffering
- **WHEN** multiple concurrent SMTP delivery sessions deliver and store messages simultaneously
- **THEN** the in-memory buffer accepts concurrent transaction entries without lock contention, race conditions, or dropped entries

#### Scenario: Code Hygiene and Documentation Standards
- **WHEN** engineers inspect or compile the transaction and delivery codebase
- **THEN** all method parameters, local variables, and immutable fields declare the `final` modifier
- **AND** all classes, interfaces, records, methods, and algorithmic branches contain descriptive Javadoc and inline explanatory code comments

#### Scenario: Concurrent Delivery to Inactive Mailbox
- **WHEN** multiple concurrent SMTP delivery sessions deliver messages to the same inactive or expired mailbox simultaneously
- **THEN** the system increments the mailbox suppression counter atomically in the database without encountering optimistic locking conflicts or aborting SMTP delivery sessions

#### Scenario: Concurrent Forwarding Email Delivery
- **WHEN** multiple concurrent SMTP delivery sessions deliver and forward messages for the same active mailbox simultaneously
- **THEN** the system increments the mailbox forward counter atomically in the database without encountering optimistic locking conflicts or aborting SMTP delivery sessions

#### Scenario: Oversized Message Rejection
- **WHEN** an incoming email exceeds the configured maximum message size limit
- **THEN** the SMTP server aborts delivery processing, rejects the data stream with an SMTP size limit error, and drops the message
