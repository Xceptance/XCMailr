# Tasks

## 1. Backend Controller Parameter Fix & Timestamp Formatting

- [x] 1.1 In `MailboxWebController.java`, change `createMailbox` parameter to `@RequestParam(value = "forwardEmails", defaultValue = "false") final boolean forwardEmails` so omitted form checkboxes evaluate to `false`.
- [x] 1.2 Expose an ISO-8601 UTC timestamp formatter (`DateTimeFormatter.ISO_INSTANT` or helper) to model attributes in `MailboxWebController`, `MailWebController`, and `AdminWebController` for HTML5 `<time>` element attributes.
- [x] 1.3 Add comprehensive unit tests in `MailboxValidationUnitTest.java` adhering to the testing pyramid:
  - Happy path tests: Expiration calculation for standard durations (1h, 2h, 24h, 168h, 720h) and explicit `forwardEmails` booleans.
  - Error and edge cases: Expiration calculation with duration <= 0 yielding unlimited (0L), boundary duration values.
  - Special cases: Parameter resolution when `forwardEmails` is omitted defaulting to `false`, verifying strict use of `final` keywords on all parameters and variables.

## 2. Web Integration Tests for Mailbox Forwarding Persistence

- [x] 2.1 Add integration tests in `MailboxWebControllerTest.java` verifying mailbox creation with forwarding flags:
  - Happy path: POST `/mailboxes` with `forwardEmails=true` creates and persists a mailbox with `forwardEmails=true`.
  - Happy path: POST `/mailboxes` without `forwardEmails` parameter creates and persists a mailbox with `forwardEmails=false`.
  - Special case: POST `/mailboxes` with explicit `forwardEmails=false` persists with `forwardEmails=false`.
  - Error cases: Mailbox creation with duplicate address, invalid characters, or unconfigured domain fails cleanly with validation feedback, preserving `final` modifiers throughout.

## 3. Frontend HTML5 `<time>` Element Integration & Browser Localization

- [x] 3.1 Update `mailbox-table.html` (desktop expiration column and mobile subline) and `modal-edit.html` to render timestamps using `<time class="local-time" th:datetime="${...}" th:text="${...}">` with fallback UTC text, preserving "Unlimited" for `ts_Active == 0`.
- [x] 3.2 Update mail inspection fragments (`mail-list.html`, `all-mail-list.html`, `mail-detail.html`, `mails.html`) and admin `transaction-table.html` to adopt standard `<time class="local-time" ...>` elements.
- [x] 3.3 Add vanilla JavaScript localization routine in `base.html` hooked to `htmx.onLoad` using `Intl.DateTimeFormat` to format all `<time class="local-time">` elements into the client's local system timezone on initial page load and after HTMX swaps.
- [x] 3.4 Create template contract tests (`MailboxLocalTimestampContractTest.java`) adhering to the testing pyramid:
  - Happy path: Verify rendered mailbox table includes `<time class="local-time">` tags containing valid ISO-8601 UTC timestamps in the `datetime` attribute.
  - Special cases: Verify unlimited expiration (`ts_Active == 0`) renders "Unlimited" text and suppresses `<time>` formatting.
  - Fallback / error cases: Verify server-rendered text content serves as a valid readable UTC fallback, using `final` modifiers on all variables and fixtures.

## 4. Verification & Testing Pyramid Validation

- [x] 4.1 Run the full Maven test suite (`mvn clean test`) to confirm that all unit, web integration, and template contract tests pass cleanly.
- [x] 4.2 Verify that all newly added and modified Java code adheres strictly to the convention of using `final` modifiers wherever possible.

