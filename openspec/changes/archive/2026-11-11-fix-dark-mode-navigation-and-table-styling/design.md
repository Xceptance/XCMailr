# Design: Functional Color Mode Persistence and Dark Mode Styling

## Context

XCMailr's web console uses Spring Boot with Thymeleaf for server-side HTML rendering and HTMX for partial page updates. Corporate branding stylesheets (`xceptance` and `emerald`) are managed by `ThemeResolver` and exposed via `@ModelAttribute("activeTheme")`.

Currently, the user's color mode choice (`light` or `dark`) is stored purely in client-side `localStorage`. Because the server cannot read `localStorage`, every page navigation receives HTML hardcoded with `data-bs-theme="light"`, causing an intrusive white flash (FOIT) on every click. Furthermore, data tables across the console are wrapped in hardcoded `.bg-white` containers with `.table-light` header elements, causing tables to remain blindingly white even when dark mode is active.

See `proposal.md` for motivation and `specs/web-console-ui/spec.md` for normative requirements.

## Goals / Non-Goals

**Goals:**
- Eliminate white page flashes and theme resets on navigation by resolving color mode on the server via an HTTP cookie (`xcmailr_color_mode`) and rendering `data-bs-theme` directly on `<html>`.
- Provide clean, legible dark mode styling for all data tables across the application by removing hardcoded light classes and configuring theme CSS variables.
- Harmonize Bootstrap dark theme variables with custom corporate palettes (Slate 900/800/700) and eliminate remaining hardcoded `bg-white`/`bg-light` classes on card headers, inputs, and preview containers.
- Enforce strict Java code quality (`final` modifiers, complete Javadoc) and thorough unit/integration test coverage.

**Non-Goals:**
- Modifying the brand stylesheet architecture (`activeTheme` slug resolution for `xceptance` vs `emerald`).
- Storing color mode in the database per-user account (a 1-year browser cookie provides instant, zero-friction persistence for both authenticated and unauthenticated sessions).

## Decisions

### Decision 1: Single Source of Truth via `xcmailr_color_mode` Cookie
- **Approach**: Persist color mode preference in a dedicated cookie named `xcmailr_color_mode` (`path=/; max-age=31536000; SameSite=Lax`).
- **Rationale**: Cookies are transmitted with every HTTP request, allowing Thymeleaf to render the correct `data-bs-theme` attribute on the very first byte delivered to the client. This guarantees 0 white flashes on navigation without needing client-side DOM patching after load.
- **Alternative considered**: Dual storage (`localStorage` + cookie). Rejected to eliminate state synchronization hazards, duplicate write logic, and potential desynchronization bugs.
- **Initial Visitor Fallback**: If no cookie is present, the server renders `data-bs-theme="light"`. A lightweight, inline `<head>` script checks `window.matchMedia('(prefers-color-scheme: dark)')` and sets `data-bs-theme="dark"` only if the user's OS prefers dark mode. Once the user clicks the toggle button, the cookie is set and governs all subsequent requests.

### Decision 2: Backend Color Mode Resolution in `ThemeResolver`
- **Approach**: Extend [`ThemeResolver`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/main/java/com/xceptance/xcmailr/config/ThemeResolver.java) (which is already a global `@ControllerAdvice`) with `@ModelAttribute("activeColorMode")`.
- **Implementation**:
  ```java
  public static final String COLOR_MODE_COOKIE_NAME = "xcmailr_color_mode";
  public static final String COLOR_MODE_DARK = "dark";
  public static final String COLOR_MODE_LIGHT = "light";

  @ModelAttribute("activeColorMode")
  public String resolveActiveColorMode(final HttpServletRequest request)
  {
      if (request != null && request.getCookies() != null)
      {
          for (final Cookie cookie : request.getCookies())
          {
              if (COLOR_MODE_COOKIE_NAME.equals(cookie.getName()))
              {
                  final String value = cookie.getValue();
                  if (COLOR_MODE_DARK.equalsIgnoreCase(value))
                  {
                      return COLOR_MODE_DARK;
                  }
                  return COLOR_MODE_LIGHT;
              }
          }
      }
      return COLOR_MODE_LIGHT;
  }
  ```
