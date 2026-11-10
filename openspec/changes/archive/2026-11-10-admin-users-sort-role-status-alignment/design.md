# Design: Admin Users Sort Role and Status Alignment

## Context

In `AdminWebController.java`, the static whitelist method `resolveSort` maps column identifiers (`email`, `name`, `status`, `role`) into typed Spring Data `Sort` orders. For the two boolean columns (`status` mapping to `User.active` and `role` mapping to `User.admin`), the controller currently forwards the requested `direction` (`ASC` or `DESC`) directly to Spring Data:
```java
case "status" -> Sort.by(Sort.Order.by("active").with(direction), mailTiebreaker);
case "role"   -> Sort.by(Sort.Order.by("admin").with(direction), mailTiebreaker);
```

Because SQL evaluates boolean false before true (`false (0) < true (1)`), `direction = ASC` generates SQL `ORDER BY active ASC` and `ORDER BY admin ASC`, resulting in inactive users before active users and standard users before administrators.

In contrast, `MailboxWebController.java` maps mailbox status to the negative flag `expired`, where `false` represents an active mailbox. As a result, `status ASC` on mailboxes naturally displays active mailboxes first.

See [proposal.md](file:///Users/rbaumgarten/checkouts/XCMailr/openspec/changes/admin-users-sort-role-status-alignment/proposal.md) for full motivation and scope.

## Goals / Non-Goals

**Goals:**
- Invert the SQL direction mapping for `status` and `role` in `AdminWebController.resolveSort` so that `dir=asc` displays `true` values first (Active before Inactive, Admin before User) and `dir=desc` displays `false` values first.
- Align user management status sorting behavior with the established mailbox table behavior.
- Preserve the case-insensitive `mail ASC` tiebreaker for both columns to guarantee deterministic ordering across paginated views.
- Maintain complete backwards compatibility for client requests and UI template markup (`hx-get` URLs and parameters remain `sort=role`, `sort=status`, `dir=asc`, `dir=desc`).
- Enforce strict coding conventions: `final` on all declarations, parameters, and local variables, with comprehensive Javadoc and inline comments.
- Update test suites across the testing pyramid (unit tests in `AdminWebControllerTest.java` and integration tests in `AdminUsersSortingWebTest.java`).

**Non-Goals:**
- Modifying the underlying database schema or `User` entity columns.
- Modifying UI templates (`user-table.html` or `users.html`), as client links already pass the logical column and direction.
- Changing `email` or `name` sorting logic.

## Decisions

### Decision 1: Invert Sort Direction for Boolean Columns via Direction Flipping

**Choice**: Invert the Spring Data `Sort.Direction` specifically for boolean fields:
```java
final Sort.Direction booleanDirection = direction.isAscending() ? Sort.Direction.DESC : Sort.Direction.ASC;
```
For `status`:
```java
case "status" -> Sort.by(
    Sort.Order.by("active").with(booleanDirection),
    mailTiebreaker);
```
For `role`:
```java
case "role" -> Sort.by(
    Sort.Order.by("admin").with(booleanDirection),
    mailTiebreaker);
```

**Rationale**:
- Standard SQL treats `1` (true) as greater than `0` (false). By querying `DESC` when the user requests `ASC`, the database returns `1` (`active = true` / `admin = true`) first.
- Requires no custom JPQL expressions, native SQL, or unsafe JPA sorting.
- Zero runtime overhead.

**Alternatives Considered**:
- *JPQL CASE expressions (`CASE WHEN u.active = true THEN 0 ELSE 1 END`)*: Unnecessary complexity, harder to maintain, and requires bypassing standard typed Spring Data `Sort.Order`.
- *Inverting `dir` in the HTML template*: Pushes database-specific schema quirks into presentation templates and breaks external API callers expecting standard `dir=asc`.

### Decision 2: Maintain Existing Tiebreaker Contract

**Choice**: Keep `mailTiebreaker` as `Sort.Order.by("mail").with(Sort.Direction.ASC).ignoreCase()`.

**Rationale**:
- When multiple users share identical status (`active = true`) or role (`admin = false`), deterministic ordering is required to prevent records from jumping across pagination pages.
- Secondary ordering by email ascending (A &rarr; Z) is standard throughout the application.

## Risks / Trade-offs

- **[Risk] Existing unit and web slice tests fail due to assertion inversion**  
  *Mitigation*: Update `AdminWebControllerTest.java` and `AdminUsersSortingWebTest.java` in the same change to assert that ASC places active before inactive and admin before standard user.
- **[Risk] Inconsistency if new boolean columns are added in the future**  
  *Mitigation*: Clearly document in `AdminWebController.java` Javadoc and inline comments why boolean direction inversion is applied.
