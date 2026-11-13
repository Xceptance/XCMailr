# Tasks

## 1. Backend Batch Operations and Repository Layer

- [x] 1.1 Add batch operations in `MailboxRepository` (or helper methods) for querying user-owned mailboxes by ID list and performing batch counter resets, with full Javadoc and `final` qualifiers. Verify with unit tests.
- [x] 1.2 Implement repository unit tests covering batch ID lookups, tenant user boundaries, and counter updates across happy paths, empty lists, and boundary conditions. Verify with `mvn test -pl xcmailr-webapp -Dtest=MailboxRepositoryTest`.

## 2. Spring MVC Batch and Export Endpoints

- [x] 2.1 Implement bulk management endpoints in `MailboxWebController` (`POST /mailboxes/bulk/enable`, `POST /mailboxes/bulk/disable`, `POST /mailboxes/bulk/extend`, `POST /mailboxes/bulk/reset-counters`, and `DELETE /mailboxes/bulk`) with strict tenant isolation, Javadoc, and `final` qualifiers.
- [x] 2.2 Implement plain text address export endpoints in `MailboxWebController` (`POST /mailboxes/bulk/export`, `GET /mailboxes/export/all`, `GET /mailboxes/export/active`) and legacy URL routes (`/mail/mymaillist.txt`, `/mail/myactivemaillist.txt`, `/mail/myselectedmaillist.txt`).
- [x] 2.3 Implement comprehensive WebMvc controller tests in `MailboxBulkActionsWebTest` covering the full testing pyramid: happy paths (enable, disable, extend, reset, delete, export), error cases (empty selection, invalid input, unauthenticated requests), and security cases (cross-tenant ID tampering / IDOR protection). Verify with `mvn test -pl xcmailr-webapp -Dtest=MailboxBulkActionsWebTest`.

## 3. Web UI: Compact Icon Row Actions & Contextual Floating Batch Bar

- [x] 3.1 Update `mailbox-table.html` row actions to use compact icon buttons `(Toggle) (+1d) [...]` with tooltips on desktop and responsive collapse to kebab dropdown on mobile viewports (< 768px).
- [x] 3.2 Add table header master checkbox (`#selectAllMailboxes`) and per-row checkboxes (`input.mailbox-select`) in `mailbox-table.html`.
- [x] 3.3 Add the contextual floating bottom batch bar in `index.html` / `mailbox-table.html` featuring selection count, deselect button, and HTMX-enabled bulk action triggers (`Enable`, `Disable`, `+24h`, `Reset`, `Delete`, `Export`).
- [x] 3.4 Implement lightweight client-side selection synchronization script supporting master checkbox toggle, indeterminate state, selection counter updates, and floating bar visibility.
- [x] 3.5 Add styling for the floating bottom pill in `xceptance-theme.css` ensuring clean elevation, border contrast, and accessibility in both light and dark themes.

## 4. End-to-End Verification and Full Test Suite Execution

- [x] 4.1 Implement HTML fragment assertions in WebMvc tests verifying that table rows render the compact icon controls, row checkboxes, and floating batch bar elements.
- [x] 4.2 Run the full test suite across `xcmailr-webapp` to ensure zero regressions across existing web console, security, and API capabilities. Verify with `mvn test`.

