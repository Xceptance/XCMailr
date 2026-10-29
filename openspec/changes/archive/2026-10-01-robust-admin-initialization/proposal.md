# Proposal

## Why

During application startup, administrative account initialization must be non-destructive across all deployment environments (development, testing, and production). The system must allow initial administrator bootstrapping on fresh databases and disaster recovery if an administrator is accidentally deleted, while strictly guaranteeing that existing user credentials and permissions are never overwritten or altered.

## What Changes

- **Credential Presence Guard First**: Before performing any database lookups, check whether an administrator email and password are configured. If either property (`xcmailr.admin.address` or `xcmailr.admin.password`) is missing or blank, the system immediately skips initialization without executing any database queries.
- **Non-Destructive Initialization**: When credentials are provided, remove the aggressive credential overwrite in `AdminUserInitializer`. If an account for the configured administrator address already exists in the database, the system leaves it completely untouched, preserving user-updated passwords and permissions.
- **Absence-Only Bootstrapping**: If and only if valid credentials are configured and the administrator account does not exist in the database (such as on a fresh deployment or after an accidental deletion), create the administrative account and seed the default domain if absent.
- **Comprehensive Test Pyramid**: Implement unit tests, component tests, and integration tests covering happy path (fresh database bootstrap), error cases (missing/blank credentials skipping DB queries), and edge cases (existing user with customized password preserved, recovery after account deletion).
- **Code Standards**: Apply `final` modifiers wherever possible, add full Javadoc documentation, and include clear code comments.
- **README Documentation**: Provide clear, dedicated documentation in `README.md` covering initial administrator setup, environment variable configuration (`ADMIN_ADDRESS`, `ADMIN_PASSWORD`), non-destructive behavior, production security recommendations, and disaster recovery procedures.

## Capabilities

### Modified Capabilities
- `security-hardening`: Add requirement for non-destructive administrative credential bootstrapping and disaster recovery.

## Impact

- **Affected Code**: `com.xceptance.xcmailr.config.AdminUserInitializer`
- **Affected Tests**: `com.xceptance.xcmailr.config.AdminUserInitializerTest`, `com.xceptance.xcmailr.controllers.WebAuthControllerTest`
- **Affected Documentation**: `README.md` (new section on Administrator Account Management & Disaster Recovery)
- **Configuration**: Preserves standard `xcmailr.admin.*` properties in `application.yml` and environment variables without requiring custom profile flags.
- **Breaking Changes**: None.
