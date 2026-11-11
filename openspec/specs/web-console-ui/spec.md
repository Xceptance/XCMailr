# Web Console UI Specification

## Purpose

Provides a responsive, server-driven web management console using Thymeleaf and HTMX for self-service mailbox operations, email inspection, and system administration.

## Requirements

### Requirement: User Authentication and Self-Service
The system SHALL provide web views for user registration, email verification, session login, password reset, and profile management including language preference updates and self-service account deletion, handling both success and error conditions gracefully and ensuring that profile language updates take immediate effect across subsequent views and asynchronous HTMX requests without requiring re-authentication.

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

#### Scenario: Server-Side Registration Validation for Blank Forename or Surname (Error Case)
- **WHEN** an inbound registration request is submitted with a blank or whitespace-only forename or surname, bypassing client UI attributes
- **THEN** the server-side validator rejects the registration immediately via form binding validation, displays localized field-specific error messages, and avoids database persistence and uncaught server errors

#### Scenario: Server-Side Registration Validation for Malformed Email (Error Case)
- **WHEN** an inbound registration request is submitted with a malformed email address (e.g. `invalid-email`, missing domain, or illegal characters), bypassing client UI attributes
- **THEN** the server-side validator rejects the registration via email format validation, re-renders the registration view with a localized error message, and avoids database persistence

#### Scenario: Server-Side Registration Validation for Field Length Limits (Boundary / Error Case)
- **WHEN** an inbound registration request contains forename, surname, or email fields exceeding 255 characters
- **THEN** the server-side validator rejects the registration with a size violation error, preventing database column truncation or unhandled database exceptions

