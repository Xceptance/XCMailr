# Tasks

## 1. Documentation Updates

- [x] 1.1 Update `docs/legacy-migration-local-test-guide.md` with Flyway v4 startup and restart logs, and add comment field verification to post-cutover checkpoints
- [x] 1.2 Update `docs/schema-migration-guide.md` to add `V4__Add_Mailbox_Comment.sql` to the migration history table, advance next migration template to `V5`, and update sample logs
- [x] 1.3 Update `docs/data-compatibility-and-safety.md` to reference migrations `V1–V4` and include `comment` (`VARCHAR(255)`, nullable) in the `models.MBox` column mapping table

## 2. Test Updates & Verification

- [x] 2.1 Update `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/FlywayMigrationTest.java` display name, Javadoc, and assertion to require at least 4 migrations (`V1`–`V4`)
- [x] 2.2 Execute `mvn test -pl xcmailr-webapp -Dtest=FlywayMigrationTest` and verify that the test suite passes cleanly with 4 applied migrations

