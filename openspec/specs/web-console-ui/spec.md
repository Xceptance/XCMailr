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
The system SHALL render an interactive mailbox dashboard supporting search, inline editing, modal dialogs, and bulk actions via HTMX without full page reloads, ensuring modal hosts are placed outside layout stacking contexts to prevent backdrop occlusion, while adapting table columns and actions responsively across viewport sizes with a calm corporate palette.

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

#### Scenario: Mailbox Deletion UI Refresh
- **WHEN** the user confirms deletion of a mailbox from the mailbox actions menu
- **THEN** the mailbox and associated emails are deleted on the server, and the mailbox table automatically refreshes via HTMX without blanking out the page or removing the table container element from the DOM, displaying remaining mailboxes or the empty state if none remain

#### Scenario: Inline Row Action UI Refresh
- **WHEN** the user triggers an inline or dropdown action on a mailbox row (such as enable/disable toggle, +24h validity extension, or counter reset)
- **THEN** the action is processed and the mailbox table refreshes smoothly via HTMX without removing the table container or causing page content to disappear

#### Scenario: Preservation of Filter and Search on Table Refresh
- **WHEN** a mailbox action (create, edit, delete, toggle, extend, or reset) triggers an automatic table refresh
- **THEN** the refreshed table retains the currently active search query and status filter selection

#### Scenario: Responsive Mobile Table Viewport
- **WHEN** a user views the mailbox table on a mobile viewport narrower than 768px
- **THEN** the table hides secondary columns (forwards, suppressed, and expiration) to prevent horizontal scrolling and displays essential expiration and forwarding status as a subline beneath the primary email address

#### Scenario: Responsive Action Menu Consolidation
- **WHEN** a user interacts with a mailbox row on a mobile viewport narrower than 768px
- **THEN** standalone quick action buttons (Disable and +24h) are collapsed into the action dropdown menu, presenting a single compact action button per row

#### Scenario: Subtle Visual Status and Mail Count Indicators
- **WHEN** the mailbox table is rendered
- **THEN** active and expired statuses are displayed using subtle desaturated tokens or status dots rather than high-saturation solid badges, and zero mail counts are rendered with neutral muted styling

#### Scenario: Responsive Filter and Search Bar Stacking
- **WHEN** the mailbox page is displayed on a mobile viewport
- **THEN** the search input expands to full width and the status filter select element stacks cleanly underneath without horizontal clipping or misaligned margins

### Requirement: Admin Management Console
The system SHALL provide administrative interfaces for managing user accounts, reviewing transaction logs, analyzing traffic metrics, and maintaining the domain whitelist.

#### Scenario: Domain Whitelist Configuration
- **WHEN** an administrator adds or removes an allowed domain via the admin console
- **THEN** the system updates the registered domain table and enforces the change on the inbound SMTP listener immediately

#### Scenario: Paged Transaction Auditing
- **WHEN** an administrator inspects the mail transaction log
- **THEN** the system displays paginated records with filtering by timestamp, status, and address

#### Scenario: Default Domain Whitelist Seeding on Startup
- **WHEN** the application starts up against a fresh or migrated legacy database
- **THEN** the system ensures the administrator's default domain is present in the registered domain whitelist if absent, ensuring that mailbox creation domain options are available

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

### Requirement: Custom Error Handling and Navigation Recovery
The system SHALL provide styled, user-friendly error views for HTTP client and server error conditions matching the corporate layout, displaying clear non-technical diagnostic summaries and navigational recovery options while suppressing sensitive stack traces from unprivileged users.

#### Scenario: Not Found Error View (404)
- **WHEN** a user or visitor navigates to a non-existent or invalid URL path
- **THEN** the system renders a themed error page displaying an HTTP 404 status indicator, a clear explanation that the requested resource does not exist, and a primary action button to navigate back to the home/dashboard page

#### Scenario: Server Error View (500)
- **WHEN** an unhandled exception or internal error occurs during request processing
- **THEN** the system renders a themed error page displaying an HTTP 500 status indicator, an apology message indicating a temporary issue, without exposing raw exception traces or internal database details

#### Scenario: HTMX Error Feedback
- **WHEN** an asynchronous HTMX partial request encounters an error status code
- **THEN** the system provides a clean contextual error indicator without breaking the page layout or replacing parent container markup with raw error HTML

### Requirement: Unified Cross-Mailbox Inbox View
The system SHALL provide an interactive, unified inbox view at `/mails` displaying emails across all mailboxes owned by the authenticated user, indicating recipient mailbox addresses, providing HTMX-driven partial updates, ensuring strict tenant data isolation, and adapting responsively across viewport sizes.

#### Scenario: Full Page Load of Unified Inbox (Happy Path)
- **WHEN** an authenticated user navigates to `/mails`
- **THEN** the system renders a complete HTML page containing the list of received emails across all owned mailboxes, each showing received timestamp, sender, subject, and the recipient mailbox address badge
- **AND** the response body is non-empty and includes breadcrumb navigation and the message viewer container `#mail-viewer-pane`

#### Scenario: Asynchronous Refresh via HTMX (Happy Path)
- **WHEN** the user triggers an inbox refresh on the `/mails` page
- **THEN** HTMX requests `/mails` with header `HX-Request: true`
- **AND** the system responds with only the `mailList` fragment markup containing the updated email items, without the full page layout shell

