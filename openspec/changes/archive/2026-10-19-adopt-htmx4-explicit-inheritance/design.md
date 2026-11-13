# Design: Adopt Standard HTMX 4 Explicit Inheritance

## Context

During the upgrade to HTMX 4.0.0, `htmx.config.implicitInheritance = true` was introduced in `layout/base.html` to mirror legacy HTMX 2 behavior. However, in HTMX 4, setting `implicitInheritance: true` causes *all* HTMX attributes—including `hx-trigger`—to be inherited by descendants.

In `mail-list.html` and `all-mail-list.html`, the message table is wrapped in:
```html
<div id="mail-list-container" hx-trigger="mailListChanged from:body">
```
Descendant email rows (`<tr id="mail-row-..." class="mail-item-row" th:attr="hx-get=@{/mailboxes/{mbId}/mails/{mId}...}" hx-target="#mail-viewer-pane" hx-swap="innerHTML">`) do not declare an explicit `hx-trigger`. With `implicitInheritance: true`, HTMX 4 resolves `hx-trigger="mailListChanged from:body"` onto every row, attaching an event listener to `document.body` instead of a local `click` handler. As a result, clicking the row never issues the request to load the email preview in `#mail-viewer-pane`.

See `proposal.md` for motivation and scope boundaries.

## Goals / Non-Goals

**Goals:**
- Eliminate `implicitInheritance: true` from `layout/base.html` (both `<meta name="htmx-config">` and inline `<script>`), aligning the application with HTMX 4 standard explicit inheritance.
- Retain `noSwap: [204, 304, '4xx', '5xx']` to suppress raw error markup swapping.
- Annotate `<tr class="mail-item-row">` elements in `mail-list.html` and `all-mail-list.html` with explicit `hx-trigger="click"` for defensive self-documentation.
- Update the test suite following the test pyramid:
  - Controller slice test in `MailWebControllerTest`: verifies fragment resolution for `GET /mailboxes/{id}/mails/{mailId}` with `HX-Request: true`.
  - Static configuration contract test in `HtmxCsrfClientContractTest`: verifies HTMX configuration attributes in rendered HTML.
  - Interactive browser-level contract test in `HtmxCsrfClientContractTest` using HtmlUnit: simulates user clicking an email row and asserts `#mail-viewer-pane` loads the email preview.
- Enforce strict code quality standards across all changes: `final` keywords everywhere possible, full Javadoc, and structured `// GIVEN`, `// WHEN`, and `// THEN` comments.

**Non-Goals:**
- Upgrading or downgrading HTMX version (keep `org.webjars.npm:htmx.org:4.0.0`).
- Refactoring backend email retrieval services or domain models.
- Changing HTML layout structure or CSS styling beyond HTMX trigger attributes.

## Decisions

### Decision 1: Adopt Standard HTMX 4 Explicit Inheritance vs Container Scope Isolation

- **Choice**: Option A (Adopt Standard Explicit Inheritance by removing `implicitInheritance: true`).
- **Rationale**: An exhaustive audit of all 26 Thymeleaf templates revealed that zero elements in the codebase rely on implicit attribute inheritance from parent containers; every interactive element explicitly declares its own `hx-get`/`hx-post`/`hx-delete`, `hx-target`, and `hx-swap`. Turning on `implicitInheritance: true` inadvertently broke encapsulation by leaking container lifecycle triggers (`mailListChanged from:body`) to child rows and buttons. Removing it aligns with HTMX 4's primary design principle where inheritance is opt-in via `hx-inherit`.
- **Alternatives Considered**:
  - *Option B (Container Scope Isolation)*: Keep `implicitInheritance: true` and add `hx-disinherit="hx-trigger"` to container divs or decouple triggers to empty sentinel sibling divs. Rejected because it requires ongoing vigilance across every template to avoid trigger leakage, whereas Option A solves the issue holistically.

### Decision 2: Defensive `hx-trigger="click"` Declaration on Email Rows

- **Choice**: Explicitly declare `hx-trigger="click"` on `<tr class="mail-item-row">` in `mail-list.html` and `all-mail-list.html`.
- **Rationale**: In HTMX 4 without implicit inheritance, elements without a natural trigger (like `<tr>`) default to `click` when an `hx-get` is present. However, adding `hx-trigger="click"` makes the developer intent unambiguous, prevents regressions if table wrappers change, and documents hypermedia contracts directly in template markup.

### Decision 3: Multi-Layered Test Pyramid

- **Choice**: Implement tests across three levels:
  1. *Unit / Slice*: `MailWebControllerTest` tests `GET /mailboxes/{id}/mails/{mailId}` with `HX-Request: true` to verify that `mailboxes/fragments/mail-detail :: detail` is returned with the populated `mail` model attribute.
  2. *Configuration Contract*: `HtmxCsrfClientContractTest` verifies that rendered pages contain `<meta name="htmx-config">` with `noSwap` and without `implicitInheritance`.
  3. *Browser-Level Contract (HtmlUnit)*: `HtmxCsrfClientContractTest` tests user interaction end-to-end: loads the email list page, clicks `#mail-row-{mailId}`, lets HTMX handle the event and perform the DOM swap, and verifies that `#mail-viewer-pane` contains the email preview content.
- **Rationale**: Slice tests guarantee server contract correctness without browser overhead, while HtmlUnit tests verify the client-side JavaScript hypermedia contract in an integrated browser environment.

## Risks / Trade-offs

- **[Risk]** Removing `implicitInheritance: true` could break templates that inadvertently relied on inheriting attributes like `hx-target` or `hx-swap`.
  - **Mitigation**: All 26 Thymeleaf templates were audited in explore mode and confirmed to specify `hx-target` and `hx-swap` explicitly on each interactive element. Existing comprehensive WebMvc slice tests and contract tests cover all existing dashboard, mailbox, user, and domain operations.
- **[Risk]** HtmlUnit JavaScript execution timing with asynchronous HTMX DOM updates.
  - **Mitigation**: Use HtmlUnit's `webClient.waitForBackgroundJavaScript(...)` or poll `mailViewerPane.getTextContent()` with a short bounded timeout to ensure asynchronous hypermedia swaps settle reliably before assertions.

