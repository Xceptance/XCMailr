# Proposal

## Why

When users create a temporary mailbox with email forwarding disabled, the application ignores this choice and creates the mailbox with forwarding enabled because `MailboxWebController.createMailbox` specifies `@RequestParam(value = "forwardEmails", defaultValue = "true")`, and standard HTML forms omit unchecked checkboxes from the submission payload.

Additionally, expiration and activity dates are rendered server-side as bare strings (`yyyy-MM-dd HH:mm`) formatted in the server JVM's timezone (UTC in Docker/containerized environments). When a user in a non-UTC timezone (such as CEST, UTC+2) creates a mailbox with a 2-hour validity duration, the resulting UTC expiration timestamp (`14:13`) matches the user's local wall clock (`14:13 CEST`), creating the misleading impression that the mailbox expired immediately upon creation.

## What Changes

- Fix `MailboxWebController.createMailbox` by setting `defaultValue = "false"` for `forwardEmails`, ensuring that unchecked forwarding toggles cleanly evaluate to `false` (matching `updateMailbox`).
- Standardize all user-facing timestamp displays using HTML5 `<time class="local-time" th:datetime="${isoUtc}" th:text="${formattedUtc}">` elements across the mailbox table, modals, and mail inspection views.
- Provide ISO-8601 UTC timestamp formatting in controllers and models to populate the machine-readable `datetime` attribute on `<time>` tags.
- Add a lightweight, vanilla client script integrated into `htmx.onLoad` in `base.html` that uses the native browser `Intl.DateTimeFormat` API to format timestamps into the client's local system timezone and locale, without manual offset math or risk of DST calculation errors.
- Supply a comprehensive test suite adhering to the testing pyramid: unit tests for controller request parameter evaluation and duration math (happy path, error cases, special cases), integration tests verifying mailbox creation persistence with forwarding flags, and template contract tests verifying `<time>` ISO attributes, using `final` modifiers on all variables and parameters.

## Capabilities

### Modified Capabilities

- `web-console-ui`: Modifies requirements under `Dynamic Mailbox Dashboard` to ensure the creation modal honors the unchecked forwarding switch state and that expiration and activity timestamps render as localized times matching the user's browser clock via standard `<time>` elements and `Intl.DateTimeFormat`.

## Impact

- **Backend**:
  - `MailboxWebController.java`: Update `@RequestParam(value = "forwardEmails", defaultValue = "false")` in `createMailbox`. Expose ISO-8601 UTC formatter or ISO timestamp string on model attributes.
  - Ensure all controllers exposing date timestamps (`MailboxWebController`, `MailWebController`, `AdminWebController`) provide ISO-formatted datetime values for HTML templates.
- **Frontend Templates**:
  - `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/mailbox-table.html`
  - `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/modal-edit.html`
  - `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/mail-list.html`
  - `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/all-mail-list.html`
  - `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/mail-detail.html`
  - `xcmailr-webapp/src/main/resources/templates/admin/fragments/transaction-table.html`
  - `xcmailr-webapp/src/main/resources/templates/layouts/base.html`: Include vanilla `Intl.DateTimeFormat` script attached to `htmx.onLoad`.
- **Tests**:
  - `MailboxWebControllerTest.java`, `MailboxValidationUnitTest.java`, and template contract tests.

