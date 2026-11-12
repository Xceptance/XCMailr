# Proposal

## Why

In the admin users table, deactivated user accounts were unexpectedly disappearing and being permanently deleted from the database after a short period. This occurred because a background maintenance task (`purgeExpiredInactiveUsers` in `ExpirationScheduledService`) was erroneously introduced during modernization to delete inactive accounts with expired confirmation timestamps. In XCMailr, user accounts must never be purged automatically by background jobs; account deletion must strictly be explicit, performed either by an administrator or by the user via self-service.

## What Changes

- **Remove automated user purge job**: Completely eliminate `purgeExpiredInactiveUsers` from `ExpirationScheduledService`, ensuring scheduled background tasks never delete user accounts under any circumstances.
- **Remove user purge repository query**: Remove `findExpiredInactiveUsers` from `UserRepository` as automated user purging is completely retired.
- **Defensive token hygiene on administrative activation**: In `AdminWebController.toggleUserActive`, ensure that when an administrator activates an inactive account, any lingering confirmation token and timestamp are defensively cleared.
- **Retain deactivated users indefinitely**: Guarantee that deactivated users (and unconfirmed registrations) remain in the database and visible in the admin users console (`/admin/users`) until explicitly deleted by an administrator or self-service account deletion.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-console-ui`: Clarify and extend the `Admin Management Console` requirement to mandate that deactivated user accounts remain in the database indefinitely, are visible in the user management table, are never purged by background tasks, and that administrative activation sanitizes lingering confirmation tokens.

## Impact

- **Affected Code**:
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/services/ExpirationScheduledService.java`
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/repositories/UserRepository.java`
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/AdminWebController.java`
- **Affected Tests**:
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/services/ExpirationScheduledServiceTest.java` (add test verifying deactivated and unconfirmed users are preserved across scheduled maintenance runs)
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/AdminWebControllerTest.java` (verify token clearance upon admin activation and user retention)
- **APIs and Database**: No schema migrations needed; no breaking changes to external APIs.
