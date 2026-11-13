# Design

## Context

In legacy XCMailr (`develop` branch), `Application.verifyActivation()` only set `user.setActive(true)` upon email confirmation and never cleared `confirmation` or `ts_confirm`. As verified against the production database (`92` total users), `90` active legacy users still carry non-null 20-character `CONFIRMATION` tokens and expired `TS_CONFIRM` timestamps from 2015–2025, and `1` already-deactivated legacy user (`m.pfotenhauer+hb@xceptance.com`) has `ACTIVE = FALSE`, a non-null `CONFIRMATION` token from 2019, and `2,049` mailboxes.

Because `User` defines `@OneToMany(mappedBy = "usr", cascade = CascadeType.ALL, orphanRemoval = true)` on `boxes`, deleting a `User` entity cascades deletion to all of their mailboxes and stored emails. Reintroducing scheduled cleanup of expired unconfirmed registrations (`See proposal.md - Why`) therefore requires coordinated safeguards across repository queries, administrative status toggling, and self-service password reset so that no existing or newly deactivated user is ever mistaken for an unconfirmed registration, without requiring a database data migration.

## Goals / Non-Goals

**Goals:**
- Reintroduce `ExpirationScheduledService.purgeExpiredInactiveUsers(final long now)` and `UserRepository.findExpiredInactiveUsers(final Long timestamp)` with an explicit `u.boxes IS EMPTY` and `u.ts_confirm > 0` JPQL guard.
- Unconditionally clear `confirmation` and `ts_confirm` in `AdminWebController.toggleUserActive` whenever an administrator activates or deactivates a user account.
- Restrict `WebAuthController.forgotPasswordSubmit` so that confirmation tokens and password-reset emails are only generated when `user.isActive()` is `true`, while returning the identical `redirect:/login?resetRequested` response for inactive or non-existent accounts (CWE-204 anti-enumeration).
- Follow strict test-driven development (TDD) across the testing pyramid (unit tests, `@SpringBootTest` repository/service integration tests, and `@WebMvcTest` controller slice tests) covering happy paths, error cases, and special/boundary cases.
- Enforce `final` on all method parameters, local variables, and catch blocks, add comprehensive Javadoc and inline comments, and document the behavior in `readme.md`.

**Non-Goals:**
- Adding a Flyway SQL data migration to mutate existing production `USERS` rows.
- Changing self-service confirmation link expiration handling in `WebAuthController.confirmAccount`.
- Modifying explicit administrative deletion (`AdminWebController.deleteUser`) or self-service account deletion (`ProfileWebController.deleteAccountProcess`).

## Decisions

### Decision 1: Three-layer code safeguard instead of a Flyway data migration
- **Choice**: Protect legacy and deactivated accounts purely in application code via three complementary safeguards:
  1. **JPQL Empty-Mailbox & Positive-Timestamp Guard (`UserRepository.findExpiredInactiveUsers`)**:
     ```sql
     SELECT u FROM User u
     WHERE u.active = false
       AND u.confirmation IS NOT NULL
       AND u.ts_confirm > 0
       AND u.ts_confirm < :timestamp
       AND u.boxes IS EMPTY
     ```
     An unconfirmed registrant can never log in or obtain an API token before confirming their account, so a genuinely unconfirmed registration always has `u.boxes IS EMPTY`. Conversely, any already-deactivated legacy user who owns mailboxes (such as `m.pfotenhauer+hb@xceptance.com` with 2,049 mailboxes) is immediately excluded at the SQL level without touching existing database rows.
  2. **Unconditional Token Clearing on Admin Toggle (`AdminWebController.toggleUserActive`)**:
     Clear `targetUser.setConfirmation(null)` and `targetUser.setTs_confirm(null)` on every administrative status toggle—both when activating an inactive user and when deactivating an active user. Even if one of the 17 legacy active users with 0 mailboxes is deactivated by an administrator, their leftover 2015–2025 confirmation token is stripped in the same transaction, ensuring `u.confirmation IS NOT NULL` is `false`.
  3. **Active-Account Guard on Password Reset (`WebAuthController.forgotPasswordSubmit`)**:
     In `WebAuthController.forgotPasswordSubmit`, check `if (userOpt.isPresent() && userOpt.get().isActive())` before generating a reset token, setting `ts_confirm`, saving the user, and sending the reset email. Because `forgotPasswordSubmit` reuses `user.confirmation` and `user.ts_confirm`, allowing password resets on an inactive/deactivated user would plant a non-null confirmation token that expires in 2 hours (marking an inactive user with 0 mailboxes for deletion, or allowing an inactive user to reactivate their own account via `/confirm/{token}`).
