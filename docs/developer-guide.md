# XCMailr Developer Guide

A concise reference for developers implementing new features or fixing bugs in XCMailr.

---

## 1. Quick Start (2 Minutes)

### Prerequisites
- **Java 25** (OpenJDK 25 / Eclipse Temurin 25)
- **Apache Maven 3.9+** (or `./mvnw`)
- **Docker** *(optional, for local Mailpit SMTP relay)*

### Option A: Fast Local Mode (No External SMTP Needed)
Disables email registration confirmation so newly registered accounts are immediately active:
```bash
APP_REQUIRE_CONFIRMATION=false mvn spring-boot:run -pl xcmailr-webapp
```

### Option B: Full Email Flow (With Mailpit Mock Relay)
Starts Mailpit (`localhost:1025` SMTP, `http://localhost:8025` Web UI) and runs XCMailr with the `dev` profile:
```bash
docker compose -f docker-compose.dev.yml up -d
mvn spring-boot:run -pl xcmailr-webapp -Dspring-boot.run.profiles=dev
```
*(See the [Local Email Testing Guide](local-testing-guide.md) for end-to-end inbound/outbound SMTP testing).*

### Local Endpoints, Credentials & Database
| Resource | Value |
| :--- | :--- |
| **Web Console & REST API** | `http://localhost:8080` |
| **Inbound SMTP Server** | `localhost:25000` (SubEthaSMTP) |
| **Mailpit Outbound Viewer** | `http://localhost:8025` |
| **Default Admin Login** | `admin@xcmailr.test` / `1234` |
| **Default Admin API Token** | `xcmailr-admin-api-token-2026` (`Authorization: Bearer ...`) |
| **Default H2 Database File** | `~/xcmailrDB.mv.db` (reset via `rm -f ~/xcmailrDB.*`) |

> [!TIP]
> To run with a transient in-memory H2 database that resets on every restart:
> ```bash
> SPRING_DATASOURCE_URL="jdbc:h2:mem:xcmailrDB;DB_CLOSE_DELAY=-1" mvn spring-boot:run -pl xcmailr-webapp
> ```

---

## 2. Codebase & Package Map

### Maven Modules
- **`xcmailr-webapp`**: Spring Boot 4.1 web application, embedded SubEthaSMTP engine, and REST API.
- **`xcmailr-client`**: Lightweight Java HTTP client SDK (`XCMailrClient`) for test automation suites.
- **`xcmailr-load-test-suite`**: XLT load and performance test suite.

### `xcmailr-webapp` Structure
```text
xcmailr-webapp/src/main/
├── java/com/xceptance/xcmailr/
│   ├── config/         # Spring configuration, XcmailrProperties, AdminUserInitializer, ThemeResolver
│   ├── controllers/    # Web MVC controllers (HTMX/Thymeleaf) and REST API controllers (/api/v1/*)
│   ├── dto/            # Request/response DTOs, form validation objects, and view models
│   ├── models/         # JPA entities (User, MBox, Mail, Domain, MailTransaction, MailStatistics)
│   ├── repositories/   # Spring Data JPA repositories
│   ├── security/       # Spring Security 6 filters (session + Bearer API token), UserPrincipal, BCrypt
│   ├── services/       # Business logic, SMTP server/delivery, and scheduled maintenance tasks
│   └── util/           # Stateless helper utilities (HelperUtils, etc.)
└── resources/
    ├── application.yml                 # Default configuration (ddl-auto: validate, Flyway enabled)
    ├── assets/i18n/
    │   ├── messages.properties         # English (default) UI localization keys
    │   └── messages_de.properties      # German UI localization keys
    ├── db/migration/                   # Flyway SQL migrations (V1__ through V6__)
    ├── static/                         # CSS themes (static/css/*-theme.css) and client JS
    └── templates/                      # Thymeleaf 3 layouts, pages, modals, and HTMX fragments
```

---

## 3. Workflow: Implementing a Feature or Fixing a Bug

### Step 1: Database Schema or Data Changes (If Needed)
- XCMailr runs with `spring.jpa.hibernate.ddl-auto: validate`. Hibernate **never** alters the schema automatically.
- **Never edit an existing migration (`V1`–`V6`)**. Add a new script `V7__<Description>.sql` in `xcmailr-webapp/src/main/resources/db/migration/` using cross-database ANSI SQL (compatible with H2, PostgreSQL, and MySQL/MariaDB).
- Update the matching JPA entity in `com.xceptance.xcmailr.models`.
- See the [Schema Migration Guide](schema-migration-guide.md) for DDL templates and examples.

