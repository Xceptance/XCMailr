# Proposal

## Why

The current web console navigation displays all menu items (including administrative links and authenticated account menus) to all visitors regardless of role, because the Thymeleaf Spring Security dialect dependency (`thymeleaf-extras-springsecurity6`) is missing from the classpath. Additionally, the existing fixed dark navigation bar lacks responsive drawer support on small screens, relies on raw emojis for iconography, and is unaligned with Xceptance's corporate design system (`https://www.xceptance.com/en/`). Modernizing the frontend architecture resolves navigation permission leaks, guarantees OWASP Broken Access Control (A01:2021) compliance through multi-layer server-side authorization enforcement, elevates responsive usability with an offcanvas drawer, standardizes offline iconography with Bootstrap Icons, and introduces native dark mode theme support. All changes are governed by a strict testing pyramid (unit, integration, and end-to-end tests) covering happy paths, error scenarios, and security edge cases.

## What Changes

- **Role-Based Navigation Filtering & OWASP Access Control**: Add `org.thymeleaf.extras:thymeleaf-extras-springsecurity6` (version `3.1.5.RELEASE`, with `3.1.3.RELEASE` local repository fallback) so Thymeleaf's `sec:authorize` and `sec:authentication` directives properly restrict guest, authenticated user, and administrator menu visibility.
- **Server-Side Authorization Defense**: Ensure compliance with OWASP Top 10 (A01:2021 - Broken Access Control) by reaffirming that role validation is strictly enforced on the server via `SecurityConfig` (`.requestMatchers("/admin/**").hasRole("ADMIN")`) and method security (`@PreAuthorize`), preventing any client-side role manipulation.
- **Corporate Layout & Theme Alignment**: Redesign `layout/base.html` to adopt Xceptance corporate styling: clean sticky navbar, Xceptance blue (`#004682`) primary actions, `Roboto` and `Roboto Condensed` typography, and polished footer.
- **Responsive Mobile Offcanvas Navigation**: Implement a mobile-first Bootstrap 5 Offcanvas slide-out drawer triggered by a standard hamburger button for screens narrower than 992px, presenting cleanly sectioned menus without displacing viewport content.
- **Standardized Bootstrap Iconography**: Add `org.webjars.npm:bootstrap-icons:1.11.3` to replace ad-hoc emojis and text symbols (`🔍`, `+`, `☀️/🌙`) with standard, monochrome, offline-ready icon classes (`bi bi-search`, `bi bi-plus-lg`, `bi bi-moon-stars`, `bi bi-envelope-at`).
- **Native Dark Mode Theme**: Implement accessible Light/Dark theme switching using Bootstrap 5.3's native `data-bs-theme`, supporting system preference auto-detection (`prefers-color-scheme`) and persistent `localStorage` selection.
- **Testing Pyramid & Code Quality**: Implement comprehensive test suites following the testing pyramid (unit tests for authority mapping and principal state, MockMvc slice tests for navigation rendering and 403/302 security denial matrices, and full end-to-end integration tests). Enforce `final` keywords on parameters and local variables, and provide complete Javadoc and code comments.

## Capabilities

### Modified Capabilities
- `web-console-ui`: Add requirements and scenarios for role-based navigation menu rendering, server-side OWASP access control enforcement, responsive mobile offcanvas drawer navigation, and accessible theme switching (Light / Dark).

## Impact

- **Dependencies**: Adds `org.thymeleaf.extras:thymeleaf-extras-springsecurity6` (`3.1.5.RELEASE` / `3.1.3.RELEASE`) and `org.webjars.npm:bootstrap-icons:1.11.3` to `xcmailr-webapp/pom.xml`.
- **Security & Authorization**: Validates server-side authorization defenses against OWASP A01:2021 Broken Access Control across web and administration endpoints.
- **Templates**: Updates `xcmailr-webapp/src/main/resources/templates/layout/base.html` and replaces icon usages across admin and mailbox templates.
- **CSS / Assets**: Introduces Xceptance theme stylesheet and color custom property overrides for light and dark modes.
- **Testing**: Adds comprehensive unit, slice, and integration tests across the testing pyramid covering happy paths, error conditions, CSRF omission, and unauthorized access attempts.

