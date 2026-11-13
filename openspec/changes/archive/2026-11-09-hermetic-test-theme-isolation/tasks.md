# Tasks

## 1. Hermetic Isolation in Theme Web Tests

- [x] 1.1 Annotate `DefaultThemeWebTest` in `ThemeResolutionWebTest.java` with `@TestPropertySource(properties = "xcmailr.ui.theme=xceptance")` and verify execution via `mvn test -pl xcmailr-webapp -Dtest=ThemeResolutionWebTest` with `XCMAILR_UI_THEME=emerald`
- [x] 1.2 Add nested test slice `FallbackThemeWebTest` in `ThemeResolutionWebTest.java` annotated with `@TestPropertySource(properties = "xcmailr.ui.theme=")` to explicitly verify blank/unset property fallback to `/css/xceptance-theme.css`

## 2. Navigation Layout Test Isolation and Assertion Decoupling

- [x] 2.1 Annotate `WebNavigationSecurityTest.java` with `@TestPropertySource(properties = "xcmailr.ui.theme=xceptance")` to isolate navigation tests from host environment variables
- [x] 2.2 Decouple `WebNavigationSecurityTest.testResponsiveLayoutAndThemeMarkup` by asserting that a theme stylesheet ending in `-theme.css` is linked rather than strictly hardcoding `xceptance-theme.css`, and verify execution via `mvn test -pl xcmailr-webapp -Dtest=WebNavigationSecurityTest`

## 3. Comprehensive Verification

- [x] 3.1 Run the full test suite with `XCMAILR_UI_THEME=emerald mvn clean test` and verify that all 680 tests pass with 0 failures
- [x] 3.2 Run the full test suite without `XCMAILR_UI_THEME` (`mvn test`) and verify that all 680 tests pass with 0 failures
