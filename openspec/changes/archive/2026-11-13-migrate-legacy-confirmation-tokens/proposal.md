# Proposal

## Why

In legacy XCMailr, `confirmation` tokens and `ts_confirm` timestamps were never cleared when a user confirmed their account, leaving historical tokens on all legacy accounts in existing databases. Moving the legacy-account safety net into a dedicated Flyway data migration (`V6`) permanently cleans up these stale tokens in the database for all active users and any deactivated users who own mailboxes, allowing us to remove the runtime `u.boxes IS EMPTY` join check from `UserRepository.findExpiredInactiveUsers`.

## What Changes

- **Add Flyway data migration `V6__Clean_Legacy_Confirmation_Tokens.sql`**:
  - Execute `UPDATE users SET confirmation = NULL, ts_confirm = NULL WHERE active = TRUE OR EXISTS (SELECT 1 FROM mailboxes WHERE mailboxes.usr_id = users.id);` on startup to permanently clear historical confirmation tokens and timestamps on all active users as well as any already-deactivated users who own mailboxes (such as `m.pfotenhauer+hb@xceptance.com`), while leaving genuinely unconfirmed signups without mailboxes intact.
- **Remove runtime `u.boxes IS EMPTY` check from `UserRepository.findExpiredInactiveUsers`**:
  - Simplify `UserRepository.findExpiredInactiveUsers` to `SELECT u FROM User u WHERE u.active = false AND u.confirmation IS NOT NULL AND u.ts_confirm > 0 AND u.ts_confirm < :timestamp` now that legacy database state is sanitized by the `V6` migration and runtime transitions are guarded by `AdminWebController.toggleUserActive` and `WebAuthController.forgotPasswordSubmit`.
- **Update test pyramid and documentation**:
  - Update `FlywayMigrationTest` and `ExpirationScheduledServiceTest` (TDD: happy path, error cases, and special cases) to verify `V6` migration execution and the simplified repository query, enforcing `final` modifiers, Javadoc, and code comments.
  - Update `readme.md` to document the `V6` Flyway data migration and updated cleanup behavior.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-console-ui`: Update `Admin Management Console` requirement and scenarios to specify that legacy confirmation tokens on active users and mailbox-owning inactive users are sanitized via the startup Flyway database migration (`V6`), and background maintenance purges expired unconfirmed registrations based on `active = false`, `confirmation IS NOT NULL`, `ts_confirm > 0`, and `ts_confirm < cutoff` without requiring a runtime mailbox-collection subquery.

## Impact

- **Affected Database Migrations**:
  - `xcmailr-webapp/src/main/resources/db/migration/V6__Clean_Legacy_Confirmation_Tokens.sql` (new)
- **Affected Code**:
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/repositories/UserRepository.java`
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/services/ExpirationScheduledService.java`
- **Affected Tests**:
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/FlywayMigrationTest.java`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/services/ExpirationScheduledServiceTest.java`
- **Documentation**:
  - `readme.md`
