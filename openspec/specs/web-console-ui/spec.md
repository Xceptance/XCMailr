# Web Console UI Specification

## Purpose

Provides a responsive, server-driven web management console using Thymeleaf and HTMX for self-service mailbox operations, email inspection, and system administration.

## Requirements

### Requirement: User Authentication and Self-Service
The system SHALL provide web views for user registration, email verification, session login, password reset, and profile management including self-service account deletion, handling both success and error conditions gracefully.

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

#### Scenario: Self-Service Account Deletion with Valid Password (Happy Path)
- **WHEN** an authenticated user opens the account deletion modal on their profile page, enters their correct current password, and submits the form
- **THEN** the system transactionally removes all stored emails for the user's mailboxes, removes all mailboxes owned by the user, and deletes the user record
- **AND** the system invalidates the current HTTP session, clears security credentials, and redirects to `/login?accountDeleted` displaying an informative confirmation banner

#### Scenario: Self-Service Account Deletion with Incorrect Password (Error Case)
- **WHEN** an authenticated user attempts to delete their account but submits an incorrect password
- **THEN** the system rejects the deletion, retains all user mailboxes, emails, and account records unaltered, and reloads the profile page with a clear error alert indicating the password was incorrect

#### Scenario: Self-Service Account Deletion with Blank Password (Error Case)
- **WHEN** an authenticated user submits the account deletion request with a blank or whitespace-only password
- **THEN** the system rejects the deletion and reloads the profile page with an error notification requiring password entry

#### Scenario: Sole Administrator Account Deletion Prevention (Special Case / Security)
- **WHEN** an authenticated administrator attempts to delete their own account and they are the only remaining active administrator in the system
- **THEN** the system blocks the deletion, keeps all administrator and mailbox data intact, and reloads the profile page displaying an error message stating that the last administrator account cannot be deleted

#### Scenario: Administrator Self-Service Deletion When Multiple Administrators Exist (Happy Path)
- **WHEN** an authenticated administrator enters their valid password to delete their account and at least one other active administrator account exists
- **THEN** the system permits the deletion, cascades data cleanup, terminates the session, and redirects to `/login?accountDeleted`

#### Scenario: Unauthenticated Account Deletion Attempt (Security Case)
- **WHEN** an unauthenticated request attempts to invoke `POST /profile/delete`
- **THEN** the system rejects the request and redirects to `/login` without modifying any user records

#### Scenario: Account Deletion Attempt With Missing CSRF Token (Security Case)
- **WHEN** an account deletion request is transmitted without a CSRF token
- **THEN** the system rejects the request immediately with an HTTP 403 Forbidden response without modifying any user data

#### Scenario: Account Deletion Attempt With Invalid CSRF Token (Security Case)
- **WHEN** an authenticated user submits an account deletion request with an invalid or forged CSRF token
- **THEN** the system rejects the request with an HTTP 403 Forbidden response and preserves all account data untouched

### Requirement: Dynamic Mailbox Dashboard
The system SHALL render an interactive mailbox dashboard supporting search, inline editing, modal dialogs, and bulk actions via HTMX without full page reloads, ensuring modal hosts are placed outside layout stacking contexts to prevent backdrop occlusion, while adapting table columns and actions responsively across viewport sizes with a calm corporate palette, presenting mailbox active duration selection via responsive segmented radio button pills, honoring forwarding preferences on creation, preserving duration and forwarding selections across validation error re-renders, rendering timestamps localized to the client browser's timezone, defaulting table order to creation order (`id DESC`) so newly created mailboxes appear at the top, providing full-dataset server-side column sorting with case-insensitive natural alphabetical ordering for email addresses, displaying clear modern sortable indicator chevrons that adapt cleanly to mobile viewports, treating permanent unlimited mailboxes as having infinite lifespan ($+\infty$) in expiration sorting, offering extended status filter options (`ALL`, `ACTIVE`, `EXPIRED`, `EXPIRING`, `UNLIMITED`), providing uniform vertical alignment across all table header elements, and presenting a streamlined 7-column layout without redundant calculated mail counts.

