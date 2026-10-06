# Proposal: Document and Automate Load Test Verification

## Why

The `xcmailr-load-test-suite` module validates critical production scenarios (mailbox provisioning and SMTP-to-REST mail polling) using `XCMailrClient`, but lacks documentation on how to configure and execute scenarios manually against running servers. Furthermore, while isolated REST client tests and SMTP delivery tests run during `mvn test`, there is no combined automated end-to-end test executed on every build that exercises the full roundtrip (create mailbox via client -> deliver email via SMTP -> retrieve and verify via client -> delete mailbox via client). Adding this documentation and an automated build-time roundtrip test ensures ongoing stability and clear developer ergonomics.

## What Changes

- **Load Test Documentation**: Create `xcmailr-load-test-suite/README.md` documenting:
  - Architecture and role of the XLT load test suite.
  - Available scenarios (`CreateAndDeleteMailbox`, `SendMail`, `SendAndCheckMail`).
  - Configuration properties (`project.properties`, system properties `-Dxcmailr.baseUrl`, `-Dxcmailr.apiToken`, `-Dsmtp.port`).
  - Manual execution instructions with concrete CLI commands against local and remote XCMailr instances.
  - Interpreting XLT test results and HTML reports in `results/`.
- **Automated End-to-End Smoke Test**: Add a comprehensive end-to-end integration test in `xcmailr-webapp` that executes automatically with `mvn test`:
  - Spins up both the embedded Spring Boot HTTP server (`RANDOM_PORT`) and embedded SubEtha SMTP server.
  - Creates a mailbox using `XCMailrClient`.
  - Sends a multipart (HTML + plain text) email to that mailbox over the embedded SMTP socket.
  - Polls and verifies receipt of the email using `XCMailrClient.mails().listMails(...)` and `getMail(...)`.
  - Cleans up the mailbox via `XCMailrClient.mailboxes().deleteMailbox(...)`.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `integration-safety-net`: Adds automated end-to-end roundtrip verification connecting `XCMailrClient` with the inbound SMTP engine and REST API during build testing.

## Impact

- **Documentation**: New file `xcmailr-load-test-suite/README.md`.
- **Testing**: New integration test class in `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/client/` (e.g. `ClientSmtpRoundtripIT.java`).
- **Dependencies & Production Code**: No production code or dependency changes; 100% backward compatible.

