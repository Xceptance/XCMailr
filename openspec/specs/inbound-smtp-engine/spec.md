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
The system SHALL persist incoming emails, extract metadata and attachments, and purge expired messages according to retention policies.

#### Scenario: Store Incoming Message with Attachments
- **WHEN** an email is delivered to a valid mailbox configured for storage
- **THEN** the system persists the message body, sender, subject, receive timestamp, and attachments associated with the mailbox ID

#### Scenario: Retention Expiration Cleanup
- **WHEN** the background expiration job executes
- **THEN** emails exceeding the retention period are permanently removed from storage
