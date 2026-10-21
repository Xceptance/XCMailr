# Tasks

## 1. Controller & Hypermedia Trigger Updates

- [x] 1.1 In `MailboxWebController.java`, update `createMailbox` and `updateMailbox` to emit `.header("HX-Trigger", "mailboxChanged, closeModal")` and eliminate obsolete `HX-Trigger-After-Swap` headers. Verify by compiling the webapp module (`mvn test-compile -pl xcmailr-webapp`).
- [x] 1.2 Update unit tests in `MailboxWebControllerTest.java` (`testCreateMailbox_Success`, `testUpdateMailbox_Success`) to assert the new `HX-Trigger` header, and add unit test cases for creation validation failures and update non-existent mailboxes (verifying no `closeModal` trigger is emitted), enforcing `final` modifiers on all parameters and local variables. Verify by running `mvn test -pl xcmailr-webapp -Dtest=MailboxWebControllerTest`.
- [x] 1.3 Update the contract test in `HtmxCsrfClientContractTest.java` (`testContract_MailboxModalSubmissionCloseEvent`) to assert that modal submissions emit `HX-Trigger` containing `closeModal`, enforcing `final` modifiers on all variables. Verify by running `mvn test -pl xcmailr-webapp -Dtest=HtmxCsrfClientContractTest`.

## 2. Base Layout Client Script & Yearless Footer Updates

- [x] 2.1 In `templates/layout/base.html`, harden the `closeModal` event listener to use `bootstrap.Modal.getOrCreateInstance(modalEl).hide()`, ensuring modal dialogs, backdrops, and body classes are cleanly dismissed upon receiving the event. Verify through template review.
- [x] 2.2 In `templates/layout/base.html`, update the corporate footer paragraph to replace `&copy; 2013-2026 <a ...>...</a>` with the yearless copyright notice `&copy; <a ...>...</a>`, eliminating hardcoded year maintenance churn. Verify through template review.
- [x] 2.3 Create `BaseLayoutFooterContractTest.java` to test layout rendering across the testing pyramid:
  - Happy path: public (`/login`) and authenticated (`/mailboxes`) views render the yearless copyright notice referencing Xceptance without year numbers (`2013`, `2026`).
  - Error case: styled error views (e.g., 404) render the base layout with the yearless copyright notice.
  - Special case: base template markup contains the resilient `closeModal` event listener binding to `#actionModal`.
  Enforce strict `final` modifiers on all class members, parameters, and local variables. Verify by running `mvn test -pl xcmailr-webapp -Dtest=BaseLayoutFooterContractTest`.

## 3. End-to-End Test Suite Verification

- [x] 3.1 Execute the complete test suite across all modules with `mvn clean test` to confirm zero regressions and verify all tests pass cleanly.