#### Scenario: Live Search and Filter
- **WHEN** the user types into the mailbox filter input
- **THEN** HTMX triggers an asynchronous partial update of the mailbox table matching the filter criteria

#### Scenario: Creation Modal Display
- **WHEN** the user clicks the "New Mailbox" button on the dashboard
- **THEN** the system dynamically loads and displays the creation modal in the foreground with input focus and domain options, with the modal backdrop positioned behind the modal dialog

#### Scenario: Mailbox Creation Active Duration Segmented Radio Controls
- **WHEN** the user opens the mailbox creation modal
- **THEN** active duration is presented as a segmented radio button pill group displaying preset durations (`1h`, `2h`, `1d`, `7d`, `30d`, `Unlimited`) with standard form name `durationHours`
- **AND** `2 Hours` (`value="2"`) is selected by default
- **AND** selecting any pill immediately activates that radio option with single-click visual feedback without dropdown menus or popup dismiss ambiguities

#### Scenario: Mailbox Creation with 30-Day Duration Preset (Happy Path)
- **WHEN** the user selects the `30 Days` duration pill (`durationHours = 720`) and submits the creation form with valid parameters
- **THEN** the system persists the mailbox with expiration timestamp calculated exactly matching the selected duration (`now + 720 hours`)
- **AND** the table refreshes displaying the corresponding localized expiration date approximately 30 days in the future

#### Scenario: Mailbox Creation with Unlimited Duration (Happy Path)
- **WHEN** the user selects the `Unlimited` duration pill (`durationHours = 0`) and submits the creation form with valid parameters
- **THEN** the system persists the mailbox with `ts_Active = 0` (permanent mailbox)
- **AND** the table refreshes displaying "Unlimited" for expiration

#### Scenario: Successful Mailbox Creation and Table Refresh
- **WHEN** the user submits the new mailbox form with valid parameters
- **THEN** the modal dialog closes and its backdrop is cleanly removed from the DOM, restoring normal viewport brightness and interactivity, the newly created mailbox appears in the dashboard table, and the table container markup is retained intact

#### Scenario: Mailbox Creation Forwarding Switch Disabled
- **WHEN** the user unchecks the "Forward incoming emails" switch in the creation modal and submits the form
- **THEN** the system persists the mailbox with forwarding disabled (`forwardEmails = false`), and the resulting dashboard row reflects inactive forwarding ("forwarding off") rather than active forwarding

#### Scenario: Mailbox Creation Forwarding Switch Enabled
- **WHEN** the user submits the creation modal with the "Forward incoming emails" switch enabled
- **THEN** the system persists the mailbox with forwarding enabled (`forwardEmails = true`), and the resulting dashboard row reflects active forwarding ("-> forwards")

#### Scenario: Standard Client-Side Localized Timestamps
- **WHEN** the mailbox table, modals, or mail views render expiration or activity timestamps
- **THEN** the server outputs standard HTML5 `<time class="local-time">` elements with an ISO-8601 UTC `datetime` attribute and UTC fallback text, and client-side execution formats the displayed text into the user's local timezone matching their system clock using `Intl.DateTimeFormat` on both initial load and subsequent HTMX DOM swaps

#### Scenario: Creation Validation Feedback
- **WHEN** the user submits the new mailbox form with missing, malformed, or unauthorized domain data
- **THEN** the modal dialog remains open and presents inline validation feedback highlighting the specific error
- **AND** the user's selected duration radio pill, address prefix, domain, and forwarding switch state are preserved in the re-rendered modal

#### Scenario: Modal Dialog Actions
- **WHEN** the user clicks to edit, delete, or extend expiration on an existing mailbox
- **THEN** HTMX loads the corresponding modal fragment dynamically into the shared modal host, and upon successful submission the modal dialog and its backdrop are dismissed cleanly and the table updates

#### Scenario: Mailbox Deletion UI Refresh
- **WHEN** the user confirms deletion of a mailbox from the mailbox actions menu
- **THEN** the mailbox and associated emails are deleted on the server, and the mailbox table automatically refreshes via HTMX without blanking out the page or removing the table container element from the DOM, displaying remaining mailboxes or the empty state if none remain

