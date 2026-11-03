# Technical Design: Load Test Documentation & Automated Roundtrip Verification

## Context

XCMailr consists of three primary modules:
- `xcmailr-webapp`: The modernized Spring Boot application hosting the web console, REST API, and embedded SubEtha SMTP server.
- `xcmailr-client`: The Java SDK providing fluent HTTP clients (`XCMailrClient`, `MailboxApi`, `MailApi`) for interacting with XCMailr's REST API.
- `xcmailr-load-test-suite`: An XLT-based load testing suite that exercises `XCMailrClient` and SMTP mail delivery under simulated load.

Currently, the load test suite lacks documentation describing scenario behaviors, required configuration properties, and manual invocation commands. Furthermore, while unit and component-level integration tests exist in `xcmailr-webapp` (`XcmailrClientIT` for REST, `SmtpDeliveryIT` for SMTP), there is no combined build-time test that executes the full roundtrip (REST mailbox creation -> SMTP message delivery -> REST mail retrieval and content verification -> REST mailbox cleanup).

## Goals / Non-Goals

**Goals:**
- Provide clear, comprehensive documentation in `xcmailr-load-test-suite/README.md` explaining manual execution, configuration flags, and XLT report interpretation.
- Implement an automated end-to-end integration test (`ClientSmtpRoundtripIT`) in `xcmailr-webapp` that executes on every `mvn test`, proving that the client SDK and SMTP server interoperate seamlessly.
- Enforce strict Java code quality standards in the new test code: mark parameters, local variables, and fields as `final` wherever possible, and provide thorough class and method Javadoc and explanatory comments.

**Non-Goals:**
- Do not run `xcmailr-load-test-suite` scenarios automatically during default `mvn test`. The load test suite is explicitly designed for high-concurrency load testing against external environments and must remain decoupled from the webapp build.
- Do not modify existing REST endpoints or `xcmailr-client` public APIs.

## Decisions

### Decision 1: Host the Automated Roundtrip Test in `xcmailr-webapp`
- **Choice**: Implement `ClientSmtpRoundtripIT` inside `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/client/`.
- **Rationale**: `xcmailr-webapp` already manages the embedded Spring Boot HTTP server (`webEnvironment = RANDOM_PORT`) and embedded SubEtha SMTP service (`SmtpServerService`). It already depends on `xcmailr-client`. Placing the test here allows automated execution during `mvn test` without adding circular dependencies or heavy server dependencies to `xcmailr-load-test-suite`.
- **Alternatives Considered**: 
  - *Adding webapp dependency to `xcmailr-load-test-suite`*: Rejected because `xcmailr-load-test-suite` is packaged and distributed to remote XLT load agent machines. Adding server dependencies would bloat the distribution artifact and create architectural coupling.

### Decision 2: Coding Standards (Finals, Javadoc, and Comments)
- **Choice**: In `ClientSmtpRoundtripIT` (and helper methods), declare all method parameters, local variables, and fields with the `final` modifier wherever possible. Provide comprehensive class-level and method-level Javadoc comments detailing test preconditions, step-by-step actions, and assertions.

### Decision 3: Async Ingestion Polling Pattern
- **Choice**: Implement polling with a timeout (e.g. up to 10 seconds with 250–500ms intervals) when querying `client.mails().listMails(...)` after sending the email via SMTP.
- **Rationale**: Inbound SMTP delivery and persistence occur asynchronously over network sockets. Mirroring the retry strategy used in `xcmailr-load-test-suite/Actions.java` prevents flakiness across varied hardware and CI runners.

### Decision 4: Structure of `xcmailr-load-test-suite/README.md`
- **Choice**: Structure documentation to cover:
  1. Purpose and Architecture (XLT + `XCMailrClient`).
  2. Available Scenarios (`CreateAndDeleteMailbox`, `SendMail`, `SendAndCheckMail`).
  3. Configuration Properties (`xcmailr.baseUrl`, `xcmailr.apiToken`, `smtp.host`, `smtp.port`).
  4. Manual Execution Guide with copy-pasteable Maven CLI commands.
  5. Inspecting XLT Result Browser reports (`results/.../index.html`).

## Risks / Trade-offs

- **[Risk] SMTP Delivery Latency in CI**: SMTP message processing is asynchronous and may take several hundred milliseconds under CI CPU throttling.  
  *Mitigation*: The test polls with `Thread.sleep(250)` up to 10 seconds before failing.
- **[Risk] Port Collisions**: Static SMTP ports (e.g. 25000) could conflict with other running processes during parallel builds.  
  *Mitigation*: The test reads `smtpServerService.getPort()`, dynamically allocated by the Spring Boot test context.
