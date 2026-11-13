# Proposal: Mailbox Table Server-Side Sorting

## Why

During the modernization of XCMailr to Spring Boot and HTMX, column sorting on the mailbox dashboard was lost. In the legacy version, sorting was performed client-side on the currently loaded page, which caused Issue #91 where sorting only reordered the current page instead of the entire dataset. Users need the ability to sort their full list of mailboxes by address, expiration, status, or traffic counters cleanly across pages without regressions.

## What Changes

- Add server-side sorting for the mailbox dashboard table powered by Spring Data JPA `Pageable` and HTMX partial swaps.
- Make the primary mailbox table headers clickable to toggle sort directions (ASC / DESC), complete with visual direction indicators (arrows/chevrons).
- Support sorting across core attributes: Address (address/domain), Status (expired), Expiration (ts_Active), Forwards (forwards), and Suppressed (suppressions).
- Ensure sorting sorts the entire user dataset at the database level before applying pagination limits (preventing Issue #91).
- Preserve active sort field and direction across HTMX operations: live search filtering, status selection, pagination page navigation, and action-driven table refreshes (`mailboxChanged`).
- Maintain a safe whitelist of sortable fields in `MailboxWebController` to guard against invalid parameters or unexpected JPA queries.
- Adhere to strict Java code standards: declare `final` wherever possible (parameters, local variables, fields), provide full Javadoc on all modified/new methods, and include clear explanatory code comments.
- Establish comprehensive automated tests following the testing pyramid (unit, web slice, and integration tests) covering happy paths, error cases, and special cases to avoid manual testing and prevent testing gaps.

## Capabilities

### New Capabilities

<!-- None -->

### Modified Capabilities

- `web-console-ui`: Adds server-side column sorting, whitelist fallback safety, and sort state preservation to the Dynamic Mailbox Dashboard requirement.

## Impact

- `MailboxWebController`: Accept `sort` and `dir` request parameters, map allowed fields to JPA `Sort` orders, and pass `Sort` inside `PageRequest.of(...)`. Pass current sort parameters to the template model.
- `mailbox-table.html`: Update table header markup to provide HTMX sort links/buttons with active sort direction icons, add hidden inputs for `sort` and `dir` inside `#mailbox-table-container`, and include sort parameters in pagination controls and `hx-include`.
- Tests: Multi-tiered testing pyramid covering unit tests (whitelist & defaults), web slice tests (HTMX fragments & header markup), and database integration tests (multi-page distribution & Issue #91 verification).
