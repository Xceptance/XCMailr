# Proposal

## Why

The current web console layout footer publicly displays an internal technology stack banner: `XCMailr Modernized • Java 25 • Spring Boot 4.x • HTMX`. Displaying framework names and version numbers in a production web UI is a software engineering and security anti-pattern (CWE-200 / automated fingerprinting surface), provides no functional value to regular end users, and creates unnecessary maintenance churn as underlying libraries or language runtimes are upgraded.

Removing this tech stack summary streamlines the footer to a clean, professional corporate notice with only the yearless copyright text.

## What Changes

- **Remove Tech Stack Banner**: Delete the `<p class="small mb-0">` paragraph displaying `layout.footer.summary` from `base.html`.
- **Clean Up Message Bundles**: Remove obsolete localization key `layout.footer.summary` from both `messages.properties` and `messages_de.properties`.
- **Update Automated Tests**:
  - Update `BaseLayoutFooterContractTest` to assert that the footer contains the yearless copyright notice while explicitly verifying that technology stack details (e.g. `Java 25`, `Spring Boot`, `HTMX`, `XCMailr Modernized`) are no longer exposed.
  - Update any controller integration tests (e.g. `AuthWebControllerTest`) that previously asserted the presence of the tech stack string to check for the copyright notice or view-specific content instead.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-console-ui`: Update the `Corporate Footer and Brand Notice` requirement to state that the corporate footer renders the yearless copyright notice while strictly omitting internal technology stack and framework version details.

## Impact

- **Affected Files**:
  - `xcmailr-webapp/src/main/resources/templates/layout/base.html`
  - `xcmailr-webapp/src/main/resources/conf/messages.properties`
  - `xcmailr-webapp/src/main/resources/conf/messages_de.properties`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/BaseLayoutFooterContractTest.java`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/AuthWebControllerTest.java`
- **Dependencies & APIs**: No external APIs, dependencies, or database schemas affected.

