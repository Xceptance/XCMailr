# Proposal

## Why

During code review of the web console UI, it was noted that the application footer displays neither the application version nor the copyright year ("no version and year in footer"). The footer previously rendered as a yearless notice without version metadata following earlier cleanup commits. Restoring the inception-to-current copyright year range and the application version in the footer aligns the web UI with company branding standards and provides clear application build traceability for operators and users without disclosing sensitive internal runtime or framework stack details.

## What Changes

- **Global View Attributes (`@ControllerAdvice`)**: Expose dynamic model attributes `copyrightYear` (inception-to-current year range, e.g., `2013-2026`, auto-calculated via `Year.now().getValue()`) and `appVersion` (resolved via Spring Boot's `BuildProperties` with a graceful fallback to `4.0.0`).
- **Web Console Base Layout (`base.html`)**: Update the corporate footer to render `&copy; 2013-2026 Xceptance Software Technologies GmbH. All rights reserved. • Version: 4.0.0` with localized copyright and version labels and a distinct CSS class (`xcmailr-version`) for testing and client verification.
- **Maven Build Configuration (`xcmailr-webapp/pom.xml`)**: Add `<goal>build-info</goal>` to `spring-boot-maven-plugin` execution so `META-INF/build-info.properties` is generated during packaging, and remove obsolete Ninja `version.ftl.html` include/exclude resource filtering rules.
- **Localization Bundles**: Add `layout.footer.version = Version` to English (`messages.properties`) and German (`messages_de.properties`) message properties.
- **Specification and Test Contracts**: Update `web-console-ui` requirement `Corporate Footer and Brand Notice` and update `BaseLayoutFooterContractTest` and `AuthWebControllerTest` to positively verify footer year range and version presence across public, authenticated, and error views.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-console-ui`: Update requirement `Corporate Footer and Brand Notice` to specify rendering of the inception-to-current copyright year range and application version alongside the corporate notice, while continuing to prohibit internal framework and runtime stack metadata disclosure.

## Impact

- **Templates**: `xcmailr-webapp/src/main/resources/templates/layout/base.html`
- **Backend / Configuration**: New `AppInfoAdvice.java` (or addition to existing controller advice) in `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/config/`, `xcmailr-webapp/pom.xml`
- **Localization**: `xcmailr-webapp/src/main/resources/conf/messages.properties` and `messages_de.properties`
- **Specifications**: `openspec/specs/web-console-ui/spec.md`
- **Tests**: `BaseLayoutFooterContractTest.java`, `AuthWebControllerTest.java`
