# Spec Delta

## ADDED Requirements

### Requirement: Table Pagination Window with Ellipsis Truncation
The system SHALL render pagination navigation bars across all paginated web console tables (`/mailboxes`, `/admin/users`, `/admin/transactions`, `/mailboxes/{id}/mails`, `/mails`) with at most 7 page slots using unclickable ellipsis items (`...`) for omitted ranges when the total number of pages exceeds 7, flanked by Previous and Next navigation controls, keeping controls visible when data exists and disabling inactive links.

#### Scenario: Pagination Window When Total Pages Is Seven or Fewer (Happy Path)
- **WHEN** an authenticated user views a table where total pages $N \le 7$
- **THEN** the pagination bar displays all page numbers $1$ through $N$ sequentially without ellipsis items

#### Scenario: Sliding Pagination Window Near Start (Happy Path)
- **WHEN** an authenticated user views a table where total pages $N > 7$ and the current page $P \le 4$
- **THEN** the pagination bar renders exactly 7 page slots formatted as `[1] [2] [3] [4] [5] [...] [N]`

#### Scenario: Sliding Pagination Window Near End (Happy Path)
- **WHEN** an authenticated user views a table where total pages $N > 7$ and the current page $P \ge N - 3$
- **THEN** the pagination bar renders exactly 7 page slots formatted as `[1] [...] [N-4] [N-3] [N-2] [N-1] [N]`

#### Scenario: Sliding Pagination Window in Middle (Happy Path)
- **WHEN** an authenticated user views a table where total pages $N > 7$ and $4 < P < N - 3$
- **THEN** the pagination bar renders exactly 7 page slots formatted as `[1] [...] [P-1] [P] [P+1] [...] [N]`

#### Scenario: Ellipsis Item Inactivity and Accessibility (Special Case)
- **WHEN** an ellipsis item is rendered in the pagination bar
- **THEN** the element is rendered as a disabled list item (`page-item disabled`) with `aria-hidden="true"`, preventing keyboard focus and mouse clicks from triggering navigation

#### Scenario: Single-Page Navigation Controls State (Happy Path)
- **WHEN** a table contains data that fits entirely on a single page (`totalPages == 1` and `totalElements > 0`)
- **THEN** the pagination bar remains visible displaying page `1` as active
- **AND** the Previous and Next navigation links are rendered in a disabled state (`page-item disabled`)

#### Scenario: Empty Table Pagination Visibility (Edge Case)
- **WHEN** a table query matches zero items (`totalElements == 0`)
- **THEN** the pagination bar and Display Count selector are omitted from the empty state view

### Requirement: Table Display Count Page Size Selector
The system SHALL render a segmented Display Count button group styled symmetrically with the pagination bar on all paginated web console tables, providing preset sizes `5`, `15`, `25`, `50`, and `All`, highlighting the active page size with visual distinction, resetting the view to page 0 upon size change, and preserving active sorting, direction, and search or filter parameters.

#### Scenario: Display Count Preset Selection (Happy Path)
- **WHEN** an authenticated user clicks a preset page size button (`5`, `15`, `25`, `50`, or `All`) on any paginated table
- **THEN** HTMX issues an asynchronous GET request with the selected `size` parameter and `page=0`
- **AND** the table refreshes displaying the corresponding number of items per page with the selected preset marked with the active style

#### Scenario: Display Count All Preset Handling (Happy Path)
- **WHEN** an authenticated user selects the `All` preset
- **THEN** the client transmits `size=0` (or `size=1000`)
- **AND** the server resolves the query without pagination limits up to the defensive maximum of 1000 items, displaying all records on page 0 with page count 1

#### Scenario: Display Count Preservation of Table State (Happy Path)
- **WHEN** an authenticated user changes the page size while an active search query, status filter, or column sort is applied
- **THEN** the resulting HTMX request and re-rendered table retain the active `search`, `status`, `sort`, and `dir` parameters

#### Scenario: Column Sort Preservation of Page Size (Happy Path)
- **WHEN** an authenticated user clicks a sortable column header while a non-default page size is active
- **THEN** the sorting request includes the current `size` parameter, ensuring page density is not reset to the default on column reordering

#### Scenario: Defensive Bounds Clamping for Page Size (Error / Boundary Case)
- **WHEN** an inbound request specifies a `size` parameter that is non-numeric, less than 1, or greater than 1000
- **THEN** the server defensively clamps the size within safe boundaries (between 1 and 1000) or falls back to the table default size without throwing server exceptions or causing database integer overflow

#### Scenario: SQL Injection Prevention in Page Size Parameter (Security Case)
- **WHEN** a malicious client supplies SQL injection payloads in the `size` query parameter (such as `size=15; DROP TABLE mailboxes;--` or `size=1' OR '1'='1`)
- **THEN** Spring MVC type conversion rejects the non-integer parameter with HTTP 400 Bad Request before controller execution or database query formulation
- **AND** no database error or SQL execution error is triggered

#### Scenario: Display Count Symmetrical Styling and Responsive Layout (Happy Path)
- **WHEN** a paginated table is rendered across desktop and mobile viewports
- **THEN** the pagination bar is positioned at the start (left) and the Display Count group is positioned at the end (right) using matching Bootstrap small pagination styling (`.pagination.pagination-sm mb-0`)
- **AND** on mobile viewports narrower than 576px, the controls wrap cleanly without horizontal clipping or overflow
