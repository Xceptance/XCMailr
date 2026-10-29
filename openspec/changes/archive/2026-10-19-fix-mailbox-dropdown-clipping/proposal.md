# Proposal

## Why

When viewing the mailbox dashboard with few mailboxes or opening the actions menu on the bottom rows, the action dropdown menu is clipped by the `.table-responsive` container's overflow boundary. This hides critical mailbox operations such as *Reset Counters*, the divider, and *Delete Mailbox*. Configuring Bootstrap 5's dropdown with a fixed viewport strategy and viewport boundary ensures all menu items remain fully visible and interactable regardless of table height or viewport dimensions.

## What Changes

- Add `data-bs-boundary="viewport"` and `data-bs-popper-config='{"strategy":"fixed"}'` to the mailbox row action dropdown toggle button in `mailbox-table.html`.
- Prevent vertical clipping by `.table-responsive` across desktop, tablet, and mobile viewports by elevating the dropdown positioning context to the viewport.
- Maintain seamless compatibility with HTMX dynamic row refreshes, pagination, and search/filter swaps.
- Add test assertions verifying the presence and correct format of the fixed viewport dropdown attributes in mailbox table rendering.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `web-console-ui`: Modifies `Dynamic Mailbox Dashboard` requirement to specify that mailbox action dropdown menus use fixed positioning and viewport boundary to prevent overflow clipping.

## Impact

- Affected template: `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/mailbox-table.html`.
- Affected tests: `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailboxWebControllerTest.java`.
- No database schema, API contract, or backend service changes.

