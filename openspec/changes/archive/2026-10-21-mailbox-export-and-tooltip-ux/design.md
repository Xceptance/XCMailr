# Design: Mailbox Export Download and Tooltip UX Refinements

## Context

See [proposal.md](proposal.md) for background and motivation.

During dashboard modernization, row action buttons were configured with Bootstrap tooltips (`data-bs-toggle="tooltip"`). Because Bootstrap 5 appends tooltip elements to `document.body` and triggers tooltips on both hover and focus, clicking an action button that triggers an asynchronous HTMX table reload results in an orphaned `.tooltip` element permanently visible on screen.

Simultaneously, legacy bottom export links were moved to the top page header as an `Export ▾` dropdown button directly next to `+ New Mailbox`. This gives an auxiliary utility disproportionate visual hierarchy, while conflicting with the floating batch bar's bulk export feature. Furthermore, the export endpoints return `Content-Type: text/plain` without a `Content-Disposition: attachment` header, causing web browsers to open raw text in a new tab instead of initiating a file download.

## Goals / Non-Goals

**Goals:**
- Eliminate orphaned tooltip bubbles during HTMX swaps by replacing dynamic Bootstrap tooltip instantiation on table row buttons with robust native HTML `title` attributes.
- Restore visual balance to the dashboard header by removing the `Export ▾` dropdown and placing subtle utility text links (`All (.txt)` and `Active (.txt)`) in the table footer/pagination area.
- Ensure all text export endpoints (`/mailboxes/export/all`, `/mailboxes/export/active`, and `/mailboxes/bulk/export`) include `Content-Disposition: attachment; filename="..."` so browsers trigger genuine file downloads.
- Construct a comprehensive test pyramid with unit, controller, and template tests covering happy paths, error scenarios (unauthenticated access, cross-tenant isolation), and edge cases (empty results, inactive mailboxes, special characters).
- Enforce strict `final` modifier usage across all Java classes, methods, parameters, and local variables.

**Non-Goals:**
- Modifying the text format itself (addresses remain formatted one per line).
- Adding complex CSV or JSON export schemas (plain text address list satisfies existing user workflows).
- Altering the floating bulk batch bar behavior (its export action remains available when rows are selected).

## Decisions

### Decision 1: Native HTML `title` Attributes for Dynamic Table Actions
- **Choice**: Remove `data-bs-toggle="tooltip"` from row buttons in `mailbox-table.html` and rely exclusively on standard HTML `title` attributes. Remove the `htmx:afterSwap` Bootstrap tooltip re-initialization loop in `index.html`.
- **Rationale**: Bootstrap tooltips attached to `document.body` create lifecycle conflicts when elements are replaced asynchronously by HTMX. Native `title` attributes:
  1. Automatically close immediately upon user click or focus change.
  2. Require zero JavaScript execution or re-binding after DOM swaps.
  3. Never leak DOM nodes or create memory leaks.
  4. Are fully supported and accessible across all browsers and screen readers.
- **Alternatives Considered**: Adding custom JavaScript to intercept `htmx:beforeSwap` and call `.dispose()` on all Bootstrap tooltip instances. Rejected due to added complexity, edge cases with fast repetitive clicking, and susceptibility to timing race conditions.

### Decision 2: Relocate Text Export Links to Table Footer Utility Area
- **Choice**: Remove the `Export ▾` dropdown from `index.html`'s top header. Place two compact utility links (`All (.txt)` and `Active (.txt)`) in `mailbox-table.html` inside the footer row alongside the pagination controls.
- **Rationale**:
  - The top page header should reserve its primary visual weight for the main call-to-action (`+ New Mailbox`).
  - Placing export links in the table footer keeps them easily discoverable when reviewing mailbox lists without cluttering the main workspace.
  - Aligns with legacy XCMailr's proven utility link placement while styling them with modern muted typography (`text-muted small text-decoration-none`).
- **Alternatives Considered**: Placing an export dropdown in the search/filter toolbar. Rejected because the search bar is intended for filtering the view, and placing export there confuses filter state with download actions.

### Decision 3: Standard `Content-Disposition: attachment` Response Headers
- **Choice**: Update `renderAddressTextList` in `MailboxWebController.java` to set the `Content-Disposition` header with a descriptive filename:
  - `mailboxes.txt` for `/mailboxes/export/all`
  - `mailboxes-active.txt` for `/mailboxes/export/active`
  - `mailboxes-selected.txt` for `/mailboxes/bulk/export`
- **Implementation**:
  ```java
  final HttpHeaders headers = new HttpHeaders();
  headers.setContentType(MediaType.TEXT_PLAIN);
  headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
  return new ResponseEntity<>(sb.toString(), headers, HttpStatus.OK);
  ```
- **Rationale**: RFC 6266 specifies `Content-Disposition: attachment` as the standard mechanism instructing user agents to download content to local disk rather than displaying it inline in a viewport.
- **Alternatives Considered**: Client-side JavaScript blob generation (`data:text/plain...`). Rejected because native HTTP response headers are universally reliable, work directly with standard hyperlinks and form submissions, and require zero client-side script overhead.

### Decision 4: Test Pyramid Architecture
The test suite will follow a clear testing pyramid structure:

```
                  / \
                 /   \
                / MVC \      MailboxExportWebTest & MailboxBulkActionsWebTest
               / Integration \ (HTTP Security, Sessions, CSRF, Template Rendering)
              /---------------\
             /     Service     \   MailboxService & Repository Tests
            /    / Unit Tests   \  (Filtering, Active/Expired Partitioning)
           /---------------------\
```

1. **Happy Path Tests**:
   - Export all mailboxes returns 200 OK, `Content-Type: text/plain`, `Content-Disposition: attachment; filename="mailboxes.txt"`, and lists all user-owned addresses.
   - Export active mailboxes returns 200 OK, attachment header with `mailboxes-active.txt`, and includes only non-expired mailboxes.
   - Bulk export of selected IDs returns 200 OK, attachment header with `mailboxes-selected.txt`, containing only the selected mailboxes.
2. **Error Cases**:
   - Unauthenticated access to `/mailboxes/export/*` is rejected (HTTP 302 redirect to login or HTTP 401/403).
   - Cross-tenant access: user attempts to export mailbox IDs belonging to another user; system excludes foreign mailboxes and returns only user-owned addresses.
   - Tampered requests: invalid or malformed `mailboxIds` or `jsonObj` handled gracefully without 500 errors.
3. **Special Cases**:
   - User with 0 mailboxes receives an empty text file (200 OK with empty body and attachment header).
   - User with only inactive mailboxes receives an empty body on `/mailboxes/export/active`.
   - Addresses with uppercase characters or special email symbols are preserved exactly.
4. **UI & Template Tests**:
   - Mailboxes dashboard header renders `+ New Mailbox` without `Export ▾` dropdown button.
   - Mailbox table footer renders `All (.txt)` and `Active (.txt)` links.
   - Mailbox row action buttons render `title` and `aria-label` without `data-bs-toggle="tooltip"`.

## Risks / Trade-offs

- **[Native browser tooltip visual appearance differs from Bootstrap black pill]** → Mitigation: Native browser tooltips are clean, familiar, high-contrast, zero-maintenance, and completely free of DOM orphaning bugs.
- **[Existing test assertions depend on header export links]** → Mitigation: Update test assertions in `MailboxBulkActionsWebTest` to verify that export links appear in the table footer rather than the header.

