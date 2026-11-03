# Spec Delta

## MODIFIED Requirements

### Requirement: Dynamic Mailbox Dashboard
The system SHALL render an interactive mailbox dashboard supporting search, inline editing, modal dialogs, and bulk actions via HTMX without full page reloads, ensuring modal hosts are placed outside layout stacking contexts to prevent backdrop occlusion, while adapting table columns and actions responsively across viewport sizes with a calm corporate palette, presenting mailbox active duration selection via responsive segmented radio button pills, honoring forwarding preferences on creation, preserving duration and forwarding selections across validation error re-renders, and rendering timestamps localized to the client browser's timezone.

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
- **THEN** the refreshed table retains the currently active search query and status filter selection

#### Scenario: Dashboard Header Primary Action Exclusivity
- **WHEN** the mailbox dashboard page header is rendered
- **THEN** the top header action area presents solely the primary creation action ("New Mailbox"), omitting secondary export dropdown controls to maintain visual hierarchy and prevent action clutter

#### Scenario: Table Footer Utility Export Links
- **WHEN** the mailbox table is rendered
- **THEN** subtle secondary text export links ("All (.txt)" and "Active (.txt)") are rendered within the table footer utility and pagination area below the table, remaining accessible without competing with primary page actions

#### Scenario: Responsive Mobile Table Viewport
- **WHEN** a user views the mailbox table on a mobile viewport narrower than 768px
- **THEN** the table hides secondary columns (forwards, suppressed, and expiration) to prevent horizontal scrolling and displays essential expiration and forwarding status as a subline beneath the primary email address

#### Scenario: Responsive Action Menu Consolidation
- **WHEN** a user interacts with a mailbox row on a mobile viewport narrower than 768px
- **THEN** standalone compact icon quick action buttons (toggle and +1d) are collapsed into the `[...]` kebab dropdown menu, presenting a single compact action button per row with all actions accessible within the dropdown

#### Scenario: Subtle Visual Status and Mail Count Indicators
- **WHEN** the mailbox table is rendered
- **THEN** active and expired statuses are displayed using subtle desaturated tokens or status dots rather than high-saturation solid badges, and zero mail counts are rendered with neutral muted styling

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
