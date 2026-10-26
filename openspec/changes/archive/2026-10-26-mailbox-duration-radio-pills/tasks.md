# Tasks

## 1. Unit Testing for Duration Calculations and Presets (Testing Pyramid Layer 1)

- [x] 1.1 Add parameterized unit tests in `MailboxValidationUnitTest.java` for all duration presets (`1`, `2`, `24`, `168`, `720`, `0`), negative values, and 64-bit millisecond overflow protection using `final` modifiers and Javadoc comments, and verify with `mvn test -Dtest=MailboxValidationUnitTest`
- [x] 1.2 Verify unit test assertions for entity timestamp status conversion across happy paths (active future timestamps) and edge cases (expired, unlimited/zero), running `mvn test -Dtest=MailboxValidationUnitTest`

## 2. Controller State Preservation and Default Alignment (Testing Pyramid Layer 2)

- [x] 2.1 Update `MailboxWebController.java` to align `@RequestParam(value = "durationHours", defaultValue = "2")`, inject `selectedDuration` (defaulting to 2) and `forwardEmails` (defaulting to true) into the model in `newMailboxModal` and `listMailboxes`, and retain both parameters in the model across all validation error responses (`invalidAddress`, `invalidDomain`, `mailboxExists`), using `final` keywords and comprehensive Javadoc comments
- [x] 2.2 Add MockMvc tests in `MailboxWebControllerTest.java` covering happy path creation for preset durations `720` (30 days) and `0` (unlimited), verifying persisted `ts_active` timestamp offsets, and verify with `mvn test -Dtest=MailboxWebControllerTest`
- [x] 2.3 Add MockMvc tests in `MailboxWebControllerTest.java` verifying that validation error re-renders (`invalidAddress`, `invalidDomain`, duplicate address) preserve `selectedDuration` and `forwardEmails` in the returned model and HTML response, and verify with `mvn test -Dtest=MailboxWebControllerTest`
- [x] 2.4 Add MockMvc edge case tests in `MailboxWebControllerTest.java` verifying fallback behavior when `durationHours` is omitted, negative, or invalid, using `final` modifiers throughout, and verify with `mvn test -Dtest=MailboxWebControllerTest`

## 3. Segmented Radio Pill UI Template and Contract Tests (Testing Pyramid Layer 3)

- [x] 3.1 Replace `<select id="durationSelect" name="durationHours">` in `modal-new.html` with responsive segmented radio button pills (`btn-group`, `btn-check` inputs, and labels) for presets `1h`, `2h`, `1d`, `7d`, `30d`, `Unlimited` with dynamic `th:checked` evaluation bound to `selectedDuration`, and verify markup structure
- [x] 3.2 Create template contract test `MailboxCreationModalContractTest.java` with tests for happy path rendering (all 6 radio buttons present, 2h checked by default), custom model pre-selection (e.g. 30d / 720 checked), error re-render state retention, and unlimited duration styling, using `final` modifiers and Javadoc, and verify with `mvn test -Dtest=MailboxCreationModalContractTest`

## 4. Full Verification and Quality Gate

- [x] 4.1 Run the complete test suite across all web controllers, contract tests, and unit tests via `mvn clean test` to confirm zero regressions
- [x] 4.2 Verify Javadoc documentation, code comments, and universal `final` modifier compliance across all modified and newly created source and test files

