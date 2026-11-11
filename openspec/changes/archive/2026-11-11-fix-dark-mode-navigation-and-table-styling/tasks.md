# Tasks

## 1. Backend Color Mode Resolution

- [x] 1.1 Add `COLOR_MODE_COOKIE_NAME`, `COLOR_MODE_DARK`, `COLOR_MODE_LIGHT` constants and `@ModelAttribute("activeColorMode")` method to `ThemeResolver.java` to read the `xcmailr_color_mode` cookie and return `"dark"` or `"light"`. Verify with unit tests in `ThemeResolverTest.java`.
- [x] 1.2 Update view and controller integration tests (e.g. in `ThemeResolverTest` or `WebNavigationSecurityTest`) to verify that HTTP requests carrying `xcmailr_color_mode=dark` populate `activeColorMode="dark"` in the model and render `data-bs-theme="dark"` on the root `<html>` element.

## 2. Layout & Client Script Updates

- [x] 2.1 Update the root `<html>` tag in `layout/base.html` to dynamically render `th:attr="data-bs-theme=${activeColorMode != null ? activeColorMode : 'light'}"` directly from the server. Verify HTML output across view tests.
- [x] 2.2 Refactor client-side theme switcher JavaScript in `layout/base.html` to remove `localStorage` dependencies, persist toggles directly to the `xcmailr_color_mode` cookie (`max-age=31536000; SameSite=Lax`), and maintain the initial `<head>` script fallback for OS `prefers-color-scheme: dark`. Verify toggle functionality in browser contract tests.

## 3. Data Table Modernization & Dark Mode Support

- [x] 3.1 Replace `.bg-white` with `.bg-body` on `.table-responsive` across all table fragments (`domain-table.html`, `transaction-table.html`, `user-table.html`, `mailbox-table.html`, `all-mail-list.html`, `mail-list.html`). Verify that table container backgrounds inherit the active theme.
- [x] 3.2 Remove `.table-light` from `<thead>` elements in all table fragments and both statistics tables in `admin/statistics.html` so headers inherit dark theme text and border tokens. Verify headers render without light mode overrides.
- [x] 3.3 Add dark mode table tokens and row hover states (`[data-bs-theme="dark"] .table`) to `xceptance-theme.css` and `emerald-theme.css`. Verify styles in `PaginationThemeStylingTest`.

## 4. Dark Palette Harmonization & Component Polish

- [x] 4.1 Harmonize `[data-bs-theme="dark"]` CSS variables in `xceptance-theme.css` and `emerald-theme.css` by mapping `--bs-body-bg`, `--bs-body-color`, `--bs-card-bg`, and `--bs-border-color` to cohesive Slate tokens (Slate 900 canvas, Slate 800 cards, Slate 700 borders). Verify token definitions in theme CSS files.
- [x] 4.2 Replace hardcoded `bg-white` and `bg-light` utility classes with semantic classes (`bg-transparent`, `bg-body-secondary`, `bg-body-tertiary`) across card headers (`profile.html`, `mails.html`, `all-mails.html`, `mail-detail.html`), input groups (`domains.html`), and mail inspection preview panes (`mail-detail.html`). Verify template markup.

## 5. System Verification

- [x] 5.1 Execute full test suite via `mvn test` across all modules to verify complete test pass, code quality (`final` modifiers, Javadoc), and zero regressions across security, navigation, and theme styling.
