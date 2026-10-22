# Proposal

## Why

When creating a mailbox from the web dashboard, clicking the "New Mailbox" button causes the browser screen to dim with a dark backdrop while failing to display the creation form or modal dialog. This is caused by an architectural placement bug where the modal host is trapped inside a CSS stacking context within `<main>`, coupled with an invalid intermediate wrapper `div` and an empty-body HTMX response that erases the mailbox table upon submission.

Additionally, developers currently lack an integrated local testing recipe and configuration profile to test both inbound receiving (port 25000) and outbound forwarding without sending real emails or configuring complex external mail relays, and need a unified developer configuration profile (`application-dev.yml`) demonstrating local development (via Mailpit) and production (via Google Workspace).

## What Changes

- **Modal Host Relocation**: Move `#actionModal` from `templates/mailboxes/index.html` to `templates/layout/base.html` directly before `</body>` to prevent layout stacking context traps with `.modal-backdrop`.
- **Modal DOM Alignment**: Restructure the modal host to adhere to Bootstrap 5 standards (`.modal > .modal-dialog > .modal-content`), eliminating arbitrary intermediate wrappers and ensuring proper centering, transitions, and accessibility.
- **Robust HTMX Dialog & Submission Lifecycle**: Update `templates/mailboxes/fragments/modal-new.html` (and `modal-edit.html`) and `MailboxWebController` so that form submissions gracefully handle validation errors, display feedback, and refresh the mailbox table without wiping `#mailbox-table-container` from the DOM.
- **Unified Developer Configuration (`application-dev.yml`)**: Introduce a dedicated `application-dev.yml` Spring profile pre-configured for local developer testing with Dockerized Mailpit (outbound SMTP on `localhost:1025` without auth or TLS, verbose developer logging, and persistent dev DB), alongside complete developer documentation detailing the local round-trip delivery test flow and production Google Workspace relay settings.

## Capabilities

### Modified Capabilities
- `web-console-ui`: Refine the `Dynamic Mailbox Dashboard` requirement to explicitly govern modal host positioning, stacking-context isolation, robust modal form rendering, and smooth table refresh behavior after mailbox creation/editing.

## Impact

- **Affected Code**:
  - `templates/layout/base.html`: Host `#actionModal` directly under `<body>`.
  - `templates/mailboxes/index.html`: Remove redundant page-level modal declaration and update modal trigger bindings.
  - `templates/mailboxes/fragments/modal-new.html` & `modal-edit.html`: Return clean `.modal-content` fragments with proper HTMX targets and error handling.
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/MailboxWebController.java`: Adjust HTMX modal responses and table refreshes.
  - `xcmailr-webapp/src/main/resources/application-dev.yml`: Unified Spring profile for local development with Mailpit.
  - `docs/local-email-testing.md` (and reference in `README.md`): Step-by-step local testing walkthrough with Docker Mailpit, port 25000 injection, and production Google Workspace configuration.
- **Dependencies**: No new external dependencies required; utilizes existing Bootstrap 5.3.3 and HTMX 2.0.4 WebJars.

