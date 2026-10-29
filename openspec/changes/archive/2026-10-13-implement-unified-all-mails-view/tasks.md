# Tasks

## 1. Responsive Template Implementation

- [x] 1.1 Implement `mailboxes/all-mails.html` extending `layout/base.html` with responsive grid layout (`col-lg-5` / `col-lg-7` desktop split-pane, `col-12` mobile stacked view), breadcrumb navigation, refresh button with HTMX trigger, and `#mail-viewer-pane`, and verify the template parses cleanly.
- [x] 1.2 Implement `mailboxes/fragments/all-mail-list.html` rendering cross-mailbox message items, recipient address badges with null safety (`${mailboxAddressMap[m.mailbox.id] ?: 'Unknown Mailbox'}`), touch-friendly clickable rows (`>= 44px`), mobile-responsive metadata sublines, and empty-state notice, and verify fragment structure.

## 2. Controller Slice Tests - Happy Path Coverage

- [x] 2.1 Enhance `MailWebControllerTest.java` with happy path full-page test asserting HTTP 200, non-empty HTML body (`not(emptyOrNullString())`), page title/breadcrumbs, sender, subject, recipient badge, and `#mail-viewer-pane` container via MockMvc.
- [x] 2.2 Enhance `MailWebControllerTest.java` with HTMX partial test (`HX-Request: true`) asserting that only `mailboxes/fragments/all-mail-list` is returned with email rows and without outer page layout tags (`<html>`, `<nav>`).
- [x] 2.3 Add test in `MailWebControllerTest.java` verifying multi-mailbox badge disambiguation when a user has received emails across multiple distinct mailboxes.

## 3. Controller Slice Tests - Edge, Error & Security Cases

- [x] 3.1 Add test in `MailWebControllerTest.java` for empty inbox state, verifying a user with zero emails receives an informative empty-state notice and no table breakdown.
- [x] 3.2 Add test in `MailWebControllerTest.java` verifying null-safe fallback (`Unknown Mailbox`) when an email references a mailbox ID missing from `mailboxAddressMap`.
- [x] 3.3 Add security tests in `MailWebControllerTest.java` verifying unauthenticated access redirects to `/login` (HTTP 302) and verifying cross-user tenant isolation (User A cannot observe User B's emails).
- [x] 3.4 Add test in `MailWebControllerTest.java` verifying unsupported HTTP methods (POST, PUT, DELETE) to `/mails` return HTTP 405 Method Not Allowed.

## 4. Responsive Verification & Test Suite Execution

- [x] 4.1 Add test in `MailWebControllerTest.java` asserting rendered HTML contains responsive grid classes (`col-12`, `col-lg-5`, `col-lg-7`) and touch-target padding.
- [x] 4.2 Execute test suite `mvn test -pl xcmailr-webapp -Dtest=MailWebControllerTest,WebNavigationSecurityTest` and verify all tests pass.
