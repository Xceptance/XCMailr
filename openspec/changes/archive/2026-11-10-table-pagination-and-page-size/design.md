# Design

## Context

The web management console provides 5 paginated tabular views:
- `/mailboxes` (`mailboxes/fragments/mailbox-table.html`): Self-service mailbox management
- `/admin/users` (`admin/fragments/user-table.html`): Administrator user account auditing
- `/admin/transactions` (`admin/fragments/transaction-table.html`): Inbound SMTP transaction logs
- `/mailboxes/{id}/mails` (`mailboxes/fragments/mail-list.html`): Single mailbox email inspection
- `/mails` (`mailboxes/fragments/all-mail-list.html`): Unified multi-mailbox inbox

Currently, table footers render an unbounded list of page buttons when datasets grow (e.g. 40+ pages in active test environments), causing button bars to wrap or break page layouts. Furthermore, none of the tables provide an interactive page size selector. Historical production implementations utilized a compact 7-slot sliding window with ellipsis (`...`) and a symmetrical segmented Display Count button group (`5`, `15`, `25`, `50`, `All`).

See `proposal.md` for background and user feedback motivation, and `specs/web-console-ui/spec.md` for formal requirement contracts.

## Goals / Non-Goals

**Goals:**
- Enforce a strict maximum of 7 page slots across all paginated tables whenever total pages exceed 7, using non-interactive ellipsis items (`...`) for omitted ranges.
- Implement an interactive, production-styled Display Count button bar (`5`, `15`, `25`, `50`, `All`) with symmetrical Bootstrap small pagination styling (`.pagination.pagination-sm mb-0`).
- Ensure navigation and size selector controls remain visible when data exists (`totalElements > 0`), including single-page results (`totalPages == 1`), while disabling inactive controls.
- Maintain full hypermedia state preservation: pagination links, sortable column headers, and Display Count buttons must preserve `page`, `size`, `sort`, `dir`, and active filters (`search`, `status`).
- Guarantee single-item and bulk action isolation: table row actions bind strictly to entity database primary keys (`id`), and bulk checkbox selections are isolated per page swap.
- Defensively clamp `size` parameters (`Math.min(Math.max(1, size), 1000)`) and reject or sanitize malicious inputs to strictly prevent SQL injection.
- Implement comprehensive unit, integration, and security tests following the testing pyramid, applying `final` modifiers and thorough Javadoc everywhere.

**Non-Goals:**
- Client-side JavaScript pagination or in-memory grid table components (tables remain 100% server-driven via Thymeleaf and HTMX).
- Persisting page size preferences across sessions in user profile database columns (stored per URL/HTMX request).
- Modifying REST API endpoints (`/api/v1/**`), which already utilize standard Spring Data `Pageable` parameters.

## Decisions

### 1. 7-Slot Sliding Pagination Algorithm in `PaginationHelper`

**Decision:** Create a shared utility class `com.xceptance.xcmailr.util.PaginationHelper` annotated as a Spring component (`@Component("paginationHelper")`) with a public static method `getPageSequence(final int currentPage, final int totalPages)`.

**Algorithm Details:**
Page numbers are 1-based for human display and SpEL template evaluation.
- When $N \le 0$: returns `List.of()`.
- When $N \le 7$: returns sequential list $[1, 2, \dots, N]$.
- When $N > 7$: returns an immutable list of exactly 7 integers, where `-1` represents an unclickable ellipsis (`...`):
  - **Start Case** ($P \le 4$): `[1, 2, 3, 4, 5, -1, N]`
  - **End Case** ($P \ge N - 3$): `[1, -1, N - 4, N - 3, N - 2, N - 1, N]`
  - **Middle Case** ($4 < P < N - 3$): `[1, -1, P - 1, P, P + 1, -1, N]`

```
Case 1 (Near Start, P <= 4):
+---+---+---+---+---+-----+---+
| 1 | 2 | 3 | 4 | 5 | ... | N |
+---+---+---+---+---+-----+---+

Case 2 (Near End, P >= N - 3):
+---+-----+-----+-----+-----+-----+---+
| 1 | ... | N-4 | N-3 | N-2 | N-1 | N |
+---+-----+-----+-----+-----+-----+---+

Case 3 (Middle, 4 < P < N - 3):
+---+-----+-----+---+-----+-----+---+
| 1 | ... | P-1 | P | P+1 | ... | N |
+---+-----+-----+---+-----+-----+---+
```

