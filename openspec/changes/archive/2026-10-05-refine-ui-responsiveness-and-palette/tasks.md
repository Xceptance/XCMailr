# Tasks: Refine UI Responsiveness and Color Palette

## 1. Corporate Theme Tokens & Vector Icons

- [x] 1.1 Define reusable status dot classes (`.status-dot`, `.status-dot-active`, `.status-dot-expired`) and subtle badge utility tokens in `static/css/xceptance-theme.css`, verifying clean rendering in both light and dark themes.
- [x] 1.2 Replace raw OS emojis (`📋`, `📬`) with vector Bootstrap Icons (`<i class="bi bi-clipboard"></i>`, `<i class="bi bi-inbox"></i>`) and add inline clipboard copy feedback in `mailboxes/fragments/mailbox-table.html` and `mailboxes/mails.html`.

## 2. Mailbox Dashboard Table & Responsive Actions

- [x] 2.1 Remove hardcoded 930px min-width table column styles in `mailboxes/fragments/mailbox-table.html` and apply responsive column hiding classes (`d-none d-md-table-cell` on `Forwards` and `Suppressed`, `d-none d-lg-table-cell` on `Expires`), verifying columns degrade smoothly without horizontal scrollbars.
- [x] 2.2 Add a responsive metadata subline to the address cell in `mailboxes/fragments/mailbox-table.html` displaying status, expiration time, and forwarding state for viewports where separate columns are hidden.
- [x] 2.3 Implement responsive action button grouping in `mailboxes/fragments/mailbox-table.html`: display 1-click `Disable`/`Enable` and `+24h` buttons on desktop (`>= 768px`) while collapsing them into the `[ Actions ▾ ]` dropdown menu on mobile (`< 768px`), verifying all HTMX actions function correctly in both viewports.
- [x] 2.4 Update status badges and mail counters in `mailboxes/fragments/mailbox-table.html` to use subtle tokens (`bg-success-subtle`, `bg-body-secondary` when mail count is `0`, `bg-primary-subtle` when mail count `> 0`).

## 3. Search Bar, Header & Inbox View Responsiveness

- [x] 3.1 Refactor dashboard page header and search/filter bar in `mailboxes/index.html` to use responsive flex wrapping (`d-flex flex-wrap`), verifying the "+ New Mailbox" button and filter select stack cleanly on mobile viewports.
- [x] 3.2 Update `mailboxes/fragments/mail-list.html` and `mailboxes/mails.html` to use responsive column widths, subtle status badges, and streamlined action buttons.

## 4. Admin Views Palette & Verification

- [x] 4.1 Update `admin/fragments/user-table.html` and `admin/fragments/transaction-table.html` to adopt Bootstrap 5.3 subtle tokens (`*-subtle`) and responsive column hiding, verifying administrative tables render cleanly on mobile viewports.
- [x] 4.2 Standardize metric cards in `admin/statistics.html` to use cohesive neutral typography with subtle accents instead of competing saturated text colors.
- [x] 4.3 Run existing web console tests via `mvn test -Dtest=MailboxWebControllerTest,AdminWebControllerTest` and verify all routes and template fragments render without regressions.