- **Alternatives Considered**:
  - *Flyway data migration (`UPDATE USERS SET CONFIRMATION = NULL, TS_CONFIRM = 0 WHERE ACTIVE = TRUE OR ...`)*: Rejected because the user prefers a zero-migration solution that works directly against existing production databases without mutating historical records at startup.

### Decision 2: Cutoff calculation and configuration guard in `ExpirationScheduledService`
- **Choice**: In `ExpirationScheduledService.purgeExpiredInactiveUsers(final long now)`, read `final int confirmHours = properties.getApp().getConfirmationPeriodHours()`. Only execute the query when `confirmHours > 0`, computing `final long cutoff = now - (confirmHours * 3_600_000L)`. Invoke `purgeExpiredInactiveUsers(now)` inside `runMaintenanceTasks()`.
- **Rationale**: Matches the existing retention pattern in `expireUserTokens` and `aggregateAndCleanTransactions`, allowing confirmation cleanup to be disabled if `confirmationPeriodHours <= 0` and ensuring a grace buffer beyond the token's expiration timestamp.

### Decision 3: Test-Driven Development (TDD) across the Testing Pyramid
- **Choice**: Write failing tests first at each layer of the testing pyramid before implementing production changes:
  - **Unit Tests**:
    - `WebAuthControllerUnitTest` & `AdminWebControllerTest` (direct/mocked unit checks): Verify `forgotPasswordSubmit` on active vs. inactive vs. unknown users; verify `toggleUserActive` clears `confirmation` and `ts_confirm` on both activation and deactivation, and rejects self-deactivation or missing user IDs.
  - **Service & Repository Integration Tests (`@SpringBootTest` / `@Transactional`)**:
    - `ExpirationScheduledServiceTest`:
      - *Happy path*: Expired unconfirmed user (`active = false`, `confirmation != null`, `ts_confirm > 0 && ts_confirm < cutoff`, `boxes` empty) is purged during `purgeExpiredInactiveUsers` and `runMaintenanceTasks()`.
      - *Special case (Legacy deactivated user with mailboxes)*: Inactive user with expired `confirmation` and `ts_confirm` who owns at least one `MBox` (`boxes` non-empty) is preserved along with all mailboxes.
      - *Special case (Admin-deactivated user without token)*: Inactive user with `confirmation = null` and `boxes` empty is preserved.
      - *Boundary / Error cases*: Unconfirmed user within grace period (`ts_confirm >= cutoff`), inactive user with `ts_confirm = 0` or `null`, active user with leftover legacy `confirmation` token (`active = true`), and disabled cleanup (`confirmationPeriodHours <= 0`) are all preserved.
  - **WebMvc Controller Slice Tests (`@WebMvcTest`)**:
    - `AdminWebControllerTest` & `WebAuthControllerTest`: End-to-end HTTP POST with CSRF verification for `/admin/users/{id}/toggle-active` (deactivation and activation token clearing) and `/forgot-password` (active user vs. inactive user anti-enumeration redirect without token mutation or email dispatch).

## Risks / Trade-offs

- **[Pre-existing deactivated legacy user with 0 mailboxes]** → If a legacy database had an already-deactivated user (`active = false`) with a leftover token who owned 0 mailboxes prior to deployment, that record would look identical to an abandoned registration.
  - *Mitigation*: Production database inspection confirmed that the **only** inactive user in production (`m.pfotenhauer+hb@xceptance.com`) owns `2,049` mailboxes and is therefore 100% protected by `u.boxes IS EMPTY`. All other 91 users are active, so any future deactivation goes through `AdminWebController.toggleUserActive`, which clears `confirmation` and `ts_confirm`.
- **[Unconfirmed user requests password reset before confirming]** → An unconfirmed user (`active = false`) who clicks "Forgot Password" before activating their account will not receive a password-reset email.
  - *Mitigation*: An unconfirmed user must first complete email verification via their registration confirmation link (or re-register after their unconfirmed registration expires and is purged). Preventing password reset on inactive accounts also closes a security loophole where `/forgot-password` + `/confirm/{token}` could bypass administrative deactivation.

## Migration Plan

- No database schema or data migrations required.
- Deploy updated application archive; safeguards take effect immediately on startup and during scheduled maintenance runs.
- Rollback strategy: Standard application rollback.
