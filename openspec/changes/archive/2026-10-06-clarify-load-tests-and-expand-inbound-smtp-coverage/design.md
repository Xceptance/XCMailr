# Design

## Context

See `proposal.md` for motivation. XCMailr's inbound mail pipeline is driven by an embedded SubEthaSMTP listener (`SmtpServerService`) coupled with `MailDeliveryService`. `MailDeliveryService` handles `accept` (SMTP `RCPT TO` anti-relay validation) and `deliver` (MIME parsing, database persistence, transaction logging, and forwarding).

In the load testing suite (`xcmailr-load-test-suite`), `Utils` instantiates a shared static `XCMailrClient` during class loading (`<clinit>`), which validates that `xcmailr.apiToken` is not blank. However, `SendMail` solely dispatches email via standard SMTP on port 25000 and never invokes the REST client. Furthermore, uncreated addresses on whitelisted domains are accepted on the SMTP socket and silently black-holed by design, creating ambiguity in user expectations.

Currently, integration tests in `SmtpDeliveryIT` only cover unmanaged domain relay rejection, active mailbox delivery, expired mailbox delivery, and loop detection. Base unit tests for `MailDeliveryService` do not exist.

## Goals / Non-Goals

**Goals:**
- Clarify `xcmailr-load-test-suite/README.md` to explain the architectural difference between SMTP load generation (`SendMail`) and REST API validation (`SendAndCheckMail`), detailing why `SendMail` accepts any non-blank token and black-holes unknown addresses.
- Fill coverage gaps in `SmtpDeliveryIT` by adding integration test methods for non-existent mailbox drops (status 100), inactive owning user drops (status 600), and oversized mail stream rejections.
- Establish mock unit test coverage at the base of the test pyramid by creating `MailDeliveryServiceTest` for all routing and transaction disposition branches.
- Enforce clean coding standards across new tests: `final` on all local variables, parameters, and fields, complete Javadocs, and explanatory comments.

**Non-Goals:**
- Modifying Java sources or configuration in `xcmailr-load-test-suite` (explicit user directive: leave load test suite code untouched).
- Changing production logic in `MailDeliveryService`, `SmtpServerService`, or Spring Security.
- Modifying REST API schemas or behavior.

## Decisions

### 1. Document Protocol Separation in Load Test README
- *Decision*: Update `xcmailr-load-test-suite/README.md` with an architectural callout box comparing `SendMail` (pure SMTP throughput benchmark, no REST authentication, black-holes uncreated mailboxes) with `SendAndCheckMail` (full E2E lifecycle asserting REST authentication and database storage).
- *Rationale*: Prevents user confusion when running `SendMail` with arbitrary tokens without modifying the test suite code.
- *Alternatives Considered*: Modifying `Utils.java` to lazily instantiate `XCMailrClient` only when requested. Rejected because the user explicitly instructed not to touch the load test suite code.

### 2. Isolated Mock Unit Tests for `MailDeliveryService`
- *Decision*: Create `MailDeliveryServiceTest` using JUnit 5 and Mockito (`@ExtendWith(MockitoExtension.class)`).
- *Rationale*: Unit tests execute in under 100ms without starting an embedded HTTP/SMTP server, allowing exhaustive testing of edge cases (e.g., malformed email strings, database whitelists vs property whitelists, inactive users, loop headers, and size limits).
- *Alternatives Considered*: Relying exclusively on integration tests (`SmtpDeliveryIT`). Rejected because testing every edge case through live socket I/O is slow and conflicts with the test pyramid principle.

### 3. Supplement `SmtpDeliveryIT` for Protocol Integration
- *Decision*: Add test methods in `SmtpDeliveryIT`:
  - `testInboundDeliveryToNonExistentMailbox()`
  - `testInboundDeliveryToInactiveUser()`
  - `testOversizedEmailRejection()`
- *Rationale*: Verifies that SubEthaSMTP interacts correctly with the live database and returns the expected SMTP status codes (`250 OK` for drops, `552` for size exceeded) over a real TCP socket.

## Risks / Trade-offs

- **[Risk] SmtpDeliveryIT socket binding in sandbox environments** $\rightarrow$ *Mitigation*: The test class binds to ephemeral port 0 and tests run cleanly when network sandbox permissions are granted.
- **[Risk] Size limit test allocating large memory buffers** $\rightarrow$ *Mitigation*: Use a configured small limit or generate a controlled byte payload slightly above the configured limit.
