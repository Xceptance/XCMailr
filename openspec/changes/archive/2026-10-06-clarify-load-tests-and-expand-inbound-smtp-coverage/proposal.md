# Proposal

## Why

Recent exploration of the XCMailr load test suite revealed that running `SendMail` with an invalid API token unexpectedly succeeds. This occurs because `SendMail` is a pure SMTP throughput load test that never communicates with the HTTP REST API where tokens are authenticated, and XCMailr's inbound SMTP architecture intentionally accepts any recipient on a whitelisted domain (such as `jw@xcmailr.test`), black-holing the message with a transaction log entry (status 100) if the mailbox does not exist.

Furthermore, an evaluation against the test pyramid identified gaps in inbound SMTP testing:
1. The load test suite documentation does not clarify that `SendMail` is a pure SMTP benchmark (where the token is only a placeholder for static client initialization), contrasting it with `SendAndCheckMail` (which actively authenticates via REST and validates message persistence).
2. `SmtpDeliveryIT` lacks integration test scenarios for non-existent mailbox drops (status 100), inactive owning user drops (status 600), and oversized message rejections (SMTP 552).
3. `MailDeliveryService` has zero isolated mock unit tests at the base of the test pyramid to verify all routing disposition paths and status code records without starting an embedded server.

## What Changes

- **Clarify Load Test Documentation**:
  - Update `xcmailr-load-test-suite/README.md` to explicitly describe the architectural difference between the pure SMTP throughput test (`SendMail`) and the authenticated REST roundtrip test (`SendAndCheckMail`).
  - Explain why `SendMail` accepts any non-blank token (placeholder for `Utils.<clinit>`), and explain the disposable email "black hole" behavior where emails to uncreated mailboxes are accepted by SMTP (`250 OK`) and dropped with transaction status 100.
  - Leave Java code in `xcmailr-load-test-suite` untouched per explicit requirement.
- **Expand Inbound SMTP Integration Test Coverage**:
  - Add integration tests in `SmtpDeliveryIT` for:
    - Delivery to a non-existent mailbox on a valid domain (`250 OK` SMTP response, mail not saved, transaction status 100 logged).
    - Delivery to an active mailbox owned by an inactive user (mail dropped, suppressions incremented, transaction status 600 logged).
    - Delivery of an email exceeding `mbox.max-size` (rejected with SMTP 552 / `TooMuchDataException`).
- **Add Isolated Mock Unit Tests for `MailDeliveryService`**:
  - Create `MailDeliveryServiceTest` at the base of the test pyramid testing:
    - `accept(from, recipient)` with valid domain, unmanaged domain (relay rejection / status 500), and malformed addresses (status 0).
    - `deliver(from, recipient, data)` across all disposition paths (status 100, 200, 300, 600, loop breaking, and size limit exceptions).
  - Use `final` modifiers wherever possible, comprehensive Javadocs, and clean code comments.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `inbound-smtp-engine`: Specify requirements for inbound mailbox routing and disposition, defining the expected behaviors for uncreated mailboxes (status 100 black-hole drop), inactive mailboxes/users (status 200/600 drops), and maximum message size limits (SMTP 552 rejection).

## Impact

- **Affected Files**:
  - `xcmailr-load-test-suite/README.md` (documentation update only)
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/services/SmtpDeliveryIT.java` (additional integration tests)
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/services/MailDeliveryServiceTest.java` (new unit test class)
- **APIs and Runtime Behavior**: No changes to production application code or public APIs. No modifications to load test suite code.
- **Dependencies**: No new external dependencies required (uses existing Spring Boot Test, Mockito, and JUnit 5).
