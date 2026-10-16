# Design: Modern Frontend Layout & Theme Modernization

## Context

The XCMailr web interface is built with Spring Boot, Thymeleaf, and Bootstrap 5.3.3. However, the current template layout in `layout/base.html` has three architectural and usability gaps:
1. **Unenforced Navigation Presentation**: Thymeleaf template directives `sec:authorize="hasRole('ADMIN')"` and `sec:authorize="isAuthenticated()"` are currently not evaluated because `thymeleaf-extras-springsecurity6` is not declared in `pom.xml`. As a result, all menu elements appear in the rendered HTML regardless of caller privileges.
2. **Cluttered Mobile Navigation**: Viewport resizing below 992px relies on a standard collapse navbar with an invalid class (`navbar-toggler-span`) that vertically pushes page content down when opened, making multi-level menus cumbersome on mobile devices.
3. **Design & Icon Discrepancy**: The layout uses an inverted dark navbar (`navbar-dark bg-dark`) instead of the clean corporate aesthetic of Xceptance (`https://www.xceptance.com/en/`), and relies on emojis (`🔍`, `+`) instead of standardized icons.

## Goals / Non-Goals

**Goals:**
- Enable Thymeleaf Spring Security dialect so navigation items strictly adhere to authentication state and `ROLE_ADMIN` permissions during server-side rendering.
- Verify and enforce OWASP A01:2021 (Broken Access Control) compliance through multi-layer server-side authorization: filter security and method security.
- Implement an accessible Bootstrap 5 Offcanvas drawer for screens below 992px with dedicated sections for core navigation, administration tools, and user account management.
- Align the global layout with Xceptance's corporate identity: sticky light navbar, corporate blue (`#004682`), `Roboto` and `Roboto Condensed` typography, and standard brand typography.
- Standardize all icons using `org.webjars.npm:bootstrap-icons` bundled locally via WebJars.
- Add instant Light/Dark mode switching powered by Bootstrap 5.3's native `data-bs-theme` attribute with OS preference auto-detection and persistent storage.
- Structure tests following the Testing Pyramid (Unit tests, Slice/Integration tests, End-to-End tests), covering happy paths, error conditions, and security edge cases.
- Enforce strict Java code quality: `final` on parameters and local variables, comprehensive Javadoc, and detailed inline comments.

**Non-Goals:**
- Changing existing controller route mappings or backend database schemas.
- Replacing Bootstrap 5 with another CSS framework.
- Redesigning email rendering or SMTP transmission engines.

## Decisions

### 1. Thymeleaf Spring Security Integration & Version Selection
- **Choice**: Add `org.thymeleaf.extras:thymeleaf-extras-springsecurity6` to `xcmailr-webapp/pom.xml`.
  - **Latest Version on Maven Central**: `3.1.5.RELEASE` (released recently).
  - **Local Offline Fallback**: `3.1.3.RELEASE` (currently cached in local Maven repository).
  - We configure `3.1.5.RELEASE` (or `3.1.3.RELEASE` in offline environments) in `pom.xml`.
- **Rationale**: Spring Boot auto-configures `SpringSecurityDialect` when this library is on the classpath. Thymeleaf evaluates `sec:authorize` and `sec:authentication` server-side against Spring Security's `SecurityContext`. Unauthorized elements are completely stripped from the output stream before HTML is transmitted to the client.

### 2. OWASP Security Criteria & Server-Side Authorization Defense
- **Threat Model: Client-Side Role Tampering**:
  - *Can a user manipulate their role in the browser to make `hasRole('ADMIN')` evaluate to true?*
    **No.** Thymeleaf executes exclusively on the server (Server-Side Rendering). The browser never receives Thymeleaf expressions or role checks; it only receives the resulting static HTML.
  - *Where is the user's role stored?*
    Roles and granted authorities (`ROLE_USER`, `ROLE_ADMIN`) are stored strictly on the server in the HTTP session (`SecurityContext`). Authorities are loaded directly from the PostgreSQL/H2 database (`User.admin` column) upon authentication via `UserDetailsService` and `UserPrincipal`. The client receives only a cryptographically random, opaque session cookie (`JSESSIONID`) flagged `HttpOnly`.
  - *Is there server-side validation if a client attempts to bypass the UI?*
    **Yes.** Even if an attacker uses browser Developer Tools to inject an administrative link into the DOM, or submits HTTP requests directly using `curl` or Postman:
    1. **Filter-Level Security**: In `SecurityConfig.java`, `.requestMatchers("/admin/**").hasRole("ADMIN")` intercepts every incoming request prior to controller routing. Non-admin users are immediately rejected with an HTTP 403 Forbidden status.
    2. **Method-Level Security**: `@EnableMethodSecurity` is enabled in `SecurityConfig.java`. Adding `@PreAuthorize("hasRole('ADMIN')")` at the controller level provides a redundant defense-in-depth barrier.
    3. **CSRF Mitigation**: In compliance with OWASP guidelines, all state-altering administrative actions (POST, DELETE) require a cryptographically verified CSRF token (`_csrf`).

### 3. Mobile Navigation via Bootstrap 5 Offcanvas Drawer
- **Choice**: Structure the mobile navigation inside `<div class="offcanvas offcanvas-end" id="navbarOffcanvas">`.
- **Rationale**: Native to Bootstrap 5.3, the offcanvas slide-out drawer appears smoothly over an overlay backdrop without displacing main content. It provides ample vertical space to structure links into clear sections with headers.
- **Alternatives Considered**: Classic vertical collapse accordion (rejected because nested dropdown menus clutter small mobile viewports).

