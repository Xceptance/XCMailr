## MODIFIED Requirements

### Requirement: User Authentication and Self-Service
The system SHALL provide web views for user registration, email verification, session login, password reset (issuing reset tokens and dispatching emails exclusively for active accounts while preserving anti-enumeration responses for inactive or unknown addresses), and profile management including language preference updates and self-service account deletion, handling both success and error conditions gracefully and ensuring that profile language updates take immediate effect across subsequent views and asynchronous HTMX requests without requiring re-authentication.

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

#### Scenario: Password Reset Request for Active Account (Happy Path)
- **WHEN** an unauthenticated visitor submits `POST /forgot-password` with the email address of an existing active user (`active = true`)
- **THEN** the system generates a new confirmation token, updates `ts_confirm` to the current timestamp, persists the user record, dispatches the password-reset email, and redirects to `/login?resetRequested`

#### Scenario: Password Reset Request for Inactive or Deactivated Account (Security / Special Case)
- **WHEN** an unauthenticated visitor submits `POST /forgot-password` with the email address of an inactive or administrator-deactivated user (`active = false`)
- **THEN** the system does not generate or overwrite `confirmation` or `ts_confirm`, does not persist changes to the user record, and does not dispatch a password-reset email
- **AND** still redirects to `/login?resetRequested` so that account existence or activation state cannot be enumerated (CWE-204 protection), while preventing inactive or deactivated accounts from being marked for unconfirmed-user cleanup or reactivated via `/confirm/{token}`

### Requirement: Admin Management Console
The system SHALL provide administrative interfaces for managing user accounts, reviewing transaction logs, analyzing traffic metrics, and maintaining the domain whitelist, offering server-side column sorting with bidirectional visual chevrons across user management table controls including dedicated First Name and Last Name columns, clearing confirmation tokens and expiration timestamps whenever an administrator activates or deactivates a user account, preserving administrator-deactivated user accounts and any inactive accounts owning mailboxes indefinitely while periodically purging genuinely abandoned unconfirmed registrations (`active = false`, `confirmation IS NOT NULL`, `ts_confirm > 0`, `ts_confirm < cutoff`, and `boxes IS EMPTY`), presenting structured time-series email traffic activity views over the past 24 hours (hourly line chart) and past 7 days (daily stacked bar chart) covering forwarded, stored, and dropped categories backed by aggregated email traffic metrics from `MAIL_STATISTICS` with zero-filled continuity, dynamic theme synchronization, robust DOM-ready lifecycle execution, and graceful fallback handling, organizing activity into distinct sections with modern monochrome badges and standard icons, displaying top metric cards with accurate values and granular breakdowns for administrators/users and active/expired mailboxes, presenting an aggregate transaction delivery status badge distribution, displaying 24-hour and 7-day sender domain traffic breakdown tables backed by aggregated domain metrics from `MAIL_STATISTICS` with the 24-hour table aggregating across the sliding 24-hour window matching the activity chart and daily date and weekday breakdowns in the 7-day view and client-side case-insensitive column sorting with directional visual chevrons across both tables, and enforcing consistent code hygiene with `final` modifiers, Javadoc, and comprehensive test pyramid coverage.

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

#### Scenario: User Table Column Sorting by First Name (Happy Path)
- **WHEN** an administrator sorts the user table by first name (`sort=firstname` or alias `forename`) in ascending or descending direction
- **THEN** the server orders user accounts primarily by `forename` and secondarily by `surname` in the requested direction using case-insensitive ordering (`ignoreCase()`)
- **AND** applies `mail ASC` case-insensitively as a secondary tiebreaker to guarantee deterministic ordering across paginated views
- **AND** the table header renders an active directional chevron (`bi-chevron-up` for ASC, `bi-chevron-down` for DESC) on the First Name column with localized tooltip feedback

#### Scenario: User Table Column Sorting by Last Name (Happy Path)
- **WHEN** an administrator sorts the user table by last name (`sort=lastname` or alias `surname`) in ascending or descending direction
- **THEN** the server orders user accounts primarily by `surname` and secondarily by `forename` in the requested direction using case-insensitive ordering (`ignoreCase()`)
- **AND** applies `mail ASC` case-insensitively as a secondary tiebreaker to guarantee deterministic ordering across paginated views
- **AND** the table header renders an active directional chevron (`bi-chevron-up` for ASC, `bi-chevron-down` for DESC) on the Last Name column with localized tooltip feedback

