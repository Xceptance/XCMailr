# Tasks: Configurable UI Theming

## 1. Core Theme Resolution & Tier 1 Unit Tests

- [x] 1.1 Implement `ThemeResolver` in `com.xceptance.xcmailr.config` to read `xcmailr.ui.theme` (defaulting to `xceptance`), sanitize the slug against `^[a-zA-Z0-9_-]+$` with a 32-character boundary limit, verify classpath resource existence of `/static/css/{theme}-theme.css` via `ResourceLoader`, and fall back safely to `xceptance` with a `WARN` log if missing or malformed.
- [x] 1.2 Implement Tier 1 unit tests in `ThemeResolverTest.java` verifying happy paths (default, explicit `emerald`, custom valid slug), fallback cases (missing file, empty/blank/null property), and parameterized security tests demonstrating that path traversal vectors (`../`, `..\\`, `/etc/passwd`, `%2e%2e`, null bytes, script tags, shell metacharacters, and boundary lengths > 32 chars) are strictly rejected and default to `xceptance`, verifying via `mvn test -pl xcmailr-webapp -Dtest=ThemeResolverTest`.

## 2. Layout Integration & Tier 2 WebMvc Slice Tests

- [x] 2.1 Update `xcmailr-webapp/src/main/resources/templates/layout/base.html` to dynamically link the active theme stylesheet via `th:href="@{'/css/' + ${@themeResolver.activeTheme} + '-theme.css'}"`.
- [x] 2.2 Implement Tier 2 WebMvc slice tests in `ThemeResolutionWebTest.java` asserting rendered HTML `<link>` attributes across configuration permutations: default configuration rendering `/css/xceptance-theme.css`, configured `emerald` rendering `/css/emerald-theme.css`, missing theme falling back to `/css/xceptance-theme.css`, and malicious path traversal property (`xcmailr.ui.theme=../../etc/passwd`) safely rendering `/css/xceptance-theme.css` without throwing exceptions or leaking paths, verifying via `mvn test -pl xcmailr-webapp -Dtest=ThemeResolutionWebTest`.

## 3. Emerald Theme Asset & Tier 3 Integration Tests

- [x] 3.1 Author `xcmailr-webapp/src/main/resources/static/css/emerald-theme.css` implementing all `--xc-*` design tokens and component styling (emerald green primary `#059669`, hover `#047857`, dark mode `#10b981`, amber accent `#f59e0b`, Inter typography, cards, buttons, navbar, and floating batch bar), verifying stylesheet structure and syntax.
- [x] 3.2 Implement Tier 3 integration test in `ThemeIntegrationTest.java` verifying that HTTP GET requests for both `/css/xceptance-theme.css` and `/css/emerald-theme.css` return HTTP 200 OK with `Content-Type: text/css` and non-empty content, verifying via `mvn test -pl xcmailr-webapp -Dtest=ThemeIntegrationTest`.

## 4. Configuration & Documentation

- [x] 4.1 Update `xcmailr-webapp/src/main/resources/application.yml` to declare `xcmailr.ui.theme: ${XCMAILR_UI_THEME:xceptance}` with descriptive inline comments explaining available options and custom theme extension.
- [x] 4.2 Update `README.md` with configuration table entries for `xcmailr.ui.theme` / `XCMAILR_UI_THEME` and a dedicated UI Theming section explaining how to switch themes or add custom corporate stylesheets.

## 5. Verification & Full Regression Testing

- [x] 5.1 Run full regression test suite across `xcmailr-webapp` via `mvn test -pl xcmailr-webapp "-Dtest=Theme*"` and full test suite to ensure all new and existing tests pass cleanly without regression.
