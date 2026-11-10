# Spec Delta

## MODIFIED Requirements

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
