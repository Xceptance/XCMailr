# Spec Delta: web-console-ui

## MODIFIED Requirements

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
