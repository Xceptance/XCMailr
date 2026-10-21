# Proposal

## Why

During the modernization migration from the legacy Ninja Web Framework to Spring Boot, HTMX, and Thymeleaf, bulk mailbox operations (bulk enable, disable, extend validity, reset counters, delete, and text export) were temporarily omitted from the web console. Furthermore, the desktop mailbox table rows currently display wide text buttons (`[Disable] [+24h] [v]`) that consume excessive horizontal space. Restoring full legacy parity with a sleek contextual floating batch bar and transitioning row actions to a compact, icon-based hybrid control (`(Toggle) (+1d) [...]`) provides modern UX while ensuring zero lost capabilities.

## What Changes

- **Contextual Floating Batch Bar**:
  - Add master and row checkboxes to the mailbox overview table.
  - Render a modern floating bottom pill/bar whenever one or more mailboxes are selected, indicating the selection count with a clear button.
  - Provide bulk operations from the batch bar:
    - **Bulk Enable**: Activates selected mailboxes via `POST /mailboxes/bulk/enable`.
    - **Bulk Disable**: Deactivates selected mailboxes via `POST /mailboxes/bulk/disable`.
    - **Bulk Extend**: Extends expiration for selected mailboxes (e.g. +24 hours) via `POST /mailboxes/bulk/extend`.
    - **Bulk Reset Counters**: Clears forwards and suppressions counters via `POST /mailboxes/bulk/reset-counters`.
    - **Bulk Delete**: Prompts for confirmation and deletes selected mailboxes and stored messages via `DELETE /mailboxes/bulk`.
    - **Bulk Text Export**: Streams selected mailbox addresses as plain text via `POST /mailboxes/bulk/export` (and legacy-compatible text endpoints for all / active addresses).
- **Compact Icon-Based Hybrid Row Actions**:
  - Replace desktop text buttons (`[Disable] [+24h]`) with compact icon/badge buttons (`(Power/Play)` toggle and `(+1d)` extension) paired with a `[...]` kebab dropdown menu for secondary actions (`Inbox Mails`, `Edit Settings`, `Reset Counters`, and `Delete Mailbox`).
  - Preserve responsive mobile behavior where quick actions collapse into the kebab menu on viewports narrower than 768px.
- **Backend Batch Endpoints & Tenant Isolation**:
  - Implement secure Spring MVC batch endpoints in `MailboxWebController` ensuring strict tenant isolation (validating all submitted IDs against the authenticated user).
  - Add corresponding JPA batch update/query methods in `MailboxRepository` where applicable.
- **Automated Test Pyramid**:
  - Unit tests for repository batch updates and controller logic.
  - Integration and WebMvc tests verifying happy path, error handling (empty selection, cross-tenant ID injection, invalid IDs), and special/edge cases (mixed active/expired boxes, pagination interactions).

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `web-console-ui`: Enhance the `Dynamic Mailbox Dashboard` requirement with concrete specifications for the contextual floating batch bar, bulk operations, plain text address exports, and compact icon-based hybrid row actions.

## Impact

- **Affected Controller**: `MailboxWebController` in `xcmailr-webapp`.
- **Affected Repository**: `MailboxRepository` in `xcmailr-webapp`.
- **Affected Templates**: `xcmailr-webapp/src/main/resources/templates/mailboxes/index.html` and `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/mailbox-table.html`.
- **Affected Styles / Static Assets**: CSS styling for the floating batch bar in `xceptance-theme.css` or component stylesheet.
- **Tests**: New controller integration tests and unit tests in `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailboxWebControllerTest.java` (or dedicated `MailboxBulkActionsTest.java`).