**Template Invocation:**
Thymeleaf evaluates the list via Spring bean invocation:
```html
<th:block th:each="p : ${@paginationHelper.getPageSequence(page.number + 1, page.totalPages)}">
    <li th:if="${p == -1}" class="page-item disabled" aria-hidden="true">
        <span class="page-link">&hellip;</span>
    </li>
    <li th:if="${p != -1}" class="page-item" th:classappend="${p == page.number + 1} ? ' active' : ''">
        <a class="page-link" ... th:text="${p}">1</a>
    </li>
</th:block>
```

**Alternatives Considered:**
- *Pure Thymeleaf `#numbers.sequence` arithmetic*: Rejected because conditional ellipsis logic with multiple branches in Thymeleaf templates creates complex, unmaintainable, and untestable template code.
- *External pagination taglib/library*: Rejected to avoid adding third-party dependencies for a 40-line pure algorithmic helper.

---

### 2. Segmented Display Count Bar Styled with `.pagination.pagination-sm`

**Decision:** Match the visual appearance and responsive alignment of the production UI by rendering the Display Count bar on the right side of the table footer using Bootstrap 5 `.pagination.pagination-sm mb-0` styling.

```
+---------------------------------------------------------------------------------------------------+
| [ Previous ] [ 1 ] [ 2 ] [ 3 ] [ ... ] [ 12 ] [ Next ]       [ Display Count ] [ 5 ] [ 15 ] [ 25 ] [ 50 ] [ all ] |
+---------------------------------------------------------------------------------------------------+
```

**Layout & Presets:**
- **Container**: `<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3">`
  - Left: Pagination bar (`Previous`, numbers, `Next`)
  - Right: Display Count group (`Display Count`, `5`, `15`, `25`, `50`, `All`)
- **Label Item**: `<li class="page-item disabled"><span class="page-link text-muted" th:text="#{macro_EntriesPerPage}">Display Count</span></li>`
- **Preset Values**:
  - `5`: Compact inspection
  - `15`: Standard default for Mailboxes and Users
  - `25`: Standard default for Mails and Transactions
  - `50`: High density list
  - `All` (`size=0` / `size=1000`): Full dataset view
- **Active State**: Highlighted with `.active` class matching active pagination button styling.

**Alternatives Considered:**
- *HTML `<select>` dropdown*: Rejected because it requires 2 clicks (open dropdown, select option), does not match the production UI screenshot, and is visually inconsistent with the segmented pagination bar buttons.
- *Bootstrap Button Group (`.btn-group`)*: Rejected because button groups have different height, borders, and active styling compared to `.pagination`, creating visual asymmetry. Using `.pagination` on both sides provides exact pixel-level height and border symmetry.

---

### 3. Hypermedia State Preservation & ID-Targeted Action Safety

**Decision:** Ensure all navigation links (page numbers, Previous/Next, sort headers, and size presets) carry the complete state tuple: `(page, size, sort, dir, search, status)`.

**State Propagation Rules:**
1. **Size Preset Clicks**: Reset to `page=0`, carry `size={preset}`, `sort=${sort}`, `dir=${dir}`, `search=${search}`, `status=${status}`.
2. **Page Navigation Clicks**: Carry requested `page`, `size=${page.size}`, `sort=${sort}`, `dir=${dir}`, `search=${search}`, `status=${status}`.
3. **Column Sort Header Clicks**: Reset to `page=0`, carry `sort={column}`, `dir={toggledDir}`, `size=${page.size}`, `search=${search}`, `status=${status}`.
4. **Search / Status Filter Submissions**: Reset to `page=0`, carry `size=${page.size}`, `sort=${sort}`, `dir=${dir}`.

**Single-Item Action Safety:**
Row action buttons (e.g. edit modal, delete modal, toggle status, extend expiration, reset counters) bind directly to the immutable database entity ID (`th:attr="data-id=${box.id}"`, `th:hx-post="@{/mailboxes/{id}/toggle(id=${box.id})}"`), never table row index ($0 \dots N$). Changing sort order or page size reloads rows with their exact entity IDs, preventing actions from misrouting.

**Bulk Action Safety:**
Bulk action checkboxes are bound to row entity IDs (`name="mailboxIds" value="${box.id}"`). When HTMX swaps `#mailbox-table-card`, existing listeners reset the master select-all checkbox and hide the floating batch action bar, preventing stale selection leaks across table refreshes.

---

### 4. Defensive Backend Clamping & SQL Injection Immunity

**Decision:** Harden all 3 web controllers (`MailboxWebController`, `AdminWebController`, `MailWebController`) with uniform size parameter handling and verify multi-vector SQL injection immunity.

