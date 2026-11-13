# Design

## Context

The XCMailr web console is built with Spring Boot 4, Thymeleaf, HTMX 2.0.4, and Bootstrap 5.3.3.
In the current implementation:
- `#actionModal` is declared inside `templates/mailboxes/index.html` within `<main class="container my-4">`.
- In `xceptance-theme.css`, `body` is styled with `display: flex; flex-direction: column;` and `main` is styled with `flex: 1 0 auto;`.
- Under CSS Stacking Context specifications, any flex child element creates a local stacking context. When Bootstrap 5 instantiates a modal backdrop, it attaches `.modal-backdrop` directly to `<body>` with a z-index of 1050. Because `#actionModal` is trapped inside the lower-priority stacking context of `<main>`, the backdrop visually covers the modal, resulting in a dark grey screen with no interactive dialog.
- Furthermore, `#actionModal` includes an intermediate `<div id="modal-container">` wrapping `.modal-dialog`, disrupting Bootstrap 5's flexbox centering and dimension calculations.
- On the email delivery side, XCMailr receives inbound mail on port 25000 and forwards active mailboxes via Jakarta Mail (`OutboundMailService`) to `localhost:25` by default. Without a local mock SMTP relay, local testing of the forwarding pipeline and user verification fails with `Connection refused`.

## Goals / Non-Goals

**Goals:**
- Eliminate the CSS stacking context collision so the "New Mailbox" (and "Edit Mailbox") modal opens reliably in the foreground with working inputs and validation.
- Standardize Bootstrap 5 modal markup so that the shared modal container is placed at the document root level (`base.html`), ensuring uniform behavior across all views.
- Ensure HTMX form submissions in modals handle both success (smooth table refresh) and validation errors (inline feedback) without breaking DOM containers or closing prematurely.
- Provide a single, unified local developer profile (`application-dev.yml`) and Docker Compose setup for local development.
- Document the end-to-end local testing lifecycle (inbound port 25000, UI inspection, and outbound Mailpit port 1025/8025) alongside a production Google Workspace SMTP reference.

**Non-Goals:**
- Replace Bootstrap 5 or HTMX with a client-side JavaScript framework.
- Modify the database schema or core SMTP inbound RFC protocol implementation.
- Introduce cloud-hosted dependencies for local testing.

## Decisions

### 1. Host Shared Modal Directly in `base.html`
- **Choice**: Move `<div class="modal fade" id="actionModal" tabindex="-1">` to `templates/layout/base.html` right before `</body>`. Inside it, `<div class="modal-dialog" id="modal-container">` will directly host injected `.modal-content` fragments.
- **Rationale**: Bootstrap 5 documentation mandates placing modals as direct descendants of `<body>` to avoid CSS stacking context collisions with layout containers (`<main>`, `.container`, offcanvas wrappers).
- **Alternative Considered**: Setting custom `z-index: 99999` on `<main>`. Rejected because modifying stacking contexts of global page containers creates fragile CSS conflicts with navbars, toasts, tooltips, and offcanvas drawers.

### 2. Fragment Scope: `.modal-content` instead of Nested `.modal-dialog`
- **Choice**: Modal partials (`modal-new.html`, `modal-edit.html`) will define `th:fragment="modal"` on `<div class="modal-content">` rather than `<div class="modal-dialog">`.
- **Rationale**: Keeps the DOM hierarchy strictly conforming to `.modal > .modal-dialog > .modal-content`. Bootstrap's centering (`modal-dialog-centered`), sizing (`modal-lg`), and scrollability (`modal-dialog-scrollable`) can be controlled on the host dialog.
- **Alternative Considered**: Swapping the entire `.modal` element on every click. Rejected because destroying and re-instantiating the Bootstrap Modal JavaScript instance causes flicker and backdrop cleanup leaks.

### 3. Modal Form Submission Lifecycle via HTMX
- **Choice**: Form submissions in `modal-new.html` target `#mailbox-table-container` with `hx-swap="outerHTML"`. On success, the controller returns the updated `mailbox-table :: table` fragment with an `HX-Trigger: closeModal` response header. A small event listener on `document.body` closes the modal cleanly via `bootstrap.Modal.getInstance('#actionModal').hide()`. On validation failure, the controller returns the modal fragment with error messages intact.
- **Rationale**: Separates table re-rendering from modal dismissal and avoids inline `onsubmit` scripts that hide the dialog before the server validates the input.
- **Alternative Considered**: Redirecting the whole page (`HX-Redirect`). Rejected because it sacrifices the fast, seamless SPA experience provided by HTMX.

### 4. Unified Local Developer Configuration Profile (`application-dev.yml`)
- **Choice**: Introduce a single, comprehensive `application-dev.yml` configuration profile (`--spring.profiles.active=dev`):
  ```yaml
  logging:
    level:
      com.xceptance.xcmailr: DEBUG

  xcmailr:
    app:
      require-confirmation: true
    outbound-smtp:
      host: localhost
      port: 1025
      auth: false
      tls: false
      ssl: false
      enabled: true
  ```
  Provide a lightweight Docker Compose file (`docker-compose.mailpit.yml`) and document the `docker run` one-liner (`axllent/mailpit`).
- **Rationale**: Developers want a single configuration file (`application-dev.yml`) rather than juggling multiple ad-hoc files. Activating the `dev` profile configures XCMailr to connect directly to Mailpit on `localhost:1025` out of the box, enables verbose logging for mail delivery and controllers, and ensures user confirmation emails can be tested end-to-end.
- **Alternative Considered**: Multiple fragmented profiles (e.g., `application-mailpit.yml`, `application-debug.yml`). Rejected because having a single `dev` profile is standard in Spring Boot and provides the best developer experience.

## Risks / Trade-offs

- **[Risk] Multiple Modals on the same page**: If another feature requires a second modal concurrently, sharing `#actionModal` might cause conflicts.
  → **Mitigation**: XCMailr's workflow is strictly single-modal-at-a-time (New Mailbox, Edit Mailbox, Delete Confirm). Swapping `.modal-content` inside the shared host handles all current and planned modal interactions cleanly.
- **[Risk] Mailpit port conflict**: Developer might already have another service running on port 8025 or 1025.
  → **Mitigation**: Standardize on standard Mailpit defaults, and document how to override ports via standard Spring Boot environment variables (`SMTP_PORT=...`).

## Migration Plan

1. Update `base.html` to declare the global `#actionModal`.
2. Update `mailboxes/index.html` to remove the localized modal definition and bind the "New Mailbox" button.
3. Update `modal-new.html` and `modal-edit.html` fragments and their controller endpoints in `MailboxWebController`.
4. Add `application-dev.yml` and `docker-compose.mailpit.yml`.
5. Update documentation with testing walkthrough.
6. Verify with integration tests and manual browser test.