### 4. Iconography with WebJars Bootstrap Icons
- **Choice**: Integrate `org.webjars.npm:bootstrap-icons:1.11.3` and reference icons via standard CSS classes (`bi bi-search`, `bi bi-plus-lg`, `bi bi-moon-stars`, `bi bi-envelope-at`).
- **Rationale**: Bootstrap Icons is the official companion to Bootstrap 5, is already cached in the local Maven repository, and serves locally without external CDN requests.
- **Alternatives Considered**: Inlined SVG elements (rejected as verbose and harder to maintain across templates) or raw emojis (rejected due to inconsistent platform rendering).

### 5. Native Bootstrap 5.3 Color Modes (Dark Mode)
- **Choice**: Toggle `data-bs-theme="dark"` / `"light"` on the root `<html>` element, supported by a lightweight `<script>` in `<head>` to prevent Flash of Incorrect Theme (FOIT).
- **Rationale**: Bootstrap 5.3 natively recalibrates all color tokens, cards, dropdowns, inputs, and tables when `data-bs-theme` changes. Storing the preference in `localStorage` with a fallback to `window.matchMedia('(prefers-color-scheme: dark)')` gives immediate, zero-latency adaptation.
- **Alternatives Considered**: Custom CSS `.dark-mode` classes (rejected as redundant and error-prone compared to Bootstrap's standard token system).

### 6. Corporate Design Token Overrides
- **Choice**: Define an `xceptance-theme.css` stylesheet overriding Bootstrap CSS custom properties:
  - `--bs-primary: #004682`
  - `--bs-primary-rgb: 0, 70, 130`
  - `--bs-link-color: #004682`
  - `--bs-link-hover-color: #003868`
  - Typography: Heading font set to `"Roboto Condensed", sans-serif; font-weight: 500`
- **Rationale**: Directly reflects the styling used in production at `xceptance.com`.

## Testing Pyramid Strategy & Security Verification

To ensure robust quality and protect against security regressions, the implementation adheres strictly to the **Testing Pyramid**:

```text
               /\
              /  \     End-to-End Tests (Full Session Lifecycle & Navigation Flow)
             /----\
            /      \   Integration / Slice Tests (MockMvc, RBAC Matrices, 403/302, CSRF)
           /--------\
          /          \ Unit Tests (UserPrincipal Authorities, Inactive Accounts, Immutability)
         --------------
```

### 1. Unit Tests (Base of Pyramid)
- **`UserPrincipalUnitTest`**:
  - *Happy Path*: Verifies that a `User` entity with `admin=true` produces authorities `ROLE_USER` and `ROLE_ADMIN`.
  - *Happy Path*: Verifies that a `User` entity with `admin=false` produces only `ROLE_USER`.
  - *Error & Special Cases*:
    - Verifies that a `User` with `active=false` causes `isEnabled()` to return `false`, preventing authentication.
    - Verifies non-null handling, account non-locked, and credentials non-expired attributes.
- **Fast execution**: No Spring context initialization; runs entirely with JUnit 5.

### 2. Integration / Slice Tests (Middle of Pyramid)
- **`WebNavigationSecurityTest` (MockMvc Web Slice)**:
  - *Happy Paths*:
    - Anonymous request to `/login`: HTML contains login and register links; contains **no** `/` (Mailboxes), `/admin/**`, or `/profile` links.
    - Standard user request to `/`: HTML contains `/` (Mailboxes) and `/profile` account dropdown; contains **no** `/admin/**` dropdown or links.
    - Administrator request to `/`: HTML contains `/` (Mailboxes), full `/admin/**` dropdown, and `/profile`.
  - *Error & OWASP Security Cases (A01:2021)*:
    - Standard user directly requesting `/admin/users`, `/admin/domains`, `/admin/transactions`, `/admin/statistics` receives **HTTP 403 Forbidden**.
    - Anonymous requester directly accessing `/admin/**` receives **HTTP 302 Found** redirecting to `/login`.
    - State-altering administrative request (e.g., `POST /admin/users/{id}/toggle-active`) without CSRF token receives **HTTP 403 Forbidden**.
    - Inactive or disabled user account attempting protected access receives an authentication or authorization denial.
- **`ResponsiveLayoutAndThemeIntegrationTest`**:
  - *Happy Paths*:
    - Verifies presence of `#navbarOffcanvas` drawer markup and toggler button in rendered pages.
    - Verifies presence of theme mode toggle button (`#themeToggle`) with standard `bi-moon-stars` icon.
    - Verifies inclusion of `xceptance-theme.css` and `bootstrap-icons.min.css`.
  - *Special Cases*:
    - Verifies that interactive drawer links include `data-bs-dismiss="offcanvas"` to avoid viewport traps.

### 3. End-to-End Verification (Top of Pyramid)
- Comprehensive test suite execution (`mvn clean test`) ensuring all 100+ tests pass with zero regressions across security, mail handling, and administration.

## Coding Standards & Conventions

- **Immutability & Finality**: Use `final` on all method parameters, local variables, and class fields wherever technically feasible.
- **JavaDoc Documentation**: Class-level and method-level Javadoc must be provided for all new and modified classes, including `@param`, `@return`, and `@throws` tags.
- **Code Comments**: Clear inline comments explaining security controls, OWASP mapping, and non-obvious template/styling logic.

## Risks / Trade-offs

- **[Flash of Incorrect Theme (FOIT)]** → **Mitigation**: Place the theme initialization script inline inside `<head>` in `base.html` before DOM rendering, reading `localStorage` immediately.
- **[Offcanvas drawer lingering after route changes or in-page anchors]** → **Mitigation**: Add `data-bs-dismiss="offcanvas"` on drawer links so the backdrop closes automatically upon clicking.
- **[Font availability in offline or restricted environments]** → **Mitigation**: Specify a robust font stack with system fallbacks: `"Roboto Condensed", system-ui, -apple-system, sans-serif`.

