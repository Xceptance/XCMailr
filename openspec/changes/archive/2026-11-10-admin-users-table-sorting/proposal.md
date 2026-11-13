# Proposal: Admin Users Table Column Sorting

## Why

In the administrative console (`/admin/users`), administrators currently view user accounts in a fixed order hardcoded to email ascending (`mail ASC`). Column headers are static, preventing administrators from sorting users by surname, activation status, or administrative role when managing large user populations. Adding interactive, case-insensitive server-side column sorting with bidirectional indicators brings the users table to parity with the mailbox dashboard table.

## What Changes

- Add server-side dynamic column sorting to the user management endpoint (`GET /admin/users`) supporting `sort` and `dir` query parameters with strict whitelist validation.
- Remove hardcoded `ORDER BY u.mail ASC` from `UserRepository.searchUsers` `@Query` to enable dynamic sorting via Spring Data JPA `Pageable.getSort()`, and use `userRepository.findAll(pageable)` for non-search views.
- Implement strict whitelist resolution methods in `AdminWebController` mapping allowed keys (`email`/`mail`, `name`, `status`, `role`) to entity properties and safe defaults (`mail ASC`), safeguarding against SQL injection.
- Configure case-insensitive natural ordering (`ignoreCase()`) for `mail`, `surname`, and `forename`.
- Configure composite name sorting by `surname` then `forename` (directory style), with `mail ASC` as a secondary tiebreaker.
- Configure boolean status (`active`) and role (`admin`) sorting with `mail ASC` as a deterministic secondary tiebreaker.
- Update `user-table.html` table headers with interactive anchor links, accessible tooltips, and bidirectional chevron icons (`bi-chevron-expand`, `bi-chevron-up`, `bi-chevron-down`).
- Preserve active `sort` and `dir` parameters across pagination navigation, display-count page size selection, search input triggers, and table container HTMX swaps (`hx-include`).
- Add localized tooltip message keys in `messages.properties` and `messages_de.properties`.
- Add comprehensive web, unit, and SQL injection security test coverage.

## Capabilities

### New Capabilities
<!-- None: this change modifies existing web console UI behavior -->

### Modified Capabilities
- `web-console-ui`: Add user table column sorting requirements and scenarios under the Admin Management Console capability, specifying server-side column sorting, case-insensitive natural ordering, composite surname-forename ordering, deterministic tiebreaking, and parameter preservation across table controls.

## Impact

- **Affected Code**:
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/repositories/UserRepository.java`
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/AdminWebController.java`
  - `xcmailr-webapp/src/main/resources/templates/admin/fragments/user-table.html`
  - `xcmailr-webapp/src/main/resources/templates/admin/users.html`
  - `xcmailr-webapp/src/main/resources/conf/messages.properties`
  - `xcmailr-webapp/src/main/resources/conf/messages_de.properties`
  - Test suites: `AdminWebControllerTest.java`, `TableSecuritySqlInjectionTest.java`, and new `AdminUsersSortingWebTest.java`.
- **APIs / Contracts**: Internal web console endpoints only (`GET /admin/users`). No breaking API changes; `sort` and `dir` default gracefully to `email` and `asc`.
- **Dependencies**: No new external dependencies required.
