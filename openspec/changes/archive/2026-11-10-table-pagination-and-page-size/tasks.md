# Tasks

## 1. Core Pagination Utility & Unit Tests

- [x] 1.1 Implement `com.xceptance.xcmailr.util.PaginationHelper` with 7-slot sliding window sequence calculation (`getPageSequence`), size bounds clamping (`clampPageSize`), `final` modifiers, and exhaustive Javadoc; verify compilation with `./mvnw compile -DskipTests`.
- [x] 1.2 Implement comprehensive unit tests in `PaginationHelperTest` covering testing pyramid tiers: happy paths ($N \le 7$, Start, End, Middle), error and boundary cases ($N \le 0$, $P \le 0$, $P > N$), special transition cases ($N=8, 9, 100$), and size clamping; verify with `./mvnw test -Dtest=PaginationHelperTest`.

## 2. Localization Keys

- [x] 2.1 Add localized entries for `macro_EntriesPerPage` ("Display Count" / "Einträge/Seite") and `common.pagination.all` ("all" / "alle") to `xcmailr-webapp/src/main/resources/conf/messages.properties` and `conf/messages_de.properties`; verify keys are present and correctly formatted.

## 3. Web Controller Defenses & State Propagation

- [x] 3.1 Update `MailboxWebController.java` to clamp `size` defensively (mapping `size=0` to 1000 and clamping between 1 and 1000), apply `final` modifiers, update Javadoc, and propagate `size` into model attributes and sort handlers; verify compilation with `./mvnw test-compile`.
- [x] 3.2 Update `AdminWebController.java` (`/admin/users`, `/admin/transactions`) and `MailWebController.java` (`/mailboxes/{id}/mails`, `/mails`) with uniform size clamping, `final` modifiers, and model propagation; verify compilation with `./mvnw test-compile`.

## 4. Template Modernization across 5 Tabular Views

- [x] 4.1 Update `mailboxes/fragments/mailbox-table.html` to render the 7-slot pagination bar using `@paginationHelper.getPageSequence(...)` and the symmetrical segmented Display Count bar (`5`, `15`, `25`, `50`, `all`), preserving `size` in sort headers and `sort`/`dir`/`search`/`status` in pagination and size buttons; verify with `MailboxWebControllerTest`.
- [x] 4.2 Update `admin/fragments/user-table.html` and `admin/fragments/transaction-table.html` with the 7-slot pagination bar and Display Count selector; verify with `AdminWebControllerTest`.
- [x] 4.3 Update `mailboxes/fragments/mail-list.html` and `mailboxes/fragments/all-mail-list.html` with the 7-slot pagination bar and Display Count selector; verify with `MailWebControllerTest`.
- [x] 4.4 Update `mailboxes/index.html` and `admin/users.html` search inputs to include parameter preservation for `size`; verify table swaps retain active page size.

## 5. Web MVC Integration & Component Tests

- [x] 5.1 Add integration tests in `MailboxWebControllerTest` verifying 7-slot pagination rendering, ellipsis presence for large page counts, Display Count preset switching, single-page disabled controls visibility, and sorting/size state preservation; verify with `./mvnw test -Dtest=MailboxWebControllerTest`.
- [x] 5.2 Add integration tests in `AdminWebControllerTest` and `MailWebControllerTest` verifying pagination slot limits and size selector interactions for user audit, transaction logs, and inbox views; verify with `./mvnw test -Dtest="AdminWebControllerTest,MailWebControllerTest"`.

## 6. Security & SQL Injection Testing

- [x] 6.1 Implement `TableSecuritySqlInjectionTest` testing all 5 paginated endpoints against SQL injection payloads on `size`, `page`, `sort`, `dir`, and `search` query parameters, asserting HTTP 400 Bad Request on type mismatch and safe default fallback on sort column attacks; verify with `./mvnw test -Dtest=TableSecuritySqlInjectionTest`.
- [x] 6.2 Execute full regression build and verify all tests pass without errors; verify with `./mvnw verify`.
