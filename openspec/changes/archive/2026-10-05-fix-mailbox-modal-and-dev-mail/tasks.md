# Tasks

## 1. Modal Host Relocation and DOM Alignment

- [x] 1.1 Relocate `#actionModal` to `templates/layout/base.html` directly before `</body>` with `<div class="modal-dialog" id="modal-container">` to eliminate the CSS stacking context occlusion from `<main>`.
- [x] 1.2 Remove the redundant `#actionModal` declaration from `templates/mailboxes/index.html` and update the "New Mailbox" button trigger attributes.
- [x] 1.3 Update `templates/mailboxes/fragments/modal-new.html` and `templates/mailboxes/fragments/modal-edit.html` so `th:fragment="modal"` is rooted on `<div class="modal-content">`, ensuring strict Bootstrap 5 `.modal > .modal-dialog > .modal-content` hierarchy.

## 2. HTMX Form Lifecycle and Controller Handling

- [x] 2.1 Update `MailboxWebController` endpoints (`/mailboxes/new`, `POST /mailboxes`, `POST /mailboxes/{id}`) to return updated table fragments upon success with `HX-Trigger: closeModal`, and return modal partials with field errors upon validation failure.
- [x] 2.2 Add an event listener in `templates/layout/base.html` listening for the `closeModal` HTMX trigger to dismiss `#actionModal` smoothly using the Bootstrap JavaScript Modal API.
- [x] 2.3 Add unit and integration test cases in `MailboxWebControllerTest` verifying `GET /mailboxes/new` renders the modal form, successful creation refreshes the table with `HX-Trigger: closeModal`, and invalid input keeps the modal open with error indicators.

## 3. Unified Developer Configuration and Mailpit Integration

- [x] 3.1 Create `xcmailr-webapp/src/main/resources/application-dev.yml` with the unified developer profile targeting localhost:1025 without TLS/auth, enabling confirmation email flows, and adding verbose developer logging.
- [x] 3.2 Add `docker-compose.mailpit.yml` defining the `axllent/mailpit` service with ports 1025 (SMTP) and 8025 (Web UI).
- [x] 3.3 Create `docs/local-email-testing.md` with complete documentation for running `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`, testing inbound delivery (port 25000), dashboard mailbox inspection, outbound forwarding verification via Mailpit (port 8025), and contrast with production Google Workspace SMTP Relay configuration.

## 4. Verification and Safety Net

- [x] 4.1 Run the full Maven test suite (`mvn clean test`) and verify all controller, web security, and email forwarding tests pass.
- [x] 4.2 Run `openspec validate` to confirm all spec deltas and schema requirements pass validation.
