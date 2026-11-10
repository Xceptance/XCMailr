# Spec Delta

## MODIFIED Requirements

### Requirement: Admin Management Console
The system SHALL provide administrative interfaces for managing user accounts, reviewing transaction logs, analyzing traffic metrics, and maintaining the domain whitelist, offering server-side column sorting with bidirectional visual chevrons across user management table controls, and presenting vertically stacked time-series line charts for email traffic activity over the past 24 hours (hourly resolution) and past 7 days (daily resolution) with zero-filled continuity and dynamic theme synchronization.

#### Scenario: Domain Whitelist Configuration
- **WHEN** an administrator adds or removes an allowed domain via the admin console
- **THEN** the system updates the registered domain table and enforces the change on the inbound SMTP listener immediately

#### Scenario: Paged Transaction Auditing
- **WHEN** an administrator inspects the mail transaction log
- **THEN** the system displays paginated records with filtering by timestamp, status, and address

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

#### Scenario: Last 24 Hours Email Activity Chart (Happy Path)
- **WHEN** an administrator views the statistics page (`/admin/statistics`)
- **THEN** the system renders a line chart (`#chart-last-24h`) displaying email activity over the preceding 24 hours
- **AND** the chart displays two distinct datasets: forwarded email counts and dropped email counts
- **AND** the horizontal axis represents 24 continuous hourly buckets formatted with localized hour labels ending at the current hour

#### Scenario: Last 7 Days Email Activity Chart (Happy Path)
- **WHEN** an administrator views the statistics page (`/admin/statistics`)
- **THEN** the system renders a line chart (`#chart-last-7d`) displaying email activity over the preceding 7 days
- **AND** the chart displays two distinct datasets: forwarded email counts and dropped email counts
- **AND** the horizontal axis represents 7 continuous daily buckets formatted with localized day labels ending on the current day

#### Scenario: Chart Theme and Responsiveness (Happy Path)
- **WHEN** an administrator views or toggles the application theme between light and dark modes
- **THEN** the charts adapt responsively across viewport widths inside full-width card containers
- **AND** grid line colors, axis ticks, and tooltip backgrounds dynamically synchronize with the active theme tokens (`data-bs-theme="light"` or `data-bs-theme="dark"`), avoiding unreadable contrast or fixed light-only canvas rendering

#### Scenario: Empty Email Traffic Zero-Fill Continuity (Edge Case)
- **WHEN** no email forwarding or drop transactions occurred within specific hours or days of the queried time windows
- **THEN** the backend data model provides continuous time series buckets with count values set to `0` for inactive intervals
- **AND** the charts render a continuous unbroken zero baseline without missing data points or jagged gaps

#### Scenario: Localization of Chart Metrics (Happy Path)
- **WHEN** an administrator views the statistics page in English or German
- **THEN** chart card titles, dataset legend labels (`Forwarded` / `Weitergeleitet`, `Dropped` / `Verworfen`), and axis labels render using message bundles corresponding to the user's active locale
