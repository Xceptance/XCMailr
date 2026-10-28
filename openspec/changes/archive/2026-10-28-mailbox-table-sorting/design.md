# Design: Server-Side Mailbox Table Sorting

## Context

The modernized mailbox console uses Spring Boot 4.1.1, Spring Data JPA, Thymeleaf, and HTMX. Mailboxes are displayed in a paginated table (`/mailboxes` or `/`) backed by `MailboxRepository.findByUsr*` methods that accept a `Pageable`.

Currently, `MailboxWebController` hardcodes `PageRequest.of(..., Sort.by(Sort.Direction.DESC, "id"))`, and the `mailbox-table.html` template renders non-interactive `<th>` headers. To restore sorting while preventing legacy Issue #91 (where sorting only reordered the current page on the client), sorting must be executed server-side at the database layer before pagination slicing occurs.

## Goals / Non-Goals

**Goals:**
- Provide database-level multi-page sorting via Spring Data `Sort` and `Pageable`.
- Enable interactive header links/buttons in `mailbox-table.html` with sort toggle behavior and active direction icons.
- Ensure sort state is preserved across HTMX partial updates (pagination, live search, status filter, and `mailboxChanged` events).
- Secure query parameter parsing with an explicit whitelist of sortable fields.
- Adhere to strict Java code standards: enforce `final` keywords on all parameters and local variables, provide full Javadoc on all methods, and include clear explanatory code comments.
- Establish comprehensive automated test coverage following the testing pyramid (unit, web slice, and database integration tests) covering happy paths, error cases, and special cases to eliminate testing gaps.

**Non-Goals:**
- Client-side JavaScript table sorting.
- Sorting by the dynamically computed "Mails" count (which is calculated per page via `mailRepository.countByMailbox` rather than an entity column).
- Complex custom JPQL sort expressions or database migrations.

## Decisions

### 1. Server-Side JPA Sorting via Controller Whitelist
*Decision:* Accept `sort` and `dir` query parameters in `MailboxWebController.listMailboxes(...)` and map them via an explicit whitelist switch statement to Spring Data `Sort` objects.

*Mapping:*
- `"address"` -> `Sort.by(direction, "address").and(Sort.by(direction, "domain"))`
- `"status"` -> `Sort.by(direction, "expired")`
- `"expires"` -> `Sort.by(direction, "ts_Active")`
- `"forwards"` -> `Sort.by(direction, "forwards")`
- `"suppressions"` -> `Sort.by(direction, "suppressions")`
- default / unrecognized -> `Sort.by(Sort.Direction.DESC, "id")`

*Rationale:* Prevents `PropertyReferenceException` or unexpected query formation if an invalid sort parameter is passed. Delegates the SQL `ORDER BY` directly to Hibernate and the database prior to `LIMIT` / `OFFSET`, naturally resolving Issue #91.

### 2. HTMX Header Interaction and Visual State
*Decision:* Render clickable column headers in `mailbox-table.html` as HTMX GET buttons or links targeting `#mailbox-table-container` with `hx-swap="outerHTML"`.

*Behavior:*
- If column is currently active: clicking it toggles direction (`asc` <-> `desc`).
- If column is inactive: clicking it sets the column active with default direction `asc` (or `desc` for metrics/dates).
- Clicking any sort header resets `page` to `0` so the user sees the start of the newly sorted list.
- Header displays a direction icon:
  - Active ASC: `bi bi-arrow-up` (or `bi bi-chevron-up`)
  - Active DESC: `bi bi-arrow-down` (or `bi bi-chevron-down`)
  - Inactive: subtle muted indicator or no icon to keep headers clean.

### 3. State Preservation via Hidden Inputs and `hx-include`
*Decision:* Store the current `sort` and `dir` in hidden input fields within `#mailbox-table-container`:
```html
<input type="hidden" name="sort" th:value="${sort}" />
<input type="hidden" name="dir" th:value="${dir}" />
```

*Rationale:*
- `#mailbox-table-container` refreshes on `mailboxChanged from:body`. By including `[name='sort'], [name='dir']` in `hx-include`, auto-refreshes preserve the active sort without custom JavaScript.
- Live search inputs in `index.html` include `#mailbox-table-container`, ensuring live searches filter within the currently sorted order.
- Pagination buttons in `mailbox-table.html` include `sort=${sort}` and `dir=${dir}` in their `hx-get` URLs.

