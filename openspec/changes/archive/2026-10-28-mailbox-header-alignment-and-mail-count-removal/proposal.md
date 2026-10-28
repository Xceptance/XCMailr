# Proposal

## Why

In the mailbox overview dashboard, the "Mails" column displays a dynamically calculated message count per mailbox (`countByMailbox`), introducing an N+1 SQL query pattern on every dashboard interaction (search, filter, sort, pagination) that did not exist in the legacy XCMailr application. Furthermore, the `Actions` table header (along with the select-all checkbox) is vertically misaligned and rendered deeper (lower down) than the sortable column headers due to Bootstrap's default bottom vertical alignment on `thead` combined with missing vertical padding/flexbox wrapper on non-sortable headers. Removing the unneeded mail count calculation streamlines dashboard performance and matches legacy design, while harmonizing table header heights and vertical alignment ensures a clean, consistent UI presentation.

## What Changes

- **Remove "Mails" Column & Controller Query**:
  - Remove the per-page `mailRepository.countByMailbox(box.getId())` computation loop and `mailCounts` model attribute from `MailboxWebController`.
  - Remove the `<th>Mails</th>` header and corresponding `<td>` mail count badge cell from `mailbox-table.html`.
  - Adjust the empty-state row `colspan` from 8 to 7.
- **Harmonize Table Header Vertical Alignment**:
  - Wrap the `Actions` header text in `<span class="d-inline-flex align-items-center py-1">Actions</span>` in `mailbox-table.html` to match the exact vertical padding (`py-1`), display model, and height of sortable headers.
  - Apply `align-middle` to the `<thead>` row (`<tr class="align-middle">`) to vertically center all header cells (including the select-all checkbox) uniformly across the table.
- **Update Automated Tests**:
  - Update `MailboxSortingWebTest` to reflect the 7-column header structure and the absence of the "Mails" column and `mailCounts` attribute.
  - Add slice tests verifying vertical alignment classes, clean 7-column structure, and model attributes.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `web-console-ui`: Remove dynamic "Mails" column and N+1 count calculation from the mailbox dashboard, and align table header columns vertically.

## Impact

- **Affected Code**:
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/MailboxWebController.java`
  - `xcmailr-webapp/src/main/resources/templates/mailboxes/fragments/mailbox-table.html`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailboxSortingWebTest.java`
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/MailboxWebControllerTest.java`
- **APIs & Dependencies**: No breaking API or dependency changes. Mailbox email inspection remains accessible via clicking mailbox addresses, the kebab action menu ("Inbox Mails"), and the top navigation ("All Mails").
- **Performance**: Eliminates up to 25+ database count queries per mailbox page load/filter/sort request.

