# Tasks

## 1. Safety Net Integration Tests (Black-Box Characterization)

- [x] 1.1 Configure `maven-failsafe-plugin` in `xcmailr-webapp/pom.xml` to execute `*IT.java` integration tests during the `verify` phase and verify with `mvn test-compile`
- [x] 1.2 Implement `MailboxApiIT` covering mailbox CRUD lifecycle, pagination, and token authentication error cases (401, 403, 404) and verify with `mvn failsafe:integration-test -Dtest=MailboxApiIT`
- [x] 1.3 Implement `MailApiIT` covering mail retrieval, message body queries, attachment downloads, and deletion and verify with `mvn failsafe:integration-test -Dtest=MailApiIT`
- [x] 1.4 Implement `SmtpDeliveryIT` covering inbound delivery, domain whitelist enforcement, anti-relay rejection (`553`), expired mailbox dropping, and loop prevention (`X-Loop`) and verify with `mvn failsafe:integration-test -Dtest=SmtpDeliveryIT`
- [x] 1.5 Run the complete safety net test suite against the legacy application and verify 100% test pass via `mvn verify`

## 2. Java 25 & Spring Boot 4.x Foundation

- [x] 2.1 Update root and webapp `pom.xml` to Java 25 (`<java.version>25</java.version>`) and Spring Boot 4.x dependencies, removing the Ebean enhancement plugin and verifying with `mvn compile`
- [x] 2.2 Configure `application.yml` with virtual threads (`spring.threads.virtual.enabled=true`), Flyway auto-migration, and H2/JDBC data sources, verifying configuration startup
- [x] 2.3 Refactor entity classes (`User`, `MBox`, `Mail`, `MailTransaction`, `MailStatistics`, `Domain`) to standard Spring Data JPA entities applying `final` modifiers where appropriate, Javadoc, and code comments, verifying mapping integrity
- [x] 2.4 Implement Spring Data JPA repositories (`UserRepository`, `MailboxRepository`, `MailRepository`, `MailTransactionRepository`, `DomainRepository`) with clean query methods and Javadoc, verifying with repository tests
- [x] 2.5 Verify Flyway auto-migration executes cleanly against existing `V1`, `V2`, `V3` migration scripts and validates schema compatibility

## 3. Security & REST API Cutover

- [x] 3.1 Implement Spring Security configuration with `BCryptPasswordEncoder` ensuring compatibility with existing user password hashes, plus API token authentication filter, applying `final` modifiers and Javadoc, and verify with security tests
- [x] 3.2 Implement Spring MVC REST controllers for `/api/v1/mailboxes` matching legacy JSON schemas using `final` modifiers, Javadoc, and code comments, verifying against `MailboxApiIT`
- [x] 3.3 Implement Spring MVC REST controllers for `/api/v1/mails` and attachment streaming using `final` modifiers, Javadoc, and code comments, verifying against `MailApiIT`
- [x] 3.4 Implement email HTML sanitization service using OWASP Java HTML Sanitizer and header CRLF injection guard with Javadoc and code comments, verifying with unit tests

## 4. Inbound SMTP Engine & Background Schedulers

- [x] 4.1 Implement embedded SMTP listener lifecycle component (Jakarta-compatible SubEthaSMTP) honoring domain whitelists, anti-relay rules, and TLS settings with `final` modifiers and Javadoc, verifying with `SmtpDeliveryIT`
- [x] 4.2 Implement Spring `@Scheduled` tasks for mailbox expiration and email retention cleanup using `final` modifiers, Javadoc, and code comments, verifying with scheduler unit tests
- [x] 4.3 Execute complete `*IT` test suite to verify full behavioral parity against the Spring Boot backend via `mvn verify`

## 5. Web Console UI Modernization (Thymeleaf + HTMX + Bootstrap 5)

- [x] 5.1 Implement base Thymeleaf layout with Bootstrap 5 and global HTMX CSRF integration, verifying layout rendering
- [x] 5.2 Implement authentication and self-service views and controllers (login, registration, password recovery, profile, API tokens) using `final` modifiers, Javadoc, and comments, verifying auth flows
- [x] 5.3 Implement interactive mailbox dashboard with HTMX live search, pagination, and modal dialogs (create, edit, extend date, delete) with clean controller handlers, verifying dynamic HTMX responses
- [x] 5.4 Implement webmail message viewer with sanitized HTML rendering and attachment downloads, verifying message inspection
- [x] 5.5 Implement admin dashboard views and controllers for user management, paged transaction auditing, and domain whitelist management using `final` modifiers and Javadoc, verifying admin controls

## 6. End-to-End Verification & Production Readiness

- [x] 6.1 Execute the full test suite (`mvn clean verify`) covering all unit and integration tests
- [x] 6.2 Validate `xcmailr-client` library communication against the modernized Spring Boot server, verifying client test execution
- [x] 6.3 Update `README.md` to document Java 25 prerequisites, Spring Boot build and execution instructions (removing obsolete `process-classes` steps), and configuration options
- [x] 6.4 Rehearse database migration against a copy of a production database to verify zero data loss and seamless Flyway execution

