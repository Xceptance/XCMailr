# Tasks

## 1. Backend Controller Refinements: Content-Disposition Attachment Headers

- [x] 1.1 Refactor `renderAddressTextList` in `MailboxWebController.java` to set `Content-Disposition: attachment; filename="..."` with descriptive filenames (`mailboxes.txt`, `mailboxes-active.txt`, `mailboxes-selected.txt`), enforcing `final` on all parameters and local variables.
- [x] 1.2 Verify comprehensive Javadoc and yearless copyright headers on all modified controller methods.

## 2. Template Refinements: Native Tooltips & Header/Footer Layout

- [x] 2.1 Remove `data-bs-toggle="tooltip"` attributes from row action buttons in `mailbox-table.html` while preserving semantic `title` and `aria-label` attributes to ensure instantaneous tooltip dismiss on click without DOM orphaning.
- [x] 2.2 Remove the prominent `Export ▾` dropdown button from the dashboard header in `index.html`, keeping the top header focused exclusively on `+ New Mailbox`.
- [x] 2.3 Add subtle secondary text export links (`All (.txt)` and `Active (.txt)`) within the table footer/pagination area in `mailbox-table.html`.
- [x] 2.4 Clean up redundant tooltip initialization loops from `index.html`'s `htmx:afterSwap` listener.

## 3. Testing Pyramid: Happy Paths, Error Cases, and Special Cases

- [x] 3.1 Implement unit and controller tests in `MailboxExportWebTest.java` covering Happy Path scenarios:
  - Export all mailboxes (`/mailboxes/export/all`) returns 200 OK, `Content-Disposition: attachment; filename="mailboxes.txt"`, and complete list of addresses.
  - Export active mailboxes (`/mailboxes/export/active`) returns 200 OK, `Content-Disposition: attachment; filename="mailboxes-active.txt"`, and only active addresses.
  - Bulk export selected mailboxes (`/mailboxes/bulk/export`) returns 200 OK, `Content-Disposition: attachment; filename="mailboxes-selected.txt"`, and only selected addresses.
- [x] 3.2 Implement Error Cases tests in `MailboxExportWebTest.java`:
  - Unauthenticated requests to export endpoints are rejected with 302 redirect to login or 401/403.
  - Cross-tenant requests attempting to export mailbox IDs belonging to another user are filtered out, returning only addresses owned by the authenticated user.
  - Malformed or blank payloads are handled gracefully without 500 exceptions.
- [x] 3.3 Implement Special Cases tests in `MailboxExportWebTest.java`:
  - Export with 0 user mailboxes returns 200 OK with empty body and attachment header.
  - Export active with only expired mailboxes returns 200 OK with empty body and attachment header.
  - Preservation of address formatting with tags, subdomains, and mixed-case local parts.
  - Empty selection on bulk export returns 200 OK with empty body.
- [x] 3.4 Implement Template Integration tests in `MailboxExportWebTest.java`:
  - Mailboxes dashboard header renders `+ New Mailbox` without `Export ▾` dropdown button.
  - Mailbox table footer renders `All (.txt)` and `Active (.txt)` utility links.
  - Mailbox row action buttons render `title` and `aria-label` without `data-bs-toggle="tooltip"`.
- [x] 3.5 Update existing assertions in `MailboxBulkActionsWebTest.java` to verify attachment headers and footer export links.
- [x] 3.6 Run the complete Maven test suite (`mvn test`) and verify 100% green test passes with zero regressions.

