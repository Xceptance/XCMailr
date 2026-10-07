# Spec Delta

## ADDED Requirements

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