#### Scenario: Inline Row Action UI Refresh
- **WHEN** the user triggers an inline or dropdown action on a mailbox row (such as the compact status toggle icon button, +1d extension badge button, or counter reset from the overflow menu)
- **THEN** the action is processed and the mailbox table refreshes smoothly via HTMX without removing the table container or causing page content to disappear, and row button tooltips use native HTML title attributes so that no orphaned tooltip elements remain visible after the click or DOM swap

#### Scenario: Preservation of Filter and Search on Table Refresh
- **WHEN** a mailbox action (create, edit, delete, toggle, extend, or reset) triggers an automatic table refresh
- **THEN** the refreshed table retains the currently active search query and status filter selection (`ALL`, `ACTIVE`, `EXPIRED`, `EXPIRING`, or `UNLIMITED`)

#### Scenario: Dashboard Header Primary Action Exclusivity
- **WHEN** the mailbox dashboard page header is rendered
- **THEN** the top header action area presents solely the primary creation action ("New Mailbox"), omitting secondary export dropdown controls to maintain visual hierarchy and prevent action clutter

#### Scenario: Table Footer Utility Export Links
- **WHEN** the mailbox table is rendered
- **THEN** subtle secondary text export links ("All (.txt)" and "Active (.txt)") are rendered within the table footer utility and pagination area below the table, remaining accessible without competing with primary page actions

#### Scenario: Responsive Mobile Table Viewport
- **WHEN** a user views the mailbox table on a mobile viewport narrower than 576px
- **THEN** the table displays the sortable `Address` header with an inline, non-wrapping chevron indicator (`bi-chevron-expand` or active direction chevron)
- **AND** secondary columns (`Status`, `Expires`, `Forwards`, `Suppressed`) remain hidden from table headers and display their essential status, expiration, and forwarding info within the address row subline
- **AND** the header link retains comfortable tap targets without overflowing the mobile viewport or causing horizontal layout breakage

#### Scenario: Responsive Action Menu Consolidation
- **WHEN** a user interacts with a mailbox row on a mobile viewport narrower than 768px
- **THEN** standalone compact icon quick action buttons (toggle and +1d) are collapsed into the `[...]` kebab dropdown menu, presenting a single compact action button per row with all actions accessible within the dropdown

#### Scenario: Subtle Visual Status and Mail Count Indicators
- **WHEN** the mailbox table is rendered
- **THEN** active and expired statuses are displayed using subtle desaturated tokens or status dots rather than high-saturation solid badges, and the table presents a clean 7-column layout omitting redundant calculated mail counts

#### Scenario: Uniform Header Row Vertical Alignment
- **WHEN** the mailbox table is rendered across desktop and mobile viewports
- **THEN** table header cells (including the select-all checkbox, sortable headers, and the Actions header) are vertically aligned uniformly along the row using middle alignment and consistent vertical padding, preventing the Actions header and checkbox from sitting deeper or lower than sortable column headers

#### Scenario: Responsive Filter and Search Bar Stacking
- **WHEN** the mailbox page is displayed on a mobile viewport
- **THEN** the search input expands to full width and the status filter select element stacks cleanly underneath without horizontal clipping or misaligned margins

#### Scenario: Mailbox Action Dropdown Visibility and Overflow Prevention
- **WHEN** the user opens the action dropdown menu for any mailbox row in a table container with overflow or few rows
- **THEN** the dropdown menu is positioned using a fixed viewport boundary strategy so that it renders completely visible without being clipped or hidden by table responsive overflow boundaries

#### Scenario: Contextual Floating Batch Bar Visibility
- **WHEN** zero mailboxes are selected via checkboxes
- **THEN** the floating batch action bar remains completely hidden
- **WHEN** one or more mailboxes are selected via row checkboxes or the master header checkbox
- **THEN** a floating bottom batch action bar appears indicating the exact count of selected mailboxes, providing bulk action buttons and a button to deselect all

