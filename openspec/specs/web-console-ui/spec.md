# Web Console UI Specification

## Purpose

Provides a responsive, server-driven web management console using Thymeleaf and HTMX for self-service mailbox operations, email inspection, and system administration.

## Requirements

### Requirement: User Authentication and Self-Service
The system SHALL provide web views for user registration, email verification, session login, password reset, and profile management, handling both success and error conditions gracefully.

#### Scenario: User Login and Session Management
- **WHEN** a user enters valid email and password credentials for an active account on the login page
- **THEN** the system creates an authenticated HTTP session and redirects to the mailbox dashboard

#### Scenario: User Registration and Email Activation
- **WHEN** a new user completes the registration form and email confirmation is required
- **THEN** an activation token is generated, confirmation email is dispatched, account remains inactive, and the user is redirected to login with instructions to check their email

#### Scenario: User Registration with Immediate Activation
- **WHEN** a new user completes the registration form and email confirmation is disabled via configuration
- **THEN** the account is activated immediately upon creation without generating confirmation tokens or sending activation emails, allowing immediate login

#### Scenario: Unactivated Account Login Notification
- **WHEN** a user enters valid email and password credentials for an inactive account
- **THEN** the system prevents login and displays clear feedback that the account has not been activated yet

#### Scenario: Registration Password Mismatch Validation
- **WHEN** a user submits the registration form with mismatched password and confirm password fields
- **THEN** the system rejects registration, preserves entered non-sensitive fields, and displays a validation error message

#### Scenario: Registration Duplicate Email Prevention
- **WHEN** a user attempts to register with an email address that is already registered (case-insensitive)
- **THEN** the system prevents account creation and displays a user-friendly error message

#### Scenario: Invalid or Nonexistent Confirmation Token
- **WHEN** an unauthenticated request attempts to confirm an account with an invalid or non-existent token
- **THEN** the system redirects to the login view with an invalid token error alert without activating any account

#### Scenario: Expired Confirmation Token Handling
- **WHEN** an unauthenticated request attempts to confirm an account using a token whose expiration timestamp has passed
- **THEN** the system rejects activation and redirects to the login view with an expired token notification

#### Scenario: Case-Insensitive Email Authentication
- **WHEN** a user logs in using an email address whose casing differs from the registration record
- **THEN** the system normalizes the email casing and successfully authenticates the user

### Requirement: Dynamic Mailbox Dashboard
The system SHALL render an interactive mailbox dashboard supporting search, inline editing, modal dialogs, and bulk actions via HTMX without full page reloads, ensuring modal hosts are placed outside layout stacking contexts to prevent backdrop occlusion.

#### Scenario: Live Search and Filter
- **WHEN** the user types into the mailbox filter input
- **THEN** HTMX triggers an asynchronous partial update of the mailbox table matching the filter criteria

#### Scenario: Creation Modal Display
- **WHEN** the user clicks the "New Mailbox" button on the dashboard
- **THEN** the system dynamically loads and displays the creation modal in the foreground with input focus and domain options, with the modal backdrop positioned behind the modal dialog

#### Scenario: Successful Mailbox Creation and Table Refresh
- **WHEN** the user submits the new mailbox form with valid parameters
- **THEN** the modal dialog closes, the newly created mailbox appears in the dashboard table, and the table container markup is retained intact

#### Scenario: Creation Validation Feedback
- **WHEN** the user submits the new mailbox form with missing, malformed, or unauthorized domain data
- **THEN** the modal dialog remains open and presents inline validation feedback highlighting the specific error

#### Scenario: Modal Dialog Actions
- **WHEN** the user clicks to edit, delete, or extend expiration on an existing mailbox
- **THEN** HTMX loads the corresponding modal fragment dynamically into the shared modal host and updates the table upon successful submission

### Requirement: Admin Management Console
The system SHALL provide administrative interfaces for managing user accounts, reviewing transaction logs, analyzing traffic metrics, and maintaining the domain whitelist.

#### Scenario: Domain Whitelist Configuration
- **WHEN** an administrator adds or removes an allowed domain via the admin console
- **THEN** the system updates the registered domain table and enforces the change on the inbound SMTP listener immediately

#### Scenario: Paged Transaction Auditing
- **WHEN** an administrator inspects the mail transaction log
- **THEN** the system displays paginated records with filtering by timestamp, status, and address

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

