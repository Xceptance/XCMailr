# Design: Configurable UI Theming

## Context

XCMailr's web UI relies entirely on Thymeleaf layout inheritance: all 16 page templates extend `layout/base.html` using `th:replace="~{layout/base :: layout(~{::content})}"`. In `base.html`, the corporate stylesheet is currently hardcoded:
```html
<link rel="stylesheet" th:href="@{/css/xceptance-theme.css}">
```

To provide custom corporate theming and enable live demonstrations of visual adaptability, XCMailr requires a configuration-driven mechanism to dynamically link alternative theme stylesheets without hardcoding theme names in Java code or altering existing template markup.

## Goals / Non-Goals

**Goals:**
- Provide a Spring Boot configuration property `xcmailr.ui.theme` (with environment variable override `XCMAILR_UI_THEME`, defaulting to `xceptance`).
- Dynamically resolve and link `/css/{theme}-theme.css` in `base.html`.
- Safely sanitize theme names using safe slug regex (`^[a-zA-Z0-9_-]+$`) with a 32-character boundary limit to strictly prevent directory traversal, path injection, or shell metacharacters.
- Dynamically discover theme files on the classpath using Spring's `ResourceLoader`, automatically falling back to `xceptance` with a descriptive log warning if a configured theme file is missing or invalid.
- Ship an alternative out-of-the-box demo theme (`emerald-theme.css`) with modern developer-tool styling, full light/dark color mode support, and identical design token contracts.
- Implement comprehensive automated test coverage following the **Testing Pyramid** (Tier 1 unit tests, Tier 2 WebMvc slice tests, Tier 3 integration tests), with dedicated security tests for path traversal resilience.
- Document configuration options and custom theming guidelines in `application.yml` and `README.md`.

**Non-Goals:**
- Per-user database-persisted theme selection (light/dark mode toggle remains client-side; corporate theme palette is deployment/environment-wide).
- Dynamic hot-reloading of CSS without page refresh.

## Decisions

### Decision 1: Dynamic Resource Discovery with Auto-Fallback (Option 1)
- **Rationale**: An open slug-based discovery pattern allows any organization to drop a custom theme stylesheet (e.g., `acme-theme.css`) into `src/main/resources/static/css/` and configure `XCMAILR_UI_THEME=acme` without modifying or recompiling Java source code. Checking resource existence via Spring's `ResourceLoader` ensures that typos (such as `emrald`) gracefully fall back to `xceptance` with an explicit log warning rather than producing a 404 error and broken unstyled HTML.
- **Alternatives Considered**:
  - *Hardcoded Switch Statement*: Rejected because it restricts theme names to an enum/switch, forcing Java recompilation for any new theme.
  - *Pure Unchecked Slug*: Rejected because configuration typos produce browser 404s and unstyled page renders.

### Decision 2: Spring Component Helper for Theme Resolution
- **Rationale**: Implement a dedicated Spring `@Component("themeResolver")` that reads `xcmailr.ui.theme`, sanitizes the slug, checks resource existence, caches the resolved theme name, and exposes it to Thymeleaf templates via `@themeResolver.activeTheme`.
- **Thymeleaf Integration**:
  In `base.html`:
  ```html
  <link rel="stylesheet" th:href="@{'/css/' + ${@themeResolver.activeTheme} + '-theme.css'}">
  ```

### Decision 3: Emerald Theme Token Contract & Visual Identity
- **Rationale**: The `emerald` theme provides maximum visual contrast against the deep navy blue (`#004682`) of `xceptance-theme.css`. It implements the exact same CSS custom property tokens:
  - `--xc-primary`: `#059669` (Dark mode: `#10b981`)
  - `--xc-primary-hover`: `#047857` (Dark mode: `#059669`)
  - `--xc-accent`: `#f59e0b` (Amber accent)
  - `--xc-font-family`: `'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif`
  - `--xc-font-family-condensed`: `'Inter', -apple-system, sans-serif`
  - Component overrides for `.navbar-xceptance`, `.theme-toggle-btn`, `.floating-batch-bar`, and `.card`.

## Testing Pyramid Strategy

