# Design: Admin Users Table Column Sorting

## Context

The user management console at `/admin/users` allows administrators to search, activate/deactivate, promote, and delete user accounts. The list is currently rendered through `AdminWebController.listUsers` which invokes `userRepository.findAllByOrderByMailAsc(pageable)` or `userRepository.searchUsers(trimmedSearch, pageable)`. The `searchUsers` query has a hardcoded `ORDER BY u.mail ASC` clause in its `@Query` annotation, and the web endpoint does not accept `sort` or `dir` parameters. Consequently, table headers in `user-table.html` are static plain text.

This design mirrors the architectural pattern successfully implemented in `MailboxWebController.java` and `mailbox-table.html`, which provides server-side sorting, case-insensitive natural ordering, bi-directional chevron visual cues, and SQL injection prevention via strict whitelist normalization.

## Goals / Non-Goals

**Goals:**
- Provide full server-side sorting for Email, Name, Status, and Role columns on `/admin/users`.
- Support case-insensitive ordering (`ignoreCase()`) across all text columns (`mail`, `surname`, `forename`).
- Support directory-style composite sorting for Name (`surname`, then `forename`) with `mail ASC` as a secondary tiebreaker.
- Support boolean sorting for Status (`active`) and Role (`admin`) with `mail ASC` as a secondary tiebreaker.
- Display interactive table headers with dynamic chevrons (`bi-chevron-expand`, `bi-chevron-up`, `bi-chevron-down`) and localized tooltips.
- Preserve active sort and direction parameters across pagination, page size selector changes, search queries, and HTMX swaps.
- Strictly validate and whitelist all sort inputs against SQL injection and parameter manipulation.
- Implement exhaustive automated test coverage following the testing pyramid (unit, web slice integration, and security SQL injection tests) covering happy path, error cases, and special cases.
- Adhere strictly to the project's code style rules: `final` on all classes, methods, parameters, and variables; comprehensive Javadoc on all declarations; and informative inline comments.

**Non-Goals:**
- Client-side in-memory JavaScript sorting (all sorting must happen server-side on the full dataset).
- Adding sorting to the non-sortable Actions column.
- Modifying underlying database schema, tables, or entity relationships.

## Decisions

### 1. Remove Hardcoded `ORDER BY` in `UserRepository.java`
*Rationale*: Spring Data JPA automatically constructs the SQL `ORDER BY` clause from the `Sort` instance passed via `Pageable`. The existing `@Query` on `searchUsers` explicitly hardcoded `ORDER BY u.mail ASC`, which overrides or conflicts with dynamic sorting.
*Alternative Considered*: Providing distinct repository methods for each sort combination (e.g., `searchUsersOrderByMailAsc`, `searchUsersOrderByNameDesc`). *Rejected* due to combinatorial explosion and maintenance burden.

### 2. Strict Whitelist Normalization in `AdminWebController.java`
*Rationale*: Accept raw `sort` and `dir` query parameters, but pass them through static pure functions:
- `resolveSortField(final String sort)`: Maps input to `"email"`, `"name"`, `"status"`, or `"role"`, defaulting to `"email"` on any null, blank, or invalid value.
- `resolveSortDirection(final String dir)`: Returns `"asc"` if `"asc".equalsIgnoreCase(dir)`, otherwise defaults to `"desc"`. (For `"email"` as default view, initial sort is `"asc"`).
- `resolveSort(final String sort, final String dir)`: Returns a typed `org.springframework.data.domain.Sort` instance.
*Alternative Considered*: Direct parameter binding to Spring Data `Sort`. *Rejected* as unsafe against SQL injection or unexpected entity attribute leakage.

### 3. Case-Insensitive Composite Sorting for "Name"
*Rationale*: User full names are displayed as `${u.forename + ' ' + u.surname}`. In administrative directories, users are customarily located by surname first. The sort order is defined as:
```java
Sort.by(
    Sort.Order.by("surname").with(direction).ignoreCase(),
    Sort.Order.by("forename").with(direction).ignoreCase(),
    Sort.Order.by("mail").ascending().ignoreCase()
);
```
Both surname and forename use `ignoreCase()` to match user expectations.

