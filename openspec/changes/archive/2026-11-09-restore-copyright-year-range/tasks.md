# Tasks

## 1. POM and License Plugin Configuration

- [x] 1.1 Update the `<copyright>` property in root `pom.xml` to `Copyright (c) ${project.inceptionYear}-2026 ${project.organization.name}` and verify with `mvn help:evaluate -Dexpression=copyright -q -DforceStdout`.
- [x] 1.2 Modernize `license-maven-plugin` configuration in root `pom.xml` to use `<licenseSets>` referencing `${maven.multiModuleProjectDirectory}/license-header.txt` and verify with `mvn validate`.

## 2. Source Code and Theme Header Updates

- [x] 2.1 Update copyright headers in all Java source files under `xcmailr-client` to `Copyright (c) 2013-2026 Xceptance Software Technologies GmbH` and verify with `mvn license:check -pl xcmailr-client`.
- [x] 2.2 Update copyright headers in all Java source files under `xcmailr-webapp` and `xcmailr-load-test-suite` to `Copyright (c) 2013-2026 Xceptance Software Technologies GmbH` and verify with `mvn license:check -pl xcmailr-webapp,xcmailr-load-test-suite`.
- [x] 2.3 Update copyright headers in `emerald-theme.css` and `xceptance-theme.css` to `2013-2026` and verify with `git diff xcmailr-webapp/src/main/resources/static/css/`.

## 3. Verification and Integration

- [x] 3.1 Run `mvn license:check` across the entire reactor from the root directory and verify 0 violations.
- [x] 3.2 Run `mvn clean test` across all reactor modules and verify all unit and integration tests pass cleanly.
