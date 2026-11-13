# Tasks

## 1. Localization & Backend Sorting Whitelist

- [x] 1.1 Add localized messages for `admin.users.column.firstName`, `admin.users.column.lastName`, `admin.users.sort.firstName`, and `admin.users.sort.lastName` in `xcmailr-webapp/src/main/resources/conf/messages.properties` and `messages_de.properties`, and verify that property files parse cleanly without syntax errors.
- [x] 1.2 Update `AdminWebController.resolveSortField` to whitelist and normalize `"firstname"` (alias `"forename"`) and `"lastname"` (alias `"surname"`), and verify with unit tests in `AdminWebControllerTest`.
- [x] 1.3 Update `AdminWebController.resolveSort` to construct directory-style composite sorts for `"firstname"` (primary `forename`, secondary `surname`) and `"lastname"` (primary `surname`, secondary `forename`) with case-insensitivity and `mail ASC` secondary tiebreaker, and verify with unit tests in `AdminWebControllerTest`.

## 2. Table Fragment & WebMvc Tests

- [x] 2.1 Update `xcmailr-webapp/src/main/resources/templates/admin/fragments/user-table.html` to render separate, sortable `First Name` and `Last Name` column headers and data cells on desktop/tablet (`d-none d-md-table-cell`), retain combined full name on mobile viewports (`< md`), and update the empty state `colspan` from 5 to 6.
- [x] 2.2 Add WebMvc slice tests in `AdminWebControllerTest` verifying that requests with `sort=firstname` and `sort=lastname` render the table with active chevron icons on the appropriate column headers, and unwhitelisted parameters safely fall back to default email sort.

## 3. Verification & Code Quality

- [x] 3.1 Verify that all method parameters and local variables adhere to the project's `final` modifier conventions and all public/package-private methods have complete Javadoc documentation.
- [x] 3.2 Run the full test suite (`mvn clean test -pl xcmailr-webapp`) and verify all 102+ unit and WebMvc tests pass with zero failures.
