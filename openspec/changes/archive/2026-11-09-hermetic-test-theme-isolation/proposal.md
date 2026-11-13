# Proposal

## Why

When the active UI theme is configured via the environment variable `export XCMAILR_UI_THEME=emerald`, running the Maven test suite (`mvn clean test`) or executing tests within an IDE causes 2 out of 680 tests to fail (`WebNavigationSecurityTest.testResponsiveLayoutAndThemeMarkup` and `ThemeResolutionWebTest$DefaultThemeWebTest.testDefaultThemeRendered`). In Spring Boot, OS environment variables take precedence over configuration file defaults (`application.yml`). Because these test slices omitted explicit `@TestPropertySource` declarations, host environment variables leak into the test application context, violating test hermeticity and coupling test reliability to the developer's shell environment.

## What Changes

- **Hermetic Default Theme Web Test**: Annotate `ThemeResolutionWebTest$DefaultThemeWebTest` with `@TestPropertySource(properties = "xcmailr.ui.theme=xceptance")` to ensure default theme rendering is tested deterministically regardless of external environment variables.
- **Explicit Fallback Theme Resolution Web Test**: Add an explicit nested test slice in `ThemeResolutionWebTest` with `@TestPropertySource(properties = "xcmailr.ui.theme=")` to verify that empty or unconfigured theme properties cleanly fall back to `xceptance`.
- **Decoupled Navigation & Responsive Layout Test**: Annotate `WebNavigationSecurityTest` with `@TestPropertySource(properties = "xcmailr.ui.theme=xceptance")` for baseline hermeticity, and relax the assertion on the theme stylesheet link to verify the presence of a valid theme stylesheet pattern (`-theme.css`) so general layout verification is not fragilely coupled to a single theme slug.

## Capabilities

### New Capabilities
<!-- None: Pure test-suite hermeticity refactoring without adding or changing functional system capabilities. -->

### Modified Capabilities
<!-- None: No spec-level functional requirements are changing. Production code behavior remains identical. -->

## Impact

- **Affected Tests**:
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/ThemeResolutionWebTest.java`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/security/WebNavigationSecurityTest.java`
- **Dependencies & Production Code**: No production code, APIs, database schemas, or external dependencies are altered.
