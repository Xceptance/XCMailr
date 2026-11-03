# Design

## Context
XCMailr currently uses HTMX 2.0.4 imported via WebJars (`org.webjars.npm:htmx.org`) and configured in `templates/layout/base.html`. In `base.html`, a global event listener binds to `htmx:configRequest` to inject CSRF tokens into outbound mutating requests.
In HTMX 4.0.0, the underlying engine switched from `XMLHttpRequest` to `fetch()`, event names were standardized to `htmx:phase:action` (making `htmx:configRequest` obsolete in favor of `htmx:config:request`), and default swapping behavior on 4xx/5xx responses changed.
Existing tests in `MailboxWebControllerTest` and `AdminWebControllerTest` use Spring `MockMvc` to verify HTTP contracts, but do not execute client-side JavaScript or verify the browser-level CSRF injection event lifecycle.

## Goals / Non-Goals

**Goals:**
- Upgrade WebJar dependency to `org.webjars.npm:htmx.org:4.0.0` in `pom.xml`.
- Update `base.html` script import and modernize event listeners to `htmx:config:request`.
- Establish safe compatibility defaults (`htmx.config.implicitInheritance = true;` and `htmx.config.noSwap = [204, 304, '4xx', '5xx'];`) in `base.html`.
- Expand Spring MockMvc tests across the test pyramid (slice layer) for happy paths, validation errors, ownership enforcement, and special edge cases.
- Implement a lightweight browser-level / client contract test (E2E layer) verifying CSRF header attachment under `htmx:config:request` with valid, invalid, and missing tokens.
- Apply strict coding standards: `final` on all parameters, local variables, and fields wherever possible, comprehensive Javadoc on classes and methods, and structured `// GIVEN`, `// WHEN`, `// THEN` comments.

**Non-Goals:**
- Rewriting templates to remove HTMX or migrate to an SPA framework.
- Modifying backend business logic, database entities, or SMTP engine.
- Introducing heavy external browser binaries (e.g. full Selenium Chrome/Firefox grid) when a lightweight embedded solution suffices.

## Decisions

### Decision 1: HTMX 4 Compatibility Configuration in `base.html`
- **Choice**: Configure `htmx.config.implicitInheritance = true;` and `htmx.config.noSwap = [204, 304, '4xx', '5xx'];` in the `<head>` or script initialization block of `base.html`.
- **Rationale**: While XCMailr's templates mostly specify explicit targets, preserving implicit inheritance and suppressing raw 4xx/5xx swaps ensures zero regression across existing modals, tables, and partial views while taking advantage of HTMX 4's modern fetch engine and performance improvements.
- **Alternatives Considered**:
  - *Full template overhaul with `:inherited`*: High risk of subtle UI breakage with no functional benefit.

### Decision 2: Modernize Event Listener to `htmx:config:request`
- **Choice**: Replace `document.body.addEventListener('htmx:configRequest', ...)` with `document.body.addEventListener('htmx:config:request', ...)`. Also support fallback listeners if necessary during migration.
- **Rationale**: HTMX 4 standardizes event names to the `htmx:phase:action` format. Without this change, CSRF tokens fail to attach to outbound mutating requests.
- **Alternatives Considered**:
  - *Using the `htmx-2-compat` extension*: Adds unnecessary script payload when updating a single listener in `base.html` is cleaner and native.

### Decision 3: Lightweight Browser-Level Testing via `HtmlUnit` / `MockMvcHtmlUnitDriver`
- **Choice**: Utilize `MockMvcHtmlUnitDriver` (from `spring-security-test` / `htmlunit`) to execute client-side JavaScript, load `base.html`, and verify DOM event execution in memory.
- **Rationale**: HtmlUnit runs headlessly without requiring external Chrome/Chromedriver binaries, operates within the Maven build lifecycle, and executes real JavaScript to confirm that `htmx:config:request` reads the meta tags and attaches the `X-CSRF-TOKEN` header.
- **Alternatives Considered**:
  - *Selenium WebDriver with headless Chrome*: Requires local Chrome installation and separate driver binaries, creating friction across different developer machines and sandboxed CI.
  - *MockMvc alone*: Does not execute JavaScript; cannot catch broken client event listener names.

### Decision 4: Test Pyramid Architecture
- **Unit Layer**: Fast, isolated tests for address normalization, status filter conversions, and expiration calculations.
- **Web Slice Layer (MockMvc)**:
  - *Happy Path*: Asynchronous mailbox creation (`HX-Request: true`), table refresh, toggle, extend (+24h), edit, and delete with `HX-Trigger` headers.
  - *Error Cases*: Duplicate mailbox name, invalid address characters, unconfigured domain, unauthenticated access (`302`/`401`), cross-user access attempts (`403`).
  - *Edge Cases*: Fallback standard form submissions (redirects without `HX-Request`), reactivation of expired mailboxes granting grace periods, cascading deletion of associated emails.
- **E2E / Client Contract Layer (HtmlUnit)**:
  - *Valid Token*: Asynchronous request includes valid CSRF header $\rightarrow$ Server responds `200 OK`.
  - *Invalid Token*: Asynchronous request includes forged/invalid CSRF header $\rightarrow$ Server responds `403 Forbidden`.
  - *Missing Token*: Asynchronous request omits CSRF header $\rightarrow$ Server responds `403 Forbidden`.

### Decision 5: Coding Standards
- Use `final` everywhere possible (class fields, method parameters, and local variables).
- Provide Javadoc on all test classes and methods explaining test scope, preconditions, and assertions.
- Use explicit `// GIVEN`, `// WHEN`, and `// THEN` structural comment blocks.

## Risks / Trade-offs

- **[Risk] Fetch API vs XMLHttpRequest differences in HtmlUnit**: HtmlUnit's JavaScript engine must support the modern `fetch()` API used by HTMX 4.
  - *Mitigation*: HtmlUnit 4.x / modern versions bundled with Spring Boot 3.5+ have full `fetch()` and `Promise` support. If specific Fetch polyfills are needed in the test environment, they can be configured in the test harness setup.
- **[Risk] Silent CSRF failure on mutating requests**: If `htmx:config:request` is misnamed or not triggered, all POST/DELETE actions fail.
  - *Mitigation*: The dedicated three-scenario CSRF test suite (valid, invalid, missing) directly verifies this contract and fails the build if the header is not attached.

