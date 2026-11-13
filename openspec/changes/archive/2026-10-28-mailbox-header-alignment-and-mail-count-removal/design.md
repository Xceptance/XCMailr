# Design

## Context

See `proposal.md` for motivation. In the current implementation:
1. `MailboxWebController` iterates over the current page of `MBox` entities and executes `mailRepository.countByMailbox(box.getId())` for each mailbox, populating a `Map<Long, Long> mailCounts` passed into the model.
2. In `mailbox-table.html`, the `<thead>` element defaults to Bootstrap 5's `vertical-align: bottom`. Sortable column headers (`Address`, `Status`, `Expires`, `Forwards`, `Suppressed`) are wrapped inside `<a class="... d-inline-flex align-items-center py-1">` containers, establishing an ~32px content height with 0.25rem (4px) bottom padding. Non-sortable headers like `Actions` contain bare text without a wrapper or padding, causing the text baseline to rest directly on the bottom cell padding, 4px lower ("deeper") than the sortable headers. The select-all checkbox also rests on the bottom rather than being vertically centered.

## Goals / Non-Goals

**Goals:**
- Eliminate the N+1 SQL query pattern on every mailbox list render by removing `mailCounts` computation from `MailboxWebController`.
- Remove the "Mails" column header and body cell from `mailbox-table.html`, adjusting the empty table row `colspan` from 8 to 7.
- Harmonize the vertical alignment of all table header elements so that `Actions`, sortable column headers, and the select-all checkbox align uniformly in the center of the header row.
- Ensure strict Java code quality standards: `final` on all method parameters and local variables, descriptive Javadoc, and clear comments.
- Update and extend automated tests following the test pyramid to verify the 7-column layout, absence of `mailCounts`, and correct HTML structure.

**Non-Goals:**
- Modifying email inspection functionality (`/mailboxes/{id}/mails` or `/mails`).
- Changing sorting logic, status filtering, or batch action operations.
- Altering other admin or user tables that already use uniform plain text headers.

## Decisions

### Decision 1: Complete Removal of `mailCounts` vs Caching / Batching
- **Chosen Approach**: Completely remove the `mailCounts` map and repository queries from `MailboxWebController`.
- **Alternatives Considered**:
  - *Keep column and batch count queries with `GROUP BY mailboxId`*: Still adds database overhead for a metric not present in legacy XCMailr and redundant with mailbox detail views.
  - *Keep column with cached counts*: Adds cache invalidation complexity without user need.
- **Rationale**: The user clarified that the calculated mail count did not exist in the unmodernized version and is unwanted. Users can easily view their stored emails by clicking the mailbox address or choosing "Inbox Mails" from the action menu.

### Decision 2: Header Alignment via Matching Flex Wrapper and Row Alignment
- **Chosen Approach**:
  1. Wrap the text `Actions` in `<span class="d-inline-flex align-items-center py-1">Actions</span>`.
  2. Apply `class="align-middle"` to the header row `<tr class="align-middle">` (or `<thead class="table-light align-middle">`).
- **Alternatives Considered**:
  - *Remove `py-1` from sortable headers*: Would reduce tap target size on mobile and touch screens, violating responsive touch guidelines.
  - *Use custom CSS rules only*: Adding a custom CSS rule without matching markup still leaves container height discrepancies between raw inline text and flex anchors.
- **Rationale**: Wrapping `Actions` in `<span class="d-inline-flex align-items-center py-1">Actions</span>` gives it the exact same box height (~32px) and vertical padding as the sortable headers. Combining this with `align-middle` ensures that the select-all checkbox, sortable column headers, and the `Actions` column header all center cleanly along the exact same vertical baseline.

## Risks / Trade-offs

- **[Risk] Existing tests expecting `Mails` column header or `mailCounts` model attribute**
  - *Mitigation*: Update `MailboxSortingWebTest.java` (which checks for `<th scope="col" class="text-center" style="width: 75px;" title="Stored emails in webmail inbox">Mails</th>`) to verify the updated 7-column header markup and verify that `mailCounts` is no longer added to the model.
- **[Risk] Mobile responsive layout distortion**
  - *Mitigation*: Removing the "Mails" column frees up horizontal space. `Actions` remains styled with `white-space: nowrap` and `text-end`, preserving right-aligned kebab and action button placement across mobile and desktop.

## Migration Plan

- In-place controller and template update.
- No database migration or data conversion required.

