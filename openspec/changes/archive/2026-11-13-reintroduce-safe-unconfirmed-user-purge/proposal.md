# Proposal

## Why

Automatic cleanup of abandoned, unconfirmed user registrations is needed so that unverified signups whose confirmation window has expired do not accumulate indefinitely in the database. However, because legacy XCMailr never cleared `confirmation` or `ts_confirm` upon account activation, all legacy active users still carry historical confirmation tokens in the database; reintroducing unconfirmed-user cleanup therefore requires code-level safeguards so that deactivating a legacy user or submitting a forgot-password request for an inactive user can never cause a legitimate or deactivated account to be purged.

## What Changes

- **Reintroduce guarded unconfirmed-registration cleanup (`UserRepository` & `ExpirationScheduledService`)**:
  - Restore `findExpiredInactiveUsers` in `UserRepository` with an explicit empty-mailbox safeguard (`WHERE u.active = false AND u.confirmation IS NOT NULL AND u.ts_confirm > 0 AND u.ts_confirm < :cutoff AND u.boxes IS EMPTY`) so any account that owns mailboxes (such as an already-deactivated legacy user) is strictly excluded without requiring a database migration.
  - Restore `purgeExpiredInactiveUsers(final long now)` in `ExpirationScheduledService.runMaintenanceTasks()` to periodically delete expired, genuinely unconfirmed user registrations.
- **Unconditional token sanitation on administrative toggle (`AdminWebController`)**:
  - Update `AdminWebController.toggleUserActive` to clear `confirmation = null` and `ts_confirm = null` whenever an administrator toggles a user's active status (both when activating an inactive user **and** when deactivating an active user), stripping leftover legacy tokens immediately upon deactivation.
- **Guard password reset against inactive accounts (`WebAuthController`)**:
  - Update `WebAuthController.forgotPasswordSubmit` to generate a reset token and send a reset email only when `user.isActive()` is `true` (while still returning the identical `redirect:/login?resetRequested` response to preserve CWE-204 anti-enumeration). This prevents `/forgot-password` from setting `confirmation` and `ts_confirm` on a deactivated account (which would otherwise mark it for deletion after 2 hours or allow bypassing administrative deactivation via `/confirm/{token}`).
- **Test-driven coverage across the testing pyramid & documentation**:
  - Implement unit, JPA/service integration, and WebMvc tests covering happy paths, error cases, and special/legacy edge cases, enforcing `final` modifiers, Javadoc, and explanatory code comments.
  - Update `readme.md` to document the unconfirmed-registration cleanup behavior and legacy/deactivated account safeguards.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-console-ui`: Update `User Authentication and Self-Service` and `Admin Management Console` requirements to specify automatic purging of expired unconfirmed registrations with empty mailboxes, unconditional token clearing on administrative activation and deactivation, and restriction of password-reset token issuance to active accounts.

## Impact

- **Affected Code**:
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/repositories/UserRepository.java`
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/services/ExpirationScheduledService.java`
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/AdminWebController.java`
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/WebAuthController.java`
- **Affected Tests**:
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/services/ExpirationScheduledServiceTest.java`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/AdminWebControllerTest.java`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/WebAuthControllerTest.java`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/WebAuthControllerUnitTest.java`
- **Documentation**:
  - `readme.md`
- **APIs and Database**: No Flyway schema or data migrations required; works seamlessly against existing legacy production databases.
