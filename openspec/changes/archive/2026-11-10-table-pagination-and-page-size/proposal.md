# Proposal

## Why

Paginated tables across the web management console currently either render an unbounded list of page buttons (up to 40+ buttons overflowing viewport boundaries) or lack ellipsis truncation, and none provide an interactive page size selector. Aligning table navigation with the proven production interface restores a familiar, responsive 1-click Display Count bar (`5`, `15`, `25`, `50`, `All`) and enforces a strictly bounded 7-slot sliding pagination window with ellipsis (`...`), while safeguarding global sorting order, single-item actions, and multi-tenant security.

## What Changes

- **7-Slot Pagination Bar with Ellipsis**: Standardize pagination controls across all 5 paginated tables (`/mailboxes`, `/admin/users`, `/admin/transactions`, `/mailboxes/{id}/mails`, `/mails`) to render at most 7 page slots (using `...` for omitted ranges), flanked by Previous and Next controls.
- **Production-Style Display Count Bar**: Introduce a segmented Display Count button group (`5`, `15`, `25`, `50`, `All`) styled symmetrically with the pagination bar (`.pagination.pagination-sm`), highlighting the active size and enabling instant 1-click page density adjustments.
- **Permanent Control Visibility**: Keep pagination and Display Count controls visible when data exists (`totalElements > 0`), including single-page results (`totalPages == 1`), while disabling navigation buttons when inactive.
- **State Preservation**: Ensure hypermedia links (page size buttons, pagination numbers, Previous/Next links, and sortable column headers) preserve the complete tuple of `page`, `size`, `sort`, `dir`, and active filters (`search`, `status`).
- **Defensive Backend Clamping & SQL Injection Protection**: Clamp `size` defensively across all web controllers (`Math.min(Math.max(1, size), 1000)`), map `All` safely, and verify that strict whitelist column mapping and parameterized queries strictly prevent SQL injection.
- **Code Quality & Testing**: Enforce `final` modifiers wherever possible, provide comprehensive Javadoc and architectural comments, and write automated tests following the testing pyramid (unit, web integration, happy paths, error boundaries, special cases, and security SQL injection tests).

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `web-console-ui`: Update table pagination and density requirements across the mailbox dashboard, inbox message lists, and administration consoles to mandate a maximum 7-slot pagination bar with ellipsis truncation, an interactive segmented Display Count bar, and state-preserving table interactions.

## Impact

- **Templates**:
  - `mailboxes/fragments/mailbox-table.html`
  - `admin/fragments/user-table.html`
  - `admin/fragments/transaction-table.html`
  - `mailboxes/fragments/mail-list.html`
  - `mailboxes/fragments/all-mail-list.html`
  - `mailboxes/index.html` (search & filter inclusion)
  - `admin/users.html` (search inclusion)
- **Controllers & Utilities**:
  - New `com.xceptance.xcmailr.util.PaginationHelper` utility for 7-slot page sequence calculation.
  - `MailboxWebController.java`, `AdminWebController.java`, `MailWebController.java` (bounds clamping, `all` handling, and model propagation).
- **Localization**:
  - `conf/messages.properties` and `conf/messages_de.properties` (`common.pagination.pageSize`, `macro_EntriesPerPage`, `common.pagination.all`).
- **Tests**:
  - `PaginationHelperTest` (unit test covering all branch permutations).
  - Integration and MockMvc tests verifying 7-slot rendering, ellipsis presence, page size switching, sorting preservation, and SQL injection safety.
