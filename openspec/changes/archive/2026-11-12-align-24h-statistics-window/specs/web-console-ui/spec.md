# Spec Delta

## MODIFIED Requirements

### Requirement: Admin Management Console
The system SHALL provide administrative interfaces for managing user accounts, reviewing transaction logs, analyzing traffic metrics, and maintaining the domain whitelist, offering server-side column sorting with bidirectional visual chevrons across user management table controls, presenting structured time-series email traffic activity views over the past 24 hours (hourly line chart) and past 7 days (daily stacked bar chart) covering forwarded, stored, and dropped categories backed by aggregated email traffic metrics from `MAIL_STATISTICS` with zero-filled continuity, dynamic theme synchronization, robust DOM-ready lifecycle execution, and graceful fallback handling, organizing activity into distinct sections with modern monochrome badges and standard icons, displaying top metric cards with accurate values and granular breakdowns for administrators/users and active/expired mailboxes, presenting an aggregate transaction delivery status badge distribution, displaying 24-hour and 7-day sender domain traffic breakdown tables backed by aggregated domain metrics from `MAIL_STATISTICS` with the 24-hour table aggregating across the sliding 24-hour window matching the activity chart and daily date and weekday breakdowns in the 7-day view and client-side case-insensitive column sorting with directional visual chevrons across both tables, and enforcing consistent code hygiene with `final` modifiers, Javadoc, and comprehensive test pyramid coverage.

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
