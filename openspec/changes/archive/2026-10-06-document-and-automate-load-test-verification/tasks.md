# Tasks

## 1. Documentation

- [x] 1.1 Author `xcmailr-load-test-suite/README.md` documenting the test suite architecture, scenario descriptions (`CreateAndDeleteMailbox`, `SendMail`, `SendAndCheckMail`), configuration properties, manual CLI execution steps, and XLT report locations.
- [x] 1.2 Verify that `xcmailr-load-test-suite/README.md` contains accurate flags matching `config/project.properties` and renders valid Markdown formatting.

## 2. Automated End-to-End Build Verification

- [x] 2.1 Implement `ClientSmtpRoundtripIT.java` in `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/client/` verifying the full roundtrip lifecycle: provisioning a mailbox via `XCMailrClient`, delivering a multipart email over the embedded SubEtha SMTP socket, retrieving and verifying email contents via `XCMailrClient`, and deleting the mailbox. Use `final` modifiers wherever possible, and include thorough Javadoc and step-by-step code comments.
- [x] 2.2 Execute `mvn test` across all reactor modules (`xcmailr-webapp`, `xcmailr-client`, `xcmailr-load-test-suite`) to verify that the automated roundtrip test and all existing tests pass with zero failures.
