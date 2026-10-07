# Proposal

## Why

When migrating an existing legacy XCMailr database to the modernized Spring Boot application, users cannot create temporary mailboxes because the domain dropdown in the "Create Temporary Mailbox" modal is completely empty. 

In legacy XCMailr, mailbox domains were configured in `conf/application.conf` (`mbox.dlist`), while the SQL table `register_domains` was merely an optional registration whitelist and remained empty. In the modernized architecture, `MailboxWebController` queries `register_domains` to populate the domain dropdown. While `AdminUserInitializer` was designed to seed the administrator's domain (`xcmailr.test`) into `register_domains`, it currently contains an early `return;` when the administrator account already exists, unintentionally skipping domain seeding on migrated databases.

Additionally, `docs/legacy-migration-local-test-guide.md` currently documents that startup logs should show Flyway already up to date at version 3, which confuses operators during initial cutovers when Flyway actually performs a baseline (version 0) and applies migrations V1–V3 against the legacy database.

## What Changes

- **Fix Early Return in `AdminUserInitializer`**: Ensure `seedDefaultDomain(normalizedMail)` executes unconditionally on application startup (guarded by its existing idempotent `!domainRepository.existsByDomainnameIgnoreCase(domainPart)` check), even when the administrator account already exists.
- **Update Legacy Migration Runbook**: Revise `docs/legacy-migration-local-test-guide.md` to document the expected Flyway output for both initial cutover (baselining at version 0, executing migrations V1–V3) and subsequent restarts (schema version 3 up to date).

## Capabilities

### Modified Capabilities
- `web-console-ui`: Ensure default domain availability in the web UI for temporary mailbox creation across both fresh installations and migrated legacy databases.

## Impact

- **Affected Code**: `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/config/AdminUserInitializer.java`
- **Affected Documentation**: `docs/legacy-migration-local-test-guide.md`
- **Runtime Performance**: Zero impact during request or email handling. Executes once at startup via `ApplicationRunner` with a single fast, indexed query.
- **Backward Compatibility**: Fully backward compatible; existing domains in `register_domains` are never overwritten or duplicated.
