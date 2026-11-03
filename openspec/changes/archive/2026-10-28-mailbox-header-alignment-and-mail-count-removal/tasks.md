# Tasks

## 1. Controller Optimization & Mail Counts Removal

- [x] 1.1 Remove the `mailCounts` calculation loop and model attribute in `MailboxWebController.java`, ensuring all parameters and local variables remain `final` and Javadoc/comments are preserved and accurate.
- [x] 1.2 Verify `MailboxWebControllerTest` compiles and passes without referencing `mailCounts`.

## 2. Table Template Cleanup & Header Vertical Alignment

- [x] 2.1 Remove the `Mails` column header `<th>` and mailbox body cell `<td>` from `mailbox-table.html`.
- [x] 2.2 Update the empty-state row `colspan` in `mailbox-table.html` from 8 to 7.
- [x] 2.3 Wrap the `Actions` column header text in `<span class="d-inline-flex align-items-center py-1">Actions</span>` in `mailbox-table.html` to match the height and vertical padding of sortable headers.
- [x] 2.4 Apply `align-middle` to the `<thead>` row `<tr class="align-middle">` in `mailbox-table.html` so that the select-all checkbox, sortable headers, and Actions header are centered along the exact same vertical baseline.

## 3. Automated Testing & Regression Safety

- [x] 3.1 Update `MailboxSortingWebTest.java` to verify the updated 7-column header markup, the `<tr class="align-middle">` row, the `<span class="d-inline-flex align-items-center py-1">Actions</span>` element, and the absence of `<th>Mails</th>`.
- [x] 3.2 Add explicit assertions in `MailboxWebControllerTest.java` / `MailboxSortingWebTest.java` verifying that the `mailCounts` model attribute is not present across standard and empty list responses.
- [x] 3.3 Run all web slice tests and integration safety tests (`mvn test -pl xcmailr-webapp -Dtest="Mailbox*"`) to confirm zero regressions.

