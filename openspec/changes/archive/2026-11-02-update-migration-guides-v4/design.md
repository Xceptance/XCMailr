# Design

## Context

Following the implementation of the mailbox comment feature, Flyway migration script `V4__Add_Mailbox_Comment.sql` is active in the repository. The application schema version is now `4`. However, developer and operator runbooks (`docs/legacy-migration-local-test-guide.md`, `docs/schema-migration-guide.md`, and `docs/data-compatibility-and-safety.md`) and the automated migration smoke test (`FlywayMigrationTest.java`) still document and assert schema version 3 (`V1..V3`). See `proposal.md` for motivation.

## Goals / Non-Goals

**Goals:**
- Update all documentation runbooks to reflect schema version 4 and migrations `V1`–`V4`.
- Provide verbatim console log output from Spring Boot 3.5 / Flyway 11 in the local migration testing guide.
- Update the next available migration version template from `V4` to `V5` in `docs/schema-migration-guide.md`.
- Document the `comment` column in `docs/data-compatibility-and-safety.md` as a non-destructive, nullable addition.
- Update `FlywayMigrationTest.java` to verify that at least 4 migrations (`V1`–`V4`) succeed on application startup.

**Non-Goals:**
- Modifying SQL migration scripts (`V1` through `V4`) or introducing a new `V5` migration.
- Modifying entity models, repositories, controllers, or web templates.
- Altering legacy migration test data or git worktree scripts.

## Decisions

### Decision 1: Use Authentic Spring Boot 3.5 / Flyway 11 Log Snippets
In `docs/legacy-migration-local-test-guide.md`, replace the schema version 3 console log snippet with the actual runtime output observed during cutover testing:
```text
o.f.core.internal.command.DbValidate     : Successfully validated 4 migrations (execution time 00:00.007s)
o.f.core.internal.command.DbBaseline     : Creating Schema History table "PUBLIC"."flyway_schema_history" with baseline ...
o.f.core.internal.command.DbBaseline     : Successfully baselined schema with version: 0
o.f.core.internal.command.DbMigrate      : Current version of schema "PUBLIC": 0
o.f.core.internal.command.DbMigrate      : Migrating schema "PUBLIC" to version "1 - Initial Setup"
o.f.core.internal.command.DbMigrate      : Migrating schema "PUBLIC" to version "2 - Change Mail Message Type"
o.f.core.internal.command.DbMigrate      : Migrating schema "PUBLIC" to version "3 - Remove Mail2MailBox Reference"
o.f.core.internal.command.DbMigrate      : Migrating schema "PUBLIC" to version "4 - Add Mailbox Comment"
o.f.core.internal.command.DbMigrate      : Successfully applied 4 migrations to schema "PUBLIC", now at version v4
```
For subsequent restarts:
```text
o.f.core.internal.command.DbMigrate      : Current version of schema "PUBLIC": 4
o.f.core.internal.command.DbMigrate      : Schema "PUBLIC" is up to date. No migration necessary.
```
*Rationale*: Verbatim logs prevent operators from suspecting an anomaly or unexpected migration behavior when they see version 4.

### Decision 2: Advance Schema Migration Guide Template to Version 5
In `docs/schema-migration-guide.md`:
- Add `V4__Add_Mailbox_Comment.sql` to the migration history table:
  - File: `V4__Add_Mailbox_Comment.sql`
  - Purpose: Added optional `comment` column (`VARCHAR(255)`) to `mailboxes` table using non-destructive `IF NOT EXISTS` DDL.
- Update the next available migration version from `V4__<Your_Feature>.sql` to `V5__<Your_Feature>.sql`.
- Update the step-by-step example in Section 3 from `V4__Add_Mailbox_Notes.sql` to `V5__Add_Mailbox_Notes.sql`.
- Update log snippets and deployment behavior descriptions to show transitions from version 4 to version 5.

*Rationale*: Keeps the guide actionable and prevents developers from inadvertently trying to create a conflicting `V4` migration.

### Decision 3: Document `comment` Column in Data Compatibility & Safety Guide
In `docs/data-compatibility-and-safety.md`:
- Update Section 1 ASCII diagram to state `V1-V4 migration chain; non-destructive schema evolution`.
- Update Section 3 migration inventory to include `V4__Add_Mailbox_Comment.sql`.
- Clarify existing database behavior: legacy production databases at version 3 will auto-apply V4 safely via `IF NOT EXISTS` without locking or overwriting data; databases at version 4 execute zero DDL statements.
- Update Table 4 (`models.MBox` column mapping) to document `comment` (`VARCHAR(255)`, nullable, added in V4).

*Rationale*: Maintains complete documentation of data safety and schema invariants for compliance and audits.

### Decision 4: Update `FlywayMigrationTest.java` Assertion to `>= 4`
In `FlywayMigrationTest.java`:
- Update Javadoc comment to mention `{@code V1}, {@code V2}, {@code V3}, and {@code V4}`.
- Update `@DisplayName` to `"Verify Flyway migrations V1 through V4 are applied and valid"`.
- Update assertion: `assertTrue(appliedMigrations.length >= 4, "At least 4 migrations (V1, V2, V3, V4) must be applied");`.

*Rationale*: Verifies that the complete migration sequence executes cleanly on startup while retaining `>=` to ensure forward compatibility with future migrations.

## Risks / Trade-offs

- **[Risk]** Log timestamps and thread names vary across environments.
  - **Mitigation**: Retain normalized log prefixes in documentation (e.g. `o.f.core.internal.command.DbMigrate`) without machine-specific PID or timestamp headers.
- **[Risk]** Developers executing migrations against uncleaned legacy test databases.
  - **Mitigation**: The local test guide's existing Phase 1 commands (`mvn clean`, clean worktree) ensure reproducible baseline testing.

