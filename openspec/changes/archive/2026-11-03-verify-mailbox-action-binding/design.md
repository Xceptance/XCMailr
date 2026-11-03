# Design: Mailbox Row Action Binding Integrity Verification (Issue #83)

## Context

See [proposal.md](file:///Users/rbaumgarten/checkouts/XCMailr/openspec/changes/verify-mailbox-action-binding/proposal.md) for background and motivation.

In the legacy architecture (`origin/master`), mailbox overview management operations relied on an AngularJS client-side view (`showAngularBoxOverview.ftl.html`) iterating over a filtered and sorted array (`tr ng-repeat="mBox in filteredBoxes | orderBy:predicate:reverse"`). Actions passed the DOM display position `$index` to controller methods such as `$scope.openDeleteBoxDialog(elementIdx)` (`controllers.js`), which looked up `$scope.filteredBoxes[elementIdx]`. Because `$scope.filteredBoxes` was the *unsorted* underlying array, changing the column sort order caused management actions (editing, deleting, toggling) to target the mailbox that originally occupied that position instead of the displayed mailbox (Issue #83).

In modernized XCMailr, the AngularJS layer was replaced with Spring Boot, Thymeleaf, and HTMX. Table rows are rendered server-side via `templates/mailboxes/fragments/mailbox-table.html`. Every row element (`<tr id="mailbox-row-${box.id}">`) embeds the database entity ID directly into HTMX attributes:
- `hx-delete="/mailboxes/{id}"`
- `hx-get="/mailboxes/{id}/edit"`
- `hx-post="/mailboxes/{id}/toggle"`
- `hx-post="/mailboxes/{id}/extend?hours=24"`
- `hx-post="/mailboxes/{id}/reset-counters"`
- `href="/mailboxes/{id}/mails"`

This design eliminates client-side array index desynchronization. However, regression test coverage is needed to formally prove and protect this invariant across all sort orders.

## Goals / Non-Goals

**Goals:**
- Add automated regression tests in `MailboxSortingWebTest` (and/or `MailboxSortingIntegrationTest`) verifying that row action bindings strictly match the displayed mailbox entity ID across sort permutations (`sort=address&dir=desc` and `sort=address&dir=asc`).
- Validate DOM row isolation: verify that slicing individual row fragments (`<tr id="mailbox-row-{id}">...</tr>`) confirms zero ID leakage or cross-talk between mailboxes.
- Validate management operation safety: verify that triggering a management operation (e.g. mailbox deletion) derived from the first row of a sorted table deletes only the displayed mailbox, leaving all other mailboxes intact.

**Non-Goals:**
- Modifying production controller, service, or template logic (the server-rendered HTMX architecture already binds IDs directly).
- Re-creating legacy AngularJS JavaScript unit tests.

## Decisions

### Decision 1: Test Placement in `MailboxSortingWebTest` and `MailboxSortingIntegrationTest`
- **Choice**: Add DOM slice and action binding verification methods in `MailboxSortingWebTest` (Tier 2 WebMvc slice) and verify delete action repository state in `MailboxSortingIntegrationTest` (Tier 3 database integration).
- **Rationale**:
  - `MailboxSortingWebTest` provides fast, focused validation of HTML markup, HTMX attributes, and rendered DOM row structure without database overhead.
  - Slicing the rendered `mailbox-table :: table` fragment allows exact assertion that Row 1's action targets belong exclusively to Row 1's mailbox.
  - Invoking the delete action verifies that end-to-end execution adheres to the expected database state changes.
- **Alternatives Considered**:
  - *End-to-End Selenium/Playwright browser test*: Heavyweight and slow. MockMvc slice testing inspects the exact rendered HTML and HTMX attributes that the browser executes.

### Decision 2: Row-Level DOM Slicing Strategy
- **Choice**: Extract the rendered HTML fragment string and split on row boundaries (`<tr th:each="box : ${mailboxes}" th:id="'mailbox-row-' + ${box.id}">` -> `<tr id="mailbox-row-...>`).
- **Structure**:
```
+-----------------------------------------------------------------+
| Table Body (<tbody id="mailbox-rows">)                         |
|                                                                 |
|  +-----------------------------------------------------------+  |
|  | Row 1 (<tr id="mailbox-row-2">)                           |  |
|  | Displays: zebra@xcmailr.test                              |  |
|  | Actions:  /mailboxes/2/toggle, /mailboxes/2/edit,         |  |
|  |           /mailboxes/2/extend, hx-delete="/mailboxes/2"   |  |
|  | Assert:   Contains ID "2", NO occurrence of ID "1"        |  |
|  +-----------------------------------------------------------+  |
|                                                                 |
|  +-----------------------------------------------------------+  |
|  | Row 2 (<tr id="mailbox-row-1">)                           |  |
|  | Displays: alpha@xcmailr.test                              |  |
|  | Actions:  /mailboxes/1/toggle, /mailboxes/1/edit,         |  |
|  |           /mailboxes/1/extend, hx-delete="/mailboxes/1"   |  |
|  | Assert:   Contains ID "1", NO occurrence of ID "2"        |  |
|  +-----------------------------------------------------------+  |
+-----------------------------------------------------------------+
```
- **Rationale**: Proves that sorting inverts the visual order without swapping, shifting, or cross-contaminating action targets.

### Decision 3: Action Execution Verification
- **Choice**: Perform a delete operation targeting the ID extracted from the first sorted row, then assert that:
  1. The target mailbox is deleted (`mailboxRepository.findById(targetId).isEmpty()`).
  2. The non-target mailbox remains intact (`mailboxRepository.findById(otherId).isPresent()`).
- **Rationale**: Directly addresses the critical failure mode of Issue #83 where deleting the top row deleted the wrong mailbox.

## Risks / Trade-offs

- **[Risk] Test fragility due to raw HTML string matching**
  - *Mitigation*: Match semantic substrings (e.g. `hx-delete="/mailboxes/` + id, `hx-post="/mailboxes/` + id + `/toggle`) rather than exact whitespace-sensitive full HTML tags.
- **[Risk] Deterministic sort ordering for fixture mailboxes**
  - *Mitigation*: Use clear, alphabetically distinct mailbox names (such as `alpha` and `zebra`) so that ascending and descending sort orders are unambiguous.