#### Scenario: Email Row Selection and Detail Preview Loading (Happy Path)
- **WHEN** an authenticated user clicks an email row within the unified inbox message list
- **THEN** HTMX issues an asynchronous GET request for the email detail fragment (`/mailboxes/{mailboxId}/mails/{mailId}`)
- **AND** the system responds with the email preview fragment (`mailboxes/fragments/mail-detail :: detail`)
- **AND** HTMX swaps the detail markup into `#mail-viewer-pane` without reloading the page

#### Scenario: Multi-Mailbox Disambiguation (Happy Path)
- **WHEN** an authenticated user owns multiple distinct mailboxes and receives emails to each
- **THEN** the unified inbox renders every email with a badge explicitly reflecting the corresponding destination mailbox address

#### Scenario: Empty Inbox State (Edge Case)
- **WHEN** an authenticated user with zero received emails across all mailboxes accesses `/mails`
- **THEN** the system displays a clear, user-friendly empty state indicator stating that no messages have been received yet, rather than rendering an empty table or broken layout

#### Scenario: Resilient Rendering with Missing Mailbox Metadata (Edge Case)
- **WHEN** an email record exists whose associated mailbox ID is not present in `mailboxAddressMap`
- **THEN** the system renders the email row gracefully using a null-safe fallback label (such as `Unknown Mailbox`) without throwing a template evaluation exception

#### Scenario: Responsive Mobile Layout on Small Screens (Edge Case / UX)
- **WHEN** a user accesses the unified inbox view on a mobile viewport narrower than 768px
- **THEN** the message list and message preview pane stack vertically (`col-12`), secondary table elements collapse into compact sublines, and email selection targets maintain accessible touch padding (`>= 44px`) without horizontal clipping

#### Scenario: Unauthenticated Access Restriction (Security Case)
- **WHEN** an unauthenticated visitor attempts to access `/mails`
- **THEN** the system rejects the request and redirects the user to the `/login` page with HTTP 302 Found

#### Scenario: Cross-User Tenant Isolation (Security Case)
- **WHEN** User A and User B both own mailboxes with received emails in the system
- **AND** User A navigates to `/mails`
- **THEN** User A only sees emails destined for mailboxes owned by User A, and zero emails, subjects, or addresses belonging to User B are exposed in the HTML output or model

#### Scenario: Disallowed HTTP Methods (Error Case)
- **WHEN** a client submits a non-GET HTTP request (e.g. POST, PUT, DELETE) directly to `/mails`
- **THEN** the system rejects the request with HTTP 405 Method Not Allowed

### Requirement: Hypermedia Request Security and Asynchronous Event Handling
The system SHALL secure all asynchronous client hypermedia operations against cross-site request forgery by attaching valid CSRF tokens to mutating requests, enforcing strict token validation, suppressing automatic error swaps, and operating under standard explicit inheritance without unintended trigger leakage.

#### Scenario: Asynchronous Request With Valid CSRF Token
- **WHEN** an authenticated user triggers an asynchronous hypermedia mutation (POST, PUT, or DELETE) through the web console
- **THEN** the client event listener extracts the CSRF token from page metadata and attaches it to the request headers
- **AND** the server accepts the request and executes the corresponding action successfully

#### Scenario: Asynchronous Request With Missing CSRF Token
- **WHEN** an asynchronous hypermedia mutation request is transmitted to any protected endpoint without a CSRF token header
- **THEN** the server denies the request immediately with an HTTP 403 Forbidden response without modifying any application state

#### Scenario: Asynchronous Request With Invalid or Forged CSRF Token
- **WHEN** an asynchronous hypermedia mutation request is transmitted with an unrecognized, expired, or forged CSRF token value
- **THEN** the server rejects the request with an HTTP 403 Forbidden response and preserves all data untouched

#### Scenario: Asynchronous Error Response Handling
- **WHEN** an asynchronous hypermedia request receives a 4xx or 5xx error status code from the server
- **THEN** the client hypermedia engine suppresses automatic DOM replacement of target elements with unformatted error bodies, preserving existing UI context

#### Scenario: Standard Explicit Inheritance Without Trigger Leakage
- **WHEN** an asynchronous hypermedia interaction occurs on any view containing nested containers with event triggers (such as `mailListChanged from:body`)
- **THEN** the client hypermedia engine operates under standard explicit inheritance (`implicitInheritance` disabled)
- **AND** child interactive elements do not inadvertently inherit ancestor triggers, retaining their default or explicitly declared triggers (such as `click`)

### Requirement: Mailbox Email Inspection View
The system SHALL provide an interactive email inspection view for individual mailboxes at `/mailboxes/{id}/mails`, listing received messages and loading message previews asynchronously into `#mail-viewer-pane` upon row selection.

#### Scenario: Mailbox Email List Full Page Load (Happy Path)
- **WHEN** an authenticated user navigates to `/mailboxes/{id}/mails` for a mailbox they own
- **THEN** the system renders the email list table, breadcrumbs, search/filter controls, and the `#mail-viewer-pane` placeholder

#### Scenario: Mailbox Email Row Selection and Preview Loading (Happy Path)
- **WHEN** an authenticated user clicks an email row in the mailbox email list
- **THEN** HTMX issues an asynchronous GET request with `HX-Request: true` to `/mailboxes/{id}/mails/{mailId}`
- **AND** the system responds with the email preview fragment (`mailboxes/fragments/mail-detail :: detail`)
- **AND** HTMX swaps the preview markup into `#mail-viewer-pane` without reloading the page
