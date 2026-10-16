# Design

## Context

See `proposal.md` for motivation.

In XCMailr's REST API, the data transfer object `MailboxData` exposes `public boolean forwardEnabled`. The Java client library `xcmailr-client` (`Mailbox.java` and `MailboxApiImpl.java`) and integration tests (`MailboxApiIT.java`) consistently use `forwardEnabled`.

Additionally, XCMailr's domain model ties email forwarding to the owning `User` account (`user.getMail()`) rather than persisting a separate forwarding address per mailbox (`MBox`). When an email arrives, `MailDeliveryService` verifies `mbox.isForwardEmails()` and forwards the message to `mbox.getUsr().getMail()`. In the Docker Compose test environment, the mailbox is created using the admin bearer token, so the owner is `admin@xcmailr.test`, and Mailpit captures the forwarded message for that address.

## Goals / Non-Goals

**Goals:**
- Align `docs/local-testing-guide.md` and `docs/cloud-test-deployment-guide.md` with the existing production REST API and domain model.
- Provide tested, copy-paste-ready `curl` commands for local container validation and cloud deployment smoke testing.
- Clearly describe where forwarded messages are routed in the test stack so users know what to expect in Mailpit.

**Non-Goals:**
- Modify Java source code, DTO annotations, or REST controllers.
- Introduce per-mailbox forwarding destinations to the data model or delivery service.

## Decisions

### Decision: Documentation alignment without API contract changes
- **Choice**: Update the documentation examples to use `"forwardEnabled": true` and remove `"forwardAddress"`.
- **Rationale**: Keeps the API contract strictly aligned with existing client libraries (`xcmailr-client`) and test suites without altering application code or introducing schema changes.
- **Alternative considered**: Adding Jackson `@JsonAlias({"forward", "forwardEnabled"})` to `MailboxData.java`. Decided against to keep this change strictly documentation-only per user preference.

### Decision: Explicitly document forwarding recipient behavior
- **Choice**: Document that Mailpit receives forwarded messages addressed to `admin@xcmailr.test` (the mailbox owner).
- **Rationale**: In the previous documentation, users were told to expect an email addressed to a fictitious address (`colleague@xceptance.com` or `qa-team@example.com`). Explaining that the mail routes to `admin@xcmailr.test` ensures test verification in Mailpit is unambiguous.

## Risks / Trade-offs

- [Risk] Ad-hoc API consumers might guess `"forward": true` instead of `"forwardEnabled": true`.
  → Mitigation: The local and cloud guides provide complete, working JSON payloads using `"forwardEnabled"`.

