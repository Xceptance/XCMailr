# Design

## Context

The mailbox table supports full-dataset server-side column sorting via HTMX (`sort` and `dir` query parameters), defaulting to mailbox ID descending (`id DESC`) so newly created mailboxes appear immediately at the top. 

However, column headers currently render directional indicators only when actively sorted, displaying no indicator when inactive. This makes sortable columns indistinguishable from static columns (`Mails`, `Actions`) on initial load and especially on touch devices (where hover cues do not exist). Additionally, classic arrows (`bi-arrow-up`, `bi-arrow-down`) appear dated and headers must be guaranteed to remain responsive on narrow viewports (`< 576px`) without awkward wrapping.

## Goals / Non-Goals

**Goals:**
- Provide clear visual affordances for all sortable columns (`Address`, `Status`, `Expires`, `Forwards`, `Suppressed`) in both inactive and active states.
- Use modern Bootstrap 5 chevron icons: `bi-chevron-expand` for inactive/sortable, `bi-chevron-up` for active ascending, `bi-chevron-down` for active descending.
- Ensure strict responsiveness on small screens (< 576px) by preventing icon wrapping (`text-nowrap`, `d-inline-flex`) and preserving thumb-friendly tap targets.
- Retain `id DESC` creation-order default sorting so the user experience when creating new mailboxes remains smooth.
- Support both light and dark themes using semantic theme utility classes (`text-body`, `text-body-secondary`, `text-primary`).
- Provide pyramid test coverage (WebMvc controller tests validating markup, active/inactive states, and responsive attributes).

**Non-Goals:**
- Client-side sorting libraries: sorting remains purely server-driven via HTMX.
- Backend SQL or repository query changes: existing sorting logic in `MailboxRepository` and `MailboxService` is fully functional and unchanged.

## Decisions

### Decision 1: Icon Family and State Representation
- **Inactive / Sortable State**:
  ```html
  <i th:if="${sort != 'address'}" class="bi bi-chevron-expand sort-icon text-body-secondary opacity-50 ms-1" aria-hidden="true"></i>
  ```
  - Displays a modern dual-chevron indicator (top and bottom arrows) indicating sortability.
  - Rendered with `opacity-50` and `text-body-secondary` for a subtle, uncluttered appearance that works in both light and dark modes.
- **Active Ascending State**:
  ```html
  <i th:if="${sort == 'address' and dir == 'asc'}" class="bi bi-chevron-up sort-icon text-primary ms-1" aria-hidden="true"></i>
  ```
- **Active Descending State**:
  ```html
  <i th:if="${sort == 'address' and dir == 'desc'}" class="bi bi-chevron-down sort-icon text-primary ms-1" aria-hidden="true"></i>
  ```
- *Alternatives considered*:
  - Single arrow icons (`bi-arrow-up`, `bi-arrow-down`): Rejected as dated and inconsistent with modern component design systems.
  - Opposed arrows (`bi-arrow-down-up`): Rejected because chevrons offer a cleaner, less heavy silhouette in table headers.

### Decision 2: Mobile Responsiveness and Touch Ergonomics
- Wrap header contents in:
  ```html
  <a href="javascript:void(0)" role="button"
     class="sortable-header text-decoration-none text-body d-inline-flex align-items-center gap-1 text-nowrap py-1"
     ...>
     <span>Address</span>
     <!-- chevrons -->
  </a>
  ```
- **`text-nowrap` & `d-inline-flex`**: Guarantees the column title and chevron stay bound together on a single line, preventing the chevron from wrapping underneath on narrow viewports.
- **Tap target size**: Adding `py-1` provides vertical padding, ensuring an accessible ~36px tap zone for mobile touchscreen thumbs without inflating table header row height.
- **Breakpoint visibility**:
  - `< 576px`: Only `Address`, `Mails`, and `Actions` columns render in the table header. The address header comfortably fits "Address ⯅⯆" (~70px width) within the available space (~180px–240px).
  - `576px - 767px`: `Status` column appears with its chevron.
  - `768px - 991px`: `Forwards` and `Suppressed` columns appear with their chevrons.
  - `≥ 992px`: `Expires` column appears with its chevron.
  - `.table-responsive` wrapping container guarantees safe horizontal containment under high system font zoom.

### Decision 3: CSS Styling in `xceptance-theme.css`
- Add dedicated rules:
  ```css
  .sortable-header {
      user-select: none;
      cursor: pointer;
  }
  .sortable-header:hover .sort-icon {
      opacity: 1 !important;
      color: var(--xc-primary);
  }
  .sort-icon {
      font-size: 0.8rem;
      transition: opacity 0.15s ease-in-out, color 0.15s ease-in-out;
  }
  ```
- On desktop, hovering over any sortable header highlights the chevron icon with the corporate primary brand color.
- On touch devices, the subtle opacity provides immediate affordance without requiring hover states.

### Decision 4: Pyramid Test Coverage Architecture
- **WebMvc Slice Tests** (`MailboxSortingWebTest.java`):
  - Happy path default view: verify `bi-chevron-expand` is rendered for inactive sortable columns when `sort` is omitted or defaults to `id DESC`.
  - Happy path active sort: verify `bi-chevron-up` is rendered for `dir=asc` and `bi-chevron-down` for `dir=desc` on the targeted column, while other columns retain `bi-chevron-expand`.
  - Responsive assertions: verify sortable header anchors contain `text-nowrap`, `d-inline-flex`, and `sortable-header` classes.
  - Error and fallback cases: verify invalid sort column or direction cleanly falls back to default creation order with neutral chevrons.
- **Code standards**:
  - Add `final` modifiers wherever possible in test and production classes.
  - Include comprehensive Javadoc comments explaining test intentions and edge cases.

## Risks / Trade-offs

- **[Risk]** Extra horizontal width of the chevron icon could cause header overflow on ultra-compact 320px screens.  
  → **Mitigation**: Total width of "Address" + chevron is ~70px, which fits inside the 160px+ available space of the Address column. `text-nowrap` prevents line breaks, and `.table-responsive` provides clean horizontal scrolling as a fallback.
- **[Risk]** Dim inactive icons could fail WCAG contrast guidelines if too faint.  
  → **Mitigation**: Using `text-body-secondary` with `opacity-50` ensures that light and dark modes adapt dynamically, keeping icons visible while clearly subordinated to the active sort state.