#### Scenario: Master Checkbox Toggle Behavior
- **WHEN** the user clicks the master checkbox in the mailbox table header
- **THEN** all visible row checkboxes on the current page are toggled to match the master checkbox state, and the floating batch bar updates its count or visibility accordingly

#### Scenario: Bulk Enable Operation
- **WHEN** the user triggers the "Enable" action from the floating batch bar with one or more mailboxes selected
- **THEN** the system issues an asynchronous request to activate all selected mailboxes owned by the user and refreshes the mailbox table via HTMX with updated statuses

#### Scenario: Bulk Disable Operation
- **WHEN** the user triggers the "Disable" action from the floating batch bar with one or more mailboxes selected
- **THEN** the system issues an asynchronous request to deactivate all selected mailboxes owned by the user and refreshes the mailbox table via HTMX with updated statuses

#### Scenario: Bulk Validity Extension
- **WHEN** the user triggers the "+24h" extension action from the floating batch bar with one or more mailboxes selected
- **THEN** the system adds 24 hours to the expiration timestamps of all selected mailboxes owned by the user and refreshes the mailbox table via HTMX

#### Scenario: Bulk Counter Reset
- **WHEN** the user triggers the "Reset Counters" action from the floating batch bar with one or more mailboxes selected
- **THEN** the system resets the forward and suppression counters to zero for all selected mailboxes owned by the user and refreshes the mailbox table via HTMX

#### Scenario: Bulk Mailbox Deletion Confirmation and Execution
- **WHEN** the user triggers the "Delete" action from the floating batch bar
- **THEN** the browser displays a confirmation alert prompting the user to confirm deletion of the selected mailboxes
- **WHEN** confirmed, the system deletes the selected mailboxes and their stored messages and refreshes the mailbox table via HTMX

#### Scenario: Bulk Plain Text Address Export
- **WHEN** the user requests a text export of selected mailboxes, all owned mailboxes, or active mailboxes
- **THEN** the system generates and streams a plain text response (`text/plain`) with `Content-Disposition: attachment; filename="..."` containing the requested mailbox addresses formatted one per line, prompting the browser to trigger a genuine file download

#### Scenario: Cross-Tenant Bulk Action Security Isolation
- **WHEN** an authenticated user submits a bulk action containing IDs belonging to another user
- **THEN** the system strictly filters out or rejects mailboxes not owned by the authenticated user, preventing unauthorized modification or deletion of other tenants' mailboxes

#### Scenario: Empty Selection Bulk Action Protection
- **WHEN** a client issues a bulk action request with an empty or missing list of mailbox IDs
- **THEN** the system handles the request safely without modifying data or throwing unhandled server exceptions, returning a clean response

#### Scenario: Default Sort Order by Creation Date (Happy Path)
- **WHEN** an authenticated user accesses the mailbox dashboard without explicit sort parameters, or after creating a new mailbox
- **THEN** the mailbox list is sorted by creation date in descending order (`id DESC`), causing newly created mailboxes to appear immediately at the top of the list
- **AND** all visible sortable column headers display the neutral inactive chevron indicator (`bi-chevron-expand`)

#### Scenario: Full-Dataset Column Sorting (Happy Path)
- **WHEN** an authenticated user clicks a sortable column header (`Address`, `Status`, `Expires`, `Forwards`, or `Suppressed`)
- **THEN** HTMX issues an asynchronous GET request to `/mailboxes` with parameters `sort` (selected column key) and `dir` (`asc` or `desc`)
- **AND** the system sorts the entire user mailbox dataset at the database level before applying pagination limits, displaying the first page of results
- **AND** sorting by `Address` applies case-insensitive natural alphabetical ordering to the local address part followed by the domain name, ensuring uppercase letters do not sort ahead of lowercase letters
- **AND** the active sorted column header renders a modern directional chevron indicator (`bi-chevron-up` for ascending, `bi-chevron-down` for descending) styled with primary accent
- **AND** inactive sortable column headers render a subtle neutral sortable chevron indicator (`bi-chevron-expand`) indicating sortability across desktop and touch viewports

