# Tasks

## 1. Controller Implementation & Unit Testing

- [x] 1.1 Update `MailboxWebController.resolveSort` to construct case-insensitive `Sort.Order` for `address` and `domain` via `.ignoreCase()`, ensuring `final` keywords, updated Javadoc, and explanatory comments are present.
- [x] 1.2 Add Tier 1 unit tests in `MailboxWebControllerTest` verifying `resolveSort("address", "asc")` and `resolveSort("address", "desc")` generate `Sort` orders with `isIgnoreCase() == true` for both address and domain, and verify via `mvn test -pl xcmailr-webapp -Dtest=MailboxWebControllerTest`.

## 2. Web Slice, Integration Testing & Validation

- [x] 2.1 Update Tier 2 web slice tests in `MailboxSortingWebTest` to assert that address sorting requests (`sort=address`) preserve sort state and model attributes properly, verifying with `mvn test -pl xcmailr-webapp -Dtest=MailboxSortingWebTest`.
- [x] 2.2 Update Tier 3 integration test `testSorting_SpecialCase_CaseInsensitiveAddressOrdering` in `MailboxSortingIntegrationTest` with mixed-case addresses (including uppercase initial letters such as `UCoMf...` and `Alpha`) verifying natural alphabetical order in both ASC and DESC, verifying via `mvn test -pl xcmailr-webapp -Dtest=MailboxSortingIntegrationTest`.
- [x] 2.3 Execute the complete mailbox test suite (`mvn test -pl xcmailr-webapp -Dtest="Mailbox*"`) and validate specs via `openspec validate --specs` to confirm clean test passes and spec integrity.