### 4. Deterministic Secondary Sorting
*Rationale*: Boolean columns (`active` and `admin`) have many duplicate values. Without a secondary deterministic sort key, database paging can produce unstable pagination results across page transitions. Adding `Sort.Order.by("mail").ascending().ignoreCase()` ensures consistent pagination.

### 5. UI Markup Parity with Mailbox Table
*Rationale*: Standardize on existing CSS classes (`.sortable-header`, `.sort-icon`) defined in `xceptance-theme.css` and `emerald-theme.css`:
- Unsorted columns render `<i class="bi bi-chevron-expand sort-icon text-body-secondary opacity-50 ms-1"></i>`
- Active ASC renders `<i class="bi bi-chevron-up sort-icon text-primary ms-1"></i>`
- Active DESC renders `<i class="bi bi-chevron-down sort-icon text-primary ms-1"></i>`
- Actions column header uses `<span class="d-inline-flex align-items-center py-1">` for baseline alignment.

### 6. Code Style & Engineering Standards
To ensure codebase consistency and maintainability:
- **`final` everywhere**: All new classes, methods, method parameters, catch block exceptions, local variables, lambda parameters, and loop variables MUST be declared `final`.
- **Comprehensive Javadoc**: Every class, constructor, method (public, protected, package-private, private), and constant MUST include full Javadoc explaining its architectural responsibility, `@param`, and `@return` semantics.
- **Inline Documentation**: Non-trivial control branches, JPA sorting strategies, and security normalization steps MUST be accompanied by explanatory comments.

---

## Testing Pyramid Architecture

```
                 / \
                /   \      Security Tests (TableSecuritySqlInjectionTest)
               /  S  \     - OWASP SQL injection fuzzing on sort & dir
              /-------\    - Malicious character handling
             /         \
            /  I / W    \  Web Slice & Integration Tests (AdminUsersSortingWebTest)
           /             \ - Full HTTP GET /admin/users HTMX request/response
          /---------------\- Column order verification across all 4 columns (ASC/DESC)
         /                 \- Pagination & Display Count preservation
        /     Unit Tests    \- Search filtering combined with sorting
       /                     \
      /         U             \ Unit Tests (AdminWebControllerTest)
     /-------------------------\- Whitelist mapping (resolveSortField, resolveSortDirection)
                                - Typed Sort construction & case-insensitivity checks
                                - Null, blank, and boundary values
```

### Layer 1: Unit Tests (`AdminWebControllerTest.java`)
- **Happy Path**:
  - `resolveSortField`: Correct normalization for `"email"`, `"mail"`, `"name"`, `"status"`, `"role"`.
  - `resolveSortDirection`: Proper parsing of `"asc"` and `"desc"`.
  - `resolveSort`: Validates that `resolveSort("email", "asc")` produces `Sort.Order.by("mail").ascending().ignoreCase()`.
  - `resolveSort`: Validates that `resolveSort("name", "asc")` produces composite order on `surname` (ignoreCase), `forename` (ignoreCase), and `mail` (ignoreCase).
  - `resolveSort`: Validates that `resolveSort("status", "asc")` and `resolveSort("role", "asc")` produce primary boolean sort with secondary `mail ASC` tiebreaker.
- **Error Cases**:
  - Null, empty string, or whitespace string passed to `resolveSortField` safely defaults to `"email"`.
  - Non-existent column names (e.g., `"unknown"`, `"passwd"`, `"token"`, `"id"`) safely default to `"email"`.
  - Invalid direction strings (e.g., `"ascending"`, `"down"`, `"1"`, `null`, `""`) safely default to `"desc"`.
- **Special Cases**:
  - Case-insensitivity of parameter strings (e.g., `"EMAIL"`, `"Name"`, `"STATUS"`, `"ASC"`, `"Desc"`) normalized correctly.
  - Verification that model attributes `sort` and `dir` are set to sanitized strings.