### Step 2: Repository & Service Layer
- Add query methods to the appropriate interface in `com.xceptance.xcmailr.repositories`.
- Place business logic, validation, and `@Transactional` boundaries in `com.xceptance.xcmailr.services`.
- Background expiration and cleanup tasks live in `ExpirationScheduledService`.

### Step 3: Web UI, HTMX & Localization
- Add or update endpoints in `com.xceptance.xcmailr.controllers` (`MailboxWebController`, `MailWebController`, `AdminWebController`, `WebAuthController`).
- For partial page updates, return Thymeleaf fragment views (e.g., `"mailboxes/index :: mailboxTable"`) or trigger client refreshes via `HX-Trigger` response headers.
- **Localization is mandatory**: Add every new UI label, tooltip, or validation message to **both** `assets/i18n/messages.properties` (English) and `assets/i18n/messages_de.properties` (German).

### Step 4: REST API & Client SDK
- If adding or modifying `/api/v1/*` endpoints in `MailboxApiController` or `MailApiController`, update the corresponding client methods and models in `xcmailr-client` so the SDK stays in sync.

---

## 4. Coding Standards & Security Checklist

- **`final` Everywhere**: Mark all method parameters and local variables `final` wherever possible.
- **Javadoc & Comments**: Provide Javadoc on all classes and methods, and add concise inline comments explaining non-obvious business rules or SQL/JPQL queries.
- **Formatting**: Follow the project's Allman brace style (opening `{` on a new line) and 4-space indentation in Java files.
- **Security Guardrails (OWASP)**:
  - **Access Control**: Annotate admin endpoints with `@PreAuthorize("hasRole('ADMIN')")` and conditionally render admin UI elements with `sec:authorize="hasRole('ADMIN')"`.
  - **Tenant Isolation**: Always verify that a queried `MBox` or `Mail` belongs to the authenticated `User` before reading, modifying, or deleting it.
  - **CSRF Protection**: All state-changing web requests (`POST`, `PUT`, `DELETE`) must carry CSRF tokens (automatically wired for HTMX via `<meta name="csrf-token">` in `layout/default.html`).
  - **HTML Sanitization & Log Hygiene**: Untrusted email HTML must pass through `HtmlSanitizerService`; never log passwords, API tokens, or confirmation tokens (CWE-532).

---

## 5. Testing Pyramid (TDD) & Commands

Develop features and bug fixes **test-driven** (write the failing test first, implement the fix, verify green) across the testing pyramid:

1. **Test Cases Required for Every Change**:
   - **Happy Path**: Standard valid inputs and expected state transitions.
   - **Error & Boundary Cases**: Null/blank/malformed inputs, exact timestamp boundaries (`0`, negative, cutoff edges), non-existent IDs (`404`).
   - **Special & Security Cases**: Cross-tenant access attempts (`403`/`404`), missing CSRF tokens, case-insensitivity, idempotency, and legacy data states.
2. **Test Layers**:
   - **Unit Tests**: Fast tests for services, utilities, and DTOs.
   - **Controller / WebMvc Tests**: MockMvc tests verifying routing, HTMX fragment rendering, localization, and security rules.
   - **Integration Tests**: `@SpringBootTest` tests against H2 (`FlywayMigrationTest`, `RestApiIntegrationTest`, `SmtpDeliveryIntegrationTest`, `ClientSmtpEndToEndIntegrationTest`).

### Essential Maven Commands
```bash
# Run a single test class (fast feedback loop during TDD)
mvn test -pl xcmailr-webapp -Dtest=ExpirationScheduledServiceTest

# Run a single test method
mvn test -pl xcmailr-webapp -Dtest=ExpirationScheduledServiceTest#testMailboxExpiration

# Run all tests in the client SDK module
mvn test -pl xcmailr-client

# Run the full test suite across all modules (always run before committing)
mvn test

# Build executable JARs without running tests
mvn clean package -DskipTests
```

---

## 6. Further Reading
- [Schema Extension & Database Migration Guide](schema-migration-guide.md)
- [Data Compatibility & Safety Guarantees](data-compatibility-and-safety.md)
- [Local Email Testing & Delivery Guide](local-testing-guide.md)
- [Load Testing Guide](load-testing-guide.md)
