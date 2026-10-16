# Proposal: Cleanup Legacy Ninja Framework Leftovers

## Why

During the modernization of XCMailr to Java 25, Spring Boot 4.1.1, Spring Data JPA, and Thymeleaf/HTMX, the new architecture was implemented alongside the original Ninja Framework / Ebean / Guice codebase. As a result, extensive dead code, obsolete FreeMarker templates, legacy AngularJS/Bootstrap 3 assets, unreferenced Ant build scripts, and unused Maven dependencies remain in the repository.

Cleaning up these obsolete leftovers eliminates technical debt and developer confusion, reduces package size, and streamlines the build without altering runtime behavior or breaking database compatibility.

## What Changes

- **Legacy Web & Dependency Injection Code Removal**:
  - Delete `xcmailr-webapp/src/main/java/filters/` (`AdminFilter`, `SecureFilter`, `WhitelistFilter`, etc.).
  - Delete `xcmailr-webapp/src/main/java/controllers/` legacy handlers (`BoxHandler`, `AdminHandler`, `Application`, `UserHandler`, `CachingSessionHandler`, legacy `restapi/*ApiController`).
  - Delete `xcmailr-webapp/src/main/java/conf/` (`Routes.java`, `Module.java`, `ServletModule.java`, `XCMailrConf.java`, `JacksonSetup.java`).
  - Delete `xcmailr-webapp/src/main/java/main/Main.java` (legacy NinjaJetty runner).
  - Delete `xcmailr-webapp/src/main/java/ninja/` (`ninja/ebean/*`).
  - Delete `xcmailr-webapp/src/main/java/services/` (`ExpirationService`, `MessageListener`, `MailService`, `MailrMessageSenderFactory`, `CheckDBForMailAddressDuplicates`).
- **Legacy Frontend & Configuration Removal**:
  - Delete `xcmailr-webapp/src/main/resources/views/` (all 31 legacy FreeMarker `.ftl.html` templates).
  - Delete unused legacy static assets in `xcmailr-webapp/src/main/resources/assets/` (`css/`, `js/`, `fonts/`, `img/`), while preserving `assets/ico/favicon.ico` referenced by `templates/layout/base.html`.
  - Delete `xcmailr-webapp/src/main/resources/ehcache.xml`.
  - Delete legacy Ninja configuration files (`xcmailr-webapp/conf/` and `xcmailr-webapp/src/main/resources/conf/`).
  - Delete `xcmailr-webapp/bin/run.sh` and `xcmailr-webapp/bin/run.cmd` (legacy Ninja runner scripts).
- **Build & Dependency Cleanup**:
  - Remove `ninja-maven-plugin` and `maven-assembly-plugin` (with `assembly.xml`) from `xcmailr-webapp/pom.xml`.
  - Remove obsolete dependencies from `xcmailr-webapp/pom.xml`: `ninja-standalone`, `ninja-db-classic`, `ninja-test-utilities`, `io.ebean:ebean`, `io.ebean:ebean-ddl-generator`, `org.apache.commons:commons-email`.
  - Delete `xcmailr-webapp/assembly.xml`.
- **Legacy Test Suite & Duplicate Directory Cleanup**:
  - Delete legacy tests in `xcmailr-webapp/src/test/java/` that depended on Ninja (`controllers/*Test`, `services/*Test`, `testutils/StaticNinjaTest.java`, etc.).
  - Delete unreferenced Ant test suite directory `xcmailr_testsuite/`.
  - Delete accidental duplicate project directories `xcmailr-client/bin/` and `xcmailr-load-test-suite/bin/`.
- **Preserved Artifacts (Migration & Compatibility Guardrails)**:
  - **KEEP** all database migration scripts: `xcmailr-webapp/src/main/resources/db/migration/` (`V1`, `V2`, `V3`).
  - **KEEP** H2 database utility scripts: `xcmailr-webapp/bin/` (`export.sh`, `import.sh`, `shell.sh`, `runSqlFile.sh`, `.cmd` equivalents) to facilitate migration/dumps of live H2 data.
  - **KEEP** all JPA entities in `models/` (`User`, `MBox`, `Mail`, `Domain`, `MailTransaction`, `MailStatistics`, `MailStatisticsKey`).
  - **KEEP** DTOs in `controllers.restapi.*` (`MailboxData`, `MailData`, `ApiError`, `ApiErrors`) and utility classes in `etc.*` (`HelperUtils`, `SizeLimitExceededException`) used by active Spring Boot services.

## Capabilities

### New Capabilities
None. This is an architectural cleanup and dead code removal change.

### Modified Capabilities
None. No spec-level requirements or externally observable behaviors are modified (`skip_specs: true` declared).

## Impact

- **Build Time & Dependency Surface**: Strips unnecessary transitive dependencies and obsolete plugins from Maven builds.
- **Runtime Footprint**: Eliminates dead classes, unused static files, and orphaned templates from the packaged jar.
- **Backward Compatibility**: Fully preserved. Database migrations, JPA entity mappings, and H2 export/import tools are kept intact.
- **Verification**: All modern Spring Boot unit and integration tests under `com.xceptance.xcmailr.*` will continue to pass.

