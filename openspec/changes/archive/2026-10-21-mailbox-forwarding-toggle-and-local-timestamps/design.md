# Design

## Context

In `MailboxWebController.java`, the `createMailbox` endpoint defines `@RequestParam(value = "forwardEmails", defaultValue = "true")`. Standard HTML forms omit unchecked checkboxes from the submission payload. Consequently, unchecking the forwarding toggle in `modal-new.html` results in an omitted parameter that Spring MVC assigns the default value `true`, preventing users from creating non-forwarding mailboxes.

Concurrently, server-side date rendering across Thymeleaf templates uses a static `DateTimeFormatter` locked to `ZoneId.systemDefault()`. In Docker or Linux server runtimes, the system timezone defaults to UTC. The resulting bare formatted timestamp (`yyyy-MM-dd HH:mm`) lacks timezone information. For users in non-UTC regions (such as CEST / UTC+2), creating a mailbox with a 2-hour duration yields a UTC expiration timestamp equal to the user's current local wall-clock time, creating an optical illusion that the mailbox expired upon creation.

## Goals / Non-Goals

**Goals:**
- Correct the `createMailbox` parameter default to `defaultValue = "false"` so omitted checkboxes evaluate to `false`, allowing mailboxes to be created with forwarding disabled.
- Adopt standard HTML5 `<time class="local-time" th:datetime="${...}" th:text="${...}">` elements across mailbox tables, modal dialogs, and mail detail views.
- Provide an ISO-8601 UTC formatter (`DateTimeFormatter.ISO_INSTANT`) in controller models to populate machine-readable `datetime` attributes.
- Integrate an unobtrusive vanilla JavaScript formatter into `htmx.onLoad` using the browser's native `Intl.DateTimeFormat` to format timestamps into the user's localized system time matching their desktop clock.
- Maintain graceful degradation with UTC fallback text inside `<time>` tags for environments without JavaScript.
- Construct comprehensive tests spanning the testing pyramid (unit, web integration, and template contract tests) covering happy path, error, and special boundary conditions, strictly utilizing `final` modifiers.

**Non-Goals:**
- Adding user profile timezone fields or database-level timezone persistence.
- Altering the underlying epoch-millisecond storage (`long ts_Active`) in the `MBox` entity.
- Introducing external JavaScript dependencies (such as Moment.js or Day.js).

## Decisions

### 1. Endpoint Default Value Alignment
- **Decision**: Update `MailboxWebController.createMailbox` to use `@RequestParam(value = "forwardEmails", defaultValue = "false") final boolean forwardEmails`.
- **Rationale**: Aligns directly with `updateMailbox` (`defaultValue = "false"` at line 312). When the toggle is checked, the browser submits `forwardEmails=true`. When unchecked, the browser omits the parameter, causing Spring MVC to resolve it to `false`.
- **Alternatives Considered**: Adding a hidden input `<input type="hidden" name="_forwardEmails" value="false">` (Spring MVC convention). While effective, changing the parameter default is cleaner, eliminates redundant form fields, and matches the established controller pattern.

### 2. Browser-Native Localization via `Intl.DateTimeFormat`
- **Decision**: Render dates in HTML5 `<time class="local-time" datetime="YYYY-MM-DDTHH:mm:ssZ">` elements and format them client-side using `Intl.DateTimeFormat`.
- **Rationale**: Built into all modern browsers; handles daylight saving time, leap years, and user locale without manual math or calculation errors.
- **Alternatives Considered**:
  - *Server-side UTC label (`yyyy-MM-dd HH:mm UTC`)*: Avoids JavaScript, but forces users to perform mental time arithmetic.
  - *Third-party date libraries*: Unnecessary asset bloat given standard browser `Intl` capabilities.

### 3. HTMX Lifecycle Integration via `htmx.onLoad`
- **Decision**: Attach the localization script to `htmx.onLoad(function(content) { ... })` in `base.html`.
- **Rationale**: XCMailr relies on HTMX swaps for mailbox table re-rendering, pagination, search, and modal loading. `htmx.onLoad` executes on initial document load and on every HTMX fragment swap, ensuring newly rendered rows are immediately localized.

### 4. Controller Model ISO Timestamp Provider
- **Decision**: Supply an ISO-8601 UTC formatter (`DateTimeFormatter.ISO_INSTANT`) to template models, or expose a utility method to generate ISO timestamps for epoch values.
- **Rationale**: Enables Thymeleaf templates to generate compliant `datetime="2026-10-21T14:13:00Z"` attributes from epoch milliseconds (`box.ts_Active`).

### 5. Testing Pyramid Strategy
- **Decision**: Structure tests into three distinct layers:
  - **Unit Tests**: Request parameter handling, duration calculations, edge cases (duration = 0 for unlimited, negative duration fallback, boundary limits).
  - **Controller / Web Tests**: `MockMvc` tests verifying form submission with `forwardEmails` present and omitted, checking repository persistence.
  - **Contract / Template Tests**: Verifying that rendered HTML fragments contain `<time class="local-time">` tags with valid ISO `datetime` attributes.
- **Convention**: Strictly apply `final` modifiers to all variables, parameters, and fields across all test and source files.

## Risks / Trade-offs

- **[Risk]**: JavaScript is disabled or fails to load.
  - **Mitigation**: The `<time>` element contains a fallback UTC string rendered server-side, ensuring timestamps remain readable and informative.
- **[Risk]**: Mailbox with unlimited expiration (`ts_Active == 0`) formatted incorrectly as epoch zero (1970-01-01).
  - **Mitigation**: Retain `th:if="${box.ts_Active == 0}"` template branching to render "Unlimited" explicitly, bypassing the `<time>` element for non-expiring mailboxes.

