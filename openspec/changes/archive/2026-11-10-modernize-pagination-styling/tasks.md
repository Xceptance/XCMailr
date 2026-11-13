# Tasks

## 1. Theme Styling Modernization

- [x] 1.1 Add `.pagination` CSS custom properties to `xcmailr-webapp/src/main/resources/static/css/xceptance-theme.css` configuring neutral text (`var(--bs-body-color)`), subtle borders (`var(--xc-border-color)`), hover wash (`var(--bs-tertiary-bg)`), and dark charcoal active state (`#212529` in light mode, `#334155` in dark mode); verify stylesheet syntax and property definitions.
- [x] 1.2 Add `.pagination` CSS custom properties to `xcmailr-webapp/src/main/resources/static/css/emerald-theme.css` configuring matching neutral text and dark charcoal active state; verify stylesheet syntax and property definitions.

## 2. Automated Testing & Verification

- [x] 2.1 Implement `PaginationThemeStylingTest` in `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/config/` with `final` modifiers and Javadoc, validating that both `xceptance-theme.css` and `emerald-theme.css` define `.pagination` custom properties, verify absence of `#0d6efd` defaults, and check dark mode overrides; verify with `mvn test -Dtest=PaginationThemeStylingTest`.
- [x] 2.2 Execute full regression test build and verify all tests pass without errors; verify with `mvn test`.