- **Rationale**: Colocating `activeColorMode` alongside `activeTheme` in `ThemeResolver` keeps theme and color mode handling unified in one controller advice without introducing extra filter overhead.

### Decision 3: Root Template and Client-Side Script Refactoring
- **Layout Attribute**:
  In [`layout/base.html`](file:///Users/rbaumgarten/checkouts/XCMailr/xcmailr-webapp/src/main/resources/templates/layout/base.html):
  ```html
  <html xmlns:th="http://www.thymeleaf.org"
        xmlns:sec="http://www.thymeleaf.org/extras/spring-security"
        th:fragment="layout(content)"
        th:lang="${#locale.language}"
        lang="en"
        th:attr="data-bs-theme=${activeColorMode != null ? activeColorMode : 'light'}">
  ```
- **Client Script**:
  Simplify client script to manipulate `document.cookie` and update icons:
  ```javascript
  function setColorMode(mode) {
      document.documentElement.setAttribute('data-bs-theme', mode);
      document.cookie = `xcmailr_color_mode=${mode}; path=/; max-age=31536000; SameSite=Lax`;
      updateThemeIcons(mode);
  }
  ```

### Decision 4: Table Container and Header Semantic Migration
- **Containers**: In all 6 table fragments (`domain-table.html`, `transaction-table.html`, `user-table.html`, `mailbox-table.html`, `all-mail-list.html`, `mail-list.html`), replace:
  `class="table-responsive bg-white rounded shadow-sm border"`
  with:
  `class="table-responsive bg-body rounded shadow-sm border"`
- **Headers**: Remove `.table-light` from `<thead>` elements in all 6 table fragments and both statistics tables in `admin/statistics.html`.
- **CSS Tokens**: Define table dark mode tokens in `xceptance-theme.css` and `emerald-theme.css`:
  ```css
  [data-bs-theme="dark"] .table {
      --bs-table-color: #f1f5f9;
      --bs-table-border-color: #334155;
  }
  ```

### Decision 5: Color Palette Harmonization
- In both theme stylesheets, configure `[data-bs-theme="dark"]`:
  - `--bs-body-bg: #0f172a;` (Slate 900)
  - `--bs-body-color: #f1f5f9;` (Slate 100)
  - `--bs-card-bg: #1e293b;` (Slate 800)
  - `--bs-border-color: #334155;` (Slate 700)
  - `--bs-tertiary-bg: #1e293b;`
  - `--bs-secondary-bg: #334155;`
- In templates:
  - Replace `bg-white` on card headers with `bg-transparent` (`user/profile.html`, `mailboxes/mails.html`, `mailboxes/all-mails.html`, `mailboxes/fragments/mail-detail.html`).
  - Replace `bg-white` on `@` input addon with `bg-body-secondary` (`admin/domains.html`).
  - Replace `bg-light` on disabled profile inputs and mail inspection tabs with `bg-body-secondary` / `bg-body-tertiary`.
  - Style `<pre>` blocks in `mail-detail.html` with `--bs-body-bg` / border styling so message headers and text bodies render legibly in dark mode.

## Risks / Trade-offs

- **[Risk] Browsers with Cookies Disabled**: If a user completely disables cookies in browser privacy settings, the server cannot read `xcmailr_color_mode`.
  - *Mitigation*: The inline script in `<head>` checks `prefers-color-scheme: dark` as a graceful client-side fallback, ensuring users with OS dark mode still get dark styling even with cookies disabled.
- **[Risk] Stale Cookie Value**: If a user manually clears site data or cookies, the application falls back safely to light mode or OS preference.
  - *Mitigation*: Explicit validation in `ThemeResolver` guarantees that any non-matching cookie value safely defaults to `"light"`.
