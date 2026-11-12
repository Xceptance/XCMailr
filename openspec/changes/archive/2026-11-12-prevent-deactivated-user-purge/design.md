# Design

## Context

In modern XCMailr, `ExpirationScheduledService.runMaintenanceTasks()` executes periodically (every minute by default) and calls `purgeExpiredInactiveUsers(now)`. This method finds all user accounts with `active = false`, `confirmation IS NOT NULL`, and `ts_confirm` older than `xcmailr.app.confirmation-period-hours` via `userRepository.findExpiredInactiveUsers(cutoff)` and deletes them via `userRepository.deleteAll(expiredUsers)`.

Because `User` defines `cascade = CascadeType.ALL` on its associated mailboxes, deleting an inactive user permanently purges the user, all their mailboxes, and all stored emails. In legacy XCMailr, user accounts were never automatically purged by background jobs; accounts were preserved indefinitely until explicitly deleted by an administrator (`AdminHandler.deleteUserProcess`) or by self-service deletion (`UserHandler.deleteUserProcess`).

## Goals / Non-Goals

**Goals:**
- Completely eliminate background automated user account deletion by removing `purgeExpiredInactiveUsers` from `ExpirationScheduledService`.
- Remove `findExpiredInactiveUsers` query method from `UserRepository`.
- Ensure deactivated user accounts remain in the database and visible in `/admin/users` indefinitely.
- Implement defensive token sanitation in `AdminWebController.toggleUserActive` so that administratively activating an account clears lingering confirmation tokens and expiration timestamps.
- Add regression tests verifying that scheduled maintenance runs never delete active or inactive users.

**Non-Goals:**
- Changing token expiration validation during self-service confirmation link redemption (`WebAuthController.confirmAccount` continues to reject expired token links).
- Altering explicit user deletion functionality in `AdminWebController.deleteUser` or `ProfileWebController.deleteAccountProcess`.
- Modifying mailbox expiration (`expireMailboxes`), API token expiration (`expireUserTokens`), or email retention cleanup (`purgeExpiredMails`).

## Decisions

### Decision 1: Complete removal of automated user deletion vs. configurable toggle
- **Choice**: Completely remove `purgeExpiredInactiveUsers` from `ExpirationScheduledService` and `findExpiredInactiveUsers` from `UserRepository`.
- **Rationale**: Automated user deletion was an unintentional addition during modernization. Legacy XCMailr had no such job. User account records should only be deleted through deliberate user or administrator action. Retaining a flag or dormant code introduces ongoing risk of accidental account loss.
- **Alternatives Considered**: Making user purging opt-in via a configuration toggle (e.g., `xcmailr.app.purge-inactive-users: false`). Rejected because automated account purging is not a supported concept in XCMailr.

### Decision 2: Preserving `confirmation-period-hours` for token validity only
- **Choice**: Keep `confirmationPeriodHours` in `XcmailrProperties` and `application.yml`.
- **Rationale**: This property controls the validity duration of email confirmation tokens during registration. When a user clicks an activation link after the period expires, `WebAuthController` rejects the activation. The property remains useful for this purpose, but will never trigger user record deletion.

### Decision 3: Defensive token hygiene on administrative activation
- **Choice**: In `AdminWebController.toggleUserActive`, when an administrator toggles an account from inactive to active, set `confirmation = null` and `ts_confirm = null`.
- **Rationale**: If an unconfirmed or deactivated account has stale confirmation tokens or timestamps, administratively activating it should make it immediately usable and clear the pending confirmation state.

## Risks / Trade-offs

- **[Unconfirmed registration accumulation]** → Users who register but never confirm their email will remain in the database as inactive accounts indefinitely.
  - *Mitigation*: Administrators can view all inactive accounts in `/admin/users` and explicitly delete them using the existing delete action. Inactive user records have negligible storage footprint.
- **[No automatic mailbox cleanup for abandoned accounts]** → Mailboxes belonging to deactivated users will not be automatically deleted.
  - *Mitigation*: This is the intended behavior matching legacy XCMailr. Mailboxes for inactive users expire according to their own expiration rules (`ts_Active`), but user records and mailbox configurations are preserved.

## Migration Plan

- No database migrations or schema alterations required.
- Deployment takes immediate effect; once deployed, running background scheduled tasks will never delete user records.
- Rollback strategy: Standard application rollback if necessary.
