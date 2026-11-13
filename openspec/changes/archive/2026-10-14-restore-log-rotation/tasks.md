# Tasks

## 1. Logging Configuration & Environment

- [x] 1.1 Configure native Spring Boot file logging and Logback rolling policy properties in `xcmailr-webapp/src/main/resources/application.yml` and verify YAML validity
- [x] 1.2 Update `.gitignore` to ignore the runtime `log/` directory and verify with `git status`

## 2. Automated Verification & Regression Testing

- [x] 2.1 Implement `LoggingConfigurationTest` in `xcmailr-webapp` to verify that `RollingFileAppender` is registered with the expected target file (`log/xcmailr.log`), file size limit (10MB), history count (10), and total size cap (100MB), and verify using `mvn test -pl xcmailr-webapp -Dtest=LoggingConfigurationTest`
- [x] 2.2 Run full test suite via `mvn test` to verify that existing test execution succeeds without regression