#### Scenario: Column Sort Direction Toggle (Happy Path)
- **WHEN** an authenticated user clicks an already active sort column header
- **THEN** the sort direction is inverted (from `asc` to `desc`, or from `desc` to `asc`), and the table re-renders reflecting the inverted order from page 0

#### Scenario: Sort State Preservation Across Search, Filter, Pagination, and Refreshes (Happy Path)
- **WHEN** an authenticated user performs a live search, selects a status filter, navigates across pages, or triggers a mailbox action resulting in a `mailboxChanged` table refresh while an active sort is applied
- **THEN** the system preserves the active `sort` and `dir` parameters, returning table results ordered according to the selected sort criteria

#### Scenario: Status Filter by Expiring Mailboxes (Happy Path)
- **WHEN** the user selects the "Expiring" status filter option (`status=EXPIRING`)
- **THEN** the system filters the mailbox list to display exclusively active mailboxes with finite expiration dates (`expired = false AND ts_Active > 0`), excluding permanent unlimited mailboxes and already expired mailboxes

#### Scenario: Status Filter by Unlimited Mailboxes (Happy Path)
- **WHEN** the user selects the "Unlimited" status filter option (`status=UNLIMITED`)
- **THEN** the system filters the mailbox list to display exclusively active permanent mailboxes (`ts_Active = 0`), excluding expiring mailboxes and deactivated/expired mailboxes

#### Scenario: Status Filter Unknown Fallback (Error Case)
- **WHEN** a request supplies an unknown, empty, or unsupported status filter parameter (e.g. `status=INVALID` or arbitrary strings)
- **THEN** the system safely defaults to displaying all user mailboxes (`status=ALL`) without raising an error

#### Scenario: Unrecognized Sort Column Fallback (Error Case)
- **WHEN** a request supplies an unknown, empty, or malicious `sort` parameter (e.g. `sort=invalid` or SQL injection strings)
- **THEN** the system rejects the arbitrary parameter and safely defaults to sorting by mailbox ID in descending order (`id DESC`) without throwing server exceptions or exposing database errors

#### Scenario: Malformed Sort Direction Fallback (Error Case)
- **WHEN** a request supplies an unrecognized or malformed `dir` parameter (e.g. `dir=sideways` or `dir=123`)
- **THEN** the system safely defaults the direction to `desc` (or `asc` depending on default column preference) without failing the request

#### Scenario: Multi-Page Dataset Sorting Preventing Local-Page-Only Sorting (Special Case / Issue #91)
- **WHEN** a user owns mailboxes spanning multiple pagination pages and requests a sorted view
- **THEN** the entire collection is sorted before slicing into pages, such that page 1 contains the globally first elements and page 2 contains subsequent elements, preventing client-side page-only reordering

#### Scenario: Case-Insensitive Address Sorting with Mixed-Case Mailboxes (Special Case)
- **WHEN** an authenticated user sorts mailboxes by `Address` in ascending order (`dir=asc`) or descending order (`dir=desc`) in a dataset containing mixed-case, lowercase, and uppercase email addresses
- **THEN** the system orders mailboxes strictly in case-insensitive alphabetical order (for example, `alpha`, `Bravo`, `charlie`), ensuring that uppercase initial letters (such as `UCoMfSLVUDQudxjJ@xcmailr.test`) do not sort ahead of lowercase initial letters (such as `aaaa@xcmailr.test`) due to ASCII code point collation

#### Scenario: Expiration Timestamp Ordering with Permanent Unlimited Mailboxes (Special Case)
- **WHEN** an authenticated user sorts by `Expires` (`ts_Active`) in ascending order (`dir=asc`) in a dataset containing both finite expiration dates and permanent mailboxes (`ts_Active = 0`)
- **THEN** finite expiration mailboxes are sorted chronologically with expiring soonest first, and permanent unlimited mailboxes (`ts_Active = 0`) appear at the very end of the sorted dataset as $+\infty$
- **WHEN** an authenticated user sorts by `Expires` (`ts_Active`) in descending order (`dir=desc`) in a dataset containing both finite expiration dates and permanent mailboxes (`ts_Active = 0`)
- **THEN** permanent unlimited mailboxes (`ts_Active = 0`) appear at the very top of the sorted dataset as $+\infty$, followed by finite expiration mailboxes sorted in reverse chronological order with expiring furthest in future first down to expiring soonest

