# Proposal: Functional Color Mode Persistence and Dark Mode Styling

## Why

In the modernized web console, users selecting dark mode experience jarring visual regressions:
1. Navigating to any link (e.g., Mailboxes, All Mails, or Administration pages) causes the application to immediately flash white or switch back to light mode, because the color mode preference is only stored in client-side `localStorage` while the server hardcodes `data-bs-theme="light"` on every HTTP response.
2. Tables across the entire application remain blindingly white in dark mode due to hardcoded `.bg-white` container wrappers and `.table-light` header classes.
3. The dark mode color palette suffers from visual clashes (mismatched dark tokens between Bootstrap and custom themes, and hardcoded `bg-white`/`bg-light` classes on card headers, form controls, tabs, and preformatted preview blocks).

Fixing these issues ensures a seamless, flicker-free dark mode experience across full-page navigations and HTMX updates with professional, accessible dark mode styling.

## What Changes

- **Cookie-Based Color Mode Persistence**:
  - Store the user's color mode choice (`light` or `dark`) in a persistent, secure cookie (`xcmailr_color_mode`) with a 1-year lifespan (`SameSite=Lax`).
  - Read `xcmailr_color_mode` in the backend (`ThemeResolver` / ControllerAdvice) and expose `activeColorMode` as a global model attribute.
  - Dynamically render `data-bs-theme="${activeColorMode != null ? activeColorMode : 'light'}"` directly on the root `<html>` element in `layout/base.html`, eliminating the white navigation flash (FOIT).
  - Streamline client-side JavaScript to manage the cookie as the single source of truth, retaining a minimal `<head>` inline script only to honor OS `prefers-color-scheme: dark` on initial visit before a cookie is established.
- **Dark Mode Table Styling**:
  - Remove hardcoded `.bg-white` from `.table-responsive` across all table fragments (`domain-table.html`, `transaction-table.html`, `user-table.html`, `mailbox-table.html`, `all-mail-list.html`, `mail-list.html`).
  - Remove `.table-light` from `<thead>` elements in all table fragments and in `admin/statistics.html` so headers inherit dark theme colors and borders.
  - Define dark mode table tokens and row hover states in both `xceptance-theme.css` and `emerald-theme.css`.
- **Color Design and Component Polish**:
  - Harmonize Bootstrap 5 dark theme CSS tokens (`--bs-body-bg`, `--bs-body-color`, `--bs-card-bg`, `--bs-border-color`) in `[data-bs-theme="dark"]` for both `xceptance` and `emerald` stylesheets.
  - Replace hardcoded `bg-white` and `bg-light` utility classes on card headers (`profile.html`, `mails.html`, `all-mails.html`, `mail-detail.html`), input groups (`domains.html`), tabs, and `<pre>` code preview containers (`mail-detail.html`) with semantic theme tokens (`bg-transparent`, `bg-body`, `bg-body-tertiary`).
  - Ensure high contrast ratios (WCAG AA) for transaction pills, chart tooltips, and badges in dark mode.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-console-ui`: Adding requirements for cookie-backed server-side color mode resolution (`activeColorMode`), zero-flash navigation persistence, and comprehensive dark mode styling across tables, cards, form controls, and theme stylesheets.

## Impact

- **Templates**: `layout/base.html`, `admin/statistics.html`, `admin/domains.html`, `user/profile.html`, `mailboxes/mails.html`, `mailboxes/all-mails.html`, and table/mail fragments.
- **Stylesheets**: `xceptance-theme.css` and `emerald-theme.css`.
- **Backend**: `ThemeResolver.java` updated to read and expose `activeColorMode`.
- **Tests**: Unit and integration tests in `ThemeResolverTest`, `WebNavigationSecurityTest`, and view rendering tests to verify cookie evaluation and dark mode attributes.
