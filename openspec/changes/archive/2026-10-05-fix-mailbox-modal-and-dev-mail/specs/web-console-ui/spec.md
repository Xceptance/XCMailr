# Spec Delta

## MODIFIED Requirements

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

