# Design

## Context

The XCMailr web console uses Bootstrap 5.3.3 styled via two custom theme stylesheets:
- `xceptance-theme.css`: Corporate theme with Xceptance design tokens.
- `emerald-theme.css`: Alternative developer theme with emerald tokens.

Bootstrap 5 scopes pagination styling via CSS variables on `.pagination`:
- `--bs-pagination-color`: Text color for pagination links (defaults to `--bs-link-color`: `#0d6efd`)
- `--bs-pagination-bg`: Background color for pagination links (defaults to `--bs-body-bg`)
- `--bs-pagination-border-color`: Border color (defaults to `--bs-border-color`)
- `--bs-pagination-hover-color`: Text color on hover
- `--bs-pagination-hover-bg`: Background color on hover
- `--bs-pagination-active-color`: Text color for active page (defaults to `#fff`)
- `--bs-pagination-active-bg`: Background color for active page (defaults to `--bs-primary`: `#0d6efd`)
- `--bs-pagination-active-border-color`: Border color for active page (defaults to `--bs-primary`: `#0d6efd`)
- `--bs-pagination-disabled-color`: Text color for disabled elements (defaults to `--bs-secondary-color`)
- `--bs-pagination-disabled-bg`: Background color for disabled elements (defaults to `--bs-secondary-bg`)

Currently, neither theme customizes `.pagination`, leaving both the active state and inactive links with Bootstrap's loud electric blue.

## Goals / Non-Goals

**Goals:**
- Replace electric blue pagination and page size selector styles with calm monochrome / dark charcoal tokens.
- Support both light and dark mode (`[data-bs-theme="dark"]`) across both `xceptance-theme.css` and `emerald-theme.css`.
- Ensure all 5 paginated views (`/mailboxes`, `/admin/users`, `/admin/transactions`, `/mailboxes/{id}/mails`, `/mails`) inherit the clean styling immediately.
- Preserve all existing keyboard accessibility, ARIA attributes, and HTMX attributes without HTML template churn.
- Add automated verification tests ensuring theme tokens are properly declared and tested.

**Non-Goals:**
- Modifying backend controller logic (`MailboxWebController`, `AdminWebController`, `MailWebController`) or `PaginationHelper`.
- Restructuring table footer HTML markup or HTMX triggers.

## Decisions

### Decision 1: Scope styling via Bootstrap 5 `.pagination` CSS custom properties
- **Rationale**: Overriding `--bs-pagination-*` within the theme stylesheets allows all existing `.pagination` and `.pagination-sm` elements to adopt the modernized design without editing template files or introducing breaking class changes.
- **Alternatives Considered**:
  - *Custom utility classes (e.g. `.pagination-monochrome`)*: Would require updating every template fragment and risk inconsistency if new tables are added.
  - *Inline styles*: Anti-pattern, violates CSP and maintainability.

### Decision 2: Dark Charcoal active state with neutral body text
- **Rationale**:
  - Inactive links: Use `var(--bs-body-color)` instead of default link blue, rendering numbers in calm dark slate in light mode and off-white in dark mode.
  - Active item: Use `#212529` (dark slate / charcoal) in light mode and `#334155` (slate 700) in dark mode. This provides clear contrast (meeting WCAG AAA) while avoiding saturated primary colors.
  - Hover state: Soft neutral wash (`var(--bs-tertiary-bg)`).
- **Alternatives Considered**:
  - *Xceptance Navy (`#004682`)*: Considered in exploration, but charcoal is more neutral, modern, and does not conflict when switching between Xceptance and Emerald themes.
  - *Light gray pill with border*: Lower contrast, harder to distinguish at a glance on smaller viewports.

## Risks / Trade-offs

- **[Risk] Dark Mode Contrast**: If dark mode tokens are not explicitly defined, dark charcoal on dark backgrounds could suffer from poor contrast.
  - *Mitigation*: Define explicit `[data-bs-theme="dark"] .pagination` rules setting `--bs-pagination-active-bg: #334155` with lighter border `#475569` and white text, maintaining WCAG contrast ratios.
- **[Risk] SASS / CSS Cache**: Browsers or test runners might serve cached CSS.
  - *Mitigation*: Existing asset bundling and automated integration tests verify stylesheet content directly on the classpath.
