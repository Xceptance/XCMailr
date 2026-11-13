# Design

## Context

See `proposal.md` for motivation. XCMailr is currently a monolithic mail forwarding application using Ninja Framework 6.9, SubEthaSMTP 3.1.7, Ebean ORM, Flyway migrations (`V1`, `V2`, `V3`), FreeMarker, and AngularJS 1.x. It runs in production with live user accounts, mailboxes, and email history. The modernization must upgrade the runtime to Java 25 and Spring Boot 4.x while preserving all data, external API contracts, and business rules without requiring Node/npm tooling.

## Goals / Non-Goals

**Goals:**
- Implement framework-agnostic integration tests (`*IT.java`) executed automatically via `mvn verify` prior to replacing legacy code.
- Migrate to Spring Boot 4.x and Java 25, utilizing Virtual Threads for concurrent SMTP and HTTP connections.
- Replace Ebean and the Maven bytecode enhancement plugin with standard Spring Data JPA repositories while keeping existing entity schemas.
- Replace FreeMarker and AngularJS 1.x with Thymeleaf and HTMX styled with Bootstrap 5, preserving a 100% pure Maven build.
- Preserve Flyway migration lineage (`V1`, `V2`, `V3`) and BCrypt password authentication for zero data migration downtime.
- Enforce OWASP security guidelines: anti-open-relay protection, HTML email XSS sanitization, CRLF injection prevention, and CSRF tokens across HTMX requests.
- Keep implementation simple, short, and smart (no overengineering): enforce `final` modifiers wherever possible, comprehensive Javadoc, clear code comments, and an updated `README.md`.

**Non-Goals:**
- Schema re-architecting: Table names and column definitions remain unchanged so existing databases upgrade in-place.
- API breaking changes: Endpoints under `/api/v1/*` retain identical JSON request/response formats.
- Node/npm toolchains: No frontend build pipelines; static vendor assets are managed via WebJars or static resources.
- Overengineering: No speculative abstraction layers, custom micro-frameworks, or excessive indirection. Prefer standard, idiomatic Spring Boot idioms.

## Decisions

### 1. Spring Boot 4.x with Java 25 Baseline
- **Decision**: Adopt Spring Boot 4.x targeting Java 25 with Virtual Threads enabled (`spring.threads.virtual.enabled=true`).
- **Rationale**: Long-term platform support, native Jakarta EE 10 baseline, robust Spring Security and Spring Mail integrations, and excellent throughput for blocking network I/O in SMTP listeners.
- **Alternatives Considered**: 
  - *Quarkus*: High efficiency, but less common for custom embedded SMTP server setups.
  - *In-place Ninja update*: Dead-end due to unmaintained upstream framework and JDK 17+ reflection barriers.

### 2. Frontend: Thymeleaf + HTMX + Bootstrap 5
- **Decision**: Build server-rendered HTML views with Thymeleaf and dynamic interactions (modals, search-as-you-type, live table filtering) with HTMX.
- **Rationale**: Completely replaces AngularJS 1.x while keeping the build 100% inside Maven without Node.js, npm, or Webpack/Vite overhead.
- **Alternatives Considered**: 
  - *Vue 3 / React SPA*: Great developer ergonomics, but introduces Node toolchains and multi-stage packaging complexity.

### 3. Persistence: Spring Data JPA (Hibernate)
- **Decision**: Migrate from Ebean ORM to Spring Data JPA repositories.
- **Rationale**: The existing entity classes (`User`, `MBox`, `Mail`, etc.) are already annotated with `jakarta.persistence.*`. Spring Data JPA eliminates the fragile `ebean-maven-plugin:enhance` compile step while providing idiomatic query derivation.
- **Alternatives Considered**: 
  - *Ebean Spring Starter*: Feasible, but maintains dependency on the bytecode enhancement plugin.

### 4. Inbound SMTP: Modernized Embedded SMTP Listener
- **Decision**: Wrap an embedded Jakarta-compatible SMTP server in a Spring `@Component` implementing `SmartLifecycle`.
- **Rationale**: Clean startup and shutdown coordination with the Spring context, dynamic port assignment during tests, and direct injection of JPA repositories and mail services.
- **Alternatives Considered**: 
  - *External Mail Server (e.g., Postfix/James)*: Increases operational complexity; XCMailr's value is its self-contained, lightweight footprint.

### 5. Test Safety Net Placement: `xcmailr-webapp/src/test` with Failsafe
- **Decision**: Place safety net integration tests under `xcmailr-webapp/src/test/java/.../*IT.java` managed by `maven-failsafe-plugin`.
- **Rationale**: Guarantees automatic execution during `mvn verify` locally and in CI. Uses `@SpringBootTest(webEnvironment = RANDOM_PORT)` to boot ephemeral test instances on dynamic HTTP and SMTP ports.
- **Alternatives Considered**: 
  - *Separate Maven submodule*: Requires multi-module coordination and risk of skipped verification steps.

### 6. Code Guidelines, Simplicity, and Documentation
- **Decision**: Keep implementation simple, short, and smart. Avoid overengineering or unnecessary abstractions.
  - **`final` by Default**: Use `final` wherever possible (classes, fields, method parameters, local variables).
  - **Javadoc & Comments**: Require Javadoc on all public/package-private classes and methods, accompanied by concise code comments explaining non-obvious logic.
  - **README Documentation**: Keep `README.md` updated with Java 25 requirements, Spring Boot run/build workflows, and configuration properties.
- **Rationale**: Improves code maintainability, minimizes mutable state bugs, and ensures clear developer onboarding.

## Risks / Trade-offs

- **[Risk] Cookie/Session Invalidation during Cutover** → **Mitigation**: Users will be asked to log in once after deployment. REST API tokens remain completely valid as they are validated against database records.
- **[Risk] Email HTML XSS in Viewer** → **Mitigation**: Use `owasp-java-html-sanitizer` to sanitize all rendered email HTML in Thymeleaf before rendering in the DOM.
- **[Risk] Open Mail Relay Abuse** → **Mitigation**: Strict domain matching against `register_domains` and configured domain list; inbound connection immediately rejected (`553`) if recipient domain is unmanaged.
- **[Risk] Flyway Schema Version Collision** → **Mitigation**: Leave `V1`, `V2`, `V3` untouched in `src/main/resources/db/migration/`; future schema evolutions begin at `V4`.

## Migration Plan

1. **Step 1: Characterization Tests**: Implement `*IT.java` test suites covering REST API, SMTP delivery, and edge cases.
2. **Step 2: Core Migration**: Configure Spring Boot 4.x POM, entities, repositories, and Flyway. Pass tests.
3. **Step 3: Service & SMTP Porting**: Implement embedded SMTP listener, expiration jobs, and REST controllers. Pass safety net tests.
4. **Step 4: Web UI Porting**: Implement Thymeleaf templates, HTMX controllers, and Bootstrap 5 styling.
5. **Step 5: Production Verification**: Rehearse migration on a restored production database copy before final cutover.