#### Scenario: User Table Column Sorting by Name (Happy Path)
- **WHEN** an administrator sorts the user table by user name
- **THEN** the administrator sorts either by First Name (`sort=firstname`) or Last Name (`sort=lastname`), with the server ordering by the selected name component with secondary tiebreakers
- **AND** legacy single `sort=name` queries without first/last specification are safely normalized to default sorting by `email` ascending

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
- **WHEN** an inbound request provides a sort column parameter that is not in the allowed whitelist (`email`, `mail`, `firstname`, `forename`, `lastname`, `surname`, `status`, `role`), such as a non-existent column name or random text
- **THEN** the server safely normalizes the sort column to the default (`email`), applies default ascending direction, and renders the table without throwing an exception

#### Scenario: Invalid Sort Direction Fallback (Error Case)
- **WHEN** an inbound request provides an invalid direction string other than `asc` (e.g., `invalid`, `none`, or malformed input)
- **THEN** the server safely normalizes the direction to `desc` (or `asc` for the default `email` view), preventing query syntax errors

#### Scenario: SQL Injection Attempt in Sort Parameters (Error Case)
- **WHEN** an attacker submits SQL injection payloads in `sort` or `dir` parameters (e.g., `' OR 1=1--`, `email; DROP TABLE users;`)
- **THEN** the server's strict whitelist parser rejects the unsanitized input, normalizes the query to `mail ASC`, returns HTTP 200 with safely ordered data, and logs no SQL syntax exceptions

#### Scenario: Case-Insensitive Parameter Normalization (Special Case)
- **WHEN** an inbound request provides sort or direction parameters with mixed casing (e.g., `sort=FIRSTNAME`, `sort=LastName`, `sort=StAtUs`, `dir=ASC`, `dir=Desc`)
- **THEN** the server normalizes the parameters to lowercase and matches the corresponding whitelist property correctly

#### Scenario: Duplicate Value Deterministic Ordering (Special Case)
- **WHEN** multiple user records share identical values for the primary sort column (e.g., multiple inactive users, multiple standard users, or users with the same surname)
- **THEN** the secondary tiebreaker `mail ASC` ensures an identical, deterministic order across consecutive requests and page boundary transitions

#### Scenario: Empty Search Results Sorting Headers (Special Case)
- **WHEN** an administrator searches for a query string that yields zero matching user accounts
- **THEN** the table displays the localized empty state message spanning all columns (6 columns) while preserving sortable header links with their chevron indicators intact

#### Scenario: User Account Deactivation and Data Preservation (Happy Path)
- **WHEN** an administrator deactivates an active user account via `/admin/users/{id}/toggle-active`
- **THEN** the user's active status is set to `false`, any lingering historical `confirmation` token and `ts_confirm` timestamp are cleared (`null`), the user's mailboxes and emails are retained without deletion, and the user continues to be displayed in the user management table with an inactive badge
- **AND** the deactivated user account remains in the database indefinitely unless explicitly deleted by an administrator or self-service deletion

#### Scenario: Inactive User Reactivation and Token Sanitization (Happy Path)
- **WHEN** an administrator activates an inactive user account via `/admin/users/{id}/toggle-active`
- **THEN** the user's active status is set to `true`, any lingering confirmation tokens and expiration timestamps are cleared (`null`), and the user can authenticate immediately

#### Scenario: Background Maintenance User Purge Exemption
- **WHEN** periodic background maintenance tasks (such as scheduled mailbox and retention expiration) execute
- **THEN** active user accounts, administrator-deactivated user accounts (whose confirmation tokens have been cleared), and any inactive user accounts that own one or more mailboxes (`boxes IS NOT EMPTY`) are strictly exempted and never deleted or purged from the database
- **AND** only genuinely abandoned, unconfirmed user registrations (`active = false`, `confirmation IS NOT NULL`, `ts_confirm > 0`, `ts_confirm < cutoff`, and `boxes IS EMPTY`) are purged once their confirmation window has expired

