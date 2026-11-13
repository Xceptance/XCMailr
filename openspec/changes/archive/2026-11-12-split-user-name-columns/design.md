# Design

## Context

See [proposal.md](proposal.md) for motivation and problem statement.

The user administration table (`/admin/users`, rendered via `user-table.html`) lists registered user accounts with pagination, text filtering, and server-side column sorting managed by `AdminWebController`.

Currently, `user-table.html` displays 5 columns: Email, Name, Status, Role, and Actions. The `Name` column renders `${u.forename + ' ' + u.surname}` but invokes `sort=name`, which `AdminWebController.resolveSort(...)` maps to `surname` (direction) followed by `forename` (direction).

In the database, the `User` entity already maintains separate `forename` and `surname` attributes mapped to `users.forename` and `users.surname`. Legacy XCMailr (`showUsers.ftl.html`) provided separate, sortable "First Name" and "Name" columns.

## Goals / Non-Goals

**Goals:**
- Provide two distinct desktop/tablet columns in `user-table.html`: "First Name" (`u.forename`) and "Last Name" (`u.surname`).
- Enable independent, server-side case-insensitive sorting for both First Name and Last Name with bidirectional visual chevrons.
- Preserve deterministic sorting across paginated pages using `mail ASC` as a secondary tiebreaker.
- Retain the clean, compact mobile view (`< md`) displaying the combined full name beneath the email address.
- Update the empty state row to span all 6 table columns.

**Non-Goals:**
- Client-side sorting for the user table (user table is server-side paginated).
- Altering the database schema or `User` entity attributes.
- Changing sorting behavior for mailboxes, transactions, or domains.

## Decisions

### 1. Two Dedicated Columns vs. Single Column Sorted by Full Name

- **Decision**: Split the single `Name` table column into two dedicated columns—`First Name` and `Last Name`—on viewport sizes medium and larger (`class="d-none d-md-table-cell"`).
- **Rationale**:
  - Eliminates the cognitive dissonance of seeing "First Last" while rows are ordered by "Last".
  - Grants administrators granular sorting by either forename or surname.
  - Restores visual and functional parity with legacy XCMailr's administrative user overview.
- **Alternatives Considered**:
  - *Keep single column, sort by `forename, surname`:* Rejected because administrators frequently need directory-style sorting by family name (surname).
  - *Keep single column, display "Last, First" (e.g. "Doe, John"):* Rejected because western naming conventions in the application elsewhere display "First Last", and users still could not sort by first name.

### 2. Whitelist and Sort Aliases in `AdminWebController`

- **Decision**:
  - Update `resolveSortField` to accept:
    - `"firstname"`, `"forename"` $\rightarrow$ normalized to `"firstname"`.
    - `"lastname"`, `"surname"` $\rightarrow$ normalized to `"lastname"`.
  - Update `resolveSort` to construct:
    - `"firstname"`: `Sort.Order.by("forename").with(direction).ignoreCase()`, then `Sort.Order.by("surname").with(direction).ignoreCase()`, then `mailTiebreaker`.
    - `"lastname"`: `Sort.Order.by("surname").with(direction).ignoreCase()`, then `Sort.Order.by("forename").with(direction).ignoreCase()`, then `mailTiebreaker`.
  - Unrecognized sort fields (including retired `"name"`) fall back safely to default `"email"` sorting.
- **Rationale**:
  - Since this is part of the modernization feature branch (`modernizeXCMailr`), legacy `sort=name` is retired rather than aliased, keeping sort parameters clean and explicit (`firstname` and `lastname`).
  - Adding `"forename"` and `"surname"` as aliases accommodates developers and scripts using entity attribute names directly.

### 3. Responsive Breakpoints

- **Decision**:
  - Desktop / Tablet (`>= md`): Render distinct `First Name` and `Last Name` columns.
  - Mobile (`< md`): Hide both name columns (`d-none d-md-table-cell`), preserving the combined `${u.forename + ' ' + u.surname}` line directly underneath the email address in the Email column.
- **Rationale**:
  - Prevents horizontal overflow and cramped tabular layouts on mobile viewports.
  - Maintains full name visibility on small screens without cluttering the interface.

### 4. Localization Keys

- **Decision**:
  - Add `admin.users.column.firstName`, `admin.users.column.lastName`, `admin.users.sort.firstName`, and `admin.users.sort.lastName` to both `messages.properties` (English) and `messages_de.properties` (German).
  - Retain legacy `admin.users.column.name` and `admin.users.sort.name` keys in the property files to prevent missing-message warnings if referenced elsewhere.
- **Rationale**:
  - Ensures clean i18n support matching the application's English and German localization standard.

## Risks / Trade-offs

- **[Risk] Table horizontal width congestion on tablet viewports (md, 768px-991px)** $\rightarrow$ **Mitigation**: The table header and cells for First Name and Last Name use standard padding and text-break classes; Actions, Status, and Role columns maintain compact width styling (`style="width: 110px;"`, `style="width: 90px;"`, `style="width: 1%; white-space: nowrap;"`).
- **[Risk] Empty table view layout glitch** $\rightarrow$ **Mitigation**: Update `colspan="5"` to `colspan="6"` on the empty table placeholder row in `user-table.html`.

## Migration Plan

1. Update message property bundles (`messages.properties` and `messages_de.properties`).
2. Update `AdminWebController.java` sort normalization and sort order construction.
3. Update `user-table.html` fragment table headers, data rows, and empty state colspan.
4. Execute unit and WebMvc tests verifying sorting and template rendering.
5. Zero database migration or data modification required.
