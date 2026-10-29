# Design: Fix Mailbox Delete Blank Page Bug

## Context
See `proposal.md` for motivation.

In the mailbox management web UI (`mailbox-table.html`), the table container is structured as:
```html
<div th:fragment="table" id="mailbox-table-container"
     hx-get="/mailboxes"
     hx-trigger="mailboxChanged from:body"
     hx-target="#mailbox-table-container"
     hx-swap="outerHTML">
```
When a row mutation occurs (e.g. `DELETE /mailboxes/{id}`, `POST /mailboxes/{id}/toggle`, `POST /mailboxes/{id}/extend`, `POST /mailboxes/{id}/reset-counters`), `MailboxWebController` returns an HTTP 200 OK response with an empty body (`""`) and header `HX-Trigger: mailboxChanged`.

Currently, the action buttons in `mailbox-table.html` specify:
`hx-target='#mailbox-table-container'` and `hx-swap='outerHTML'`.
Upon receiving the response, HTMX performs the swap on the target first, replacing `#mailbox-table-container` with the empty body (`""`). By the time the `mailboxChanged` event fires on `body`, `#mailbox-table-container` has been removed from the DOM, leaving a blank white space on the page.

## Goals / Non-Goals

**Goals:**
- Eliminate the blank white page when deleting a mailbox or executing table row actions.
- Use HTMX event-driven reloading cleanly: row action buttons send requests with `hx-swap="none"` (no `hx-target`), allowing the container's event listener to refresh the table.
- Preserve active `search` and `status` filter parameters when the table refreshes via `mailboxChanged`.
- Apply consistent event-driven reloading patterns to admin user and domain management tables where similar empty-body swap bugs exist.
- Ensure all automated unit and integration tests pass.

**Non-Goals:**
- Altering the visual design or CSS styling of the mailbox table.
- Modifying the REST API (`/api/**`).
- Changing backend database deletion semantics or cascade rules.

## Decisions

### Decision 1: Use `hx-swap="none"` and remove `hx-target` on table row mutation buttons
- **Choice**: Remove `hx-target='#mailbox-table-container'` and set `hx-swap='none'` on:
  - Delete Mailbox button (`hx-delete`)
  - Status Toggle button (`hx-post`)
  - +24h Expiration button (`hx-post`)
  - Reset Counters button (`hx-post`)
- **Rationale**: When `hx-swap="none"` is configured, HTMX does not alter the DOM upon receiving the empty 200 OK response. It then processes the `HX-Trigger: mailboxChanged` response header, firing the event on `document.body`. `#mailbox-table-container` catches this event and issues its own `GET /mailboxes` request, replacing itself with the updated table fragment containing accurate counts, pagination, and empty-state handling.
- **Precedent**: This exact pattern is already implemented and working in `templates/mailboxes/fragments/mail-list.html` (line 47) for email deletion.
- **Alternatives Considered**:
  - *Returning table HTML directly from DELETE/POST*: Would require duplicating pagination and search querying logic inside every mutation endpoint in `MailboxWebController`.
  - *Using `hx-target="closest tr" hx-swap="delete"`*: Deleting only the row does not update the total counter ("X total mailboxes"), does not update pagination controls, does not render the "No mailboxes found" empty state if the last mailbox was deleted, and cannot be used for toggle/extend/reset actions.
  - *Returning HTTP 204 No Content*: While HTMX treats 204 as no-swap by default, existing Spring controller tests (`MailboxWebControllerTest`) explicitly verify `status().isOk()` (200). `hx-swap="none"` solves the issue on the client side without breaking existing API contracts or tests.

### Decision 2: Include search and filter parameters in the container reload
- **Choice**: Add `hx-include="[name='search'], #statusFilterSelect"` to `#mailbox-table-container`.
- **Rationale**: When `#mailbox-table-container` reloads in response to `mailboxChanged`, it performs `GET /mailboxes`. Including the active search input and status dropdown values ensures that any active search or status filtering remains intact after deleting or modifying a mailbox.
- **Alternatives Considered**:
  - *No `hx-include` on container*: Reloads would always reset to page 0 with empty search and "ALL" status, jarring the user if they were in the middle of filtered search results.

### Decision 3: Audit and fix admin table fragments
- **Choice**: In `templates/admin/fragments/user-table.html` and `templates/admin/fragments/domain-table.html`, ensure the container has the `hx-trigger="... from:body"` listener and the delete/toggle buttons use `hx-swap="none"`.
- **Rationale**: Prevent identical blank-screen issues in the admin console where `deleteUser`, `toggleActive`, `toggleAdmin`, and `deleteDomain` return empty bodies with `HX-Trigger: userListChanged` or `domainListChanged`.

## Risks / Trade-offs

- **[Risk] Search input selector `#statusFilterSelect` not found if included from another context**  
  → *Mitigation*: HTMX safely ignores missing CSS selectors in `hx-include` without generating JavaScript errors.
- **[Risk] Rapid consecutive clicks on action buttons**  
  → *Mitigation*: HTMX naturally handles in-flight request debouncing on the same element; destructive actions (like Delete) already display a confirmation prompt (`hx-confirm`).

