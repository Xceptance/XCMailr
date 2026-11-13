# Tasks: Mailbox Row Action Binding Integrity Verification (Issue #83)

## 1. WebMvc Slice Tests (MailboxSortingWebTest)

- [x] 1.1 Implement descending sort row action binding test in `MailboxSortingWebTest.java` verifying that sorting by address DESC renders `zebra` in row 1 with actions strictly bound to `zebra.getId()` (and no references to `alpha.getId()`) and `alpha` in row 2 bound to `alpha.getId()`, verifying via `mvn test -pl xcmailr-webapp -Dtest=MailboxSortingWebTest`.
- [x] 1.2 Implement ascending sort row action binding test in `MailboxSortingWebTest.java` verifying that sorting by address ASC renders `alpha` in row 1 with actions strictly bound to `alpha.getId()` (and no references to `zebra.getId()`) and `zebra` in row 2 bound to `zebra.getId()`, verifying via `mvn test -pl xcmailr-webapp -Dtest=MailboxSortingWebTest`.

## 2. Integration & Management Operation Tests (MailboxSortingIntegrationTest)

- [x] 2.1 Implement end-to-end management operation delete safety test in `MailboxSortingIntegrationTest.java` verifying that requesting a sorted mailbox view and executing a deletion using the top row's mailbox ID deletes only that mailbox from the database while retaining other mailboxes intact, verifying via `mvn test -pl xcmailr-webapp -Dtest=MailboxSortingIntegrationTest`.

## 3. Verification & Code Quality

- [x] 3.1 Run full regression test suite across `MailboxSortingWebTest` and `MailboxSortingIntegrationTest` via `mvn test -pl xcmailr-webapp -Dtest=MailboxSorting*` to ensure all tests pass cleanly.
- [x] 3.2 Run checkstyle and license verification on modified test files via `mvn checkstyle:check license:check -pl xcmailr-webapp` to ensure compliance with project formatting and license standards.

