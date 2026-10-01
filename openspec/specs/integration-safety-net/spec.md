# Integration Safety Net Specification

## Purpose

Provides an automated, black-box integration test harness verifying REST API contracts, inbound SMTP reception, mail forwarding, and error edge cases across builds.

## Requirements

### Requirement: Automated REST API Verification
The system SHALL verify all REST API endpoints through automated black-box HTTP integration tests during the Maven verify lifecycle phase.

#### Scenario: Verify Mailbox CRUD Lifecycle
- **WHEN** the test client creates, retrieves, lists, updates, and deletes a mailbox via `/api/v1/mailboxes` with a valid token
- **THEN** each request returns expected HTTP status codes (200/201), valid JSON payload schemas, and persists changes accurately

#### Scenario: Verify API Authentication and Error Handling
- **WHEN** requests are made to `/api/v1/*` with missing, invalid, or expired API tokens
- **THEN** the system returns HTTP 401 Unauthorized or HTTP 403 Forbidden with standard error bodies

### Requirement: Automated SMTP Protocol Verification
The system SHALL verify inbound SMTP mail delivery, storage, domain whitelisting, and forwarding behavior over network sockets.

#### Scenario: Valid Email Forwarding
- **WHEN** a client transmits an SMTP email addressed to an active, whitelisted mailbox
- **THEN** the system accepts the email (250 OK), logs a mail transaction, and forwards the email to the registered target address

#### Scenario: Unknown Domain Rejection
- **WHEN** a client attempts to send an email addressed to a domain not in the configured whitelist
- **THEN** the inbound SMTP server rejects the recipient with SMTP 553 relay error

#### Scenario: Expired Mailbox Drop
- **WHEN** an email is received for a mailbox whose active expiration timestamp has passed
- **THEN** the system does not forward the message and records an expired drop transaction

### Requirement: Email Edge Case Verification
The system SHALL verify handling of mail loops, oversized attachments, and MIME encoding variants.

#### Scenario: Loop Breaker Header Detection
- **WHEN** an inbound email contains an `X-Loop` header matching the system loop token
- **THEN** the system halts forwarding immediately to prevent mail loops

#### Scenario: Oversized Message Rejection
- **WHEN** an inbound email exceeds the maximum configured size limit
- **THEN** the system drops or rejects the message and records an error transaction
