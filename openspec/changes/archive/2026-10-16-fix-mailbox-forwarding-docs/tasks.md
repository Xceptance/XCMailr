# Tasks

## 1. Local Testing Guide Alignment

- [x] 1.1 In `docs/local-testing-guide.md`, update Step 3 pre-flight smoke test payload to use `"forwardEnabled": true` and remove `"forwardAddress"`. Verify the JSON snippet syntax.
- [x] 1.2 In `docs/local-testing-guide.md`, update Step 4 verification notes to clarify that Mailpit displays forwarded emails addressed to the mailbox owner (`admin@xcmailr.test`).

## 2. Cloud Test Deployment Guide Alignment

- [x] 2.1 In `docs/cloud-test-deployment-guide.md`, update Section 6.2 to reference `"forwardEnabled": false` for load testing and `"forwardEnabled": true` for functional forwarding testing.
- [x] 2.2 In `docs/cloud-test-deployment-guide.md`, update Section 6.3 curl payload to use `"forwardEnabled": true`, remove `"forwardAddress"`, and update verification prose to state that the mail is forwarded to `admin@xcmailr.test`.

## 3. Documentation Consistency Verification

- [x] 3.1 Run repository search across `docs/` to confirm no invalid `"forwardAddress"` or erroneous `"forward": true` mailbox creation examples remain.

