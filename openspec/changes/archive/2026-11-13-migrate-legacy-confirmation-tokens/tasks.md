# Tasks

## 1. Flyway `V6` Legacy Confirmation Token Migration (`V6__Clean_Legacy_Confirmation_Tokens.sql`)

- [x] 1.1 Write failing tests in `FlywayMigrationTest` (using `final` modifiers, Javadoc, and code comments) asserting that `V6__Clean_Legacy_Confirmation_Tokens.sql` is applied by Flyway (`appliedMigrations.length >= 6`) and verifying the `V6` migration SQL across happy paths (active legacy user with `confirmation` and `ts_confirm` has both cleared to `NULL`), special cases (inactive legacy user owning a mailbox has `confirmation` and `ts_confirm` cleared to `NULL`, whereas an inactive unconfirmed user without mailboxes retains `confirmation` and `ts_confirm`), and boundary/error cases (users already having `NULL` tokens remain unchanged), and verify the tests fail before `V6__Clean_Legacy_Confirmation_Tokens.sql` is added (`mvn test -pl xcmailr-webapp -Dtest=FlywayMigrationTest`).
- [x] 1.2 Create `xcmailr-webapp/src/main/resources/db/migration/V6__Clean_Legacy_Confirmation_Tokens.sql` with `UPDATE users SET confirmation = NULL, ts_confirm = NULL WHERE active = TRUE OR EXISTS (SELECT 1 FROM mailboxes WHERE mailboxes.usr_id = users.id);` and SQL comments explaining the legacy token cleanup, and verify `mvn test -pl xcmailr-webapp -Dtest=FlywayMigrationTest` passes.

## 2. Simplify `UserRepository.findExpiredInactiveUsers` & Update Documentation

- [x] 2.1 Update tests in `ExpirationScheduledServiceTest` (TDD: happy path, boundary/error cases, and special cases, using `final` modifiers, Javadoc, and code comments) to verify that `UserRepository.findExpiredInactiveUsers` no longer filters on `u.boxes IS EMPTY` at runtime while confirming that legacy active and mailbox-owning deactivated accounts sanitized by `V6` (`confirmation = null, ts_confirm = null`) and admin-deactivated accounts remain protected from `purgeExpiredInactiveUsers`, and verify the updated repository query test fails prior to updating `UserRepository` (`mvn test -pl xcmailr-webapp -Dtest=ExpirationScheduledServiceTest`).
- [x] 2.2 Remove `AND u.boxes IS EMPTY` from `UserRepository.findExpiredInactiveUsers`, update Javadoc and inline comments on `UserRepository` and `ExpirationScheduledService`, update `readme.md` to document the `V6__Clean_Legacy_Confirmation_Tokens.sql` migration and simplified cleanup query, and verify `mvn test -pl xcmailr-webapp -Dtest=ExpirationScheduledServiceTest` passes.

## 3. Full Test Pyramid Verification

- [x] 3.1 Execute the full test suite (`mvn test`) across all modules and verify all unit, WebMvc, and integration tests pass with zero regressions.
