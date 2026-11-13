# Design

## Context

See `proposal.md` for background motivation. In the current codebase:
- `users.active` is a boolean column (`TRUE` or `FALSE`; mapped to primitive `boolean` in `User.java`) with no third status value.
- Legacy XCMailr versions populated `confirmation` and `ts_confirm` at registration or password-reset time and only flipped `active = TRUE` on confirmation without setting `confirmation = NULL` or `ts_confirm = NULL`.
- In the previous change, `UserRepository.findExpiredInactiveUsers` included `AND u.boxes IS EMPTY` as a runtime query safeguard to protect legacy accounts that still carried historical confirmation tokens in the database.
- Moving that safety net into a one-time Flyway data migration (`V6__Clean_Legacy_Confirmation_Tokens.sql`) establishes a clean database invariant at startup: an account has a non-null `confirmation` token while `active = FALSE` if and only if it is a genuinely unconfirmed registration.

## Goals / Non-Goals

**Goals:**
- Sanitize all legacy confirmation tokens and timestamps in existing databases at startup via Flyway migration `V6__Clean_Legacy_Confirmation_Tokens.sql`, covering both active users (`active = TRUE`) and already-deactivated legacy users who own mailboxes (`EXISTS (SELECT 1 FROM mailboxes WHERE mailboxes.usr_id = users.id)`).
- Remove the runtime `AND u.boxes IS EMPTY` clause from `UserRepository.findExpiredInactiveUsers`.
- Retain runtime token hygiene in `AdminWebController.toggleUserActive` (clearing tokens on activate/deactivate) and `WebAuthController.forgotPasswordSubmit` (issuing reset tokens only to active accounts).
- Follow strict Test-Driven Development (TDD) across the test pyramid (happy paths, error/boundary cases, and special cases), using `final` wherever possible, comprehensive Javadoc and code comments, and updated documentation in `readme.md`.

**Non-Goals:**
- Modifying the `users` table schema or introducing additional status columns.
- Changing the registration, email confirmation, or password reset user flows.
- Altering mailbox expiration (`expireMailboxes`) or transaction retention (`purgeOldTransactions`) schedules.

## Decisions

### 1. Flyway `V6` Migration Predicate (Option B: Active Users OR Users With Mailboxes)

**Decision:**
Create `xcmailr-webapp/src/main/resources/db/migration/V6__Clean_Legacy_Confirmation_Tokens.sql` with standard ANSI SQL:
```sql
UPDATE users
SET confirmation = NULL,
    ts_confirm = NULL
WHERE active = TRUE
   OR EXISTS (
       SELECT 1
       FROM mailboxes
       WHERE mailboxes.usr_id = users.id
   );
```

**Rationale:**
- Clearing tokens only where `active = TRUE` (Option A) would miss legacy users who were already deactivated in the database prior to the migration (for example, `m.pfotenhauer+hb@xceptance.com` in an un-toggled production backup, where `active = FALSE`, `confirmation` is non-null from 2019, and the user owns 2,049 mailboxes). Once `AND u.boxes IS EMPTY` is removed from `UserRepository.findExpiredInactiveUsers`, any such already-deactivated legacy user would otherwise be deleted on the first scheduled cleanup cycle after startup.
- Including `OR EXISTS (SELECT 1 FROM mailboxes WHERE mailboxes.usr_id = users.id)` in `V6` guarantees that every previously active/productive account—even if currently deactivated—has its stale confirmation token cleared during migration, while genuinely unconfirmed registrations (`active = FALSE` and zero mailboxes, since unconfirmed users cannot log in to create mailboxes) keep their confirmation token and timestamp.
- The statement uses standard ANSI SQL (`BOOLEAN` literal `TRUE` and correlated `EXISTS` subquery on `mailboxes.usr_id = users.id`) compatible with H2, PostgreSQL, and MySQL/MariaDB, and is completely idempotent.

