# Tasks

## 1. Domain Seeding Implementation and Unit Tests

- [x] 1.1 In `AdminUserInitializer.java`, call `seedDefaultDomain(normalizedMail)` before returning when an existing administrator account is found, and verify completion by inspecting the code structure
- [x] 1.2 In `AdminUserInitializerTest.java`, add a unit test `testSeedsDomainWhenAdminAlreadyExistsWithoutDomain()` verifying that an existing admin account with an empty `register_domains` table successfully receives the default domain without mutating account credentials, and verify by running `mvn test -pl xcmailr-webapp -Dtest=AdminUserInitializerTest`
- [x] 1.3 Run full `AdminUserInitializerTest` test suite to verify no regressions across empty database, existing account, and disaster recovery scenarios via `mvn test -pl xcmailr-webapp -Dtest=AdminUserInitializerTest`

## 2. Documentation and Verification

- [x] 2.1 In `docs/legacy-migration-local-test-guide.md`, update Checkpoint 1 to detail expected Flyway console logs for both initial migration cutover (baselining at version 0, applying migrations V1..V3) and subsequent application restarts (schema version 3 up to date), and verify markdown clarity
- [x] 2.2 Verify end-to-end against the local legacy test database that the application starts, seeds `xcmailr.test` into `register_domains`, and renders `xcmailr.test` in the mailbox creation domain dropdown without errors
