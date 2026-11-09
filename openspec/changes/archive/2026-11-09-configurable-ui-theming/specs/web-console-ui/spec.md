# Spec Delta: web-console-ui

## ADDED Requirements

### Requirement: Configurable Web UI Theme
The system SHALL support configurable web UI theme selection via application configuration, dynamically resolving and linking the corresponding stylesheet while validating theme names against safe slug conventions, verifying file existence on the classpath, falling back safely to the default theme if the requested theme is missing or invalid, and providing an alternative modern developer theme ('emerald') out of the box.

#### Scenario: Default Theme Resolution (Happy Path)
- **WHEN** the application starts with default configuration or `xcmailr.ui.theme` is unset
- **THEN** the system links `/css/xceptance-theme.css` in the master layout
- **AND** the web console renders with the default Xceptance corporate theme

#### Scenario: Alternative Emerald Theme Selection (Happy Path)
- **WHEN** the application is configured with `xcmailr.ui.theme = "emerald"` (or environment variable `XCMAILR_UI_THEME=emerald`)
- **THEN** the system verifies that `/static/css/emerald-theme.css` exists on the classpath
- **AND** links `/css/emerald-theme.css` in the master layout
- **AND** all web console views render with the emerald palette and typography

#### Scenario: Custom Theme Discovery via Classpath (Happy Path)
- **WHEN** a custom theme stylesheet matching `[a-zA-Z0-9_-]+-theme.css` (e.g. `acme-theme.css`) is present in the static CSS directory and configured via `xcmailr.ui.theme`
- **THEN** the system verifies file existence and links the custom stylesheet without requiring application code changes

#### Scenario: Static Asset Delivery for Out-of-the-Box Themes (Happy Path)
- **WHEN** a client issues an HTTP GET request to `/css/xceptance-theme.css` or `/css/emerald-theme.css`
- **THEN** the server returns HTTP 200 OK with `Content-Type: text/css` and cache headers

#### Scenario: Missing Theme Fallback (Error / Fallback Case)
- **WHEN** `xcmailr.ui.theme` specifies a theme name whose stylesheet does not exist on the classpath (e.g. `nonexistent` or typo `emrald`)
- **THEN** the system logs a descriptive warning indicating that the requested theme stylesheet was not found
- **AND** safely falls back to linking `/css/xceptance-theme.css` so views render without broken styling

#### Scenario: Path Traversal Attack Prevention (Security Case)
- **WHEN** `xcmailr.ui.theme` contains directory traversal sequences such as `../`, `..\\`, `/etc/passwd`, `..%2f`, or absolute filesystem paths
- **THEN** the system rejects the input as unsafe and falls back to `xceptance`
- **AND** links `/css/xceptance-theme.css` without attempting filesystem lookups outside the designated static CSS directory

#### Scenario: Special Characters and Injection Prevention (Security Case)
- **WHEN** `xcmailr.ui.theme` contains shell metacharacters, punctuation, null bytes, HTML tags, or whitespace (such as `<script>`, `;`, `|`, `&`, `$`, `\0`, spaces)
- **THEN** the system rejects the input and falls back to `xceptance`

#### Scenario: Boundary Length Exceeded Fallback (Edge Case)
- **WHEN** `xcmailr.ui.theme` exceeds the maximum allowed theme slug length (32 characters)
- **THEN** the system safely defaults to `xceptance` rather than processing excessively long inputs