### 4. Code Standards & Style
*Decision:* Enforce strict code quality across all modified and new Java code:
- **`final` everywhere:** All method parameters, local variables, and class fields must be declared `final`.
- **Javadoc:** Comprehensive Javadoc comments on all modified and new classes, methods, parameters, return values, and exceptions.
- **Code comments:** Explanatory inline comments detailing the whitelist switch logic, fallback mechanisms, direction inversion, and model population.

### 5. Testing Pyramid Strategy
To eliminate testing gaps and avoid manual testing, verification is structured across three distinct tiers:

```
          / \
         /   \     Tier 3: Integration Tests (@SpringBootTest)
        /     \    - Real H2 persistence, SQL ORDER BY + LIMIT/OFFSET
       /-------\   - Issue #91 multi-page verification & special cases
      /         \
     /           \  Tier 2: WebMvc Slice Tests (@WebMvcTest / MockMvc)
    /             \ - HTMX partial swaps, fragment rendering, headers, icons
   /---------------\ - Error fallback status & query param propagation
  /                 \
 /                   \ Tier 1: Unit Tests (POJO / Controller logic)
/                     \ - Parameter extraction, whitelist mapping, defaults
-----------------------
```

#### Tier 1: Unit Tests (`MailboxWebControllerTest` / Logic Tests)
- **Happy Path:**
  - Whitelist resolution for all valid columns (`address`, `status`, `expires`, `forwards`, `suppressions`).
  - Direction resolution (`asc` -> `Sort.Direction.ASC`, `desc` -> `Sort.Direction.DESC`).
  - Direction toggling helper logic.
- **Error Cases:**
  - Null, empty, or whitespace sort parameters safely falling back to `id DESC`.
  - Unknown sort property (e.g. `sort="invalid"`) falling back to `id DESC`.
  - SQL injection or path traversal attempts (e.g. `sort="id; DROP TABLE..."`) falling back safely.
  - Invalid direction strings (e.g. `dir="sideways"`) safely falling back to default direction.
- **Special Cases:**
  - Case-insensitive parameter matching (`sort="ADDRESS"`, `dir="ASC"`).

#### Tier 2: WebMvc Slice Tests (`MailboxWebControllerWebMvcTest`)
- **Happy Path:**
  - HTMX GET `/mailboxes?sort=address&dir=asc` returns `mailbox-table :: table` fragment with 200 OK.
  - Rendered HTML contains active indicator icon on the Address header (`bi-arrow-up`).
  - Rendered HTML Address header link contains toggled direction target (`dir=desc`).
  - Hidden inputs `<input type="hidden" name="sort" value="address" />` and `<input type="hidden" name="dir" value="asc" />` are present in DOM.
  - Pagination links include active `sort` and `dir` parameters.
- **Error Cases:**
  - Request with `sort=invalid&dir=bad` succeeds with HTTP 200 and defaults to standard layout.
- **Special Cases:**
  - HTMX refresh via `mailboxChanged` includes `sort` and `dir` values.
  - Filter and search parameter combinations preserve sort parameters.

#### Tier 3: Database & Multi-Page Integration Tests (`MailboxSortingIntegrationTest`)
- **Issue #91 Verification (Multi-Page Ordering):**
  - Seed database with 30 mailboxes for a single user (e.g. addresses starting from `a@test.com` to `z@test.com`).
  - Query page 0 (size 10) sorted ASC -> verify it contains the 10 lowest addresses.
  - Query page 1 (size 10) sorted ASC -> verify it contains the next 10 addresses.
  - Query page 0 (size 10) sorted DESC -> verify it contains the 10 highest addresses.
  - Assert that sorting reorders across the entire dataset and NOT just within the active page slice.
- **Special Cases:**
  - Unlimited Expiration Ordering: Seed mailboxes with `ts_Active = 0` alongside finite future timestamps; verify sorting by `expires` executes deterministically without null pointer exceptions.
  - Empty Mailbox State: Query an account with zero mailboxes; verify no exception and valid page model.
  - Case-Insensitive Sorting: Verify mixed-case local addresses (`Alpha@test.com`, `bravo@test.com`) sort logically.

## Risks / Trade-offs

- **[Risk]** Unlimited mailboxes store `ts_Active = 0`, placing them first in standard numeric ASC order.
  *Mitigation:* Standard numeric sort is simple, deterministic, and requires no custom SQL expressions. In DESC order, upcoming expiration dates appear first and unlimited mailboxes appear at the bottom. Covered by automated Tier 3 tests.
- **[Risk]** User types into search input and active sort might be lost.
  *Mitigation:* Include `[name='sort']` and `[name='dir']` in the search field's `hx-include` attribute in `index.html` and in `#mailbox-table-container`. Verified by Tier 2 slice tests.
