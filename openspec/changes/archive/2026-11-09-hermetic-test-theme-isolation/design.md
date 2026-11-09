# Design

## Context

In Spring Boot, property resolution order places OS environment variables (e.g., `XCMAILR_UI_THEME=emerald`) above application configuration files (`application.yml`). When developers or CI environments export `XCMAILR_UI_THEME`, integration tests running under `@SpringBootTest` that do not explicitly specify test properties inherit the host environment variable rather than the defaults defined in `application.yml`.

See `proposal.md` for problem motivation and failure details.

## Goals / Non-Goals

**Goals:**
- Ensure 100% of the 680-test suite passes cleanly under `mvn clean test` regardless of whether `XCMAILR_UI_THEME` is exported in the user's shell or CI runner.
- Make all test slices in `ThemeResolutionWebTest` hermetic, self-contained, and runnable directly within any IDE (IntelliJ, Eclipse, VS Code) or CLI.
- Explicitly test both explicit default theme configuration (`xcmailr.ui.theme=xceptance`) and unconfigured/blank fallback (`xcmailr.ui.theme=`).
- Decouple `WebNavigationSecurityTest` so responsive layout and offcanvas navigation verification is not tightly bound to a specific theme slug.

**Non-Goals:**
- Modifying production theme resolution logic in `ThemeResolver.java` or `base.html`.
- Restricting or modifying how `application.yml` maps `XCMAILR_UI_THEME`.
- Changing non-theme integration or unit tests.

## Decisions

### Decision 1: Pin `@TestPropertySource` on Default Theme Web Test Slices
- **Choice**: Annotate `ThemeResolutionWebTest$DefaultThemeWebTest` with `@TestPropertySource(properties = "xcmailr.ui.theme=xceptance")`.
- **Rationale**: `@TestPropertySource` sits higher in Spring Boot's property source hierarchy than OS environment variables. This prevents host shell variables from contaminating the Spring test context.
- **Alternatives Considered**:
  - *Maven Surefire `<environmentVariables>`*: Configures Maven to clear or set the variable during `mvn test`. Rejected as the sole fix because tests executed directly from IDE runners that inherited the shell environment would still fail.
  - *Programmatic `System.setProperty` / reflection*: Brittle, potentially fails under modern JDK module systems, and pollutes JVM-wide state.

### Decision 2: Add Explicit Blank Fallback Web Test Slice
- **Choice**: Add an explicit nested test slice `ThemeResolutionWebTest$FallbackThemeWebTest` annotated with `@TestPropertySource(properties = "xcmailr.ui.theme=")`.
- **Rationale**: Specifically verifies Tier 2 web layer behavior when the configuration property is blank, ensuring it renders `/css/xceptance-theme.css` without relying on shell environment purity.

### Decision 3: Pin and Decouple `WebNavigationSecurityTest`
- **Choice**:
  1. Add `@TestPropertySource(properties = "xcmailr.ui.theme=xceptance")` to `WebNavigationSecurityTest` to ensure its test context is hermetically sealed against external environment variables.
  2. Update `testResponsiveLayoutAndThemeMarkup` assertion from strictly matching `"xceptance-theme.css"` to matching `containsString("-theme.css")` (and retaining `"bootstrap-icons.min.css"`).
- **Rationale**: `WebNavigationSecurityTest` is designed to verify responsive layout landmarks (offcanvas drawer, theme toggle button, stylesheet linkages), not theme selection mechanics. Testing for `-theme.css` ensures the layout contracts hold even if the test profile configuration evolves.

## Risks / Trade-offs

- **[Risk] Test Context Caching / Proliferation**: Adding multiple `@TestPropertySource` configurations creates distinct Spring `ApplicationContext` instances in the test suite cache.
  - *Mitigation*: The test slices in `ThemeResolutionWebTest` already use `@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)` with distinct property configurations. The additional context cost is negligible (under 0.2s) and already amortized.
