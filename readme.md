# XCMailr
## Summary
* Name: XCMailr
* Version: 3.1.0
* Release: October 2026
* License: Apache V2.0
* License URI: http://www.apache.org/licenses/LICENSE-2.0.txt
* Tags: AntiSpam, TestUtility, EmailTesting, SpringBoot, HTMX
* Contributors:
  * Patrick Thum, Xceptance Software Technologies GmbH
  * Patrick Hähnlein, Xceptance Software Technologies GmbH
  * René Baumgarten, Xceptance Software Technologies GmbH

## Description
XCMailr was built to aid software and automated test engineering. Testing often requires disposable email addresses to create test accounts, validate complex email validation flows, and conduct large-scale load and performance testing without bounce-backs overwhelming sending systems.

Commercial or public temporary mail services often block or throttle automated test accounts and discard emails. Additionally, transmitting sensitive test emails (e.g., account activation tokens, password reset links) through third-party services poses privacy and compliance risks.

XCMailr allows you to host your own disposable email testing service. You control security, availability, domains, and data retention. Incoming test emails can be inspected via an interactive web dashboard, forwarded to configured destination addresses, or queried via standard REST APIs.

## Key Features & Modern Stack
* **Java 25 & Spring Boot 4.1.1**: High throughput and modern language features with Project Loom Virtual Threads enabled for all HTTP requests and background tasks.
* **Modern Web Console & Responsive UI**: Built with server-side Thymeleaf 3, Bootstrap 5.3, and HTMX 2 for responsive, reactive single-page app UX without requiring Node.js or npm.
* **Corporate Theme & Design System**: Styled according to Xceptance corporate identity (`#004682` brand blue, `#003868` hover, `#dc3545` accent) with Roboto and Roboto Condensed typography.
* **Responsive Offcanvas Drawer**: Sticky corporate navbar on desktop screens and a smooth slide-out Bootstrap 5 Offcanvas drawer (`#navbarOffcanvas`) on mobile viewports.
* **Dark & Light Mode Support**: Native Bootstrap 5.3 color mode switching (`[data-bs-theme]`) with `prefers-color-scheme` auto-detection, `localStorage` persistence, and FOIT-free initialization.
* **Standardized Bootstrap Icons**: Integrated `bootstrap-icons:1.11.3` WebJar providing vector icons for search, actions, navigation, and theme toggling.
* **Role-Based Access Control & OWASP Compliance**: Server-side Thymeleaf Spring Security 6 dialect (`sec:authorize`) eliminates unauthorized DOM exposure; method-level `@PreAuthorize("hasRole('ADMIN')")` and URL security filters enforce multi-layer Broken Access Control (OWASP A01:2021) defenses.
* **Embedded SMTP Engine**: Powered by SubEthaSMTP 7.2.2 with virtual-thread dispatching, handling inbound emails directly on port 25000 (configurable).
* **OWASP HTML Sanitizer**: Safe webmail message rendering stripping malicious `<script>` tags, event handlers, and unsafe protocols while preserving styles and formatting.
* **Spring Security & BCrypt**: Robust password hashing with automatic transparent upgrade for legacy SHA-512 hashes upon successful login.
* **Dual Security Filters**: Session-based form login for web UI and HTTP Bearer token authentication for REST APIs.
* **Flyway Database Migrations**: Automated, zero-downtime database schema versioning supporting embedded H2, PostgreSQL, and MySQL/MariaDB.
* **Java Client Library (`xcmailr-client`)**: Lightweight client SDK for seamless integration into test frameworks (JUnit, TestNG, Selenium, Playwright).

## System Requirements
* **Java 25 runtime** (OpenJDK compatible, e.g. Eclipse Temurin or Homebrew OpenJDK 25)
* **Apache Maven 3.9+** (or included Maven wrapper `./mvnw`)

## Configuration
Application configuration is managed via standard Spring Boot properties in `xcmailr-webapp/src/main/resources/application.properties` or overridden via external files or environment variables:

| Property | Default | Description |
| :--- | :--- | :--- |
| `server.port` | `8080` | HTTP listener port |
| `xcmailr.app.domain` | `localhost` | Default application domain |
| `xcmailr.smtp.listen-port` | `25000` | Inbound SMTP server listener port |
| `xcmailr.smtp.bind-address` | `0.0.0.0` | Inbound SMTP server bind address |
| `xcmailr.mail.expiration-minutes-default` | `60` | Default mailbox validity duration |
| `xcmailr.mail.retention-days-default` | `30` | Default retention period for message and transaction purge |
| `spring.datasource.url` | `jdbc:h2:mem:xcmailr_db;...` | JDBC database connection URL |
| `spring.mail.host` | `localhost` | Outbound relay SMTP server hostname |
| `spring.mail.port` | `25` | Outbound relay SMTP server port |