#### Scenario: Combined Expiration Sorting and Status Filtering (Special Case)
- **WHEN** an authenticated user applies a status filter (`ALL`, `ACTIVE`, `EXPIRED`, `EXPIRING`, or `UNLIMITED`) and sorts by `Expires` in ascending or descending direction
- **THEN** the system applies both the status filter predicate and the infinity-aware expiration sort simultaneously across the full user dataset before pagination

#### Scenario: Sorting an Empty Mailbox List (Special Case)
- **WHEN** an authenticated user with zero mailboxes applies a column sort or filter
- **THEN** the table renders the standard empty state message with active sort headers intact without throwing errors

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
The system SHALL provide an interactive, paginated unified inbox view at `/mails` displaying emails across all mailboxes owned by the authenticated user using lightweight metadata projections that exclude raw message BLOB payloads from memory, indicating recipient mailbox addresses, providing HTMX-driven partial updates and page navigation, ensuring strict tenant data isolation so users cannot view emails belonging to other users, preventing SQL injection on all input vectors, and adapting responsively across viewport sizes.

#### Scenario: Full Page Load of Unified Inbox (Happy Path)
- **WHEN** an authenticated user navigates to `/mails`
- **THEN** the system renders a complete HTML page containing the paginated list of received emails across all owned mailboxes, each showing received timestamp, sender, subject, and the recipient mailbox address badge
- **AND** the response body is non-empty and includes breadcrumb navigation, pagination controls, and the message viewer container `#mail-viewer-pane`

#### Scenario: Paginated Navigation via HTMX (Happy Path)
- **WHEN** an authenticated user clicks a pagination link (such as "Next" or a specific page number) on `/mails`
- **THEN** HTMX requests `/mails` with parameters `page` and `size` and header `HX-Request: true`
- **AND** the system responds with only the updated `mailList` fragment markup containing the requested page of emails and refreshed pagination controls without reloading the entire page

#### Scenario: Projection-Only Fetching (Memory Protection)
- **WHEN** the system queries emails for the unified inbox list
- **THEN** the persistence layer executes a projection query selecting only `id`, `mailbox`, `sender`, `subject`, and `receiveTime`
- **AND** the full raw email MIME payload (`message` BLOB column) is excluded from the query, preventing excessive heap allocation and dirty-checking snapshot cloning

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

#### Scenario: Out of Range Page Index Handling (Edge Case)
- **WHEN** a user or client requests `/mails` with a negative page number (e.g. `page=-1`)
- **THEN** the system normalizes the page parameter to 0 and renders the first page of results
- **WHEN** a client requests a page number beyond the total number of available pages
- **THEN** the system returns an empty result page with consistent pagination controls without throwing an exception

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
- **THEN** the system resolves only mailboxes owned by User A and executes the query strictly constrained to User A's mailbox IDs
- **AND** User A sees only emails destined for mailboxes owned by User A, and zero emails, subjects, senders, or IDs belonging to User B are exposed in the HTML output, model, or database query results

#### Scenario: SQL Injection Prevention in Pagination Parameters (Security Case)
- **WHEN** a client submits a request to `/mails` with SQL injection payloads in the `page` or `size` query parameters (e.g. `page=1' OR '1'='1`, `size=10; DROP TABLE mail;--`)
- **THEN** Spring MVC type conversion rejects the non-numeric parameter and returns HTTP 400 Bad Request before invoking controller logic or database queries
- **AND** when a client passes arbitrary sort parameters attempting SQL injection (e.g. `sort=receiveTime; DROP TABLE mail`), the sort parameter is ignored because sorting is statically defined at compile time within the controller

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
The system SHALL provide an interactive, paginated email inspection view for individual mailboxes at `/mailboxes/{id}/mails`, listing received messages using lightweight metadata projections that exclude raw message BLOB payloads, providing HTMX pagination navigation, enforcing tenant authorization so users cannot inspect mailboxes belonging to other users, preventing SQL injection on path variables and query parameters, and loading message previews asynchronously into `#mail-viewer-pane` upon row selection, rendering UTF-8 plain text and sanitized HTML bodies accurately without mojibake regardless of whether Content-Type headers or charset parameters are present.