**Alternatives Considered:**
- *Option A (`WHERE active = TRUE` only)*: Rejected because already-deactivated legacy accounts with mailboxes would still carry historical `confirmation` tokens and would be purged once the runtime `u.boxes IS EMPTY` check is removed.
- *Keeping `AND u.boxes IS EMPTY` in `UserRepository` alongside `V6`*: Rejected per team decision to avoid redundant runtime join/subquery checks once the data is migrated.

### 2. Simplify `UserRepository.findExpiredInactiveUsers`

**Decision:**
Update `UserRepository.findExpiredInactiveUsers` to:
```java
@Query("SELECT u FROM User u WHERE u.active = false AND u.confirmation IS NOT NULL AND u.ts_confirm > 0 AND u.ts_confirm < :timestamp")
List<User> findExpiredInactiveUsers(@Param("timestamp") final Long timestamp);
```

**Rationale:**
- With `V6` sanitizing historical database state at startup, `WebAuthController.confirm` clearing tokens on confirmation, `AdminWebController.toggleUserActive` clearing tokens on administrative status changes, and `WebAuthController.forgotPasswordSubmit` ignoring inactive users, the database invariant `active = false AND confirmation IS NOT NULL` uniquely identifies unconfirmed registrations.
- Removing `AND u.boxes IS EMPTY` eliminates the runtime `NOT EXISTS` subquery against `mailboxes` on every scheduled maintenance execution.

### 3. Test-Driven Verification Across the Testing Pyramid

**Decision:**
1. **`FlywayMigrationTest`**:
   - Assert that Flyway discovers and applies at least 6 migrations (`V1` through `V6`), including `V6__Clean_Legacy_Confirmation_Tokens.sql`.
   - Test the `V6` migration SQL behavior across:
     - **Happy path**: Active user with legacy `confirmation` and `ts_confirm` has both fields set to `NULL`.
     - **Special case**: Inactive user (`active = false`) who owns a mailbox and has a legacy `confirmation` token has both `confirmation` and `ts_confirm` set to `NULL`, whereas an inactive user with zero mailboxes retains their `confirmation` token and `ts_confirm`.
     - **Boundary / Error case**: Users whose `confirmation` and `ts_confirm` are already `NULL` remain unaffected when the migration SQL executes.
2. **`ExpirationScheduledServiceTest`**:
   - Update unit and integration tests for `UserRepository.findExpiredInactiveUsers` and `ExpirationScheduledService.purgeExpiredInactiveUsers` to reflect the simplified query and verify that users sanitized by the `V6` migration criterion or administrative deactivation (`confirmation = null, ts_confirm = null`) are preserved while expired unconfirmed users (`active = false, confirmation != null, 0 < ts_confirm < cutoff`) are purged.

## Risks / Trade-offs

- **[Risk] An unconfirmed user somehow owns a mailbox prior to `V6`** → **Mitigation**: Unconfirmed users (`active = false` from signup) cannot authenticate (`CustomUserDetailsService` sets `disabled(!user.isActive())` and API tokens require an authenticated session to generate), so a genuinely unconfirmed signup can never own a mailbox. Any inactive user who owns a mailbox is guaranteed to have been previously active.
- **[Risk] Future code path sets a confirmation token on an inactive user** → **Mitigation**: `WebAuthController.forgotPasswordSubmit` explicitly checks `user.isActive()` before generating a token, and `AdminWebController.toggleUserActive` clears `confirmation` and `ts_confirm` whenever an account is activated or deactivated; both safeguards remain covered by controller and integration tests.

## Migration Plan

1. On application startup, Flyway automatically executes `V6__Clean_Legacy_Confirmation_Tokens.sql` before Spring scheduling starts.
2. All active users and all inactive users owning mailboxes have `confirmation` and `ts_confirm` set to `NULL` in a single transactional `UPDATE`.
3. `ExpirationScheduledService.purgeExpiredInactiveUsers()` runs on its regular schedule using the simplified `UserRepository.findExpiredInactiveUsers` query.
