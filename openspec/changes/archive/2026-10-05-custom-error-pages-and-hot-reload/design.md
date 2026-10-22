# Design

## Context

XCMailr's web tier runs Spring Boot 4.1.1 with Thymeleaf and Spring MVC. When a request does not match any registered controller or throws an unhandled exception, Spring Boot's `BasicErrorController` delegates rendering to a view named `/error` or matching HTTP status code templates (`error/404`, `error/500`). Because no corresponding templates existed, Spring defaulted to the raw, unstyled "Whitelabel Error Page".

Additionally, during development, developers testing controller route additions or bytecode edits must stop and relaunch `mvn spring-boot:run` manually. DevTools provides classloader separation (Base ClassLoader for third-party jars, Restart ClassLoader for project classes) allowing near-instant restarts when `target/classes` updates.

## Goals / Non-Goals

**Goals:**
- Provide polished, accessible, branded error views matching the corporate palette and layout structure (`layout/base.html`).
- Gracefully handle both authenticated and unauthenticated visitors on error pages with proper navigation links.
- Expose status codes and human-friendly guidance without leaking stack traces or internal implementation details.
- Add `spring-boot-devtools` as an optional runtime dependency to accelerate the inner development loop.

**Non-Goals:**
- Implementing custom REST API JSON error formats (Spring Boot's default JSON representation for REST requests already provides standard RFC 7807 problem details or structured error payloads).
- Adding complex client-side telemetry or error reporting frameworks.

## Decisions

### 1. Spring Boot Convention-Based Error Templates
- **Decision**: Create `templates/error/404.html`, `templates/error/500.html`, and a fallback `templates/error.html`, using Thymeleaf layout decoration: `th:replace="~{layout/base :: layout(~{::content})}"`.
- **Rationale**: Spring Boot's `BasicErrorController` automatically resolves view templates located under `templates/error/{status}.html` and falls back to `templates/error.html`. Using standard layout fragments ensures error pages inherit the header, footer, corporate styles, and responsive offcanvas drawer without custom controller glue.
- **Alternatives Considered**:
  - *Custom `@ControllerAdvice` handling all exceptions*: Adds boilerplate and does not naturally handle container-level 404s before requests reach a controller. Convention-based templates handle both container and controller errors uniformly.

### 2. Information Disclosure and Stack Trace Suppression
- **Decision**: Suppress stack traces from the error UI by default, rendering only the HTTP status code, standard HTTP error reason, and contextual recovery guidance.
- **Rationale**: Exposing Java stack traces, database schema snippets, or SQL queries violates OWASP security best practices.
- **Alternatives Considered**:
  - *Showing traces in dev profile*: Can lead to accidental exposure if dev profiles are mistakenly active. Logs already capture full debug traces.

### 3. Optional DevTools Runtime Dependency
- **Decision**: Add `spring-boot-devtools` with `<optional>true</optional>` and `<scope>runtime</scope>` in `xcmailr-webapp/pom.xml`.
- **Rationale**: Marking the dependency as optional ensures it is never packaged into production jar artifacts or transitive downstream builds while providing automated restarts when running locally via `mvn spring-boot:run` or an IDE.

## Risks / Trade-offs

- **[Risk]** Error templates fail to render if layout dependencies (e.g. security context) are uninitialized.
  - *Mitigation*: Ensure `layout/base.html` navigation handles both `sec:authorize="isAuthenticated()"` and `sec:authorize="!isAuthenticated()"` gracefully, and test unauthenticated 404 access.
- **[Risk]** DevTools restarting during long-running tests.
  - *Mitigation*: Spring Boot disables DevTools automatically during Maven test execution (`mvn test`), preventing unexpected restarts during automated test suites.