### Layer 2: Web Slice & Integration Tests (`AdminUsersSortingWebTest.java`)
- **Happy Path**:
  - GET `/admin/users?sort=email&dir=asc`: HTML contains rows in alphabetical email order, and Email header displays `bi-chevron-up`.
  - GET `/admin/users?sort=email&dir=desc`: HTML contains rows in reverse email order, and Email header displays `bi-chevron-down`.
  - GET `/admin/users?sort=name&dir=asc`: HTML orders users by surname ASC then forename ASC (e.g., "Jane Adams" before "Bob Baker").
  - GET `/admin/users?sort=name&dir=desc`: HTML orders users by surname DESC then forename DESC.
  - GET `/admin/users?sort=status&dir=asc`: Inactive users appear before active users; secondary order follows `mail ASC`.
  - GET `/admin/users?sort=status&dir=desc`: Active users appear before inactive users.
  - GET `/admin/users?sort=role&dir=asc`: Regular users appear before administrators; secondary order follows `mail ASC`.
  - GET `/admin/users?sort=role&dir=desc`: Administrators appear before regular users.
  - Pagination navigation preserves `sort` and `dir` in all generated page links.
  - Display count selector preserves `sort` and `dir` in all size option links.
- **Error Cases**:
  - Passing unwhitelisted sort parameter returns HTTP 200 OK with default `email ASC` order without errors.
- **Special Cases**:
  - Search query combined with column sorting (`GET /admin/users?search=test&sort=name&dir=desc`) filters matching users and orders them correctly.
  - Deterministic tiebreaking: Multiple users with identical surname or boolean flags maintain consistent order across pages.
  - Unsorted columns display `bi-chevron-expand` with low opacity.

### Layer 3: Security & SQL Injection Tests (`TableSecuritySqlInjectionTest.java`)
- **Error Cases**:
  - Injection vectors in `sort`:
    - `sort=email' OR '1'='1`
    - `sort=email; DROP TABLE users;--`
    - `sort=mail UNION SELECT * FROM users`
    - `sort=1;SELECT pg_sleep(5)--`
  - Injection vectors in `dir`:
    - `dir=asc; DROP TABLE users;`
    - `dir=asc, (SELECT CASE WHEN (1=1) THEN 1 ELSE 0 END)`
  - Verification that all malicious inputs safely normalize to `mail ASC`, return HTTP 200 OK, and execute no injected SQL.

---

## Risks / Trade-offs

| Risk | Mitigation |
| :--- | :--- |
| **SQL Injection via `sort` or `dir`** | Handled by strict whitelist mapping (`switch` statement over allowed string literals). Only predefined `Sort.Order` objects are passed to JPA queries; invalid values safely default to `mail ASC`. Tested via `TableSecuritySqlInjectionTest`. |
| **Loss of Sort State During HTMX Swaps** | Add hidden inputs `<input type="hidden" name="sort" ...>` and `<input type="hidden" name="dir" ...>` inside `#user-table-container` and include `[name='sort'], [name='dir']` in search input `hx-include`. |
| **Pagination Links Dropping Sort State** | Pass `sort=${sort}` and `dir=${dir}` into all pagination and display-count `hx-get` URLs in `user-table.html`. |
| **Theme / Visual Inconsistencies** | Reuse existing theme variables and CSS classes (`.sortable-header`, `.sort-icon`) tested across light and dark themes. |

## Migration Plan

1. Update `UserRepository.java` to remove hardcoded `ORDER BY` from `searchUsers` `@Query`.
2. Update `AdminWebController.java` with whitelist methods, declaring all methods, parameters, and variables as `final` with complete Javadoc.
3. Add localized tooltip strings to `messages.properties` and `messages_de.properties`.
4. Update `user-table.html` and `users.html` with sortable headers, chevrons, and parameter preservation.
5. Create `AdminUsersSortingWebTest.java` implementing the full testing pyramid suite.
6. Extend `AdminWebControllerTest.java` and `TableSecuritySqlInjectionTest.java`.
7. Verify full build with `mvn clean test`.
