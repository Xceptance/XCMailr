# Proposal: Admin Users Sort Role and Status Alignment

## Why

In the admin user management console (`/admin/users`), sorting by Role or Status ascending currently places standard users (`false` / "User") before administrators (`true` / "Admin") and inactive users (`false` / "Inactive") before active accounts (`true` / "Active"). This occurs because database boolean columns evaluate `false (0) < true (1)` in standard SQL.

This behavior is counter-intuitive to administrators for several reasons:
1. **Alphabetical intuition**: In both English and German, "Admin" / "Administrator" (`A`) precedes "User" / "Benutzer" (`U` / `B`), and "Active" / "Aktiv" (`A`) precedes "Inactive" / "Inaktiv" (`I`).
2. **Privilege & hierarchy**: Administrators expect the elevated role (Admin) grouped at the top when sorting by Role ascending.
3. **Consistency across consoles**: The mailbox management console (`/mailboxes`) already displays Active mailboxes first on `status ASC` (because `MBox` stores `expired`, where `false` = Active). The user management table should provide the exact same user experience.

## What Changes

- **Invert Boolean Sort Direction for Role**: When sorting by `role` ascending (`dir=asc`), order `admin = true` (Admin) before `admin = false` (User). When sorting descending (`dir=desc`), order `admin = false` before `admin = true`.
- **Invert Boolean Sort Direction for Status**: When sorting by `status` ascending (`dir=asc`), order `active = true` (Active) before `active = false` (Inactive), aligning directly with the mailbox table. When sorting descending (`dir=desc`), order `active = false` before `active = true`.
- **Maintain Deterministic Tiebreaker**: Retain the secondary `mail ASC` (`ignoreCase()`) tiebreaker for both columns to guarantee stable pagination ordering.
- **Update Specifications**: Update the delta specification for `web-console-ui` reflecting the aligned sort order for Status and Role.
- **Update Tests**: Update unit tests in `AdminWebControllerTest.java` and web slice integration tests in `AdminUsersSortingWebTest.java` to validate the new ordering.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-console-ui`: Updates `Admin Management Console` scenarios for user table Role and Status column sorting to specify that `status ASC` groups Active users before Inactive users and `role ASC` groups Administrators before standard Users.

## Impact

- **Backend**: `AdminWebController.java` (`resolveSort` method).
- **Frontend / UI**: HTML templates remain unchanged (they already pass `sort` and `dir` dynamically).
- **Tests**: `AdminWebControllerTest.java` and `AdminUsersSortingWebTest.java`.
- **API / Contract**: No breaking URL changes; query parameters `sort=role`, `sort=status`, `dir=asc`, `dir=desc` remain identical.
