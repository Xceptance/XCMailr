# Proposal

## Why

Developers contributing new features or bug fixes to XCMailr currently lack a single, concise developer onboarding and reference guide covering local startup modes, codebase package layout, implementation workflows, coding standards, and test pyramid practices. Additionally, existing database migration documentation (`docs/schema-migration-guide.md`, `docs/data-compatibility-and-safety.md`, and `docs/legacy-migration-local-test-guide.md`) still references schema version `V4` and legacy `models.*` package paths rather than the current `V1`–`V6` migration sequence and `com.xceptance.xcmailr.models.*` package structure.

## What Changes

- **Create Concise Developer Guide (`docs/developer-guide.md`)**:
  - **2-Minute Local Setup**: Prerequisites (Java 25, Maven 3.9+, optional Docker), running locally without SMTP (`APP_REQUIRE_CONFIRMATION=false`) vs. running with Mailpit (`docker-compose.dev.yml` + `dev` profile), default administrator credentials and API token, and resetting or running an in-memory H2 database.
  - **Codebase & Package Map**: Overview of Maven modules (`xcmailr-webapp`, `xcmailr-client`, `xcmailr-load-test-suite`), backend package layers (`config`, `controllers`, `services`, `repositories`, `models`, `dto`, `security`, `util`), frontend Thymeleaf + HTMX + Bootstrap 5.3 templates, `messages.properties` / `messages_de.properties` localization bundles, and `db/migration/` Flyway scripts.
  - **Step-by-Step Workflow for Features & Bug Fixes**: Concise recipe covering database/schema changes (`V7__*.sql`), JPA entities/repositories/services, web controllers & HTMX fragments with EN/DE localization, and REST API endpoints + `xcmailr-client` synchronization.
  - **Coding Standards & Security Checklist**: Using `final` on parameters and local variables, Javadoc and code comments, OWASP access control (`@PreAuthorize`, tenant ownership checks, HTMX CSRF headers), and log redaction.
  - **Testing Pyramid (TDD) & Commands**: Writing tests first across happy paths, error/boundary cases, and special/security cases; running single test classes vs. the full test suite (`mvn test`).
- **Update Schema Migration Guide (`docs/schema-migration-guide.md`)**:
  - Add `V5__Add_Store_Count_To_Mail_Statistics.sql` and `V6__Clean_Legacy_Confirmation_Tokens.sql` to the Current Migration History table and advance the next available migration version to `V7__<Your_Feature>.sql`.
  - Update step-by-step migration examples and sample Flyway log output from `V5` to `V7` (migrating from schema version `6` to `7`).
  - Update entity package references from `models.*` to `com.xceptance.xcmailr.models.*`.
- **Align Migration Version References in Related Guides & README**:
  - Update `docs/data-compatibility-and-safety.md` and `docs/legacy-migration-local-test-guide.md` to reflect migrations `V1`–`V6` and `com.xceptance.xcmailr.models.*`.
  - Link `docs/developer-guide.md` and `docs/schema-migration-guide.md` under **Development & Testing Guides** in `readme.md`.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
None.

## Impact

- **Documentation**:
  - `docs/developer-guide.md` (new)
  - `docs/schema-migration-guide.md`
  - `docs/data-compatibility-and-safety.md`
  - `docs/legacy-migration-local-test-guide.md`
  - `readme.md`
- **Runtime Code & APIs**: None (documentation-only change; `skip_specs: true`).
