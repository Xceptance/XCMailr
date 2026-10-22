# Tasks

## 1. Dependency Upgrade and Layout Configuration

- [x] 1.1 Upgrade WebJar dependency from `2.0.4` to `4.0.0` for `org.webjars.npm:htmx.org` in `xcmailr-webapp/pom.xml` and verify Maven compiles cleanly
- [x] 1.2 Update script import in `xcmailr-webapp/src/main/resources/templates/layout/base.html` to reference `htmx.org/4.0.0` and add compatibility settings (`implicitInheritance: true`, `noSwap: [204, 304, '4xx', '5xx']`)
- [x] 1.3 Update global CSRF event listener in `base.html` to listen for `htmx:config:request` and ensure CSRF token is attached to `evt.detail.headers`

## 2. Unit Testing Layer (Test Pyramid Layer 1)

- [x] 2.1 Implement `MailboxValidationUnitTest` with `final` modifiers, full Javadoc, and structured `// GIVEN`, `// WHEN`, `// THEN` comments covering mailbox address validation patterns and status conversions
- [x] 2.2 Add unit test cases for happy path (valid lowercase and trimmed addresses), error cases (invalid characters, forbidden symbols), and special edge cases (blank address auto-generation) and verify via `mvn test -Dtest=MailboxValidationUnitTest`

## 3. Web Slice Integration Testing Layer (Test Pyramid Layer 2 - MockMvc)

- [x] 3.1 Enhance `MailboxWebControllerTest` with happy path scenarios (asynchronous HTMX mailbox creation, table refresh, toggle, +24h extension, and deletion cascade) with `final` modifiers, Javadoc, and structured comments
- [x] 3.2 Add comprehensive error path scenarios to `MailboxWebControllerTest` (duplicate mailbox error fragment, unconfigured domain error, unauthenticated redirection, and cross-user ownership breach returning 403)
- [x] 3.3 Add special edge case scenarios to `MailboxWebControllerTest` (non-HTMX browser fallback form submission redirecting with query param, expired mailbox reactivation grace period) and verify with `mvn test -Dtest=MailboxWebControllerTest`

## 4. Browser-Level CSRF Client Contract Testing Layer (Test Pyramid Layer 3 - E2E)

- [x] 4.1 Create `HtmxCsrfClientContractTest` using a lightweight JS-capable test driver (`MockMvcHtmlUnitDriver` / `HtmlUnit`) to load `base.html` and execute client-side JavaScript with `final` modifiers and full Javadoc
- [x] 4.2 Implement happy path test scenario verifying that `htmx:config:request` extracts `_csrf` metadata and attaches a valid CSRF header, yielding HTTP 200 OK
- [x] 4.3 Implement error scenarios in `HtmxCsrfClientContractTest` for missing CSRF token and invalid/forged CSRF token, verifying that both are rejected with HTTP 403 Forbidden
- [x] 4.4 Run `HtmxCsrfClientContractTest` and verify all scenarios pass under the HTMX 4 runtime via `mvn test -Dtest=HtmxCsrfClientContractTest`

## 5. Verification and Full Build

- [x] 5.1 Execute full test suite across the reactor (`mvn test`) and verify zero regressions

