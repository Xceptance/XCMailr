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
The system SHALL evaluate the existence and activation status of the recipient mailbox and owning user upon email delivery, dropping messages destined for invalid recipients without bouncing and rejecting oversized messages.

#### Scenario: Non-Existent Mailbox Black-Hole Disposition
- **WHEN** an incoming email is accepted for a whitelisted domain but the target mailbox does not exist
- **THEN** the system completes the SMTP transaction successfully (`250 OK`), drops the message content without persisting it to the mail table, and records transaction status 100

#### Scenario: Inactive or Expired Mailbox Disposition
- **WHEN** an incoming email is delivered to an inactive or expired mailbox
- **THEN** the system completes the SMTP transaction, drops the message content, increments mailbox suppressions, and records transaction status 200

#### Scenario: Inactive Owning User Disposition
- **WHEN** an incoming email is delivered to a mailbox owned by an inactive user
- **THEN** the system completes the SMTP transaction, drops the message content, increments mailbox suppressions, and records transaction status 600

#### Scenario: Oversized Message Rejection
- **WHEN** an incoming email exceeds the configured maximum message size limit
- **THEN** the SMTP server aborts delivery processing, rejects the data stream with an SMTP size limit error, and drops the message

