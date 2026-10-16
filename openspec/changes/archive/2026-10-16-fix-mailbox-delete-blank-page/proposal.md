# Proposal: Fix Mailbox Delete Blank Page Bug

## Why
Deleting a mailbox from the web dashboard causes the mailbox table container to be replaced with an empty body, resulting in a blank white page until the user manually refreshes the browser. This change fixes the HTMX swapping and event handling on mailbox deletion and related row actions so the mailbox table updates seamlessly without UI disruption.

## What Changes
- **Fix Mailbox Deletion Action**: Correct the HTMX swap and target configuration on the mailbox table row "Delete Mailbox" action so it does not overwrite `#mailbox-table-container` with an empty response.
- **Fix Mailbox Table Row Actions**: Correct the HTMX configuration for mailbox row actions (Status Toggle, +24h Expiration Extension, Reset Counters) so they do not inadvertently swap empty response bodies into the table container.
- **Preserve Filter and Search State on Refresh**: Ensure that when the `#mailbox-table-container` triggers its reload upon receiving `mailboxChanged`, it includes current filter parameters (`search` query and `status` selection) so the user's active view is maintained.
- **Verify Consistency in Admin Tables**: Align user and domain deletion/status actions in the administration views to follow the same robust HTMX trigger/swap pattern so administrative tables do not suffer from the same empty-swap issue.

## Capabilities

### New Capabilities
*(None)*

### Modified Capabilities
- `web-console-ui`: Update the `Dynamic Mailbox Dashboard` requirement to ensure mailbox deletion and inline row actions update the table smoothly via HTMX without wiping out the table container or resulting in blank pages, while preserving active search filters.

## Impact
- **Web UI Templates**: `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/mailbox-table.html` (and optionally admin table fragments `user-table.html`, `domain-table.html`).
- **Controllers**: `MailboxWebController.java` (and potentially `AdminWebController.java` if response conventions or headers need adjustment).
- **Automated Tests**: Controller tests and MockMvc slice tests (`MailboxWebControllerTest.java`) ensuring HTMX requests for delete and mutations return the expected headers and statuses.

