# Spec Delta

## ADDED Requirements

### Requirement: Automated Client and SMTP Roundtrip Verification
The system SHALL verify the complete end-to-end email lifecycle by orchestrating the client library and inbound SMTP service within automated integration test suites.

#### Scenario: Client Mailbox Creation SMTP Reception and Client Verification
- **WHEN** an automated test client creates a mailbox via `XCMailrClient`, transmits a multipart email via live SMTP to that mailbox address, and polls for message arrival using `XCMailrClient`
- **THEN** the message is accepted by the SMTP server, persisted to storage, retrieved via the REST API with matching sender, recipient, subject, HTML, and text content, and cleanly removed upon test completion.

