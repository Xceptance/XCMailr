# Spec Delta: web-console-ui

## ADDED Requirements

### Requirement: Persistent Color Mode Selection and Zero-Flash Navigation
The system SHALL support user selection of application color mode (`light` or `dark`), persisting the preference via an HTTP cookie (`xcmailr_color_mode`), resolving the active color mode on the server to populate the root HTML `data-bs-theme` attribute directly in initial page responses, eliminating visual white flashes (FOIT) during navigation, while falling back to the client OS color scheme preference when no cookie is set.

#### Scenario: User Switches to Dark Mode (Happy Path)
- **WHEN** a user clicks the theme mode toggle button while in light mode
- **THEN** the client immediately updates the document root `data-bs-theme` to `dark`
- **AND** sets a persistent cookie `xcmailr_color_mode=dark` with `path=/`, `max-age=31536000` (1 year), and `SameSite=Lax`
- **AND** updates all desktop and mobile toggle icons to reflect the active mode

#### Scenario: Full-Page Navigation with Dark Mode Cookie (Happy Path)
- **WHEN** a user with the `xcmailr_color_mode=dark` cookie navigates to any application view (e.g. `/`, `/mails`, `/admin/statistics`)
- **THEN** the server evaluates the cookie and populates `activeColorMode="dark"` in the model
- **AND** the rendered HTML contains `data-bs-theme="dark"` directly on the root `<html>` element
- **AND** the view renders immediately in dark mode without flashing light mode

#### Scenario: Initial Visitor without Cookie Honors OS Dark Preference (Happy Path)
- **WHEN** a visitor accesses the application without an existing `xcmailr_color_mode` cookie and their operating system prefers dark mode (`prefers-color-scheme: dark`)
- **THEN** client-side script sets `data-bs-theme="dark"` on `document.documentElement`
- **AND** updates theme switcher icons to display the sun icon

#### Scenario: Initial Visitor without Cookie Honors OS Light Preference (Default Case)
- **WHEN** a visitor accesses the application without an existing `xcmailr_color_mode` cookie and their operating system does not prefer dark mode
- **THEN** the server and client default to `data-bs-theme="light"`

#### Scenario: Malformed or Tampered Color Mode Cookie (Security / Fallback Case)
- **WHEN** a client transmits an `xcmailr_color_mode` cookie containing unrecognized values, special characters, or injection attempts (e.g. `dark"><script>`)
- **THEN** the server rejects the value and safely defaults `activeColorMode` to `"light"`

### Requirement: Dark Mode Table and Component Styling
The system SHALL render all data tables, card containers, input group affixes, and content preview panes responsively in accordance with the active color mode, eliminating hardcoded light utility classes (`bg-white`, `bg-light`, `.table-light`), providing cohesive background and border contrast across both `xceptance` and `emerald` themes.

#### Scenario: Table Display in Dark Mode (Happy Path)
- **WHEN** an authenticated user views any data table view (`/mailboxes`, `/mails`, `/admin/users`, `/admin/domains`, `/admin/transactions`, `/admin/statistics`) with `data-bs-theme="dark"` active
- **THEN** the table responsive wrapper displays a dark surface background matching the active theme card tokens
- **AND** the table header (`<thead>`) does not use `.table-light` and renders contrasting text and borders
- **AND** table rows display proper hover contrast states matching the active theme

#### Scenario: Card Headers and Form Controls in Dark Mode (Happy Path)
- **WHEN** a user views forms, card containers, or input groups (such as in `/profile` or `/admin/domains`) with `data-bs-theme="dark"` active
- **THEN** card headers do not render with `bg-white` and blend seamlessly with card bodies
- **AND** input group affixes (such as domain `@` symbols) and readonly inputs display dark mode backgrounds matching form control boundaries

#### Scenario: Mail Inspection Previews in Dark Mode (Happy Path)
- **WHEN** a user inspects message details, headers, or raw source in `/mailboxes/{id}/mails` or `/mails` with `data-bs-theme="dark"` active
- **THEN** navigation tabs and `<pre>` code preview boxes render with dark theme backgrounds (`bg-body-tertiary`) and readable text without whiteout
