# Design

## Context

See `proposal.md` for motivation. Currently:
- `docs/` contains operational and specialized testing guides (`local-testing-guide.md`, `cloud-test-deployment-guide.md`, `load-testing-guide.md`, `production-deployment-guide.md`, `schema-migration-guide.md`, `data-compatibility-and-safety.md`, `legacy-migration-local-test-guide.md`), but no concise developer guide for writing features or fixing bugs.
- Flyway migrations `V5__Add_Store_Count_To_Mail_Statistics.sql` and `V6__Clean_Legacy_Confirmation_Tokens.sql` and the package reorganization (`models.*` -> `com.xceptance.xcmailr.models.*`) were completed after `docs/schema-migration-guide.md` was last updated.

## Goals / Non-Goals

**Goals:**
- Provide a short, high-signal `docs/developer-guide.md` (~150–200 lines) covering local startup, codebase architecture, end-to-end feature/bugfix workflow, coding & security conventions, and TDD test pyramid commands.
- Update `docs/schema-migration-guide.md`, `docs/data-compatibility-and-safety.md`, and `docs/legacy-migration-local-test-guide.md` to document migrations `V1`–`V6` (with `V7` as the next available migration version) and `com.xceptance.xcmailr.models.*`.
- Link `docs/developer-guide.md` and `docs/schema-migration-guide.md` in `readme.md`.

**Non-Goals:**
- Including AI agent or OpenSpec slash-command workflows in `docs/developer-guide.md`.
- Duplicating the full Mailpit or cloud deployment walkthroughs already covered in `docs/local-testing-guide.md` and `docs/cloud-test-deployment-guide.md` (link to them instead).
- Modifying any Java source code, SQL scripts, or configuration files.

## Decisions

### 1. Concise 5-Section Structure for `docs/developer-guide.md`

**Decision:**
Organize `docs/developer-guide.md` into five scannable sections:
1. **Quick Start (2 Minutes)**:
   - Prerequisites: Java 25, Maven 3.9+, Docker (optional for Mailpit).
   - Fast local mode (no SMTP needed): `APP_REQUIRE_CONFIRMATION=false mvn spring-boot:run -pl xcmailr-webapp`.
   - Full email mode (with Mailpit): `docker compose -f docker-compose.dev.yml up -d` + `mvn spring-boot:run -pl xcmailr-webapp -Dspring-boot.run.profiles=dev`.
   - Default credentials (`admin@xcmailr.test` / `1234`, API token `xcmailr-admin-api-token-2026`), ports (`8080` Web/API, `25000` Inbound SMTP, `8025` Mailpit UI), and H2 database reset (`rm -f ~/xcmailrDB.*` or in-memory override).
2. **Codebase & Package Map**:
   - Maven modules (`xcmailr-webapp`, `xcmailr-client`, `xcmailr-load-test-suite`).
   - Webapp packages under `com.xceptance.xcmailr`: `config`, `controllers`, `services`, `repositories`, `models`, `dto`, `security`, `util`.
   - Resources: `templates/` (Thymeleaf + HTMX + Bootstrap 5.3), `static/css/` & `static/js/`, `assets/i18n/messages.properties` & `messages_de.properties`, and `db/migration/V1..V6__*.sql`.
3. **Workflow: Writing a Feature or Fixing a Bug**:
   - **Database / Schema**: Add `V<N>__<Description>.sql` (next is `V7`), update entity in `com.xceptance.xcmailr.models`, verify against `ddl-auto: validate` (linking to `docs/schema-migration-guide.md`).
   - **Backend Logic**: Add repository query in `repositories/`, business logic and `@Transactional` boundaries in `services/`.
   - **Web UI & HTMX**: Add controller handler in `controllers/`, render full page or HTMX fragment in `templates/`, add all user-facing strings to both `messages.properties` (EN) and `messages_de.properties` (DE).
   - **REST API**: Update REST controller (`MailboxApiController` / `MailApiController`) and keep `xcmailr-client` DTOs/methods in sync.
4. **Coding Standards & Security Checklist**:
   - `final` on all method parameters and local variables.
   - Javadoc on classes and public/protected methods; inline comments on non-trivial logic.
   - Security guardrails: `@PreAuthorize("hasRole('ADMIN')")` on admin routes, strict tenant ownership checks (`mailbox.getUser().getId() == currentUser.getId()`), CSRF tokens on state-changing requests, anti-enumeration on auth endpoints, and never logging tokens/passwords.
5. **Testing Pyramid (TDD) & Commands**:
   - Write tests first covering **Happy Path**, **Error / Boundary Cases**, and **Special / Security Cases**.
   - Layers: Unit tests (services/utilities), `@WebMvcTest` controller slice tests, `@SpringBootTest` integration tests (`FlywayMigrationTest`, `RestApiIntegrationTest`, `SmtpDeliveryIntegrationTest`, etc.).
   - Commands for running a single test class, a single test method, or the full reactor test suite.

### 2. Update `docs/schema-migration-guide.md` (and Related Migration Docs) to `V6`

**Decision:**
- In `docs/schema-migration-guide.md`:
  - Add `V5__Add_Store_Count_To_Mail_Statistics.sql` (`Added STORE_COUNT column (INT DEFAULT 0) to MAIL_STATISTICS table`) and `V6__Clean_Legacy_Confirmation_Tokens.sql` (`Cleared historical confirmation tokens and timestamps on active users and mailbox-owning inactive users`) to the Current Migration History table.
  - Advance the next available version placeholder to **`V7__<Your_Feature>.sql`** and update the example filenames and Flyway log snippets from `V5` (v4 -> v5) to `V7` (v6 -> v7).
  - Replace legacy `models.*` (`xcmailr-webapp/src/main/java/models/`) references with `com.xceptance.xcmailr.models.*` (`xcmailr-webapp/src/main/java/com/xceptance/xcmailr/models/`).
- In `docs/data-compatibility-and-safety.md` and `docs/legacy-migration-local-test-guide.md`:
  - Update the migration chain references from `V1`–`V4` to `V1`–`V6` (including `STORE_COUNT` on `MailStatistics` and `V6` legacy confirmation token cleanup) and entity package references from `models.*` to `com.xceptance.xcmailr.models.*`.

## Risks / Trade-offs

- **[Risk] Developer guide grows too long over time** → **Mitigation**: Keep `docs/developer-guide.md` focused on actionable checklists and commands (~150–200 lines) and link out to specialized guides (`local-testing-guide.md`, `schema-migration-guide.md`, `load-testing-guide.md`) for deep dives.
