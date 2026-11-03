# Proposal: Mailbox Row Action Binding Integrity Across Sort Orders (Issue #83)

## Why

In the legacy XCMailr application (Issue #83, *"Management operations may target the wrong mailbox"*), sorting the mailbox table caused row management actions (such as delete, edit, or toggle) to target the wrong mailbox. This occurred because the legacy AngularJS client passed the view's sorted `$index` into controllers that indexed into an unsorted client-side memory array (`$scope.filteredBoxes[$index]`).

In modernized XCMailr, the architecture was fundamentally redesigned to use server-side HTML rendering (Thymeleaf) and server-driven interactions (HTMX). Every row element embeds the entity's immutable primary key (`${box.id}`) directly into its action attributes (`hx-delete`, `hx-get`, `hx-post`, and URLs). While this design eliminates client-side positional indexing desynchronization by architectural construction, we need automated regression and slice tests that explicitly verify this invariant across sort permutations, proving that row actions strictly bind to the displayed mailbox and that legacy Issue #83 cannot occur.

## What Changes

- Add specification requirements and scenarios under `web-console-ui` specifying row action binding integrity across sort orders.
- Add comprehensive automated tests in `MailboxSortingWebTest` (and/or `MailboxSortingIntegrationTest`) verifying that:
  - Mailboxes rendered in sorted views (`sort=address&dir=desc`, `sort=address&dir=asc`, etc.) have their row DOM elements, action buttons (`hx-delete`, `hx-get` edit modal, `hx-post` toggle, `hx-post` extend, `hx-post` reset-counters), and links strictly parameterized with the primary key of the displayed mailbox.
  - Slicing individual rows from the rendered HTML fragment confirms zero cross-talk between mailbox IDs (e.g., Row 1 displaying mailbox B binds exclusively to B's ID, while Row 2 displaying mailbox A binds exclusively to A's ID).
  - Executing a row's delete action deletes only the targeted mailbox in the database, leaving the other mailboxes intact.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-console-ui`: Add the `Mailbox Row Action Binding Integrity Across Sort Orders (Special Case / Issue #83)` scenario under `Dynamic Mailbox Dashboard` requirement to ensure that management operations on sorted tables strictly target the displayed mailbox's primary key and prevent sort-induced action desynchronization.

## Impact

- **Test Suite**: Adds dedicated slice/integration tests in `xcmailr-webapp` (`MailboxSortingWebTest` / `MailboxSortingIntegrationTest`).
- **Production Code**: No production code changes are required as the modernized HTMX/Thymeleaf architecture already binds IDs directly; this change formalizes the specification requirement and regression test safety net.
- **Dependencies & APIs**: No external dependency changes or API contract breaking changes.

