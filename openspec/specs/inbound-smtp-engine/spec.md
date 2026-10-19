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
The system SHALL evaluate the existence and activation status of the recipient mailbox and owning user upon email delivery, dropping messages destined for invalid recipients without bouncing, updating delivery counters atomically to prevent concurrency contention, and rejecting oversized messages.

#### Scenario: Non-Existent Mailbox Black-Hole Disposition
- **WHEN** an incoming email is accepted for a whitelisted domain but the target mailbox does not exist
- **THEN** the system completes the SMTP transaction successfully (`250 OK`), drops the message content without persisting it to the mail table, and records transaction status 100

#### Scenario: Inactive or Expired Mailbox Disposition
- **WHEN** an incoming email is delivered to an inactive or expired mailbox
- **THEN** the system completes the SMTP transaction, drops the message content, increments mailbox suppressions atomically in the database, and records transaction status 200

#### Scenario: Inactive Owning User Disposition
- **WHEN** an incoming email is delivered to a mailbox owned by an inactive user
- **THEN** the system completes the SMTP transaction, drops the message content, increments mailbox suppressions atomically in the database, and records transaction status 600

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
The system SHALL preserve UTF-8 headers and international characters when forwarding incoming emails to target destination addresses.

#### Scenario: Forward Email with UTF-8 Subject and Headers
- **WHEN** an incoming email with UTF-8 subject or headers is delivered to a mailbox configured for email forwarding
- **THEN** the system forwards the email via outbound SMTP preserving valid UTF-8 encoding in the forwarded message headers

#### Scenario: Forward Email with International Sender Display Name
- **WHEN** an incoming email with non-ASCII characters in the sender display name is forwarded
- **THEN** the system formats and emits the outbound `From` header preserving the Unicode display name

