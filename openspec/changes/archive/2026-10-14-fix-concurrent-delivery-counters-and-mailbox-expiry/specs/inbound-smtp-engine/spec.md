# Spec Delta

## MODIFIED Requirements

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

