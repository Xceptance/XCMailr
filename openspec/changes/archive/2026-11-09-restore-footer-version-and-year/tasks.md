# Tasks

## 1. Build & Localization Setup

- [x] 1.1 Configure `build-info` goal execution on `spring-boot-maven-plugin` in `xcmailr-webapp/pom.xml` and remove obsolete `version.ftl.html` include/exclude resource filtering rules, verifying with `mvn process-resources`.
- [x] 1.2 Add `layout.footer.version = Version` to `xcmailr-webapp/src/main/resources/conf/messages.properties` and `messages_de.properties`, verifying message keys resolve in both bundles.

## 2. Global Model Attributes

- [x] 2.1 Implement `AppInfoAdvice` in `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/config/AppInfoAdvice.java` with `@ControllerAdvice` exposing `@ModelAttribute("copyrightYear")` (`2013-` + current year) and `@ModelAttribute("appVersion")` (via optional `BuildProperties` with default fallback to `4.0.0`), and add unit test coverage in `AppInfoAdviceTest.java`.

## 3. Base Layout Template

- [x] 3.1 Update corporate footer markup in `xcmailr-webapp/src/main/resources/templates/layout/base.html` to render `copyrightYear`, company link, localized copyright notice, bullet separator, and localized version with `.xcmailr-version`.

## 4. Contract & Regression Testing

- [x] 4.1 Update `BaseLayoutFooterContractTest.java` to invert legacy negative year assertions, positively verifying `2013-` + current year and version markup across public (`/login`), authenticated (`/mailboxes`), and error (`/error`) views while preserving negative assertions against tech stack disclosure.
- [x] 4.2 Update `AuthWebControllerTest.java` to assert footer copyright notice and version label rendering under English and German locales.
- [x] 4.3 Execute full test suite via `mvn clean test` across the reactor to verify zero regressions.
