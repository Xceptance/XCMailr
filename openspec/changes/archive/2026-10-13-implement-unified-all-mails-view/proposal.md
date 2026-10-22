# Proposal: Implement Responsive Unified All Mails View

## Why

When users click the "All Mails" navigation link (`/mails`), the browser renders a completely blank white screen. 

### Why Was This Not Found by Automated Tests? (Test Gap Analysis)
An investigation of the test suite revealed an automated test gap in [`MailWebControllerTest.java`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailWebControllerTest.java):
1. **Shallow View and Model Assertions**: The existing tests `testListAllMailsFullPage` and `testListAllMailsFragment` only assert `status().isOk()`, `view().name("mailboxes/all-mails")`, and `model().attributeExists(...)`.
2. **False Confidence from 0-Byte Templates**: Because `mailboxes/all-mails.html` and `mailboxes/fragments/all-mail-list.html` existed on the filesystem as 0-byte placeholder files, Spring MVC and Thymeleaf resolved the view name without error, executed a blank template, and returned HTTP 200 with an empty body (`""`).
3. **Absence of Rendered Content & DOM Assertions**: The tests never asserted non-empty content (`content().string(not(emptyOrNullString()))`) or the presence of actual rendered DOM elements (e.g. table elements, headers, email rows, recipient badges, preview pane).
4. **Missing Test Pyramid Coverage**: The controller test suite lacked tests for happy paths with content verification, empty inbox states, multi-mailbox badge mapping, error cases, security/isolation boundaries (unauthenticated redirection, cross-user privacy), and responsive mobile layout structure.

Restoring this core capability and plugging the test gap ensures a resilient, fully tested, responsive inbox across all user temporary email addresses.

## What Changes

- **Templates**:
  - Implement `mailboxes/all-mails.html` extending `layout/base.html` with responsive layout, breadcrumb navigation, email counter badges, inbox refresh trigger, and message detail viewing pane (`#mail-viewer-pane`).
  - Implement `mailboxes/fragments/all-mail-list.html` rendering all received emails across user-owned mailboxes, displaying timestamp, sender, subject, and the recipient mailbox address badge (`mailboxAddressMap`).
- **Responsive Mobile UX (< 768px)**:
  - Stack the message list and message preview vertically rather than side-by-side (`col-12` on small screens, `col-lg-5`/`col-lg-7` on desktop).
  - Collapse secondary columns into readable metadata sublines to avoid horizontal overflow and horizontal scrolling.
  - Ensure action buttons and email list items meet touch-target sizing standards (`>= 44px`).
- **Test Pyramid Expansion in [`MailWebControllerTest.java`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailWebControllerTest.java)**:
  - **Happy Path Tests**:
    - Full-page GET `/mails`: Verify HTTP 200, non-empty body, page title/breadcrumbs, email rows, sender, subject, and recipient mailbox badges.
    - HTMX Partial GET `/mails` (`HX-Request: true`): Verify returns only `fragments/all-mail-list` with rendered rows and no outer layout wrappers.
    - Cross-Mailbox Rendering: Verify emails from distinct mailboxes render their respective recipient address badges correctly.
  - **Edge & Special Cases**:
    - Empty Inbox State: Verify user with zero emails sees an informative empty state message rather than a blank page or broken table.
    - Missing / Orphaned Mailbox Fallback: Verify null-safe fallback (e.g. `Unknown Mailbox`) if a message refers to a mailbox missing from `mailboxAddressMap`.
    - Responsive Grid Markup: Assert rendered HTML contains responsive grid classes (`col-12`, `col-lg-5`, `col-lg-7`) and accessible touch styling.
  - **Error & Security Cases**:
    - Unauthenticated Access: Verify anonymous requests to `/mails` receive HTTP 302 Redirect to `/login`.
    - Cross-User Isolation: Verify User A cannot see or leak emails belonging to User B's mailboxes when browsing `/mails`.
    - Method Constraints: Verify disallowed HTTP methods (POST, PUT, DELETE) on `/mails` are rejected.

## Capabilities

### Modified Capabilities

- `web-console-ui`: Add requirement and scenarios for the unified cross-mailbox inbox view, including responsive mobile layout adaptation, HTMX partial updates, empty state handling, and security boundaries.

## Impact

- **Templates**: Replaces empty files `xcmailr-webapp/src/main/resources/templates/mailboxes/all-mails.html` and `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/all-mail-list.html`.
- **Controllers & Models**: Reuses existing `MailWebController.java` endpoints and model attributes (`mails`, `mailboxAddressMap`, `dateFormatter`).
- **Tests**: Drastically strengthens `MailWebControllerTest.java` across the entire test pyramid (happy path, edge cases, error cases, security isolation, and content assertions).
- **Backwards Compatibility**: Fully backward-compatible; no database schema or API alterations required.