```
               / \
              /   \
             / Tier\      Tier 3: End-to-End Integration Tests
            /   3   \     ThemeIntegrationTest (Static asset HTTP 200, Content-Type)
           /---------\
          /   Tier    \   Tier 2: WebMvc Slice Tests
         /     2       \  ThemeResolutionWebTest (Thymeleaf layout rendering, HTML <link>)
        /---------------\
       /      Tier       \ Tier 1: Unit Tests
      /        1          \ThemeResolverTest (Slug sanitization, path traversal, fallback)
     /---------------------\
```

### Tier 1: Unit Tests (`ThemeResolverTest`)
Isolated JUnit 5 unit tests verifying `ThemeResolver` logic without starting an HTTP container:
- **Happy Paths**:
  - Default when property is unset or empty $\rightarrow$ resolves `"xceptance"`.
  - Explicit out-of-the-box theme `"emerald"` $\rightarrow$ resolves `"emerald"`.
  - Custom valid theme slug with existing classpath resource $\rightarrow$ resolves custom slug.
- **Error & Fallback Cases**:
  - Missing stylesheet on classpath (e.g. `"missing-theme"`, typo `"emrald"`) $\rightarrow$ logs `WARN` and safely resolves `"xceptance"`.
  - Null, empty string, or whitespace-only property $\rightarrow$ resolves `"xceptance"`.
- **Security & Path Traversal Injection Vectors (Parameterized)**:
  - Relative directory traversal: `"../foo"`, `"../../etc/passwd"`, `"..\\..\\windows"`, `"themes/../secret"` $\rightarrow$ rejected, resolves `"xceptance"`.
  - URL-encoded traversal: `"..%2f..%2f"`, `"%2e%2e%2f"` $\rightarrow$ rejected, resolves `"xceptance"`.
  - Absolute paths: `"/etc/passwd"`, `"C:\\boot.ini"` $\rightarrow$ rejected, resolves `"xceptance"`.
  - Shell / script injection characters: `"<script>"`, `"theme;rm -rf"`, `"theme|cat"`, `"theme$VAR"` $\rightarrow$ rejected, resolves `"xceptance"`.
  - Null byte injection: `"theme\0.css"` $\rightarrow$ rejected, resolves `"xceptance"`.
- **Boundary Cases**:
  - Slug exceeding 32 characters $\rightarrow$ rejected, resolves `"xceptance"`.
  - Exactly 32 characters valid slug $\rightarrow$ accepted if resource exists.

### Tier 2: WebMvc Slice Tests (`ThemeResolutionWebTest`)
MockMvc slice tests validating Thymeleaf layout rendering in `layout/base.html`:
- `@TestPropertySource(properties = "xcmailr.ui.theme=emerald")`: Asserts HTML response contains `<link rel="stylesheet" href="/css/emerald-theme.css">` and does not contain `xceptance-theme.css`.
- Default configuration (no property override): Asserts HTML response contains `<link rel="stylesheet" href="/css/xceptance-theme.css">`.
- `@TestPropertySource(properties = "xcmailr.ui.theme=../../etc/passwd")`: Asserts MockMvc render succeeds with HTTP 200 OK and `<link rel="stylesheet" href="/css/xceptance-theme.css">` without path leakage or unhandled exceptions.
- `@TestPropertySource(properties = "xcmailr.ui.theme=unknown-theme")`: Asserts MockMvc render falls back cleanly to `/css/xceptance-theme.css`.

### Tier 3: Integration Tests (`ThemeIntegrationTest`)
Full Spring Boot integration test:
- Issues HTTP GET requests for `/css/xceptance-theme.css` and `/css/emerald-theme.css`.
- Verifies HTTP 200 OK, `Content-Type: text/css`, and non-empty response bodies.
- Verifies that requesting malicious paths via HTTP GET `/css/../` is rejected by standard Spring Web security filters.

## Risks / Trade-offs

- **[Risk] Directory Traversal or Path Injection** $\rightarrow$ **Mitigation**: The theme resolver rejects any input that does not match `^[a-zA-Z0-9_-]+$` (strictly disallowing `.`, `/`, `\`, null bytes, and whitespace), capping length to 32 characters and falling back to `xceptance`.
- **[Risk] Runtime Performance Overhead on Static Page Renders** $\rightarrow$ **Mitigation**: Since the theme is an application configuration property, the resolution check can be executed once on initialization and cached, resulting in zero overhead on HTTP requests.
