# Spec Delta: Web Console UI

## ADDED Requirements

### Requirement: Role-Based Navigation Visibility
The system SHALL conditionally render navigation links and action items based on the visitor's authentication state and granted roles, ensuring that administrative and personal account controls are never exposed to unauthorized sessions.

#### Scenario: Anonymous Visitor Navigation
- **WHEN** an unauthenticated visitor accesses any public web page
- **THEN** the navigation bar displays links for login and registration, and does not display mailbox management, administrative menus, or authenticated user profile controls

#### Scenario: Authenticated Standard User Navigation
- **WHEN** a logged-in user without administrative privileges accesses the application
- **THEN** the navigation bar displays the mailbox dashboard link and the user account menu, but completely omits the administration menu and its dropdown links

#### Scenario: Authenticated Administrator Navigation
- **WHEN** a logged-in user with administrator privileges accesses the application
- **THEN** the navigation bar displays the mailbox dashboard link, the user account menu, and the full administration menu with links to User Management, Domain Whitelist, Transactions, and Statistics

### Requirement: Server-Side Access Control Enforcement
The system SHALL enforce administrative authorization strictly on the server side in compliance with OWASP A01:2021 (Broken Access Control), rejecting any direct URL request, client-side tampering attempt, or invalid CSRF submission with HTTP 403 Forbidden or redirection.

#### Scenario: Unauthorized Access to Administrative Endpoints
- **WHEN** an authenticated user who lacks `ROLE_ADMIN` attempts to directly access any `/admin/**` endpoint (such as `/admin/users`, `/admin/domains`, `/admin/transactions`, or `/admin/statistics`)
- **THEN** the server denies access immediately with an HTTP 403 Forbidden response without executing the administrative action

#### Scenario: Anonymous Access to Administrative Endpoints
- **WHEN** an unauthenticated visitor attempts to directly access any `/admin/**` endpoint
- **THEN** the server intercepts the request and redirects the visitor to the login page with an HTTP 302 Found response

#### Scenario: Missing or Forged CSRF Token on Administrative Action
- **WHEN** an authenticated administrator submits a state-altering request to an `/admin/**` endpoint without a valid cryptographic CSRF token
- **THEN** the server rejects the request with an HTTP 403 Forbidden response and logs the unauthorized modification attempt

#### Scenario: Inactive or Disabled Account Direct Access
- **WHEN** a user account that has been marked inactive or disabled attempts to access protected web or administrative endpoints
- **THEN** the server denies access and redirects the user to the login page indicating inactive account status

### Requirement: Responsive Offcanvas Mobile Navigation
The system SHALL provide responsive navigation controls on small screens and viewports below 992px width using a sliding offcanvas drawer that preserves screen layout and categorizes navigation links cleanly.

#### Scenario: Mobile Hamburger Trigger and Offcanvas Display
- **WHEN** a user on a viewport narrower than 992px views the application and clicks the hamburger menu button
- **THEN** an offcanvas navigation drawer slides into view over the page backdrop, displaying all authorized navigation links and account controls

#### Scenario: Offcanvas Menu Dismissal
- **WHEN** the offcanvas navigation drawer is open and the user clicks the close button, clicks the backdrop overlay, or selects a navigation destination
- **THEN** the offcanvas drawer closes smoothly and returns focus to the main viewport

### Requirement: Theme Mode Selection
The system SHALL support light and dark color schemes, allowing users to switch themes dynamically and persisting their preference across sessions.

#### Scenario: System Color Scheme Auto-Detection
- **WHEN** a user visits the application without an explicitly saved theme preference in local storage
- **THEN** the system applies the color theme matching the operating system preference (`prefers-color-scheme`) and sets the corresponding theme toggle icon

#### Scenario: Manual Theme Toggle and Persistence
- **WHEN** a user clicks the theme toggle button in the navigation bar
- **THEN** the system immediately switches between light and dark themes without reloading the page, updates the toggle icon, and persists the chosen mode in browser local storage for subsequent visits

#### Scenario: Unknown or Corrupted LocalStorage Theme Value
- **WHEN** the browser local storage contains an invalid or unrecognized theme value
- **THEN** the system gracefully falls back to the system preference or standard corporate light mode without JavaScript errors

