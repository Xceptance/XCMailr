# Proposal

## Why

In the administrative User Management table, users currently see a single "Name" column that renders full names in "First Last" order (e.g., "John Doe"), but sorting on this column orders records by last name (`surname ASC, forename ASC`). This produces an unexpected and visually counter-intuitive order where names starting with different first initials appear disordered (e.g. Charlie Adams, Bob Brown, Alice Smith). Furthermore, administrators cannot sort users by their first name.

Splitting the user table into two dedicated, sortable columns—**First Name** and **Last Name**—resolves this confusion, grants administrators granular sorting control by either first or last name, and restores parity with legacy XCMailr's administrative user overview.

## What Changes

- **Separate First and Last Name Columns**: Split the single `Name` table header and data cell in `user-table.html` into two separate, independently sortable columns: `First Name` (`u.forename`) and `Last Name` (`u.surname`).
- **Granular Sorting Parameters**: Update `AdminWebController` to whitelist and handle `sort=firstname` (and alias `forename`) sorting by `forename` with `surname` and `mail` tiebreakers, and `sort=lastname` (and alias `surname`) sorting by `surname` with `forename` and `mail` tiebreakers.
- **Responsive Layout**: Configure both `First Name` and `Last Name` columns to be visible on medium screens and larger (`d-none d-md-table-cell`), while preserving the compact combined full-name subtitle beneath the email address on mobile viewports (`< md`).
- **Empty State Colspan**: Adjust the empty state row's `colspan` from 5 to 6 to account for the additional column.
- **Localization**: Add dedicated localized messages for `admin.users.column.firstName`, `admin.users.column.lastName`, `admin.users.sort.firstName`, and `admin.users.sort.lastName` in both English and German message bundles.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `web-console-ui`: Modifies `Requirement: Admin Management Console` to specify distinct `First Name` and `Last Name` sortable columns and tiebreaker behaviors in user administration.

## Impact

- **Affected Files**:
  - `xcmailr-webapp/src/main/resources/templates/admin/fragments/user-table.html`: Table header and body rendering.
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/AdminWebController.java`: `resolveSortField`, `resolveSort`, and Javadoc.
  - `xcmailr-webapp/src/main/resources/conf/messages.properties` and `messages_de.properties`: Column labels and sort tooltips.
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/AdminWebControllerTest.java`: Unit and WebMvc tests verifying sorting and model attributes.
- **APIs / Endpoints**: `GET /admin/users` accepts `sort=firstname`, `sort=lastname`, and aliases alongside existing sort parameters.