**Clamping Logic:**
```java
final int effectiveSize = switch (size)
{
    case 0 -> 1000; // "All" preset maps safely to defensive upper bound
    default -> Math.min(Math.max(1, size), 1000);
};
```
- A size of `0` denotes the `All` preset and queries up to 1000 rows without memory overflow.
- Negative sizes or values exceeding 1000 are clamped strictly between 1 and 1000.
- Out-of-bounds `page` values are clamped via `Math.max(0, page)`.

**SQL Injection Protection Hierarchy:**
1. **Type Binding**: Spring MVC parses `int page` and `int size`. String or SQL payloads (e.g. `size=15; DROP TABLE mbox;--`) fail at HTTP parameter binding before controller invocation, returning HTTP 400 Bad Request.
2. **Whitelist Mapping for Sorting**: `sort` and `dir` parameters are validated against strict compile-time whitelists (`resolveSortField`, `resolveSortDirection`). Unrecognized or injected column expressions safely fall back to the default column (`id`) and direction (`desc`), preventing ORDER BY injection.
3. **Parameterized JPA Queries**: Filter parameters (`search`, `status`) are bound strictly as positional/named parameters in Spring Data JPA queries (`:search`, `:status`), preventing SQL structure manipulation.

---

### 5. Code Quality & Testing Pyramid Strategy

**Decision:** Apply `final` modifiers to all variables, parameters, and utility classes, add exhaustive Javadoc comments, and organize automated testing into 3 tiers:

```
                  / \
                 /   \
                / SEC \      Tier 3: Security & SQL Injection Tests
               /-------\     (MockMvc invalid parameter injection, ORDER BY attacks)
              /   INT   \    Tier 2: Web MVC & Integration Tests
             /-----------\   (MockMvc happy path, size switching, sort preservation)
            /    UNIT     \  Tier 1: Pure Unit Tests
           /---------------\ (PaginationHelperTest 100% branch & boundary coverage)
```

1. **Tier 1 (Unit Tests)**: `PaginationHelperTest`
   - Happy paths: $N \le 7$, Start case ($P \le 4$), End case ($P \ge N-3$), Middle case ($4 < P < N-3$).
   - Error cases: $N \le 0$ returns empty list; $P \le 0$ normalized to 1; $P > N$ normalized to $N$.
   - Special cases: $N = 8$ boundary (no middle), $N = 9$ (first single middle slot), $N = 100$ (large dataset).
   - Size clamping: `clampPageSize` with 0, negative, within bounds, and > 1000.
2. **Tier 2 (Web MVC Tests)**:
   - Happy path: tables render 7-slot pagination and Display Count controls with active size.
   - Size switching: clicking presets `5`, `15`, `25`, `50`, `All` loads correct slice and marks active preset.
   - State preservation: sort headers contain `size`, pagination links contain `sort` and `dir`.
   - Single-page visibility: when `totalPages == 1`, controls remain visible with disabled navigation links.
3. **Tier 3 (Security Tests)**: `TableSecuritySqlInjectionTest`
   - Injection payloads against `size`: `' OR '1'='1`, `15; DROP TABLE mbox;--`, `1 UNION SELECT 1,2,3--` -> HTTP 400 Bad Request.
   - Injection payloads against `page`: `0' OR '1'='1`, `0; DROP TABLE users;--` -> HTTP 400 Bad Request.
   - Injection payloads against `sort`: `address; DROP TABLE mbox;--`, `extractvalue(...)` -> sanitization to `id`.
   - Injection payloads against `dir`: `asc; DROP TABLE mbox;--` -> sanitization to `desc`.

## Risks / Trade-offs

- **[Risk] Large Datasets on "All" Selection (`size=0`)**
  - *Risk*: Users with thousands of emails or transactions clicking "All" could cause high memory allocation or slow database response times.
  - *Mitigation*: Hard-cap "All" at 1000 records (`PageRequest.of(0, 1000)`). If total records exceed 1000, the user receives the first 1000 records, preventing out-of-memory errors while satisfying the practical purpose of "All".
- **[Risk] Responsive Viewport Stacking on Narrow Mobile Screens**
  - *Risk*: On viewports narrower than 576px, displaying both a 7-slot pagination bar and a 6-button Display Count bar side-by-side could cause horizontal overflow.
  - *Mitigation*: The container uses `d-flex flex-wrap justify-content-between align-items-center gap-2`. On mobile viewports, the Display Count group wraps cleanly beneath the pagination bar.
- **[Risk] Loss of Custom Page Size on Table Actions (Create, Delete, Toggle)**
  - *Risk*: When a mailbox is toggled or deleted, the table refreshes. If the refresh request omits `size`, it reverts to default 15.
  - *Mitigation*: Action endpoints preserve active query parameters or trigger `mailboxChanged` events that instruct the client to re-fetch `/mailboxes` with the current URL search parameters.
