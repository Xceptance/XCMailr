# Proposal

## Why

With the addition of Flyway migration `V4__Add_Mailbox_Comment.sql` (schema version 4), existing documentation runbooks, schema migration guides, data safety tables, and the Flyway migration smoke test still reference schema version 3 and migrations `V1..V3`. Updating these runbooks and test assertions ensures that developers and operators performing local legacy database migrations or future schema migrations have accurate log examples, correct baseline expectations, and aligned verification checklists.

## What Changes

- **Update Local Migration Runbook (`docs/legacy-migration-local-test-guide.md`)**:
  - Update sample application startup log snippet to show 4 migrations validated and applied (`V1` through `V4`) resulting in schema version `v4`.
  - Update subsequent restart log snippet showing schema version 4 is up-to-date with 0 migrations to apply.
  - Add test address comment preservation and optionality to the post-migration cutover verification steps.
- **Update Schema Migration Guide (`docs/schema-migration-guide.md`)**:
  - Add `V4__Add_Mailbox_Comment.sql` to the migration inventory table (schema version 4, adds nullable `comment` column to `mailboxes`).
  - Advance the next available migration template from `V4__<Your_Feature>.sql` to `V5__<Your_Feature>.sql`.
  - Update sample Flyway log examples from version 3 to version 4.
- **Update Data Compatibility & Safety Guide (`docs/data-compatibility-and-safety.md`)**:
  - Update Flyway version range from `V1–V3` to `V1–V4`.
  - Add the `comment` field (`VARCHAR(255)`, nullable) to the `models.MBox` schema mapping table.
- **Update Flyway Migration Test (`xcmailr-webapp/src/test/java/com/xceptance/xcmailr/FlywayMigrationTest.java`)**:
  - Update test method display name, Javadoc, and assertion to verify that at least 4 migrations (`V1`..`V4`) are applied on clean database initialization.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
None.

## Impact

- **Documentation**: `docs/legacy-migration-local-test-guide.md`, `docs/schema-migration-guide.md`, `docs/data-compatibility-and-safety.md`.
- **Tests**: `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/FlywayMigrationTest.java`.
- **APIs & Runtime Behavior**: No runtime code, database scripts, or REST endpoints are modified; this is purely documentation and migration test alignment.

