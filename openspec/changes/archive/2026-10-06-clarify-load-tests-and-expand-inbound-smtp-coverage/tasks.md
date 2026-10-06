# Tasks

## 1. Load Test Suite Documentation

- [x] 1.1 Update `xcmailr-load-test-suite/README.md` to document the architectural distinction between `SendMail` (pure SMTP socket throughput benchmark) and `SendAndCheckMail` (full authenticated REST + SMTP roundtrip), clarifying why `SendMail` accepts any non-blank API token and black-holes emails sent to uncreated mailboxes without error. Verify markdown rendering and accuracy against test scenario implementations.

## 2. Inbound Mail Delivery Unit Tests

- [x] 2.1 Create `MailDeliveryServiceTest` in `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/services/` with Mockito unit tests verifying `accept(...)` for configured domain whitelists, database domain whitelists, unmanaged domain relay rejections (status 500), and malformed addresses (status 0). Ensure `final` modifiers on all variables/parameters, complete Javadocs, and verify by executing `mvn test -pl xcmailr-webapp -Dtest=MailDeliveryServiceTest`.
- [x] 2.2 Add unit tests to `MailDeliveryServiceTest` verifying `deliver(...)` drop logic for non-existent mailboxes (status 100), inactive/expired mailboxes (status 200), inactive users (status 600), and stream size limits (`TooMuchDataException`). Ensure `final` modifiers and complete Javadocs, verifying with `mvn test -pl xcmailr-webapp -Dtest=MailDeliveryServiceTest`.
- [x] 2.3 Add unit tests to `MailDeliveryServiceTest` verifying `deliver(...)` persistence for active mailboxes without forwarding, active mailboxes with forwarding (status 300), and loop prevention headers (`X-Loop`, `References`). Ensure `final` modifiers and complete Javadocs, verifying with `mvn test -pl xcmailr-webapp -Dtest=MailDeliveryServiceTest`.

## 3. Inbound Mail Delivery Integration Tests

- [x] 3.1 Add integration test `testInboundDeliveryToNonExistentMailbox` in `SmtpDeliveryIT` asserting that sending to an uncreated address on a managed domain succeeds on the wire (`250 OK`), persists no mail entity, and logs transaction status 100. Ensure `final` modifiers and verify with `mvn test -pl xcmailr-webapp -Dtest=SmtpDeliveryIT#testInboundDeliveryToNonExistentMailbox`.
- [x] 3.2 Add integration test `testInboundDeliveryToInactiveUser` in `SmtpDeliveryIT` asserting that delivering to a mailbox owned by an inactive user drops the message, increments suppressions, and logs transaction status 600. Ensure `final` modifiers and verify with `mvn test -pl xcmailr-webapp -Dtest=SmtpDeliveryIT#testInboundDeliveryToInactiveUser`.
- [x] 3.3 Add integration test `testOversizedEmailRejection` in `SmtpDeliveryIT` asserting that transmitting a message exceeding `mbox.max-size` triggers an SMTP size limit error and drops the message. Ensure `final` modifiers and verify with `mvn test -pl xcmailr-webapp -Dtest=SmtpDeliveryIT#testOversizedEmailRejection`.

## 4. Full Verification

- [x] 4.1 Run the full `xcmailr-webapp` test suite (`mvn test -pl xcmailr-webapp`) and verify all unit and integration tests pass cleanly with zero regressions.
