# Proposal

## Why

The pre-flight smoke test examples in `docs/local-testing-guide.md` and `docs/cloud-test-deployment-guide.md` provide an invalid mailbox creation payload containing `"forward": true` and `"forwardAddress": "..."`. Because the REST API DTO expects `forwardEnabled` and does not recognize `forwardAddress`, Jackson ignores both properties during deserialization. As a result, mailboxes are created with forwarding disabled (`forwardEmails = false`), inbound emails are never forwarded to Mailpit, and users testing container parity report unexpected failures.

## What Changes

- Update `docs/local-testing-guide.md` Step 3: Change `"forward": true` to `"forwardEnabled": true` and remove the non-existent `"forwardAddress"` property from the `curl` example.
- Update `docs/local-testing-guide.md` Step 4: Clarify that Mailpit receives forwarded messages addressed to the owning user (`admin@xcmailr.test`).
- Update `docs/cloud-test-deployment-guide.md` Section 6.2: Correct payload references from `"forward": false` / `"forward": true` to `"forwardEnabled": false` / `"forwardEnabled": true`.
- Update `docs/cloud-test-deployment-guide.md` Section 6.3: Update the mailbox creation snippet to use `"forwardEnabled": true`, remove `"forwardAddress"`, and explain that forwarded emails are dispatched to the mailbox owner (`admin@xcmailr.test`).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
None. (This is a documentation-only correction; system behavior and API specifications remain unchanged, so `skip_specs: true` is configured).

## Impact

- Affected documentation: `docs/local-testing-guide.md` and `docs/cloud-test-deployment-guide.md`.
- No changes to Java source code, REST API endpoints, database schemas, or dependencies.

