# Design: Cleanup Legacy Ninja Framework Leftovers

## Context

The modernized XCMailr application runs entirely on Java 25, Spring Boot 4.1.1, Spring Data JPA / Hibernate, Spring Security, and Thymeleaf/HTMX. However, the codebase retains the original 2013-2023 Ninja Framework / Ebean / Guice classes, FreeMarker views, legacy assets, and build configurations.

See `proposal.md` for motivation and background.

## Goals / Non-Goals

**Goals:**
- Eliminate all unused legacy Ninja, Guice, and Ebean Java sources, configuration files, views, and assets.
- Remove obsolete build plugins (`ninja-maven-plugin`, `maven-assembly-plugin`) and legacy dependencies from `xcmailr-webapp/pom.xml`.
- Remove unreferenced Ant test suite directory (`xcmailr_testsuite/`) and accidental project duplicates in `xcmailr-client/bin/` and `xcmailr-load-test-suite/bin/`.
- Ensure the modernized Spring Boot application compiles cleanly and 100% of its unit and integration tests pass.

**Non-Goals:**
- Altering or deleting database migration scripts (`xcmailr-webapp/src/main/resources/db/migration/V1..V3.sql`).
- Removing H2 database tools (`bin/export.sh`, `bin/import.sh`, `bin/shell.sh`, `bin/runSqlFile.sh`).
- Moving JPA entities in `models.*` to a new package (to avoid Hibernate schema mapping side-effects).
- Refactoring active Spring REST DTOs (`controllers.restapi.MailboxData`, etc.) or utilities (`etc.HelperUtils`, etc.).
- Adding new application features or modifying existing API endpoints.

## Decisions

### 1. Retention Boundaries for Shared Packages (`models.*`, `etc.*`, `controllers.restapi.*`)
- **Decision**: Remove only the dead handler classes, preserving active entities, DTOs, and utility classes in their existing package locations.
- **Details**:
  - In `controllers/`: Delete `BoxHandler.java`, `AdminHandler.java`, `Application.java`, `UserHandler.java`, `CachingSessionHandler.java`, and the legacy Ninja endpoints in `controllers/restapi/` (`AbstractApiController.java`, `MailApiController.java`, `MailboxApiController.java`).
  - Keep DTOs: `controllers/restapi/MailboxData.java`, `MailData.java`, `util/ApiError.java`, `util/ApiErrors.java`.
  - In `etc/`: Delete `ApiToken.java`, `AttachmentEntry.java`, `HtmlUtils.java`, `MailboxEntry.java`, `MessageComposer.java`, `MigrationEngineFlyway.java`, `StatisticsEntry.java`, `StreamRenderable.java`, `TokenGenerator.java`, `TypeRef.java`.
  - Keep utilities: `etc/HelperUtils.java` and `etc/SizeLimitExceededException.java`.
  - In `models/`: Keep all entities (`User`, `MBox`, `Mail`, `Domain`, `MailTransaction`, `MailStatistics`, `MailStatisticsKey`).
- **Alternative considered**: Move DTOs and utilities into `com.xceptance.xcmailr.*`. Rejected for this change to minimize diff churn and eliminate refactoring regression risks.

### 2. Selective Static Asset Cleanup
- **Decision**: Delete legacy AngularJS, Bootstrap 3 CSS/JS, and Glyphicons halflings from `src/main/resources/assets/`, but retain `assets/ico/favicon.ico`.
- **Rationale**: `src/main/resources/templates/layout/base.html` explicitly references `/assets/ico/favicon.ico`. All other styling and scripting is served from `src/main/resources/static/` via Bootstrap 5 and HTMX.

### 3. Preservation of Database Migration Utilities in `bin/`
- **Decision**: Delete only `bin/run.sh` and `bin/run.cmd` (which boot the dead `main.Main` Ninja class). Retain `bin/export.sh`, `bin/import.sh`, `bin/shell.sh`, `bin/runSqlFile.sh`, and their `.cmd` equivalents.
- **Rationale**: These scripts are database management and migration tools that allow dumping, inspecting, and restoring live H2 database tables during migrations or operational maintenance.

### 4. POM Dependency and Plugin Pruning
- **Decision**: Remove the following from `xcmailr-webapp/pom.xml`:
  - Plugins: `ninja-maven-plugin`, `maven-assembly-plugin` (and delete `assembly.xml`).
  - Dependencies: `ninja-standalone`, `ninja-db-classic`, `ninja-test-utilities`, `io.ebean:ebean`, `io.ebean:ebean-ddl-generator`, `org.apache.commons:commons-email`.
  - Surefire/Failsafe configuration: Remove `-Dlogback.configurationFile=conf/logback_dev.xml` and `<ninja.mode>test</ninja.mode>`.

## Risks / Trade-offs

| Risk | Mitigation |
| :--- | :--- |
| **Hidden Classpath Reference**: A Spring service or test indirectly imports a legacy class scheduled for deletion. | Comprehensive AST / grep scan of all classes in `com.xceptance.xcmailr` prior to deletion; verified with `mvn clean test`. |
| **Missing Favicon or Static Asset**: Deleting `assets/` breaks the browser icon. | Retain `src/main/resources/assets/ico/favicon.ico` and verify resolution via `WebMvcConfig`. |
| **Loss of H2 Migration Utilities**: Deleting `bin/` leaves administrators without export/import capability for old databases. | Explicitly preserve `export.sh`, `import.sh`, `shell.sh`, and `runSqlFile.sh`. |

