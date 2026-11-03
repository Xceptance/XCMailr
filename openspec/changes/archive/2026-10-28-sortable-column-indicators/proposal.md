# Proposal

## Why

In the modernized web console, mailboxes default to sorting by creation date (`id DESC`), but column headers currently lack visual affordance when inactive. Users cannot visually discern that columns are sortable until they click them. Furthermore, classic arrow icons are visually dated compared to modern UI component systems (like Bootstrap 5 / Tailwind / shadcn), and the indicators must remain strictly responsive across mobile viewports (`< 576px`) without awkward text wrapping or horizontal layout breakage.

Adding modern sort indicators (`bi-chevron-expand` for inactive sortable columns, and `bi-chevron-up` / `bi-chevron-down` for active sorted columns) provides immediate visual clarity, preserves mobile responsiveness across all device breakpoints, and maintains the primary user journey where newly created mailboxes appear at the top by default.

## What Changes

- Update sortable column headers in the mailbox table (`Address`, `Status`, `Expires`, `Forwards`, `Suppressed`) to use modern Bootstrap chevron icons:
  - Inactive sortable columns render `bi bi-chevron-expand` with subtle opacity (`opacity-50 text-muted`).
  - Active ascending column renders `bi bi-chevron-up text-primary`.
  - Active descending column renders `bi bi-chevron-down text-primary`.
- Preserve default sort order by creation date (`id DESC`), ensuring newly created mailboxes appear immediately at the top of the list upon creation.
- Ensure strict mobile responsiveness:
  - Header links use `d-inline-flex align-items-center gap-1 text-nowrap` to prevent the chevron from breaking onto a secondary line on narrow viewports (`< 576px`).
  - Responsive column visibility breakpoints are preserved: on mobile phones (`< 576px`), only `Address` (with its chevron), `Mails`, and `Actions` are shown; `Status`, `Expires`, `Forwards`, and `Suppressed` remain cleanly collapsed into the address row subline.
  - Interactive touch area on mobile is thumb-friendly (`py-1`).
- Update theme CSS (`xceptance-theme.css`) to provide smooth hover and focus transitions for `.sortable-header` and `.sort-icon`, respecting light and dark mode palettes.
- Add comprehensive testing following the test pyramid (unit/template assertions, slice tests, edge cases, mobile breakpoint markup validation).

## Capabilities

### Modified Capabilities
- `web-console-ui`: Update the `Mailbox Dashboard and Interactive Operations` requirement to specify visual sort indicators (modern inactive chevrons and active direction chevrons) and responsive small-screen display behavior while retaining `id DESC` default sort.

## Impact

- **UI Templates**: `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/mailbox-table.html`
- **Styles**: `xcmailr-webapp/src/main/resources/static/css/xceptance-theme.css`
- **Automated Tests**: `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailboxSortingWebTest.java` (and related web slice tests)
- **Dependencies & Backend APIs**: No backend database schema, API, or query changes required.