#### Scenario: Mailbox Email List Full Page Load (Happy Path)
- **WHEN** an authenticated user navigates to `/mailboxes/{id}/mails` for a mailbox they own
- **THEN** the system renders the paginated email list table, pagination controls, breadcrumbs, search/filter controls, and the `#mail-viewer-pane` placeholder

#### Scenario: Mailbox Email List Pagination Navigation (Happy Path)
- **WHEN** an authenticated user clicks a pagination control within `/mailboxes/{id}/mails`
- **THEN** HTMX requests `/mailboxes/{id}/mails` with parameters `page` and `size` and header `HX-Request: true`
- **AND** the system responds with only the updated `mailList` fragment markup containing the requested page of emails and refreshed pagination controls without reloading the entire page

#### Scenario: Mailbox Email Row Selection and Preview Loading (Happy Path)
- **WHEN** an authenticated user clicks an email row in the mailbox email list
- **THEN** HTMX issues an asynchronous GET request with `HX-Request: true` to `/mailboxes/{id}/mails/{mailId}`
- **AND** the system responds with the email preview fragment (`mailboxes/fragments/mail-detail :: detail`)
- **AND** HTMX swaps the preview markup into `#mail-viewer-pane` without reloading the page

#### Scenario: Email Preview Rendering with Untyped UTF-8 Body
- **WHEN** an authenticated user loads the preview of an email whose plain text or HTML body was transmitted as raw UTF-8 without a Content-Type header or without an explicit charset parameter
- **THEN** the plain text and HTML preview panes display the text accurately with correct Unicode characters and without mojibake (e.g. displaying `üüü` rather than `Ã¼Ã¼Ã¼`)

#### Scenario: Empty Mailbox State (Edge Case)
- **WHEN** an authenticated user navigates to `/mailboxes/{id}/mails` for a mailbox containing zero received messages
- **THEN** the system renders an empty state notice indicating no messages exist for this mailbox, preserving table headers and action controls

#### Scenario: Out of Range Page Index Handling for Mailbox (Edge Case)
- **WHEN** a client requests `/mailboxes/{id}/mails` with a page number below 0 or beyond the total available pages
- **THEN** negative page numbers are normalized to 0, and out-of-range page requests return an empty page with valid pagination controls without throwing server exceptions

#### Scenario: Unauthorized Access to Other Users Mailbox (Security Case)
- **WHEN** an authenticated user attempts to access `/mailboxes/{id}/mails` for a mailbox owned by another user
- **THEN** the system rejects the request with HTTP 403 Forbidden, preserving tenant data isolation and preventing inspection of other users' emails

#### Scenario: Non-Existent Mailbox Access (Security Case)
- **WHEN** an authenticated user attempts to access `/mailboxes/{id}/mails` for a non-existent mailbox ID
- **THEN** the system rejects the request with HTTP 404 Not Found

#### Scenario: SQL Injection Prevention in Mailbox Endpoint (Security Case)
- **WHEN** a client submits a request to `/mailboxes/{id}/mails` with SQL injection payloads in the `id` path variable (e.g. `/mailboxes/1' OR '1'='1/mails`)
- **THEN** Spring MVC type conversion rejects the non-numeric path variable with HTTP 400 Bad Request before any database query is formulated

### Requirement: Corporate Footer and Brand Notice
The system SHALL display a standardized corporate footer on all application views containing a yearless copyright notice and platform metadata, preventing recurring annual copyright maintenance churn.

#### Scenario: Standardized Yearless Copyright Display
- **WHEN** any web application view is rendered using the base layout
- **THEN** the footer renders a yearless copyright notice referencing Xceptance Software Technologies GmbH without hardcoded year ranges
- **AND** the technology stack summary line is rendered accurately underneath

