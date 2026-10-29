# Spec Delta

## MODIFIED Requirements

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

