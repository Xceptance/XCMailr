# Tasks

## 1. Controller Sorting & Whitelist Implementation (Tier 1 Unit Coverage)

- [x] 1.1 Implement sort and direction parameter extraction in `MailboxWebController.listMailboxes(...)` with `final` modifiers on all parameters and local variables, whitelist mapping (`address`, `status`, `expires`, `forwards`, `suppressions`), safe default fallback (`id DESC`), and pass the constructed `Sort` instance into `PageRequest.of(...)`.
- [x] 1.2 Add comprehensive Javadoc to all modified controller methods, documenting parameters, return values, sort semantics, and explanatory code comments on the whitelist switch.
- [x] 1.3 Add Tier 1 unit tests in `MailboxWebControllerTest` covering Happy Path (all valid columns, asc/desc directions), Error Cases (unrecognized sort keys, SQL injection attempts, malformed directions falling back to defaults), and Special Cases (case-insensitivity, null/blank parameters). Verify by running `mvn test -Dtest=MailboxWebControllerTest`.

## 2. UI Markup, HTMX Controls & WebMvc Slice Coverage (Tier 2 Slice Coverage)

- [x] 2.1 Update `mailbox-table.html` column headers (`Address`, `Status`, `Expires`, `Forwards`, `Suppressed`) with clickable HTMX elements requesting `/mailboxes` with target column, toggled direction, and `page=0`.
- [x] 2.2 Add active sort indicator icons (`bi bi-arrow-up` / `bi bi-arrow-down`) to the active column header while leaving inactive headers clean and the "Mails" column static.
- [x] 2.3 Add hidden input fields for `sort` and `dir` inside `#mailbox-table-container`, and update `hx-include` on `#mailbox-table-container` and `index.html` filter/search controls to preserve sort parameters.
- [x] 2.4 Update pagination links in `mailbox-table.html` to propagate `sort` and `dir` query parameters.
- [x] 2.5 Add Tier 2 WebMvc slice tests asserting rendered HTML fragments, direction toggle links, indicator icons, hidden inputs, and query parameter preservation across HTMX swaps. Verify by running `mvn test -Dtest="*Mailbox*Web*Test*"`.

## 3. Database Multi-Page Integration Coverage & Issue #91 Verification (Tier 3 Integration Coverage)

- [x] 3.1 Implement Tier 3 integration test `MailboxSortingIntegrationTest` asserting that multi-page queries reorder across the entire database dataset before pagination slicing, verifying resolution of legacy Issue #91.
- [x] 3.2 Add integration tests covering Special Cases: sorting datasets containing permanent unlimited mailboxes (`ts_Active = 0`), sorting empty mailbox accounts, and case-insensitive address ordering. Verify by running `mvn test -Dtest=MailboxSortingIntegrationTest`.
- [x] 3.3 Ensure all Java test and production files adhere to code standards with `final` parameters/variables, comprehensive Javadoc, and clear explanatory code comments. Verify by running the entire mailbox test suite via `mvn test -Dtest="*Mailbox*"`.
