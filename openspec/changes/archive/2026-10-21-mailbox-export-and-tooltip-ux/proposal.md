# Proposal: Mailbox Export Download and Tooltip UX Refinements

## Why

Following the modernization of the mailbox dashboard and bulk actions, user testing identified three key user experience and interaction defects:
1. Clicking row action icon buttons (such as status toggle) leaves orphaned tooltip bubbles floating on screen because Bootstrap 5 tooltips attach to `document.body` and fail to clean up when HTMX dynamically swaps table markup.
2. The `Export ▾` dropdown button placed in the page header adjacent to `+ New Mailbox` introduces visual imbalance, giving a secondary export utility equal prominence to the primary page creation action, while redundantly overlapping with the contextual bulk batch bar.
3. Clicking export links opens raw text in a browser tab rather than triggering an actual `.txt` file download because the controller responses lack `Content-Disposition: attachment` HTTP headers.

Resolving these issues restores a clean visual hierarchy, ensures reliable tooltip behavior during asynchronous DOM updates, and delivers genuine file downloads.

## What Changes

- **Clean Action Tooltips**: Remove Bootstrap JS tooltip initialization (`data-bs-toggle="tooltip"`) on table row action buttons in favor of standard, native HTML `title` attributes, ensuring tooltips naturally dismiss on click and never leave orphaned DOM nodes during HTMX table swaps.
- **Header Action Hierarchy Cleanup**: Remove the prominent `Export ▾` dropdown button from the top page header next to `+ New Mailbox`, keeping the top header focused on primary creation.
- **Table Footer Utility Export Links**: Provide subtle text links (`All (.txt)` and `Active (.txt)`) in the mailbox table footer/pagination area, matching the clean design pattern of legacy XCMailr while modernizing typography and layout.
- **Genuine `.txt` File Downloads**: Configure `Content-Disposition: attachment; filename="mailboxes.txt"` (and descriptive filenames for active/bulk exports) on `/mailboxes/export/all`, `/mailboxes/export/active`, and `/mailboxes/bulk/export` so browsers download `.txt` files directly instead of opening raw text in browser tabs.
- **Comprehensive Testing Pyramid**: Add full test coverage across unit, controller, and integration levels with happy path scenarios, error handling (unauthorized access, cross-user mailbox access), and special edge cases (empty results, filtered lists, special characters), enforcing `final` modifiers throughout.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `web-console-ui`: Update the `Dynamic Mailbox Dashboard` requirement to specify clean native action tooltips, secondary export utility link placement in table footer, and attachment content disposition headers on all mailbox export endpoints.

## Impact

- **Templates**:
  - `xcmailr-webapp/src/main/resources/templates/mailboxes/index.html` (header export button removal, listener cleanups)
  - `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/mailbox-table.html` (row button tooltip attributes, footer utility export links)
- **Controllers**:
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/MailboxWebController.java` (`renderAddressTextList` attachment headers)
- **Tests**:
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailboxBulkActionsWebTest.java` (update headers, add export tests)
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailboxExportWebTest.java` (dedicated test pyramid for export endpoints and error/special cases)

