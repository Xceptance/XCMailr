# Proposal

## Why

XCMailr is currently built on an outdated technology stack: Ninja Framework 6.9 (which has reached end-of-life), SubEthaSMTP 3.1.7 (dating from ~2012), FreeMarker templates mixed with legacy AngularJS 1.x (end-of-life January 2022), and Bootstrap 3. The build is tied to Java 11 and encounters compilation and reflection issues on modern JDKs.

Because XCMailr runs in an active production environment, modernization must be executed without service disruption, without losing existing functionality, and without breaking stored user/mailbox data. We need a test-driven modernization path targeting **Java 25** and **Spring Boot 4.x**, paired with a modern, lightweight, server-driven UI (**Thymeleaf + HTMX + Bootstrap 5**) that eliminates Node/npm build dependencies and adheres to OWASP security best practices.

## What Changes

- **Test-Driven Safety Net First**: Implement comprehensive black-box integration tests (`*IT.java` via `maven-failsafe-plugin`) covering REST API, Inbound SMTP, and Web flows across happy paths, error scenarios, and email-specific edge cases.
- **Java 25 Baseline**: Upgrade Maven and source/target configuration to Java 25 (`<java.version>25</java.version>`), leveraging records, pattern matching, and virtual threads for concurrent I/O.
- **Framework Modernization to Spring Boot 4.x**: Replace Ninja Framework and embedded Jetty with Spring Boot 4.x (Spring MVC, Spring Data JPA, Spring Security, Spring Mail).
- **Embedded SMTP Engine Modernization**: Replace legacy SubEthaSMTP 3.1.7 with a modern Jakarta Mail 2.1-compatible SMTP server component managed as a Spring lifecycle bean.
- **Frontend Modernization (No Node.js/npm)**: Replace FreeMarker and AngularJS 1.x with server-rendered Thymeleaf templates and dynamic HTMX interactions (modals, search-as-you-type, live table updates) styled with Bootstrap 5.
- **Data & Migration Continuity**: Retain existing Flyway migration history (`V1`, `V2`, `V3`) and database schema intact; ensure existing BCrypt password hashes are verified natively by Spring Security without forced user password resets.
- **Pragmatic, Non-Overengineered Implementation**: Keep the design simple, short, and smart. Code guidelines mandate `final` modifiers wherever possible, comprehensive Javadoc on public/protected APIs, and clear explanatory code comments.
- **Documentation Update**: Update `README.md` to reflect Java 25 requirements, Spring Boot build and execution instructions (removing obsolete `process-classes`), and modernized configuration details.
- **OWASP Security Hardening**: Enforce strict anti-open-relay controls, sanitize email HTML bodies with OWASP Java HTML Sanitizer before rendering in webmail views, sanitize email headers against CRLF injection, and enforce CSRF protection on all HTMX interactions.

## Capabilities

### New Capabilities
- `integration-safety-net`: Automated black-box integration testing harness covering REST API contracts, inbound SMTP reception, mail forwarding, edge cases (loops, quotas, expiration), and error paths.
- `inbound-smtp-engine`: Embedded SMTP listener service that enforces domain whitelisting, anti-relay checks, size limits, loop prevention (`X-Loop`), message storage, and outbound mail forwarding.
- `mailbox-management-api`: Token-authenticated RESTful API (`/api/v1/mailboxes`, `/api/v1/mails`) supporting full mailbox lifecycle, mail inspection, and attachment downloads.
- `web-console-ui`: Responsive web user interface powered by Thymeleaf and HTMX providing user authentication, self-service mailbox management, live message inspection, and administrative controls.
- `security-hardening`: OWASP Top 10 protections including XSS defense for email bodies, CRLF header injection mitigation, CSRF protection across HTMX requests, and role-based access control.

### Modified Capabilities
<!-- None: Greenfield OpenSpec capability tracking for this repository -->

## Impact

- **Build Tooling**: `pom.xml` configuration updated to Spring Boot 4.x parent/dependencies and Java 25. Removal of the legacy Ebean bytecode enhancement Maven plugin (`process-classes`).
- **Dependencies**: Replaces `ninja-standalone`, `ninja-ebean`, `subethasmtp:3.1.7`, `jbcrypt` with `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-mail`, `spring-boot-starter-thymeleaf`, and modern Jakarta Mail.
- **Runtime**: Runs as a standard Spring Boot executable application.
- **Data Compatibility**: Zero changes required for existing production database tables (`users`, `mailboxes`, `mail`, `mailtransactions`, `MAIL_STATISTICS`, `register_domains`).
- **Client Compatibility**: 100% backward compatibility for `xcmailr-client` and external test automations communicating over `/api/v1/*`.

