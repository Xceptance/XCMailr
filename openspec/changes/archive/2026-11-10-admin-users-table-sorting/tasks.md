# Tasks: Admin Users Table Column Sorting

## 1. Backend Repository and Controller Implementation

- [x] 1.1 Update `UserRepository.java` to remove the hardcoded `ORDER BY u.mail ASC` clause from `searchUsers` `@Query` to allow dynamic sorting via `Pageable.getSort()`, verify repository compilation, and document method behavior with Javadoc.
- [x] 1.2 Implement static whitelist helper methods `resolveSortField`, `resolveSortDirection`, and `resolveSort` in `AdminWebController.java` using `final` on all declarations, parameters, and local variables, complete with full Javadoc and inline comments detailing whitelist mapping (`email`, `name` composite `surname, forename` case-insensitive, `status`, `role`), secondary tiebreaker `mail ASC` (`ignoreCase()`), and defensive fallback.
- [x] 1.3 Update `AdminWebController.listUsers` to accept `sort` (default `"email"`) and `dir` (default `"asc"`) parameters with `final` modifiers, construct the `Sort` instance via `resolveSort`, invoke `userRepository.findAll(pageable)` when no search query is present, populate `sort` and `dir` into the Thymeleaf model, and verify controller compilation.
- [x] 1.4 Add unit tests in `AdminWebControllerTest.java` covering Layer 1 of the testing pyramid: happy path whitelisted columns (`email`, `name`, `status`, `role`) and directions (`asc`, `desc`); error cases (null, empty, invalid column name, invalid direction); and special cases (case-insensitivity of parameter strings like `"NAME"`, `"DESC"` and secondary tiebreaker structure), with `final` variables, Javadoc, and assertions.

## 2. UI Templates and Localization

- [x] 2.1 Add localized tooltip message keys (`admin.users.sort.email`, `admin.users.sort.name`, `admin.users.sort.status`, `admin.users.sort.role`) to both `messages.properties` and `messages_de.properties`.
- [x] 2.2 Update `user-table.html` table header cells with interactive `.sortable-header` anchor elements, bidirectional chevron icons (`bi-chevron-expand`, `bi-chevron-up`, `bi-chevron-down`), and baseline alignment for the non-sortable Actions header.
- [x] 2.3 Add hidden `<input type="hidden" name="sort" ...>` and `<input type="hidden" name="dir" ...>` to `user-table.html`, and update `hx-include` on `#user-table-container` and `users.html` search input to include `[name='sort'], [name='dir'], [name='size']`.
- [x] 2.4 Update pagination page links and display-count selector buttons in `user-table.html` to preserve `sort` and `dir` parameters across navigation.

## 3. Integration and Security Testing (Testing Pyramid)

- [x] 3.1 Implement Layer 2 web slice integration test `AdminUsersSortingWebTest.java` using `final` modifiers, Javadoc, and inline comments, covering:
  - Happy path: Server-side sorting for all 4 columns (Email, Name, Status, Role) in both ASC and DESC directions with HTML row order validation.
  - Happy path: Active chevron indicator rendering (`bi-chevron-up` for ASC, `bi-chevron-down` for DESC) and tooltip verification.
  - Happy path: Parameter preservation across pagination links and display-count page size buttons.
  - Special cases: Case-insensitive composite Name ordering (`surname`, then `forename`).
  - Special cases: Search filtering combined with column sorting (`searchUsers` dynamic ordering).
  - Special cases: Deterministic ordering with duplicate status or role values via `mail ASC` tiebreaker.
  - Error cases: Unrecognized sort column safely falls back to default `email ASC` order without error.
- [x] 3.2 Update Layer 3 security test suite `TableSecuritySqlInjectionTest.java` with test cases targeting `/admin/users` against OWASP SQL injection vectors in `sort` and `dir` parameters, asserting HTTP 200 OK responses with safe fallback to `mail ASC` and zero SQL execution errors.
- [x] 3.3 Execute full test suite `mvn clean test` across the entire workspace to verify that all unit, slice, integration, and security tests pass with zero regressions.
