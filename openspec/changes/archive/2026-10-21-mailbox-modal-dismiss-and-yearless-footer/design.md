# Design

## Context

In HTMX 4, the event triggering lifecycle was consolidated: the legacy `HX-Trigger-After-Swap` header was deprecated and removed because the primary `HX-Trigger` header is now processed in the `finally` block of request execution after DOM content has been swapped. When `MailboxWebController` creates or updates a mailbox, it was emitting `HX-Trigger-After-Swap: closeModal` alongside `HX-Trigger: mailboxChanged`. Because HTMX 4 ignores `HX-Trigger-After-Swap`, the browser client never received the `closeModal` custom DOM event, leaving the `#actionModal` dialog and its `.modal-backdrop` overlay active on the screen (causing the visible grey screen).

Simultaneously, `templates/layout/base.html` line 247 renders `&copy; 2013-2026 <a ...>Xceptance Software Technologies GmbH</a>`. Hardcoding year ranges in layout markup creates recurring annual maintenance chores and diverges from the repository-wide yearless copyright standard adopted in Java source files, `license-header.txt`, and `pom.xml`.

See `proposal.md` for overall motivation and `specs/web-console-ui/spec.md` for requirement deltas.

## Goals / Non-Goals

**Goals:**
- Eliminate the modal backdrop occlusion (grey screen) by emitting valid HTMX 4 trigger headers (`HX-Trigger: mailboxChanged, closeModal`) upon successful mailbox creation and edit.
- Harden the client-side `closeModal` DOM listener in `base.html` using `bootstrap.Modal.getOrCreateInstance(modalEl).hide()` to guarantee proper teardown of modal state, body classes (`modal-open`), and backdrop elements (`.modal-backdrop`).
- Convert the corporate footer copyright notice in `base.html` to a timeless yearless notice (`&copy; <a ...>Xceptance Software Technologies GmbH</a>. All rights reserved.`).
- Construct a comprehensive test suite across the testing pyramid (unit controller tests, client contract tests, and layout integration tests) covering happy paths, error cases, and edge cases, using strict `final` modifier conventions throughout.

**Non-Goals:**
- Modifying other mailbox operations that do not use modals (e.g., inline toggle, +1d extension, bulk operations).
- Changing Bootstrap modal animation durations or styling themes.
- Introducing client-side build tools or altering JavaScript packaging.

## Decisions

### 1. Unified `HX-Trigger` Header with Multiple Comma-Separated Events
- **Choice**: In `MailboxWebController.java`, emit `.header("HX-Trigger", "mailboxChanged, closeModal")` instead of separate or obsolete headers.
- **Rationale**: HTMX 4 parses comma-separated event strings in the `HX-Trigger` header (e.g., `event1, event2`) and dispatches each sequentially as a CustomEvent on the triggering element, bubbling to `document.body`.
- **Alternative Considered**: JSON object format (`{"mailboxChanged": null, "closeModal": null}`). While supported, the comma-separated string format is cleaner, standard, avoids JSON serialization overhead, and matches existing project patterns.

### 2. Resilient Client-Side Modal Dismissal via `bootstrap.Modal.getOrCreateInstance`
- **Choice**: In `templates/layout/base.html`, update the event listener to:
  ```javascript
  document.body.addEventListener('closeModal', function () {
      const modalEl = document.getElementById('actionModal');
      if (modalEl) {
          const modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
          modalInstance.hide();
      }
  });
  ```
- **Rationale**: `bootstrap.Modal.getInstance(modalEl)` returns `null` if the modal was initialized purely via Bootstrap data attributes and hasn't had its JS instance reference retained. `getOrCreateInstance` safely resolves or constructs the instance and performs a complete hide sequence, removing backdrop elements and unlocking body scrolling.

### 3. Yearless Copyright in Corporate Footer
- **Choice**: Update `base.html` from `&copy; 2013-2026 <a href="...">...</a>` to `&copy; <a href="...">...</a>`.
- **Rationale**: Conforms to the project-wide yearless copyright standard established in `standardize-yearless-copyright-headers`, preventing annual version bumps and maintenance noise.

### 4. Testing Pyramid Architecture
- **Choice**:
  - **Unit / Controller Tests (`MailboxWebControllerTest`)**: Test `createMailbox` and `updateMailbox` for HTTP 200 responses with exact `HX-Trigger` values, as well as validation errors (400/fragment re-render) ensuring no premature `closeModal` header is emitted.
  - **Contract Tests (`HtmxCsrfClientContractTest`)**: Assert HTMX response headers adhere to the hypermedia client contract.
  - **Integration / Layout Tests (`BaseLayoutFooterContractTest`)**: Use MockMvc to verify public and authenticated pages render the yearless copyright notice without hardcoded year numbers and contain the updated modal listener script.
- **Code Standards**: Strictly apply `final` to all class fields, method arguments, and local variables in new and modified test and production code.

## Risks / Trade-offs

- **[Risk] Multiple event triggers dispatched in a single `HX-Trigger` header** → **Mitigation**: Verified against HTMX 4 specifications; HTMX 4 explicitly supports comma-separated event names and dispatches both `mailboxChanged` and `closeModal`.
- **[Risk] Modal animation delay before DOM removal** → **Mitigation**: `modalInstance.hide()` immediately initiates Bootstrap fade-out and backdrop removal, while `mailboxChanged` refreshes `#mailbox-table-container` in parallel.
- **[Risk] Existing test suites failing on outdated `HX-Trigger-After-Swap` assertions** → **Mitigation**: Update assertions in `MailboxWebControllerTest` and `HtmxCsrfClientContractTest` during implementation.

## Migration Plan

1. Update `MailboxWebController.java` to set `HX-Trigger: mailboxChanged, closeModal`.
2. Update `templates/layout/base.html` with resilient modal hide listener and yearless copyright footer.
3. Update existing tests expecting `HX-Trigger-After-Swap`.
4. Add new test cases for controller validation errors and layout footer rendering contract.
5. Execute full test suite `mvn clean test` to confirm zero regressions.
6. Rollback plan: Standard git revert restores previous files if any unforeseen issue arises.

## Open Questions

- None.