#### Scenario: Scheduled Purge of Expired Unconfirmed Registrations Without Mailboxes (Happy Path)
- **WHEN** periodic background maintenance executes and an inactive user record has a non-null confirmation token, a positive `ts_confirm` older than the configured confirmation grace period (`now - (confirmGraceHours * 3,600,000)`), and zero mailboxes (`boxes IS EMPTY`)
- **THEN** the system deletes the expired unconfirmed user record from the database

#### Scenario: Legacy Deactivated User With Mailboxes Protected From Purge (Special Case)
- **WHEN** periodic background maintenance executes and an inactive user record still carries a legacy non-null confirmation token and expired `ts_confirm` timestamp in the database, but owns one or more mailboxes (`boxes IS NOT EMPTY`)
- **THEN** the empty-mailbox query safeguard (`u.boxes IS EMPTY`) excludes the user from cleanup and preserves the user and all of their mailboxes intact without requiring a database migration

#### Scenario: Unactivated User Within Confirmation Grace Period Protected From Purge (Boundary Case)
- **WHEN** periodic background maintenance executes and an inactive unconfirmed user has a `ts_confirm` timestamp within the active confirmation window (`ts_confirm >= cutoff`) or `ts_confirm <= 0`
- **THEN** the system retains the unconfirmed user record so the user can complete email verification within the allowed timeframe

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
- **THEN** the system renders a line chart (`#chart-last-24h`) displaying email activity over the preceding 24 hours aggregated from `MAIL_STATISTICS`
- **AND** the chart displays three distinct datasets: forwarded email counts (Status 300), stored email counts (Status 250), and dropped email counts (all other statuses)
- **AND** the horizontal axis represents 24 continuous hourly buckets formatted with localized hour labels ending at the current hour
- **AND** the card header displays the total volume of emails processed during the 24-hour window, matching the total volume of the 24-hour sender domain table

#### Scenario: Last 7 Days Email Activity Chart (Happy Path)
- **WHEN** an administrator views the statistics page (`/admin/statistics`)
- **THEN** the system renders a daily stacked bar chart (`#chart-last-7d`) displaying email activity over the preceding 7 days aggregated from `MAIL_STATISTICS`
- **AND** each daily bar is segmented into stacked bars for stored (blue), forwarded (green), and dropped (red) messages without overlapping zero-lines
- **AND** inactive days render with no bars rather than drawing baseline zero-lines
- **AND** the card header displays the total volume of emails processed during the 7-day window, matching the aggregate total of the 7-day sender domain table

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
- **THEN** beneath the 24-hour activity chart, the system displays a card containing a table of sender domains active over the sliding 24-hour window ending at the current quarter-hour slot
- **AND** the table lists each sender domain alongside counts for stored, forwarded, and dropped emails, ordered by total email volume descending
- **AND** the total volume of emails represented across all rows in the table matches the total volume displayed in the 24-hour activity chart

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

#### Scenario: Chart and Table Metric Parity Across Transaction Pruning
- **WHEN** detailed transaction records in `mailtransactions` are purged or omitted for silent drops
- **THEN** both the 24-hour and 7-day activity charts and the sender domain breakdown tables read from `MAIL_STATISTICS`
- **AND** the total volume of emails represented across all chart buckets matches the aggregate sum of emails in the corresponding domain breakdown table for both the sliding 24-hour window and 7-day calendar date ranges

#### Scenario: Empty Sender Domains Graceful State
- **WHEN** no inbound email traffic has been recorded for the queried time window
- **THEN** the sender domain tables render a localized empty state message indicating that no records were found spanning all columns (5 columns for 24-hour table, 6 columns for 7-day table)

#### Scenario: Web Console Code Quality Standards
- **WHEN** any web console controller, service, or repository is compiled or tested
- **THEN** all parameters and local variables are marked `final`
- **AND** all public methods feature comprehensive Javadoc documentation
- **AND** the implementation maintains test pyramid coverage including unit, WebMvc mock, and integration tests
