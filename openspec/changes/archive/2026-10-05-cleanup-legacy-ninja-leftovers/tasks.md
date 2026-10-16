# Tasks: Cleanup Legacy Ninja Framework Leftovers

## 1. Legacy Java Web & DI Layer Cleanup

- [x] 1.1 Remove dead Ninja filter classes in `xcmailr-webapp/src/main/java/filters/` and verify package directory is removed
- [x] 1.2 Remove legacy controller handlers (`BoxHandler.java`, `AdminHandler.java`, `Application.java`, `UserHandler.java`, `CachingSessionHandler.java`, and legacy `restapi/*ApiController.java`) while preserving DTOs (`MailboxData.java`, `MailData.java`, `util/ApiError.java`, `util/ApiErrors.java`)
- [x] 1.3 Remove legacy Ninja route and Guice DI bindings in `xcmailr-webapp/src/main/java/conf/` (`Routes.java`, `Module.java`, `ServletModule.java`, `XCMailrConf.java`, `JacksonSetup.java`)
- [x] 1.4 Remove legacy entry point `main/Main.java`, `ninja/ebean/` package, legacy background workers in `services/`, and unused `etc/` classes while retaining `HelperUtils.java` and `SizeLimitExceededException.java`

## 2. Legacy Frontend & Configuration Cleanup

- [x] 2.1 Remove obsolete FreeMarker templates directory `xcmailr-webapp/src/main/resources/views/`
- [x] 2.2 Remove unused legacy static assets in `xcmailr-webapp/src/main/resources/assets/` (`css/`, `js/`, `fonts/`, `img/`), verifying `assets/ico/favicon.ico` is preserved
- [x] 2.3 Remove legacy `ehcache.xml` and Ninja configuration files in `xcmailr-webapp/conf/` and `src/main/resources/conf/application.conf`

## 3. Build & Legacy Test Suite Cleanup

- [x] 3.1 Prune `xcmailr-webapp/pom.xml` by removing `ninja-maven-plugin`, `maven-assembly-plugin`, and obsolete dependencies (`ninja-standalone`, `ninja-db-classic`, `ninja-test-utilities`, `io.ebean:*`, `commons-email`), and delete `xcmailr-webapp/assembly.xml` and `bin/run.sh` / `bin/run.cmd`
- [x] 3.2 Remove obsolete Ninja-based tests in `xcmailr-webapp/src/test/java/` (`controllers/*Test`, `services/*Test`, `testutils/StaticNinjaTest.java`)
- [x] 3.3 Delete unreferenced Ant test suite directory `xcmailr_testsuite/` and duplicate project directories in `xcmailr-client/bin/` and `xcmailr-load-test-suite/bin/`

## 4. Verification & Integrity Check

- [x] 4.1 Confirm that Flyway migration scripts (`src/main/resources/db/migration/V1..V3.sql`) and H2 migration utilities (`bin/export.sh`, `import.sh`, `shell.sh`, `runSqlFile.sh`) remain intact
- [x] 4.2 Run `mvn clean test` across all modules to verify that all Spring Boot tests pass cleanly and the project compiles with zero legacy dependencies
