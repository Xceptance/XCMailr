# Tasks

## 1. Fix Mailbox Table Row Actions and Event Reload

- [x] 1.1 Update the "Delete Mailbox" action button in `templates/mailboxes/fragments/mailbox-table.html` by removing `hx-target='#mailbox-table-container'` and configuring `hx-swap='none'`, verifying the template markup compiles cleanly
- [x] 1.2 Update the remaining row mutation buttons (desktop toggle, +24h extend, mobile toggle, mobile extend, reset counters) in `templates/mailboxes/fragments/mailbox-table.html` to remove `hx-target='#mailbox-table-container'` and set `hx-swap='none'`
- [x] 1.3 Add `hx-include="[name='search'], #statusFilterSelect"` to `#mailbox-table-container` in `templates/mailboxes/fragments/mailbox-table.html` so active search query and status filter selections are preserved when the table refreshes upon `mailboxChanged`

## 2. Harmonize Admin Management Tables

- [x] 2.1 Update `templates/admin/fragments/user-table.html` so `#user-table-container` listens to `userListChanged from:body` via `hx-get="/admin/users"` and user toggle/delete actions use `hx-swap='none'` without targeting the container
- [x] 2.2 Update `templates/admin/fragments/domain-table.html` so `#domain-table-container` listens to `domainListChanged from:body` via `hx-get="/admin/domains"` and domain remove actions use `hx-swap='none'` without targeting the container

## 3. Verification and Test Suite

- [x] 3.1 Run `MailboxWebControllerTest` to verify that mailbox deletion, toggling, extension, and counter reset endpoints respond with HTTP 200 OK and `HX-Trigger: mailboxChanged`
- [x] 3.2 Run `AdminWebControllerTest` to verify that admin user delete/toggle and domain delete endpoints respond with HTTP 200 OK and appropriate `HX-Trigger` headers
- [x] 3.3 Execute the web controller test suite via Maven (`mvn test -Dtest=MailboxWebControllerTest,AdminWebControllerTest,MailWebControllerTest`) to confirm all web layer tests pass without regressions

