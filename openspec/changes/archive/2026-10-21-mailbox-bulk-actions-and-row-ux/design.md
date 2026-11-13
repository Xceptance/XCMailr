# Design

## Context

The modernized XCMailr application runs on Spring Boot 3, Thymeleaf, and HTMX. The mailbox dashboard (`/mailboxes`) renders an interactive table fragment (`mailbox-table.html`) that handles search, status filtering, and individual row operations. 

Prior to modernization, the legacy application supported:
1. Multi-mailbox selection (header checkbox + row checkboxes).
2. Bulk operations (enable, disable, extend validity, reset forwards/suppressions counters, delete).
3. Text file address exports (selected addresses, all addresses, active addresses).
4. Direct row action controls.

The modernized app currently lacks bulk selection and batch endpoints, and row actions are rendered as wide text buttons (`[Disable] [+24h] [v]`) that occupy substantial table width on desktop.

## Goals / Non-Goals

**Goals:**
- Provide full feature parity with pre-modernization bulk actions without regression.
- Render a contextual floating bottom pill/bar that appears only when one or more mailboxes are selected, showing the count, action buttons, and a deselect button.
- Implement secure, tenant-isolated Spring MVC batch endpoints in `MailboxWebController` (`POST /mailboxes/bulk/enable`, `disable`, `extend`, `reset-counters`, `export`, and `DELETE /mailboxes/bulk`).
- Provide plain text export endpoints for selected, all, and active mailbox addresses.
- Transition row actions on desktop from wide text buttons to compact icon/badge controls (`(Toggle) (+1d) [...]`) that collapse gracefully to `[...]` on mobile viewports (< 768px).
- Enforce strict Java conventions: full Javadoc, descriptive comments, `final` everywhere applicable, and yearless copyright headers.
- Build comprehensive automated tests following the testing pyramid (Unit, Integration, and WebMvc) covering happy paths, error cases, and security/edge cases.

**Non-Goals:**
- Cross-page persistent selection state (selection is scoped to the current active page, matching standard web application behavior).
- Multi-step modal date picker for bulk extension (the bulk bar uses a quick +24h extension matching row capabilities).

## Decisions

### 1. Floating Bottom Pill Architecture
- **Positioning**: Fixed at bottom center of the viewport (`position: fixed; bottom: 1.5rem; left: 50%; transform: translateX(-50%); z-index: 1040;`).
- **Styling**: Rounded pill container (`rounded-pill shadow-lg border bg-white p-2 d-flex align-items-center gap-2`), consistent with the calm corporate palette in `xceptance-theme.css`.
- **Visibility**: Hidden by default (`d-none`). A lightweight delegated change listener counts checked `.mailbox-select` inputs:
  - If count > 0: removes `d-none`, updates count text (`"N selected"`).
  - If count == 0: adds `d-none`.
- **Master Checkbox**: Header checkbox `#selectAllMailboxes` toggles all visible row checkboxes on the current page and updates indeterminate/checked states.

*Alternative Considered*: Permanent top dropdown toolbar (legacy style). Rejected because it clutters the UI when no items are selected and is offscreen when scrolled down long lists.

### 2. HTMX Batch Operation Pattern
- Batch buttons include checked inputs via `hx-include="input.mailbox-select:checked"`.
- Requests send form parameters: `mailboxIds=1&mailboxIds=2...`.
- Endpoints respond with HTTP 200 and `HX-Trigger: mailboxChanged` (or updated table fragment), refreshing the table in place while preserving the active search query and status filter.
- On table swap (`htmx:afterSwap`), the batch bar automatically resets and hides since row checkboxes re-render unchecked.

### 3. Tenant Isolation & Security
- To prevent Insecure Direct Object Reference (IDOR) attacks, the controller retrieves only the mailboxes owned by the authenticated user:
  ```java
  final List<MBox> userMailboxes = mailboxRepository.findAllById(mailboxIds).stream()
      .filter(box -> box.getUsr().equals(currentUser))
      .toList();
  ```
- Any ID submitted that does not belong to the authenticated user is silently discarded or rejected; cross-user mutations are strictly impossible.

### 4. Text Export Endpoints
- Implement `POST /mailboxes/bulk/export` accepting `mailboxIds` and returning `text/plain` with addresses separated by newlines.
- Implement `GET /mailboxes/export/all` and `GET /mailboxes/export/active` streaming user addresses as plain text.
- Provide backward-compatible routing/redirects for legacy paths (`/mail/mymaillist.txt`, `/mail/myactivemaillist.txt`, `/mail/myselectedmaillist.txt`).

### 5. Compact Icon-Based Hybrid Row Actions
- **Desktop (`>= 768px`)**:
  - `(Toggle)`: Button with `bi-power` (active, tooltip: "Disable mailbox") or `bi-play-fill` (expired, tooltip: "Activate mailbox").
  - `(+1d)`: Compact outline button with `bi-clock-history` or text badge `+1d` (tooltip: "Extend validity by 24 hours").
  - `[...]`: Button with `bi-three-dots-vertical` dropdown toggle, opening menu with text labels for `Inbox Mails`, `Edit Settings`, `Reset Counters`, and `Delete Mailbox`.
- **Mobile (`< 768px`)**:
  - Quick action buttons are hidden via responsive utility classes (`d-none d-md-inline-block`).
  - Single `[...]` kebab dropdown button displays all options including mobile-specific `Enable/Disable` and `Extend 24 Hours`.

## Risks / Trade-offs

- **[Risk]** HTMX DOM table replacement loses client-side checkbox state on search or filter change.
  - *Mitigation*: This is expected behavior; when the table contents refresh, selection resets and the floating bar hides automatically.
- **[Risk]** User deletes mailboxes by accident via bulk delete.
  - *Mitigation*: Enforce `hx-confirm="Are you sure you want to delete the selected mailboxes and all their emails?"` before the DELETE request is dispatched.
- **[Risk]** Large batch size performance.
  - *Mitigation*: Web table displays up to 50 items per page; batch processing 50 rows in a single JPA transaction takes < 20ms.

## Testing Strategy

The test suite will follow the testing pyramid with no manual testing required to verify correctness:
1. **Repository / Unit Tests**:
   - Verification of mailbox queries, counter resets, and expiration timestamp math.
2. **WebMvc / Controller Tests**:
   - **Happy Path**: Bulk enable, bulk disable, bulk extend (+24h), bulk reset counters, bulk delete, bulk export text.
   - **Error Handling**: Empty mailbox ID list, invalid IDs, missing parameters.
   - **Security / IDOR**: Submitting ID list with IDs belonging to another user verifies other user's mailboxes are never modified or deleted.
   - **Special Cases**: Mixed active and expired mailboxes in a single bulk operation, extending mailboxes that have unlimited expiration (`ts_Active == 0`), resetting counters when counters are already 0.

