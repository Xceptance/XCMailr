# Spec Delta: Inbound SMTP Engine

## MODIFIED Requirements

### Requirement: Email Storage and Retention
The system SHALL persist incoming emails, extract metadata and attachments, and purge expired messages according to retention policies, correctly handling UTF-8 and internationalized header text, sender display names, and attachment filenames.

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

#### Scenario: Retention Expiration Cleanup
- **WHEN** the background expiration job executes
- **THEN** emails exceeding the retention period are permanently removed from storage

## ADDED Requirements

### Requirement: Internationalized Email Forwarding
The system SHALL preserve UTF-8 headers and international characters when forwarding incoming emails to target destination addresses.

#### Scenario: Forward Email with UTF-8 Subject and Headers
- **WHEN** an incoming email with UTF-8 subject or headers is delivered to a mailbox configured for email forwarding
- **THEN** the system forwards the email via outbound SMTP preserving valid UTF-8 encoding in the forwarded message headers

#### Scenario: Forward Email with International Sender Display Name
- **WHEN** an incoming email with non-ASCII characters in the sender display name is forwarded
- **THEN** the system formats and emits the outbound `From` header preserving the Unicode display name