To override settings in production, provide an external configuration file:
```bash
java -jar xcmailr-webapp.jar --spring.config.additional-location=file:/path/to/custom-application.properties
```

Or use environment variables:
```bash
export XCMAILR_SMTP_LISTEN_PORT=25
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/xcmailr"
export SPRING_DATASOURCE_USERNAME="xcmailr"
export SPRING_DATASOURCE_PASSWORD="securepassword"
java -jar xcmailr-webapp.jar
```

## Build from Source
Build the entire project, run all tests, and package executable archives:
```bash
mvn clean package
```

To skip test execution during rapid packaging:
```bash
mvn clean package -DskipTests
```

## Running the Application
### Option 1: Run Executable Jar (Production)
```bash
java -jar xcmailr-webapp/target/xcmailr-webapp-3.1.0.jar
```

### Option 2: Run via Maven (Development)
```bash
mvn spring-boot:run -pl xcmailr-webapp
```

Once started, access the web dashboard at:
```
http://localhost:8080/
```

## Administrator Account Management

### Initial Bootstrap & Configuration
Upon application startup, XCMailr checks if administrator bootstrapping is configured:
* **Configuration Properties**: `xcmailr.admin.address` and `xcmailr.admin.password`
* **Environment Variables**: `ADMIN_ADDRESS` and `ADMIN_PASSWORD` (or `XCMAILR_ADMIN_ADDRESS` and `XCMAILR_ADMIN_PASSWORD`)

Default credentials for local development:
* **Username**: `admin@xcmailr.test`
* **Password**: `1234`

If valid credentials are provided and the account does not yet exist in the database (e.g. during fresh deployment on an empty database), XCMailr automatically:
1. Bootstraps the administrator user account with active status and admin privileges.
2. Automatically seeds the domain part of the administrator email into the registered domain whitelist if not already present.

> [!WARNING]
> If the administrator account is bootstrapped with the default password (`1234`), a prominent security warning is logged at startup. In any production or publicly accessible environment, configure strong initial credentials or change the password immediately.

### Non-Destructive Startup Guarantee
XCMailr enforces a strictly non-destructive startup policy:
* **No Database Lookups When Unconfigured**: If either `xcmailr.admin.address` or `xcmailr.admin.password` is omitted, `null`, empty, or whitespace-only, the initialization step exits immediately without issuing any database queries.
* **Zero Overwrites of Existing Accounts**: If an account with the configured administrator email already exists in the database, XCMailr leaves it completely untouched. Configured passwords or environment variables will **never** overwrite passwords, roles, or states of existing accounts.
* **Password Updates**: After the administrator account has been bootstrapped, all subsequent password changes must be performed through the web UI profile dashboard (`/profile`).

### Production Security Hardening
In production environments, choose one of the following approaches:
1. **Bootstrap with Strong Secrets**: Set `ADMIN_ADDRESS` and a strong, unique `ADMIN_PASSWORD` prior to the very first launch. After the initial start has initialized the database, you can safely remove or unset `ADMIN_PASSWORD`.
2. **Explicitly Suppress Auto-Bootstrap**: Leaving `ADMIN_PASSWORD=""` or unset prevents any automatic administrator creation during startup.

### Disaster Recovery
If the primary administrator account is ever deleted accidentally or administrative access is lost:
1. Ensure `ADMIN_ADDRESS` and a strong `ADMIN_PASSWORD` are supplied via environment variables.
2. Restart the XCMailr application container or service.
3. The system will detect the absence of the administrator account, safely recreate it with administrator privileges, and log the bootstrap event.
4. Once restored, log in via the web dashboard and remove or unset `ADMIN_PASSWORD` if desired.

## User Registration & Email Confirmation

By default, self-service user registration requires email verification before an account can be used to log in:

1. A visitor registers via the `/register` web form.
2. The user is created in an inactive state (`active = false`), an activation token is generated, and a verification email is dispatched via SMTP.
3. The user clicks the link in the email (`/confirm/{token}`), which activates their account (`active = true`) and clears the token.
4. The user is redirected to `/login?confirmed` and can log in with their credentials.

### Development & Offline Testing Mode

In local development, automated test pipelines, or environments without an active SMTP mail relay, you can disable email confirmation using the `APP_REQUIRE_CONFIRMATION` environment variable or configuration toggle:

```bash
# Run with automatic account activation upon registration
APP_REQUIRE_CONFIRMATION=false mvn spring-boot:run -pl xcmailr-webapp
```

Or configure in `application.yml`:

```yaml
xcmailr:
  app:
    require-confirmation: false
```

When `require-confirmation` is `false`:
* Newly registered accounts are activated immediately (`active = true`).
* No confirmation token is created, and no SMTP email is sent.
* Upon registration, the user is redirected to `/login?ready` and can log in immediately.

### Security Guarantees
* **Anti-Enumeration (CWE-204)**: During web login, credentials (email + password) are validated **before** checking account activation status. Attempting to log in with an unconfirmed email but an invalid password returns a generic error (`/login?error`) rather than an activation notice, preventing attackers from probing whether an email address is registered.
* **Token Redaction (CWE-532)**: Confirmation tokens and activation URLs are strictly excluded from application logs to prevent bearer token leakage.

## REST API & Client SDK
XCMailr provides a full REST API for programmatic mailbox generation and email assertion in test suites.

### Authentication
Authenticate API requests using an API Token in the `Authorization` header:
```http
Authorization: Bearer <your-api-token>
```
API tokens can be generated and revoked in the user Profile dashboard.

### Modern REST API (`/api/v1`)
* `GET /api/v1/mailboxes`: List all mailboxes owned by the authenticated user.
* `POST /api/v1/mailboxes`: Create a new mailbox with address, expiration minutes, and forwarding flag.
* `GET /api/v1/mailboxes/{address}`: Retrieve details for a specific mailbox.
* `PUT /api/v1/mailboxes/{address}`: Update validity, active state, or forwarding rules.
* `DELETE /api/v1/mailboxes/{address}`: Delete a mailbox and its messages.
* `GET /api/v1/mails?mailboxAddress={address}`: Retrieve messages received by a mailbox.
* `GET /api/v1/mails/{id}`: Fetch message headers, text content, and HTML body.
* `GET /api/v1/mails/{id}/raw`: Download raw `.eml` RFC-822 message payload.
* `GET /api/v1/mails/{id}/attachments/{index}`: Download binary message attachment.
* `DELETE /api/v1/mails/{id}`: Delete an individual email.

### Java Client Library (`xcmailr-client`)
To use XCMailr in your Java test suite:
```xml
<dependency>
    <groupId>com.xceptance</groupId>
    <artifactId>xcmailr-client</artifactId>
    <version>3.1.0</version>
    <scope>test</scope>
</dependency>
```

```java
final XCMailrClient client = new XCMailrClient("http://localhost:8080", "my-api-token");

// Create temporary mailbox valid for 10 minutes
final Mailbox mailbox = client.mailboxes().createMailbox("user-test@xcmailr.test", 10, false);

// Wait for email arrival and assert subject
final List<Mail> mails = client.mails().listMails(mailbox.address, null);
assertEquals("Welcome to our service!", mails.get(0).subject);

// Fetch full message content
final Mail fullMail = client.mails().getMail(mails.get(0).id);
assertTrue(fullMail.textContent.contains("Your activation code is: 123456"));

// Clean up
client.mailboxes().deleteMailbox(mailbox.address);
```

### Legacy REST API Compatibility
Legacy XCMailr endpoint URLs remain fully supported for backward compatibility:
* `GET /create/temporaryMail/{token}/{mailAddress}/{validTime}`
* `GET /mailbox/{mailAddress}/{token}`

## Reverse Proxy Setup (Nginx)
When deploying behind Nginx or Apache, configure proxy headers to pass client IP and scheme:
```nginx
server {
    listen 80;
    server_name mail.example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Local Development & Email Testing
For instructions on running XCMailr locally and testing the complete email pipeline (inbound receiving on port 25000 and outbound forwarding to a mock inbox via Mailpit), see:
* **[Local Email Testing Guide](docs/local-testing-guide.md)**

## Third-Party Libraries & Technologies
* **Spring Boot & Spring Framework**: Apache 2.0 License
* **Thymeleaf & Thymeleaf Extras Spring Security 6**: Apache 2.0 License
* **HTMX**: Zero-Clause BSD / MIT License
* **Bootstrap 5 & Bootstrap Icons**: MIT License
* **SubEthaSMTP**: Apache 2.0 License
* **OWASP Java HTML Sanitizer**: Apache 2.0 License
* **Flyway**: Apache 2.0 License
* **Hibernate ORM**: LGPL 2.1 License
* **H2 Database**: MPL 2.0 / EPL 1.0 License

## License
XCMailr is licensed under the Apache Version 2.0 license.
See the [LICENSE](LICENSE) file for the full license text.
