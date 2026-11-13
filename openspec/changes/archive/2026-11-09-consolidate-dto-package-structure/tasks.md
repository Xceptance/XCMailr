# Tasks

## 1. DTO File Relocation

- [x] 1.1 Move `MailboxData.java`, `MailData.java`, and `AttachmentData.java` from `com.xceptance.xcmailr.controllers.restapi` to `com.xceptance.xcmailr.dto`, update their package statements, and verify Javadoc, comments, and `final` modifiers
- [x] 1.2 Move `ApiError.java` and `ApiErrors.java` from `com.xceptance.xcmailr.controllers.restapi` to `com.xceptance.xcmailr.dto`, update their package statements, and verify Javadoc, comments, and `final` modifiers

## 2. Controller & Handler Updates

- [x] 2.1 Update import statements in `MailboxApiController.java` and `MailApiController.java` to reference `com.xceptance.xcmailr.dto.*`
- [x] 2.2 Update import statements in `RestValidationExceptionHandler.java` to reference `com.xceptance.xcmailr.dto.ApiError` and `com.xceptance.xcmailr.dto.ApiErrors`
- [x] 2.3 Verify `xcmailr-webapp` compiles successfully via `mvn test-compile -pl xcmailr-webapp`

## 3. Test Suite Updates & Regression Verification

- [x] 3.1 Update import statements in `MailboxApiControllerValidationTest.java`, `MailboxApiIT.java`, and `MailApiIT.java` to reference `com.xceptance.xcmailr.dto.*`
- [x] 3.2 Re-run REST API integration and validation test suites via `mvn test -pl xcmailr-webapp -Dtest=MailboxApiControllerValidationTest,MailboxApiIT,MailApiIT` and verify all tests pass
- [x] 3.3 Execute full reactor test suite via `mvn clean test` across all modules and confirm 0 failures and 0 regressions
