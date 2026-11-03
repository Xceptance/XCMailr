# Tasks

## 1. Translation Bundles & Parity Unit Testing

- [x] 1.1 Add hierarchical dot-notation translation keys for layout, auth, profile, mailboxes, mails, admin, and errors to `xcmailr-webapp/src/main/resources/conf/messages.properties` (English) and `xcmailr-webapp/src/main/resources/conf/messages_de.properties` (German), preserving all legacy keys
- [x] 1.2 Implement `MessageBundleParityTest` unit test (using `final`, Javadocs, and code comments) verifying exact 1:1 key parity between English and German properties bundles, parameter placeholder alignment (`{0}`, `{1}`), and absence of empty translations; verify with `mvn test -pl xcmailr-webapp -Dtest=MessageBundleParityTest`

## 2. Common Layout & Authentication Views

- [x] 2.1 Update `templates/layout/base.html` to replace hardcoded strings with `#{layout.*}` and `#{common.*}` expressions for header navigation links, offcanvas drawer, user dropdown, logout action, theme switcher tooltips, alerts, and corporate footer
- [x] 2.2 Update `templates/auth/login.html`, `register.html`, `forgot-password.html`, and `reset-password.html` with `#{auth.*}` expressions for titles, labels, placeholders, password requirement lists, submit buttons, and status/redirect feedback banners
- [x] 2.3 Implement or update `AuthWebControllerTest` (using `final`, Javadocs, and code comments) verifying German and English localized HTML rendering across login, registration, password recovery, and status banners (`?error`, `?logout`, `?accountDeleted`, etc.); verify with `mvn test -pl xcmailr-webapp -Dtest=AuthWebControllerTest`

## 3. User Profile & Self-Service Views

- [x] 3.1 Update `templates/user/profile.html` with `#{profile.*}` expressions for personal info, password update, API token generation/revocation, danger zone warnings, and the account deletion confirmation modal
- [x] 3.2 Update `ProfileWebControllerTest` (using `final`, Javadocs, and code comments) asserting German and English localized HTML rendering for profile details, token management actions, and account deletion confirmation modal; verify with `mvn test -pl xcmailr-webapp -Dtest=ProfileWebControllerTest`

## 4. Mailbox Dashboard, Modals & Floating Batch Bar

- [x] 4.1 Update `templates/mailboxes/index.html` and `templates/mailboxes/fragments/mailbox-table.html` with `#{mailbox.*}` expressions for search placeholder, status filter dropdown options, table headers, sorting tooltips, status badges (`Aktiv`, `Abgelaufen`, `Unbegrenzt`), kebab menu actions, tooltips, deletion confirmation prompts, floating batch bar buttons, export links, and pagination buttons (`Zurück`, `Weiter`)
- [x] 4.2 Update `templates/mailboxes/fragments/modal-new.html` and `modal-edit.html` with `#{mailbox.modal.*}` expressions for modal titles, duration radio buttons, forwarding switch, and action buttons
- [x] 4.3 Update `MailboxWebControllerTest` (using `final`, Javadocs, and code comments) asserting German and English localized markup across mailbox list, search, status filters, and modal endpoints; verify with `mvn test -pl xcmailr-webapp -Dtest=MailboxWebControllerTest`

## 5. Unified Inbox & Email Viewer

- [x] 5.1 Update `templates/mailboxes/all-mails.html`, `mails.html`, `fragments/mail-list.html`, `fragments/all-mail-list.html`, `fragments/mail-detail.html`, and `mail-view.html` with `#{mail.*}` expressions for breadcrumbs, headers, refresh buttons, content viewer tabs (`HTML`, `Klartext`, `Header`), download actions, and empty selection placeholders
- [x] 5.2 Add integration/controller tests (using `final`, Javadocs, and code comments) asserting localized inbox views, message counts, and email detail previews in German and English; verify with `mvn test -pl xcmailr-webapp -Dtest=MailboxWebControllerTest`

## 6. Administration Area & Error Pages

- [x] 6.1 Update `templates/admin/users.html` (+ `user-table.html`), `admin/domains.html` (+ `domain-table.html`), `admin/transactions.html` (+ `transaction-table.html`), and `admin/statistics.html` with `#{admin.*}` expressions for administrative titles, search placeholders, action buttons, table columns, purge dialogs, and status summary badges
- [x] 6.2 Update `templates/error/404.html`, `templates/error/500.html`, and `templates/error.html` with `#{error.*}` expressions for error badges, titles, descriptions, and return-to-dashboard navigation buttons
- [x] 6.3 Update `AdminWebControllerTest` (using `final`, Javadocs, and code comments) asserting localized administrative views and error views in German and English; verify with `mvn test -pl xcmailr-webapp -Dtest=AdminWebControllerTest`

## 7. Full Integration & Security Verification

- [x] 7.1 Verify translation key fallback behavior to default English when a key is absent from the German bundle, asserting graceful rendering without runtime exceptions
- [x] 7.2 Run the full test suite via `mvn clean test` across the entire project to ensure 100% build and test success with zero regressions across all unit and integration test suites

