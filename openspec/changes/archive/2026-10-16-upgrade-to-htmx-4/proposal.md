# Proposal

## Why
HTMX 4.0.0 was officially released in August 2026, transitioning from `XMLHttpRequest` to a modern `fetch()` architecture and standardizing event naming. Upgrading XCMailr to HTMX 4.0.0 modernizes the front-end hypermedia engine and requires rigorous automated verification across the test pyramid to guarantee CSRF header injection and server-client contract integrity.

## What Changes
- **Dependency Upgrade**: Bump `org.webjars.npm:htmx.org` from `2.0.4` to `4.0.0` in `xcmailr-webapp/pom.xml`.
- **Script Reference**: Update `templates/layout/base.html` to reference `/webjars/htmx.org/4.0.0/dist/htmx.min.js`.
- **Event Listener Modernization**: **BREAKING** Update global event listener in `base.html` from `htmx:configRequest` to `htmx:config:request` to ensure CSRF token injection into `fetch()` requests.
- **HTMX Configuration Compatibility**: Configure `htmx.config.noSwap` and `htmx.config.implicitInheritance` in `base.html` to ensure full compatibility with existing Thymeleaf templates and error handling flows.
- **Enhanced MockMvc Tests (Test Pyramid - Slice Layer)**: Add comprehensive MockMvc test cases covering happy path, validation errors, ownership enforcement, and edge cases across mailbox actions, modals, and administrative controls with `final` modifier throughout, full Javadoc, and structured comments.
- **Lightweight Browser-Level / Client Contract Tests (Test Pyramid - E2E Layer)**: Add browser/DOM client contract tests verifying CSRF token handling with valid tokens, invalid tokens, and missing tokens under the new `htmx:config:request` event contract.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-console-ui`: Updates the dynamic front-end hypermedia contract to HTMX 4 namespaced event handling (`htmx:config:request`), and defines testing pyramid verification requirements including MockMvc slice tests and browser/DOM client contract tests for valid, invalid, and missing CSRF tokens.

## Impact
- **Dependencies**: `xcmailr-webapp/pom.xml` (`org.webjars.npm:htmx.org:4.0.0`).
- **Templates**: `xcmailr-webapp/src/main/resources/templates/layout/base.html` (script reference, event listener, htmx config).
- **Test Suite**: New and expanded test classes in `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/` and client contract verification classes.

