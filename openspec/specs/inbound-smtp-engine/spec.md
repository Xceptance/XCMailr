# Inbound SMTP Engine Specification

## Purpose

Handles incoming SMTP connections, validates recipient domains, prevents open relay abuse, stores raw message content, and triggers outbound email forwarding.

## Requirements

### Requirement: Inbound SMTP Server Reception
The system SHALL operate an embedded SMTP listener on configured ports supporting plain and STARTTLS connections.

#### Scenario: Plaintext SMTP Receipt
- **WHEN** a client initiates an SMTP session without TLS on the primary listening port
- **THEN** the system completes the SMTP handshake and processes commands according to RFC 5321

#### Scenario: STARTTLS Connection Upgrade
- **WHEN** a client issues the STARTTLS command and TLS is enabled
- **THEN** the server negotiates a TLS session before accepting email data

### Requirement: Anti-Relay and Whitelist Enforcement
The system SHALL reject any inbound email addressed to a recipient domain not present in the allowed domain whitelist.

#### Scenario: Allowed Domain Acceptance
- **WHEN** an incoming email specifies a recipient address matching a configured whitelist domain
- **THEN** the SMTP server accepts the recipient (`250 OK`) for delivery processing

#### Scenario: External Domain Rejection
- **WHEN** an incoming email specifies a recipient address for an unmanaged external domain
- **THEN** the SMTP server rejects the recipient (`553 Relaying denied`)

### Requirement: Email Storage and Retention
The system SHALL persist incoming emails, extract metadata, plain and HTML bodies, and attachments, and purge expired messages according to retention policies, correctly handling UTF-8 and internationalized header text, sender display names, attachment filenames, and untyped or raw UTF-8 email bodies.

#### Scenario: Store Incoming Message with Attachments
- **WHEN** an email is delivered to a valid mailbox configured for storage
- **THEN** the system persists the message body, sender, subject, receive timestamp, and attachments associated with the mailbox ID

#### Scenario: Store Incoming Message with UTF-8 Headers and Attachments
- **WHEN** an incoming email contains raw 8-bit UTF-8 or RFC 2047 encoded subject headers, international sender names, or UTF-8 / RFC 2231 encoded attachment filenames
- **THEN** the system decodes the subject, sender, and attachment names as proper Unicode strings without character corruption or mojibake

#### Scenario: Store Incoming Message with Multi-byte Emojis and International Scripts
- **WHEN** an incoming email contains 4-byte UTF-8 emojis or non-Latin scripts (such as Cyrillic, CJK, or Arabic) in the subject or body
- **THEN** the system persists and retrieves the text preserving all Unicode codepoints accurately

#### Scenario: Store Incoming Message with Folded Multi-line Headers
- **WHEN** an incoming email contains long UTF-8 headers wrapped across multiple lines with CRLF and folding whitespace
- **THEN** the system unfolds and decodes the header text continuously without truncating or inserting mojibake

#### Scenario: Store Incoming Message with Malformed or Partial UTF-8 Header Sequences
- **WHEN** an incoming email contains invalid or truncated multi-byte UTF-8 sequences in its headers
- **THEN** the system ingests the email without unhandled exceptions, persists the raw message payload, and stores a resilient fallback string for the affected metadata

#### Scenario: Store and Parse Incoming Message with Untyped UTF-8 Body
- **WHEN** an incoming email contains raw 8-bit UTF-8 text in its body and omits a Content-Type header or omits an explicit charset parameter
- **THEN** the system parses and extracts the plain text and HTML bodies as valid UTF-8 without mojibake or character corruption

#### Scenario: Store and Parse Incoming Message with Explicit Legacy Charset Body
- **WHEN** an incoming email specifies an explicit legacy charset (such as ISO-8859-1 or Windows-1252) in its Content-Type header
- **THEN** the system decodes the body using the declared legacy charset preserving the original character encoding