#### Scenario: Server-Side Registration Password Policy Enforcement (OWASP Error Case)
- **WHEN** an inbound registration request contains a password that violates OWASP rules (fewer than 10 characters, exceeding 72 characters or 72 UTF-8 bytes, consisting of a single repeated character, or containing the user's name or email address)
- **THEN** the server-side validator rejects the registration with the specific localized password policy error message without hashing or persisting credentials

#### Scenario: Server-Side Profile Update Validation for Blank Names (Error Case)
- **WHEN** an authenticated user submits a profile update request with blank or whitespace-only forename or surname
- **THEN** the server-side validator rejects the update, retains current database state, and re-renders the profile view with localized validation error feedback without throwing an HTTP 500 error

#### Scenario: Server-Side Profile Update Happy Path
- **WHEN** an authenticated user submits valid non-blank forename, surname, and language preference
- **THEN** the server persists the updated details, synchronizes the in-memory session principal, and redirects with a success confirmation parameter

#### Scenario: Server-Side Profile Update Field Length Limits (Boundary / Error Case)
- **WHEN** an authenticated user submits a profile update with forename or surname exceeding 255 characters
- **THEN** the server-side validator rejects the update with a size validation error without modifying persisted data

#### Scenario: Server-Side Form Input Sanitization and XSS Prevention (Special Case)
- **WHEN** a user submits registration or profile fields containing HTML or script tags (e.g. `<script>alert('xss')</script>`)
- **THEN** the system strictly escapes all user input values upon re-rendering validation errors, preventing Cross-Site Scripting (XSS)

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

#### Scenario: Profile Language Update Takes Immediate Effect Across Views (Happy Path)
- **WHEN** an authenticated user updates their profile language preference (e.g. from German `de` to English `en` or vice versa) and submits the profile form
- **THEN** the system persists the change to the database, updates the active session principal, and immediately resolves the new language for subsequent full-page and asynchronous HTMX requests (such as mailbox pagination summaries) without requiring re-authentication

#### Scenario: Dynamic Locale Resolution Prioritizes Persisted User Preference Over Login Snapshot (Happy Path)
- **WHEN** an authenticated user accesses localized views or partial fragments
- **THEN** the locale resolver queries the persisted user record to obtain the current language preference, prioritizing it over any static login-time authentication snapshot
- **AND** falls back to the in-memory principal or default request locale only if database lookup is not possible

#### Scenario: Profile Update Preserves In-Memory Session Principal Consistency (Happy Path)
- **WHEN** an authenticated user updates their profile (forename, surname, language preference)
- **THEN** the system updates the in-memory user entity within the active session's authentication principal to match the newly persisted values

#### Scenario: Locale Resolution Fallback on Malformed or Unsupported Language Preference (Error / Fallback Case)
- **WHEN** an authenticated user's persisted language preference is null, blank, malformed, or an unsupported language code
- **THEN** the locale resolver safely falls back to standard default English localization without throwing exceptions or generating invalid HTTP response headers

#### Scenario: Profile Language Update Validation Failure Preserves Existing Locale (Error Case)
- **WHEN** a profile update request contains invalid input fields or validation errors
- **THEN** the profile changes are rejected, the database and session principal remain unaltered, and the error response renders in the user's previously active locale

#### Scenario: OWASP Access Control and Tenant Isolation in Locale Resolution (Security Case)
- **WHEN** an authenticated request triggers locale resolution
- **THEN** the system resolves user identity strictly from the cryptographically verified `Authentication.getName()` security principal and never from untrusted client-supplied headers or request parameters, enforcing strict tenant isolation and preventing identity spoofing

#### Scenario: Privilege Escalation Prevention During Session Principal Synchronization (Security Case)
- **WHEN** the system synchronizes the in-session `UserPrincipal` upon profile update
- **THEN** only non-security profile fields (`forename`, `surname`, `language`) are copied, while roles, authorities, admin privileges, password hashes, and account activation states remain strictly immutable and protected against tampering

### Requirement: Dynamic Mailbox Dashboard
The system SHALL render an interactive mailbox dashboard supporting search, inline editing, modal dialogs, and bulk actions via HTMX without full page reloads, ensuring modal hosts are placed outside layout stacking contexts to prevent backdrop occlusion, while adapting table columns and actions responsively across viewport sizes with a calm corporate palette, presenting mailbox active duration selection via responsive segmented radio button pills, honoring forwarding preferences on creation, preserving duration and forwarding selections across validation error re-renders, rendering timestamps localized to the client browser's timezone, defaulting table order to creation order (`id DESC`) so newly created mailboxes appear at the top, providing full-dataset server-side column sorting with case-insensitive natural alphabetical ordering for email addresses, displaying clear modern sortable indicator chevrons that adapt cleanly to mobile viewports, treating permanent unlimited mailboxes as having infinite lifespan ($+\infty$) in expiration sorting, offering extended status filter options (`ALL`, `ACTIVE`, `EXPIRED`, `EXPIRING`, `UNLIMITED`), providing uniform vertical alignment across all table header elements, presenting a streamlined 7-column layout without redundant calculated mail counts, displaying a localized item range pagination summary (e.g., `Showing 1–15 of 17 mailboxes` or `Zeige 1–15 von 17 Postfächern`) matching the authenticated user's preferred language, offering direct 1-click validity extension buttons (`+1d`, `+7d`, `+30d`, `∞`) in table row quick actions, and providing an expiration extension selector with presets (`Keep current`, `+1d`, `+7d`, `+30d`, `Unlimited`) within the Edit Mailbox modal dialog.

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

#### Scenario: Edit Mailbox Modal Expiration Extension Segmented Controls (Happy Path)
- **WHEN** the user opens the Edit Mailbox modal for an existing mailbox
- **THEN** the modal displays the current localized expiration timestamp (or "Unlimited") and renders an "Extend Validity By" segmented radio button pill group with presets: `Keep current` (`extendHours = 0`, checked by default), `+1d` (`24`), `+7d` (`168`), `+30d` (`720`), and `Unlimited` (`-1`)
- **AND** the section label uses localized key `mailbox.modal.edit.extendBy`

#### Scenario: Edit Mailbox Modal Extension Submission (Happy Path)
- **WHEN** the user edits a mailbox, selects `+7d` (`extendHours = 168`), and submits the modal form
- **THEN** the system extends the mailbox's expiration timestamp by 168 hours calculated from the current expiration (or from `now` if already expired), clears any expired state (`expired = false`), saves the record, and triggers an HTMX table refresh reflecting the updated expiration

#### Scenario: Edit Mailbox Modal Set to Unlimited (Happy Path)
- **WHEN** the user edits a mailbox, selects `Unlimited` (`extendHours = -1`), and submits the modal form
- **THEN** the system sets `ts_Active = 0` (permanent) and `expired = false`, saves the record, and triggers an HTMX table refresh displaying "Unlimited" for expiration

#### Scenario: Edit Mailbox Modal Keep Current Expiration (Happy Path)
- **WHEN** the user edits a mailbox, keeps `Keep current` (`extendHours = 0`), and submits the modal form
- **THEN** the mailbox's existing `ts_Active` timestamp remains unchanged while other edited attributes (`comment`, `forwardEmails`, `active`) are updated

#### Scenario: Table Row Quick Action Direct Validity Extension (Happy Path)
- **WHEN** an authenticated user clicks any of the 1-click extension buttons (`+1d`, `+7d`, `+30d`) on a mailbox table row
- **THEN** HTMX sends an asynchronous `POST /mailboxes/{id}/extend?hours={hours}` request
- **AND** the system extends the expiration timestamp by the specified hours, marks the mailbox as not expired (`expired = false`), and refreshes the table row via HTMX trigger without full page reload

#### Scenario: Table Row Quick Action Direct Unlimited Validity (Happy Path)
- **WHEN** an authenticated user clicks the `∞` (unlimited) action button on a mailbox table row
- **THEN** HTMX sends an asynchronous `POST /mailboxes/{id}/extend?hours=-1` request
- **AND** the system sets `ts_Active = 0` and `expired = false`, persisting the mailbox as permanent and refreshing the table row displaying "Unlimited"

#### Scenario: Quick Action Validity Extension on Expired Mailbox (Edge Case)
- **WHEN** an authenticated user clicks a quick extension button (`+1d`, `+7d`, `+30d`) on an already expired mailbox (`expired = true` or `ts_Active < now`)
- **THEN** the new expiration timestamp is calculated from the current wall-clock time (`now + hours`) rather than the past expiration timestamp, and `expired` is reset to `false`

#### Scenario: Quick Action Validity Extension on Active Mailbox (Edge Case)
- **WHEN** an authenticated user clicks a quick extension button (`+1d`, `+7d`, `+30d`) on an active mailbox with a future expiration date
- **THEN** the new expiration timestamp is calculated by adding the specified hours to the existing future expiration timestamp (`mailbox.ts_Active + hours`), preserving previously granted validity

#### Scenario: Cross-Tenant Mailbox Extension Security Isolation (Security / Error Case)
- **WHEN** an authenticated user attempts to extend or edit a mailbox ID belonging to another user via `POST /mailboxes/{id}/extend` or `POST /mailboxes/{id}/edit`
- **THEN** the system rejects the operation with HTTP 404 / access denied, leaving the targeted mailbox completely unmodified

#### Scenario: Extension on Non-Existent Mailbox (Error Case)
- **WHEN** an authenticated user submits an extension request for a non-existent mailbox ID
- **THEN** the system responds with HTTP 404 without throwing uncaught server exceptions

#### Scenario: Mailbox Deletion UI Refresh
- **WHEN** the user confirms deletion of a mailbox from the mailbox actions menu
- **THEN** the mailbox and associated emails are deleted on the server, and the mailbox table automatically refreshes via HTMX without blanking out the page or removing the table container element from the DOM, displaying remaining mailboxes or the empty state if none remain

#### Scenario: Inline Row Action UI Refresh
- **WHEN** the user triggers an inline or dropdown action on a mailbox row (such as the compact status toggle icon button, direct extension buttons, or counter reset from the overflow menu)
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

#### Scenario: Localized Item Range Display for Multi-Page Datasets (Happy Path)
- **WHEN** an authenticated user views a mailbox table containing items across multiple pages (e.g., 17 items with page size 15)
- **THEN** the table footer renders an item range indicator indicating the starting item, ending item, and total count (e.g., `Showing 1–15 of 17 mailboxes` on page 1, and `Showing 16–17 of 17 mailboxes` on page 2)
- **AND** the summary renders alongside the pagination controls and export utility links

#### Scenario: Localized Item Range Display for Single-Page Datasets (Happy Path)
- **WHEN** an authenticated user views a mailbox table where all items fit on a single page (e.g., 3 items with page size 15)
- **THEN** the footer displays `Showing 1–3 of 3 mailboxes`

#### Scenario: Singular Phrasing for Single-Item Mailbox Dataset (Special Case)
- **WHEN** an authenticated user views a mailbox table containing exactly one mailbox
- **THEN** the footer displays singular phrasing `Showing 1 of 1 mailbox` rather than plural range phrasing

#### Scenario: Localized Item Range Display with German User Language Preference (Happy Path)
- **WHEN** an authenticated user whose account profile specifies German (`language = 'de'`) views the mailbox table or performs pagination/filter swaps via HTMX
- **THEN** the system resolves German localization and displays the item range in German (e.g., `Zeige 1–15 von 17 Postfächern` or `Zeige 1 von 1 Postfach`)

#### Scenario: Locale Resolution Fallback for Unknown or Missing Language Preference (Error / Fallback Case)
- **WHEN** an authenticated user whose account profile contains a null, empty, or unsupported language code (e.g., `language = null`, `language = ""`, or `language = "fr"`) accesses the mailbox table
- **THEN** the system safely falls back to standard English localization and renders `Showing 1–15 of 17 mailboxes` without throwing exceptions

#### Scenario: Locale Resolution for Unauthenticated Request (Special Case)
- **WHEN** an unauthenticated or anonymous request accesses a localized view or partial fragment
- **THEN** the locale resolver delegates to standard header-based locale resolution using the HTTP `Accept-Language` header, defaulting to English if the header is absent or unrecognized

#### Scenario: Localized Range Indicator Preservation During HTMX Table Refreshes (Special Case)
- **WHEN** an authenticated user changes pages, updates a status filter, or executes an inline/bulk mailbox action that triggers an HTMX partial table refresh
- **THEN** the refreshed mailbox table fragment retains the correct localized range indicator matching the user's active language and current page slice

#### Scenario: Localized Item Range Display with Empty Mailbox Dataset (Edge Case)
- **WHEN** an authenticated user views the mailbox table with zero matching mailboxes
- **THEN** the table renders the empty state view without displaying active pagination ranges

#### Scenario: Responsive Mobile Table Viewport
- **WHEN** a user views the mailbox table on a mobile viewport narrower than 576px
- **THEN** the table displays the sortable `Address` header with an inline, non-wrapping chevron indicator (`bi-chevron-expand` or active direction chevron)
- **AND** secondary columns (`Status`, `Expires`, `Forwards`, `Suppressed`) remain hidden from table headers and display their essential status, expiration, and forwarding info within the address row subline
- **AND** the header link retains comfortable tap targets without overflowing the mobile viewport or causing horizontal layout breakage

#### Scenario: Responsive Action Menu Consolidation
- **WHEN** a user interacts with a mailbox row on a mobile viewport narrower than 768px
- **THEN** standalone compact quick action buttons (toggle and extension presets) are collapsed into the `[...]` kebab dropdown menu, presenting a single compact action button per row with all actions (including `+1d`, `+7d`, `+30d`, and `Set Unlimited`) accessible within the dropdown

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

#### Scenario: Mailbox Row Action Binding Integrity Across Sort Orders (Special Case / Issue #83)
- **WHEN** an authenticated user sorts the mailbox table by any column in ascending or descending direction
- **THEN** each rendered table row binds its inline and modal management action endpoints (`edit`, `delete`, `toggle`, `extend`, `reset-counters`) strictly to the primary key (`id`) of the mailbox displayed in that specific row
- **AND** triggering any management action on a row operates exclusively on the mailbox rendered in that row, regardless of original insertion order or display sort order, preventing actions from targeting the wrong mailbox due to view sorting desynchronization

### Requirement: Admin Management Console
The system SHALL provide administrative interfaces for managing user accounts, reviewing transaction logs, analyzing traffic metrics, and maintaining the domain whitelist, offering server-side column sorting with bidirectional visual chevrons across user management table controls, presenting structured time-series email traffic activity views over the past 24 hours (hourly line chart) and past 7 days (daily stacked bar chart) covering forwarded, stored, and dropped categories with zero-filled continuity, dynamic theme synchronization, robust DOM-ready lifecycle execution, and graceful fallback handling, organizing activity into distinct sections with modern monochrome badges and standard icons, displaying top metric cards with accurate values and granular breakdowns for administrators/users and active/expired mailboxes, presenting an aggregate transaction delivery status badge distribution, displaying 24-hour and 7-day sender domain traffic breakdown tables backed by aggregated domain metrics from `MAIL_STATISTICS` with daily date and weekday breakdowns in the 7-day view and client-side case-insensitive column sorting with directional visual chevrons across both tables, and enforcing consistent code hygiene with `final` modifiers, Javadoc, and comprehensive test pyramid coverage.

#### Scenario: Domain Whitelist Configuration
- **WHEN** an administrator adds or removes an allowed domain via the admin console
- **THEN** the system updates the registered domain table and enforces the change on the inbound SMTP listener immediately

#### Scenario: Paged Transaction Auditing
- **WHEN** an administrator inspects the mail transaction log
- **THEN** the system displays paginated records with filtering by timestamp, status, and address, displaying distinct badges for Forwarded (300), Stored (250), Inactive (200), Unknown (100), and Error statuses

#### Scenario: Default Domain Whitelist Seeding on Startup
- **WHEN** the application starts up against a fresh or migrated legacy database
- **THEN** the system ensures the administrator's default domain is present in the registered domain whitelist if absent, ensuring that mailbox creation domain options are available

#### Scenario: User Table Column Sorting by Email (Happy Path)
- **WHEN** an administrator sorts the user table by email in ascending or descending direction
- **THEN** the server orders user accounts using case-insensitive natural ordering (`ignoreCase()`) on the `mail` attribute in the requested direction
- **AND** the table header renders an active directional chevron (`bi-chevron-up` for ASC, `bi-chevron-down` for DESC) with localized tooltip feedback

#### Scenario: User Table Column Sorting by Name (Happy Path)
- **WHEN** an administrator sorts the user table by name in ascending or descending direction
- **THEN** the server orders user accounts primarily by `surname` and secondarily by `forename` in the requested direction using case-insensitive ordering (`ignoreCase()`)
- **AND** applies `mail ASC` as a secondary tiebreaker to guarantee deterministic ordering across paginated views

#### Scenario: User Table Column Sorting by Status and Role (Happy Path)
- **WHEN** an administrator sorts the user table by status (`active`) in ascending direction (`dir=asc`)
- **THEN** the server orders active user accounts (`active = true`) before inactive accounts (`active = false`), matching the mailbox status ordering convention, and orders inactive accounts before active accounts on descending direction (`dir=desc`)
- **WHEN** an administrator sorts the user table by role (`admin`) in ascending direction (`dir=asc`)
- **THEN** the server orders administrator accounts (`admin = true`) before standard user accounts (`admin = false`), adhering to alphabetical and privilege hierarchy expectations, and orders standard user accounts before administrator accounts on descending direction (`dir=desc`)
- **AND** applies `mail ASC` case-insensitively as a secondary tiebreaker for deterministic ordering

#### Scenario: User Table Parameter Preservation Across Pagination (Happy Path)
- **WHEN** an administrator navigates between pages on a sorted user table
- **THEN** each pagination link includes the active `sort` and `dir` parameters, and the subsequent page preserves the sort order and directional chevron state

#### Scenario: User Table Parameter Preservation Across Display Count Changes (Happy Path)
- **WHEN** an administrator selects a different page size (5, 15, 25, 50, or all) on a sorted user table
- **THEN** the display count selector passes the active `sort` and `dir` parameters, resets the view to page 0, and maintains the active sort state

#### Scenario: User Table Combined Search and Sorting (Happy Path)
- **WHEN** an administrator enters a search filter while the table is sorted by a specific column and direction
- **THEN** the server filters users matching the search query across email, forename, or surname and orders the filtered results according to the active column sort

#### Scenario: Unknown or Invalid Sort Column Fallback (Error Case)
- **WHEN** an inbound request provides a sort column parameter that is not in the allowed whitelist (`email`, `mail`, `name`, `status`, `role`), such as a non-existent column name or random text
- **THEN** the server safely normalizes the sort column to the default (`email`), applies default ascending direction, and renders the table without throwing an exception

#### Scenario: Invalid Sort Direction Fallback (Error Case)
- **WHEN** an inbound request provides an invalid direction string other than `asc` (e.g., `invalid`, `none`, or malformed input)
- **THEN** the server safely normalizes the direction to `desc` (or `asc` for the default `email` view), preventing query syntax errors

#### Scenario: SQL Injection Attempt in Sort Parameters (Error Case)
- **WHEN** an attacker submits SQL injection payloads in `sort` or `dir` parameters (e.g., `' OR 1=1--`, `email; DROP TABLE users;`)
- **THEN** the server's strict whitelist parser rejects the unsanitized input, normalizes the query to `mail ASC`, returns HTTP 200 with safely ordered data, and logs no SQL syntax exceptions

#### Scenario: Case-Insensitive Parameter Normalization (Special Case)
- **WHEN** an inbound request provides sort or direction parameters with mixed casing (e.g., `sort=NAME`, `sort=StAtUs`, `dir=ASC`, `dir=Desc`)
- **THEN** the server normalizes the parameters to lowercase and matches the corresponding whitelist property correctly

#### Scenario: Duplicate Value Deterministic Ordering (Special Case)
- **WHEN** multiple user records share identical values for the primary sort column (e.g., multiple inactive users, multiple standard users, or users with the same surname)
- **THEN** the secondary tiebreaker `mail ASC` ensures an identical, deterministic order across consecutive requests and page boundary transitions

#### Scenario: Empty Search Results Sorting Headers (Special Case)
- **WHEN** an administrator searches for a query string that yields zero matching user accounts
- **THEN** the table displays the localized empty state message while preserving sortable header links with their chevron indicators intact

#### Scenario: Top Quick Stats Cards and Granular Breakdowns (Happy Path)
- **WHEN** an administrator views the statistics page (`/admin/statistics`)
- **THEN** the system displays top metric cards for Registered Users, Active Mailboxes, Stored Messages, and Logged Transactions
- **AND** the Registered Users card displays the primary total count alongside a subtitle distinguishing administrator accounts from standard user accounts
- **AND** the Active Mailboxes card displays the accurate count of non-expired mailboxes (`expired = false`) as the primary value alongside a subtitle reporting total and expired mailbox counts
- **AND** the Stored Messages and Logged Transactions cards report accurate system-wide totals

#### Scenario: Transaction Delivery Status Overview Badges (Happy Path)
- **WHEN** an administrator views the statistics page (`/admin/statistics`)
- **THEN** the system displays an aggregate transaction status breakdown badge row rendering formatted counts for Stored (250), Forwarded (300), Inactive Mailbox (200), Unknown Mailbox (100), and Relay or Error status codes using existing localized message keys

#### Scenario: Section Visual Hierarchy and Modern Monochrome Badges (Happy Path)
- **WHEN** an administrator views the statistics page (`/admin/statistics`)
- **THEN** the page organizes activity into distinct sections for 24-Hour Activity and 7-Day Activity separated by clean dividers (`border-top`)
- **AND** each section header features a standard icon (`bi-clock-history` for 24 hours, `bi-calendar-week` for 7 days), section title, subtle monochrome pill badge (`bg-body-secondary text-secondary-emphasis border`), and resolution description without colorful or flashy accents

#### Scenario: Last 24 Hours Email Activity Chart (Happy Path)
- **WHEN** an administrator views the statistics page (`/admin/statistics`)
- **THEN** the system renders a line chart (`#chart-last-24h`) displaying email activity over the preceding 24 hours
- **AND** the chart displays three distinct datasets: forwarded email counts (Status 300), stored email counts (Status 250), and dropped email counts (all other statuses)
- **AND** the horizontal axis represents 24 continuous hourly buckets formatted with localized hour labels ending at the current hour
- **AND** the card header displays the total volume of emails processed during the 24-hour window

#### Scenario: Last 7 Days Email Activity Chart (Happy Path)
- **WHEN** an administrator views the statistics page (`/admin/statistics`)
- **THEN** the system renders a daily stacked bar chart (`#chart-last-7d`) displaying email activity over the preceding 7 days
- **AND** each daily bar is segmented into stacked bars for stored (blue), forwarded (green), and dropped (red) messages without overlapping zero-lines
- **AND** inactive days render with no bars rather than drawing baseline zero-lines
- **AND** the card header displays the total volume of emails processed during the 7-day window

#### Scenario: Chart Theme and Responsiveness (Happy Path)
- **WHEN** an administrator views or toggles the application theme between light and dark modes
- **THEN** the charts adapt responsively across viewport widths inside full-width card containers
- **AND** grid line colors, axis border lines (`border.color`), axis ticks, and tooltip backgrounds dynamically synchronize with active theme tokens, conforming to Chart.js 4 specifications without console deprecation warnings

#### Scenario: Chart Client Lifecycle and DOM Readiness (Happy Path)
- **WHEN** an administrator navigates to the statistics page
- **THEN** chart instantiation executes after DOM content is ready and element bounding dimensions are resolved
- **AND** the chart canvases render with valid dimensions without execution race conditions

#### Scenario: Chart.js Library Unavailable or Blocked Fallback (Degraded / Boundary Case)
- **WHEN** the Chart.js library cannot be loaded or executed (e.g. script blocked or network failure)
- **THEN** the statistics view displays an accessible fallback notification within the chart containers rather than an empty blank void

#### Scenario: Empty Email Traffic Zero-Fill Continuity (Edge Case)
- **WHEN** no email forwarding, storing, or drop transactions occurred within specific hours or days of the queried time windows
- **THEN** the backend data model provides continuous time series buckets with count values set to `0` for inactive intervals
- **AND** the 24-hour line chart renders an unbroken zero baseline while the 7-day bar chart cleanly displays days with zero bar height

#### Scenario: Localization of Chart Metrics (Happy Path)
- **WHEN** an administrator views the statistics page in English or German
- **THEN** chart card titles, dataset legend labels (`Forwarded` / `Weitergeleitet`, `Stored` / `Gespeichert`, `Dropped` / `Verworfen`), and axis labels render using message bundles corresponding to the user's active locale

#### Scenario: Todays Sender Domains Breakdown Table (Happy Path)
- **WHEN** an administrator views the statistics page (`/admin/statistics`)
- **THEN** beneath the 24-hour activity chart, the system displays a card containing a table of sender domains active today (last 24 hours)
- **AND** the table lists each sender domain alongside counts for stored, forwarded, and dropped emails, ordered by total email volume descending

#### Scenario: Last 7 Days Sender Domains Breakdown Table (Happy Path)
- **WHEN** an administrator views the statistics page (`/admin/statistics`)
- **THEN** beneath the 7-day activity chart, the system displays a card containing a table of sender domains active over the last 7 days grouped by calendar date and sender domain
- **AND** the table provides a `Datum / Tag` (Date / Day) column displaying the localized date and weekday (e.g., `Mi, 11.11.2026`) formatted according to the user's active locale
- **AND** each row carries an ISO date sort attribute (`data-sort-value="yyyy-MM-dd"`) ensuring deterministic chronological sorting
- **AND** the table is ordered initially by date descending and total email volume descending

#### Scenario: Sender Domain Client-Side Column Sorting (Happy Path)
- **WHEN** an administrator clicks a column header in either the 24-hour or 7-day sender domain breakdown table
- **THEN** the client-side table sorter orders the table rows immediately without a page reload or chart re-render
- **AND** the clicked column header toggles its directional indicator icon (`bi-chevron-down` for descending, `bi-chevron-up` for ascending) while non-active sortable headers display a neutral chevron (`bi-chevron-expand`)

#### Scenario: Case-Insensitive Sender Domain Column Sorting (Happy Path)
- **WHEN** an administrator sorts a sender domain table by the `Absenderdomain` (Sender Domain) column in ascending or descending direction
- **THEN** the client-side sorter orders domain entries alphabetically using case-insensitive natural comparison (`toLowerCase()` / `localeCompare`), consistent with mailbox address and user email sorting conventions

#### Scenario: Chronological Date Column Sorting (Happy Path)
- **WHEN** an administrator sorts the 7-day sender domain table by the `Datum / Tag` column
- **THEN** the client-side sorter orders rows chronologically based on the underlying ISO date value (`data-sort-value`), supporting both newest-first (descending) and oldest-first (ascending) sequences

#### Scenario: Numeric Metric Column Sorting (Happy Path)
- **WHEN** an administrator sorts either sender domain table by any metric count column (`Verworfen`, `Weitergeleitet`, `Gespeichert`, or `Gesamt`)
- **THEN** the client-side sorter orders rows numerically based on parsed integer values rather than string lexicographical order (e.g., `100` ranks above `20` in descending order)

#### Scenario: Sender Domain Metrics Persistence Across Transaction Pruning
- **WHEN** detailed transaction records in `mailtransactions` are deleted or purged according to retention limits (`maxAgeHours`)
- **THEN** the sender domain breakdown tables continue to display historical domain counts from `MAIL_STATISTICS` for the full 7-day and 30-day reporting windows

#### Scenario: Empty Sender Domains Graceful State
- **WHEN** no inbound email traffic has been recorded for the queried time window
- **THEN** the sender domain tables render a localized empty state message indicating that no records were found spanning all columns (5 columns for 24-hour table, 6 columns for 7-day table)

#### Scenario: Web Console Code Quality Standards
- **WHEN** classes and controllers supporting the statistics dashboard are developed or inspected
- **THEN** all method parameters, local variables, and controller fields declare `final`
- **AND** all public methods and classes provide complete Javadoc and descriptive code comments

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
The system SHALL display a standardized corporate footer on all application views containing the corporate copyright notice with inception-to-current year range and application version, while strictly omitting internal technology stack and framework metadata.

#### Scenario: Standardized Yearless Copyright Display
- **WHEN** any web application view is rendered using the base layout
- **THEN** the footer renders a copyright notice with the inception-to-current year range referencing Xceptance Software Technologies GmbH (e.g. `&copy; 2013-2026 Xceptance Software Technologies GmbH`)
- **AND** the footer renders the application version label and version number (e.g. `Version: 4.0.0`)
- **AND** internal technology stack details (including runtime, framework, and library versions such as Java, Spring Boot, or HTMX) are omitted from the footer display

### Requirement: Comprehensive Web Console Internationalization
The system SHALL render all user-facing web console views, layouts, navigation menus, modals, form controls, placeholders, tooltips, ARIA accessibility landmarks, status banners, validation error messages, table headers, and error pages dynamically using message bundles corresponding to the active user's locale (`en` or `de`), falling back to default English when a key is absent, while strictly escaping localized text to prevent Cross-Site Scripting (XSS).

#### Scenario: Layout and Navigation Localized Rendering (Happy Path)
- **WHEN** an authenticated user with German (`de`) language preference navigates to any application view
- **THEN** the header navigation links, offcanvas drawer titles and navigation items, user dropdown menu, theme switcher tooltips, and footer notices render in German
- **AND** when the user switches their language preference to English (`en`), all navigation and layout components render in English

#### Scenario: Authentication and Onboarding Localized Rendering (Happy Path)
- **WHEN** an unauthenticated visitor accesses `/login`, `/register`, `/forgot-password`, or `/reset-password` with a German request locale (`Accept-Language: de`)
- **THEN** form titles, field labels, input placeholders (including forename, surname, target email, password min-length guidance, and password confirmation), password requirement rules, submit buttons, and status/redirect feedback banners render in German

#### Scenario: User Profile and Account Management Localized Rendering (Happy Path)
- **WHEN** an authenticated user with German language preference navigates to `/profile`
- **THEN** personal information headers and labels, password change guidance and input placeholders (such as `"Mindestens 10 Zeichen"` and `"Neues Passwort wiederholen"`), API token descriptions, creation date fallbacks (`"Kürzlich"`), action buttons, danger zone warnings, and the account deletion confirmation modal render in German

#### Scenario: Server-Side Form Validation Error Localization (Error Case)
- **WHEN** a user submits an authentication or profile form with validation errors (such as password mismatch, incorrect current password, duplicate registration email, or sole administrator account deletion attempt) under German locale
- **THEN** the system passes the corresponding message key (`errorMessageKey`) to the view model or flash scope, and the template dynamically renders the localized error banner in German matching the user's active locale, falling back to `errorMessage` if provided
- **AND** the handling controller action methods do not declare `Locale` parameters or perform Java-side message translation

#### Scenario: Password Quality Validation Error Localization (Error Case)
- **WHEN** a user submits a candidate password failing OWASP/NIST validation rules (such as minimum length violation, maximum length violation, single repeated character, or identity term inclusion) under German locale
- **THEN** the system passes the validation result's message key to the view model, and the template dynamically renders the localized error banner in German matching the specific rule violated
- **AND** the handling controller action methods do not declare `Locale` parameters or perform Java-side message translation

#### Scenario: Accessibility and Tooltip Localization (Special Case)
- **WHEN** an authenticated or unauthenticated user navigates web views containing input pattern validation tooltips or screen reader landmarks under German locale
- **THEN** input pattern titles (`th:title`) and navigation/toolbar landmarks (`th:aria-label` on pagination and batch action controls) render localized German descriptions

#### Scenario: Mailbox Dashboard and Modal Localized Rendering (Happy Path)
- **WHEN** an authenticated user with German language preference navigates to `/mailboxes`
- **THEN** the mailbox search placeholder, status filter dropdown options, table headers, sorting titles, status badges (`Aktiv`, `Abgelaufen`, `Unbegrenzt`), kebab action options, pagination buttons (`Zurück`, `Weiter`), deletion confirmation prompts, and floating batch bar action buttons render in German

#### Scenario: Unified Inbox and Email Detail Localized Rendering (Happy Path)
- **WHEN** an authenticated user with German language preference navigates to `/mails` or `/mailboxes/{id}/mails`
- **THEN** breadcrumbs, inbox total counters, refresh buttons, content viewer tabs (`HTML`, `Klartext`, `Header`), download action buttons, and empty selection placeholders render in German

#### Scenario: Administration Views Localized Rendering (Happy Path)
- **WHEN** an authenticated administrator with German language preference accesses `/admin/users`, `/admin/domains`, `/admin/transactions`, or `/admin/statistics`
- **THEN** administrative page titles, search placeholders, action buttons (e.g. `Aktivieren`, `Deaktivieren`, `Zum Admin ernennen`), status overview pills, audit table headers, and log purge dialogs render in German

#### Scenario: Error Pages Localized Rendering (Happy Path)
- **WHEN** a client encountering an HTTP 404 or HTTP 500 error view has German language preference
- **THEN** the error title, explanation message, and dashboard navigation buttons render in German

#### Scenario: Translation Key Missing Fallback (Special / Edge Case)
- **WHEN** a template or controller attempts to resolve a translation key that is not present in `messages_de.properties`
- **THEN** the system falls back to the default message definition in `messages.properties` without throwing server exceptions or breaking template rendering

#### Scenario: OWASP Cross-Site Scripting Prevention in Localized Messages (Security Case)
- **WHEN** localized messages or message parameters containing HTML special characters (e.g., `<`, `>`, `&`, `"`) are rendered by Thymeleaf
- **THEN** standard XML/HTML escaping is applied automatically, preventing script injection or DOM manipulation attacks

### Requirement: Mailbox Comment Management and Dashboard Display
The system SHALL support an optional comment field of up to 255 characters on mailbox creation and editing in the web console, displaying non-blank comments as a subtitle beneath the address in the mailbox dashboard table and enabling case-insensitive search by comment substring.

#### Scenario: Mailbox Creation with Comment and Umlauts (Happy Path)
- **WHEN** an authenticated user enters an address prefix, selects a domain, enters an optional comment containing special characters or German umlauts (e.g. `Projekt Überweisung`), and submits the creation modal
- **THEN** the system saves the mailbox with the comment and renders the comment as a subtitle beneath the mailbox address link in the dashboard table

#### Scenario: Mailbox Creation without Comment (Happy Path)
- **WHEN** an authenticated user creates a mailbox leaving the comment field empty or blank
- **THEN** the system saves the mailbox with a null comment and renders the address row without a comment subtitle

#### Scenario: Edit Existing Mailbox to Add or Update Comment (Happy Path)
- **WHEN** an authenticated user opens the edit modal for an existing mailbox, updates or adds a comment, and submits the form
- **THEN** the system persists the updated comment and refreshes the mailbox row displaying the new comment subtitle

#### Scenario: Edit Existing Mailbox to Clear Comment (Happy Path)
- **WHEN** an authenticated user clears the comment field in the edit modal and submits the form
- **THEN** the system persists a null comment and refreshes the mailbox row with the comment subtitle removed

#### Scenario: Live Search by Comment Substring (Happy Path)
- **WHEN** an authenticated user enters a search term matching part of a mailbox's comment (such as `überweisung` or `checkout`) into the dashboard search bar
- **THEN** the system returns and displays all mailboxes whose comment contains the search substring, matched case-insensitively, alongside mailboxes matching by address or domain

#### Scenario: SQL Injection Prevention in Search Input (Special Case)
- **WHEN** an authenticated user enters potentially malicious SQL injection payloads into the search bar (such as `' OR '1'='1`, `'; DROP TABLE mailboxes; --`, or `UNION SELECT`)
- **THEN** the system binds the search query strictly as a parameterized literal value using JPA query parameters without dynamic string concatenation, safely treating the input as literal text without altering query structure or triggering SQL execution errors

#### Scenario: Comment Length Constraint Enforcement (Error Case)
- **WHEN** an authenticated user attempts to submit a comment exceeding 255 characters in the creation or edit modal
- **THEN** client-side and server-side validation reject the submission, displaying localized validation feedback without saving the invalid comment

### Requirement: Recipient Comment Visibility in Email Inboxes
The system SHALL display the recipient mailbox's comment in email inbox views, rendering a contextual comment indicator alongside recipient badges in the unified cross-mailbox inbox view (`/mails`) and displaying the comment in the single mailbox inbox header (`/mailboxes/{id}/mails`).

#### Scenario: Unified Inbox Displays Comment for Tagged Mailbox (Happy Path)
- **WHEN** an authenticated user views the unified inbox at `/mails` and received messages belong to a mailbox configured with a comment
- **THEN** each matching message row displays the mailbox comment badge or indicator alongside the recipient mailbox address badge in the Message & Recipient column

#### Scenario: Unified Inbox Omits Comment for Untagged Mailbox (Happy Path)
- **WHEN** an authenticated user views the unified inbox at `/mails` and received messages belong to a mailbox with no comment configured
- **THEN** the message row displays only the recipient address badge without an empty comment badge or placeholder

#### Scenario: Single Mailbox Inbox Displays Comment in Header (Happy Path)
- **WHEN** an authenticated user navigates to `/mailboxes/{id}/mails` for a mailbox configured with a comment
- **THEN** the view header renders the comment tag beneath or alongside the full mailbox address

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

### Requirement: Table Pagination Window with Ellipsis Truncation
The system SHALL render pagination navigation bars across all paginated web console tables (`/mailboxes`, `/admin/users`, `/admin/transactions`, `/mailboxes/{id}/mails`, `/mails`) with at most 7 page slots using unclickable ellipsis items (`...`) for omitted ranges when the total number of pages exceeds 7, flanked by Previous and Next navigation controls, keeping controls visible when data exists and disabling inactive links.

#### Scenario: Pagination Window When Total Pages Is Seven or Fewer (Happy Path)
- **WHEN** an authenticated user views a table where total pages $N \le 7$
- **THEN** the pagination bar displays all page numbers $1$ through $N$ sequentially without ellipsis items

#### Scenario: Sliding Pagination Window Near Start (Happy Path)
- **WHEN** an authenticated user views a table where total pages $N > 7$ and the current page $P \le 4$
- **THEN** the pagination bar renders exactly 7 page slots formatted as `[1] [2] [3] [4] [5] [...] [N]`

#### Scenario: Sliding Pagination Window Near End (Happy Path)
- **WHEN** an authenticated user views a table where total pages $N > 7$ and the current page $P \ge N - 3$
- **THEN** the pagination bar renders exactly 7 page slots formatted as `[1] [...] [N-4] [N-3] [N-2] [N-1] [N]`

#### Scenario: Sliding Pagination Window in Middle (Happy Path)
- **WHEN** an authenticated user views a table where total pages $N > 7$ and $4 < P < N - 3$
- **THEN** the pagination bar renders exactly 7 page slots formatted as `[1] [...] [P-1] [P] [P+1] [...] [N]`

#### Scenario: Ellipsis Item Inactivity and Accessibility (Special Case)
- **WHEN** an ellipsis item is rendered in the pagination bar
- **THEN** the element is rendered as a disabled list item (`page-item disabled`) with `aria-hidden="true"`, preventing keyboard focus and mouse clicks from triggering navigation

#### Scenario: Single-Page Navigation Controls State (Happy Path)
- **WHEN** a table contains data that fits entirely on a single page (`totalPages == 1` and `totalElements > 0`)
- **THEN** the pagination bar remains visible displaying page `1` as active
- **AND** the Previous and Next navigation links are rendered in a disabled state (`page-item disabled`)

#### Scenario: Empty Table Pagination Visibility (Edge Case)
- **WHEN** a table query matches zero items (`totalElements == 0`)
- **THEN** the pagination bar and Display Count selector are omitted from the empty state view

### Requirement: Table Display Count Page Size Selector
The system SHALL render a segmented Display Count button group styled symmetrically with the pagination bar on all paginated web console tables, providing preset sizes `5`, `15`, `25`, `50`, and `All`, highlighting the active page size with visual distinction using modern neutral monochrome styling tokens, resetting the view to page 0 upon size change, and preserving active sorting, direction, and search or filter parameters.

#### Scenario: Display Count Preset Selection (Happy Path)
- **WHEN** an authenticated user clicks a preset page size button (`5`, `15`, `25`, `50`, or `All`) on any paginated table
- **THEN** HTMX issues an asynchronous GET request with the selected `size` parameter and `page=0`
- **AND** the table refreshes displaying the corresponding number of items per page with the selected preset marked with the active style

#### Scenario: Display Count All Preset Handling (Happy Path)
- **WHEN** an authenticated user selects the `All` preset
- **THEN** the client transmits `size=0` (or `size=1000`)
- **AND** the server resolves the query without pagination limits up to the defensive maximum of 1000 items, displaying all records on page 0 with page count 1

#### Scenario: Display Count Preservation of Table State (Happy Path)
- **WHEN** an authenticated user changes the page size while an active search query, status filter, or column sort is applied
- **THEN** the resulting HTMX request and re-rendered table retain the active `search`, `status`, `sort`, and `dir` parameters

#### Scenario: Column Sort Preservation of Page Size (Happy Path)
- **WHEN** an authenticated user clicks a sortable column header while a non-default page size is active
- **THEN** the sorting request includes the current `size` parameter, ensuring page density is not reset to the default on column reordering

#### Scenario: Defensive Bounds Clamping for Page Size (Error / Boundary Case)
- **WHEN** an inbound request specifies a `size` parameter that is non-numeric, less than 1, or greater than 1000
- **THEN** the server defensively clamps the size within safe boundaries (between 1 and 1000) or falls back to the table default size without throwing server exceptions or causing database integer overflow

#### Scenario: SQL Injection Prevention in Page Size Parameter (Security Case)
- **WHEN** a malicious client supplies SQL injection payloads in the `size` query parameter (such as `size=15; DROP TABLE mailboxes;--` or `size=1' OR '1'='1`)
- **THEN** Spring MVC type conversion rejects the non-integer parameter with HTTP 400 Bad Request before controller execution or database query formulation
- **AND** no database error or SQL execution error is triggered

#### Scenario: Display Count Symmetrical Styling and Responsive Layout (Happy Path)
- **WHEN** a paginated table is rendered across desktop and mobile viewports
- **THEN** the pagination bar is positioned at the start (left) and the Display Count group is positioned at the end (right) using matching Bootstrap small pagination styling (`.pagination.pagination-sm mb-0`)
- **AND** active page and page-size items render with dark charcoal background (`#212529` in light mode, `#334155` in dark mode) and crisp white text, while inactive items render with neutral body text (`var(--bs-body-color)`), avoiding electric blue styling
- **AND** on mobile viewports narrower than 576px, the controls wrap cleanly without horizontal clipping or overflow

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


