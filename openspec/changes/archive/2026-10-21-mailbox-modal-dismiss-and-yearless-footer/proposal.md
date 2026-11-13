# Proposal

## Why

When creating or editing a mailbox on the web console dashboard, the action succeeds and the table refreshes, but the Bootstrap modal backdrop remains stuck on screen (causing a permanent grey overlay until the user manually clicks outside). This regression occurred during the migration to HTMX 4, where the legacy `HX-Trigger-After-Swap` response header was deprecated and removed in favor of standard post-swap event dispatching via `HX-Trigger`. In addition, the corporate footer template hardcodes a copyright year range (`2013-2026`), introducing annual maintenance overhead and diverging from the project's yearless copyright policy established across all source and license files.

Resolving both issues restores smooth, frictionless modal workflows on the dashboard and standardizes the web console layout on a maintenance-free yearless corporate footer.

## What Changes

- **Update Mailbox Controller Trigger Headers**: Replace legacy `HX-Trigger-After-Swap: closeModal` headers in `MailboxWebController` (`createMailbox` and `updateMailbox`) with unified HTMX 4 `HX-Trigger: mailboxChanged, closeModal` response headers.
- **Harden Client Modal Dismissal Listener**: Refactor the `closeModal` client event listener in `templates/layout/base.html` to use `bootstrap.Modal.getOrCreateInstance(modalEl).hide()`, ensuring the Bootstrap modal instance is reliably resolved, the dialog dismissed, the backdrop element (`.modal-backdrop`) cleaned up, and the `modal-open` class removed from the body.
- **Yearless Corporate Footer**: Eliminate the hardcoded year range `2013-2026` from `templates/layout/base.html`, rendering a timeless corporate copyright notice (`&copy; <a href="https://www.xceptance.com" target="_blank">Xceptance Software Technologies GmbH</a>. All rights reserved.`) aligned with the project's copyright standard.
- **Comprehensive Testing Pyramid**: Update existing contract and controller tests (`MailboxWebControllerTest`, `HtmxCsrfClientContractTest`) to assert the new `HX-Trigger` header values, and add test coverage across happy paths, validation error cases, and layout rendering contract tests verifying the yearless footer and modal dismiss mechanics. All Java code adheres to strict `final` modifier usage.

## Capabilities

### Modified Capabilities
- `web-console-ui`: Update the `Dynamic Mailbox Dashboard` requirement to explicitly specify modal dismissal, backdrop removal, and screen restoration upon successful mailbox creation or modification.

### New Capabilities
- None. (The yearless corporate footer notice is incorporated as an added requirement within `web-console-ui`).

## Impact

- **Backend**: `MailboxWebController.java` (`createMailbox` and `updateMailbox` methods).
- **Frontend / Templates**: `templates/layout/base.html` (modal dismissal event handler script and footer copyright paragraph).
- **Automated Tests**: `MailboxWebControllerTest.java`, `HtmxCsrfClientContractTest.java`, and layout/dashboard integration tests.
- **APIs / Contracts**: HTMX response headers emit comma-delimited `HX-Trigger: mailboxChanged, closeModal` instead of deprecated `HX-Trigger-After-Swap`.