#### Scenario: Store and Parse Incoming Message with Malformed UTF-8 Body Sequences
- **WHEN** an incoming email body contains invalid or truncated multi-byte UTF-8 sequences without an explicit charset
- **THEN** the system falls back gracefully to ISO-8859-1 decoding without throwing an unhandled exception or failing the parser

#### Scenario: Retention Expiration Cleanup
- **WHEN** the background expiration job executes
- **THEN** emails exceeding the retention period are permanently removed from storage

### Requirement: Inbound SMTP TLS Keystore and Certificate Management
The system SHALL support loading server TLS certificates from configurable PKCS12 or JKS keystores to authenticate the server during STARTTLS connection negotiation.

#### Scenario: Default Bundled Development Keystore
- **WHEN** inbound SMTP TLS is enabled and no external keystore path is configured
- **THEN** the system loads the bundled development keystore from the classpath and completes the STARTTLS handshake

#### Scenario: External Production Keystore Configuration
- **WHEN** an external keystore file path and password are configured
- **THEN** the system loads the specified server certificate and presents it to connecting SMTP clients during STARTTLS negotiation

#### Scenario: Opportunistic Plaintext Delivery
- **WHEN** inbound SMTP TLS is enabled with opportunistic mode (`require-tls = false`) and an unencrypted client initiates a plaintext session
- **THEN** the system completes the session and processes incoming mail without requiring a STARTTLS upgrade

#### Scenario: Mandatory TLS Enforcement
- **WHEN** inbound SMTP TLS is configured as mandatory (`require-tls = true`) and an unencrypted client attempts mail transaction commands without upgrading via STARTTLS
- **THEN** the system rejects commands requiring encryption with an SMTP 530 error until STARTTLS is negotiated

#### Scenario: Invalid or Missing Keystore Handling
- **WHEN** inbound SMTP TLS is enabled but the configured keystore path is invalid or the password is wrong
- **THEN** the system logs a descriptive diagnostic error and aborts startup or prevents invalid TLS advertising

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

### Requirement: Internationalized Email Forwarding
The system SHALL preserve UTF-8 headers, international characters, and message bodies when forwarding incoming emails to target destination addresses, ensuring untyped single-part messages declare UTF-8 character encoding while preserving explicit legacy charsets and multipart structures.

#### Scenario: Forward Email with UTF-8 Subject and Headers
- **WHEN** an incoming email with UTF-8 subject or headers is delivered to a mailbox configured for email forwarding
- **THEN** the system forwards the email via outbound SMTP preserving valid UTF-8 encoding in the forwarded message headers

#### Scenario: Forward Email with International Sender Display Name
- **WHEN** an incoming email with non-ASCII characters in the sender display name is forwarded
- **THEN** the system formats and emits the outbound `From` header preserving the Unicode display name

#### Scenario: Forward Untyped Plain Text Email Body
- **WHEN** an incoming email contains an untyped plain text body without a `Content-Type` header and is forwarded
- **THEN** the outbound forwarded message declares `Content-Type: text/plain; charset=UTF-8` and `MIME-Version: 1.0` so receiving MTAs and email clients decode body characters accurately

#### Scenario: Forward Email Body with Missing Charset Parameter
- **WHEN** an incoming single-part text email contains `Content-Type: text/plain` or `text/html` lacking an explicit `charset` parameter
- **THEN** the outbound forwarded message appends `; charset=UTF-8` to the Content-Type header

#### Scenario: Forward Email Body with Explicit Charset Preservation
- **WHEN** an incoming email specifies an explicit character set (such as `ISO-8859-1` or `UTF-8`) in its Content-Type header
- **THEN** the outbound forwarded message preserves the original Content-Type header and charset declaration unmodified

#### Scenario: Forward Multipart Email Preservation
- **WHEN** an incoming email has a `multipart/*` content type
- **THEN** the outbound forwarded message preserves the top-level multipart Content-Type header, boundary parameter, and nested body parts without alteration

