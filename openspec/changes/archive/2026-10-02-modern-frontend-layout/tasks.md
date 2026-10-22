# Tasks: Modern Frontend Layout & Theme Modernization

## 1. Dependencies and Security Unit Tests (Testing Pyramid - Base Layer)

- [x] 1.1 Add `org.thymeleaf.extras:thymeleaf-extras-springsecurity6` (version `3.1.5.RELEASE` / `3.1.3.RELEASE`) and `org.webjars.npm:bootstrap-icons:1.11.3` to `xcmailr-webapp/pom.xml`; verify successful compilation with `mvn test-compile -pl xcmailr-webapp`.
- [x] 1.2 Implement `UserPrincipalUnitTest` to verify authority mappings for standard users (`ROLE_USER`), administrators (`ROLE_USER` + `ROLE_ADMIN`), inactive account states (`isEnabled() == false`), and attribute immutability; ensure `final` usage and comprehensive Javadoc; verify tests pass with `mvn test -Dtest=UserPrincipalUnitTest`.
- [x] 1.3 Add method-level defense-in-depth security by annotating `AdminWebController` with `@PreAuthorize("hasRole('ADMIN')")`, ensuring `final` keywords on method parameters and complete Javadoc; verify compilation with `mvn test-compile -pl xcmailr-webapp`.

## 2. Corporate Styling and Icon Standardization

- [x] 2.1 Create `xcmailr-webapp/src/main/resources/static/css/xceptance-theme.css` configuring Xceptance brand colors (`#004682`, `#003868`, `#dc3545`), `"Roboto"` / `"Roboto Condensed"` typography, button states, and `[data-bs-theme=dark]` overrides; verify file creation and syntax.
- [x] 2.2 Replace raw emojis (`🔍`, `+`) in `templates/admin/users.html` and `templates/mailboxes/index.html` with standard Bootstrap Icon classes (`bi bi-search`, `bi bi-plus-lg`); verify template syntax and compilation.

## 3. Responsive Offcanvas Navigation, Dark Mode & Integration Tests (Testing Pyramid - Middle Layer)

- [x] 3.1 Redesign `templates/layout/base.html` to replace the dark fixed navbar with a sticky corporate navbar and a Bootstrap 5 Offcanvas slide-out drawer (`#navbarOffcanvas`) containing organized sections for mailboxes, administration, and user accounts; verify template markup.
- [x] 3.2 Add the theme mode switcher button (`#themeToggle` with `bi-moon-stars` / `bi-sun`) and head-level theme initialization script in `base.html` supporting `prefers-color-scheme` auto-detection and `localStorage` persistence; verify that theme toggle elements render cleanly.
- [x] 3.3 Implement `WebNavigationSecurityTest` with MockMvc to test the full matrix of happy path and OWASP security edge cases:
  - Happy path: Anonymous session at `/login` renders login/register only, omitting mailboxes, admin, and account menus.
  - Happy path: Authenticated standard user session at `/` renders mailboxes and user account menus, strictly omitting admin menus.
  - Happy path: Authenticated administrator session at `/` renders mailboxes, full admin dropdown, and account menus.
  - Security error case: Authenticated non-admin directly requesting `/admin/users`, `/admin/domains`, `/admin/transactions`, or `/admin/statistics` receives HTTP 403 Forbidden.
  - Security error case: Anonymous requester directly accessing any `/admin/**` endpoint receives HTTP 302 Found redirecting to `/login`.
  - Security error case: State-altering admin request without CSRF token receives HTTP 403 Forbidden.
  - Layout verification: Verify rendered pages contain `#navbarOffcanvas` drawer structure and `#themeToggle` button.
  - Ensure strict `final` modifier usage on all test parameters/variables and complete Javadoc comments.
  - Verify all tests pass with `mvn test -Dtest=WebNavigationSecurityTest`.

## 4. Documentation and End-to-End Regression Verification (Testing Pyramid - Top Layer)

- [x] 4.1 Update `README.md` to document the corporate theme tokens, Bootstrap Icons integration, and responsive navigation; verify document accuracy.
- [x] 4.2 Run full clean build and regression test suite across the entire project via `mvn clean test` and verify that all 100+ tests pass with 0 failures.

