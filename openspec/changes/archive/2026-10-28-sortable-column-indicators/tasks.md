# Tasks

## 1. CSS & Theme Styling

- [x] 1.1 Add `.sortable-header` and `.sort-icon` styling rules in `xcmailr-webapp/src/main/resources/static/css/xceptance-theme.css` with hover transitions, pointer cursor, and dark mode token compatibility, verifying rules parse without syntax warnings.

## 2. Table Header Template Modernization & Responsiveness

- [x] 2.1 Update sortable column headers (`Address`, `Status`, `Expires`, `Forwards`, `Suppressed`) in `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/mailbox-table.html` with `.sortable-header`, `text-body`, `d-inline-flex`, `align-items-center`, `gap-1`, `text-nowrap`, and `py-1` classes to prevent wrapping and ensure touch targets on small mobile viewports.
- [x] 2.2 Replace classic arrow icons in `mailbox-table.html` with modern Bootstrap chevrons: render `bi-chevron-expand` (`sort-icon text-body-secondary opacity-50 ms-1`) on inactive columns, `bi-chevron-up` (`sort-icon text-primary ms-1`) on active ascending columns, and `bi-chevron-down` (`sort-icon text-primary ms-1`) on active descending columns, verifying Thymeleaf condition expressions.

## 3. Automated Pyramid Test Coverage

- [x] 3.1 Add happy path tests to `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailboxSortingWebTest.java` verifying that neutral `bi-chevron-expand` icons are rendered across sortable headers when default creation order (`id DESC`) is active.
- [x] 3.2 Add active sort direction tests to `MailboxSortingWebTest.java` verifying that ascending sort renders `bi-chevron-up` and descending sort renders `bi-chevron-down` on the active column while remaining columns retain `bi-chevron-expand`.
- [x] 3.3 Add responsive and accessibility assertions to `MailboxSortingWebTest.java` verifying that sortable headers contain `text-nowrap`, `d-inline-flex`, `py-1`, and `aria-hidden="true"` attributes.
- [x] 3.4 Add fallback and edge case tests to `MailboxSortingWebTest.java` verifying that invalid sort column names or malformed direction values default safely to neutral chevrons without template exceptions.
- [x] 3.5 Ensure test classes adhere strictly to code standards: apply `final` modifiers wherever possible, add thorough Javadoc explaining test scenarios, and add explanatory code comments.

## 4. Integration Verification & Build

- [x] 4.1 Run the full mailbox test suite with `mvn test -pl xcmailr-webapp -Dtest="*Mailbox*"` and verify all unit and web slice tests pass with zero failures and zero errors.

