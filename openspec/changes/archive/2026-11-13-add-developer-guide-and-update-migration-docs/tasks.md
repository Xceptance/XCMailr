# Tasks

## 1. Create Concise Developer Guide (`docs/developer-guide.md`)

- [x] 1.1 Create `docs/developer-guide.md` covering 2-Minute Local Setup (Java 25, Maven 3.9+, no-SMTP vs. Mailpit `dev` mode, default credentials, H2 DB reset), Codebase & Package Map (`xcmailr-webapp`, `xcmailr-client`, `xcmailr-load-test-suite`, templates, i18n, Flyway migrations), Step-by-Step Feature & Bugfix Workflow, Coding Standards & Security Checklist (`final` modifiers, Javadoc, OWASP access control/CSRF/redaction), and Testing Pyramid (TDD) & Commands, and verify the file exists and all internal markdown links resolve.

## 2. Update Schema Migration Guide & Related Migration Docs to `V6`

- [x] 2.1 Update `docs/schema-migration-guide.md` to add `V5__Add_Store_Count_To_Mail_Statistics.sql` and `V6__Clean_Legacy_Confirmation_Tokens.sql` to the Current Migration History table, advance the next migration template and step-by-step examples from `V5` to `V7` (migrating from schema version `6` to `7`), and update entity package paths from `models.*` to `com.xceptance.xcmailr.models.*`, and verify no stale `V5__<Your_Feature>` or `java/models/` references remain.
- [x] 2.2 Update `docs/data-compatibility-and-safety.md` and `docs/legacy-migration-local-test-guide.md` to reference the `V1`–`V6` Flyway migration sequence (including `V5` `STORE_COUNT` and `V6` legacy confirmation token cleanup) and `com.xceptance.xcmailr.models.*` entity paths, and verify all file links resolve.
- [x] 2.3 Update `readme.md` under **Development & Testing Guides** to link `docs/developer-guide.md` and `docs/schema-migration-guide.md`, and verify the markdown links match the files in `docs/`.
