# Tasks: Admin Users Sort Role and Status Alignment

## 1. Controller Implementation & Layer 1 Unit Tests

- [x] 1.1 Invert the boolean sort direction in `AdminWebController.resolveSort` for `status` (`active DESC` on ASC, `active ASC` on DESC) and `role` (`admin DESC` on ASC, `admin ASC` on DESC) while retaining the secondary `mail ASC` (`ignoreCase()`) tiebreaker, using `final` modifiers on all declarations and variables with full Javadoc and explanatory inline comments. Verify compilation via `mvn compile -pl xcmailr-webapp`.
- [x] 1.2 Update Layer 1 unit tests in `AdminWebControllerTest.java` to assert that `resolveSort("status", "asc")` generates `active: DESC`, `resolveSort("status", "desc")` generates `active: ASC`, `resolveSort("role", "asc")` generates `admin: DESC`, and `resolveSort("role", "desc")` generates `admin: ASC` with `mail ASC` tiebreaker, using `final` modifiers and clear assertions. Verify with `mvn test -pl xcmailr-webapp -Dtest=AdminWebControllerTest`.

## 2. Layer 2 Web Slice Integration Tests & Verification

- [x] 2.1 Update Layer 2 web slice integration tests in `AdminUsersSortingWebTest.java` to validate rendered HTML row order:
  - `status=asc` places active users (`active = true`) before inactive users (`active = false`), matching mailbox table status behavior.
  - `status=desc` places inactive users before active users.
  - `role=asc` places administrator accounts (`admin = true`) before standard users (`admin = false`).
  - `role=desc` places standard users before administrator accounts.
  - Assert that the secondary `mail ASC` tiebreaker guarantees deterministic row ordering in each case.
  Verify with `mvn test -pl xcmailr-webapp -Dtest=AdminUsersSortingWebTest`.
- [x] 2.2 Execute the full Maven test suite `mvn clean test` across the entire workspace to verify that all unit, slice, integration, and security tests pass with zero regressions.
