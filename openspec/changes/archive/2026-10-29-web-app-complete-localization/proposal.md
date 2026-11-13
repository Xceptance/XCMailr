# Proposal

## Why

While XCMailr's backend supports English and German locale resolution based on user profile preferences, the vast majority of web views, layout components, navigation menus, modals, and status alerts across the modern Thymeleaf/HTMX UI contain hardcoded English string literals. Consequently, when a user selects German (`de`) in their profile settings, only a small subset of elements (such as pagination ranges) switches to German, leaving the rest of the application in English. Full internationalization across all views is required to deliver a consistent, professional, bilingual experience.

## What Changes

- **Message Bundles**: Expand `conf/messages.properties` (English) and `conf/messages_de.properties` (German) with standardized, hierarchical dot-notation translation keys covering all application areas (`layout.*`, `auth.*`, `profile.*`, `mailbox.*`, `mail.*`, `admin.*`, `error.*`, `common.*`).
- **Layout & Common Fragments**: Replace hardcoded text in `layout/base.html` (navigation links, offcanvas drawer, user dropdown, logout action, theme switcher tooltips, alerts, footer) with Thymeleaf message expressions (`#{...}`).
- **Authentication Views**: Localize `auth/login.html`, `auth/register.html`, `auth/forgot-password.html`, and `auth/reset-password.html`, including titles, input labels, placeholders, password requirement lists, buttons, and all status/redirect banners.
- **User Profile View**: Localize `user/profile.html`, covering personal info, password updates, REST API token generation and actions, danger zone warnings, and the account deletion confirmation modal.
- **Mailbox Dashboard & Modals**: Localize `mailboxes/index.html`, `mailboxes/fragments/mailbox-table.html`, `modal-new.html`, and `modal-edit.html`, including search placeholders, status filters, table headers, status pills, kebab actions, tooltips, confirmation prompts, batch action bar controls, and export links.
- **Unified Inbox & Email Viewer**: Localize `mailboxes/all-mails.html`, `mailboxes/mails.html`, `mailboxes/fragments/mail-list.html`, `mailboxes/fragments/all-mail-list.html`, `mailboxes/fragments/mail-detail.html`, and `mailboxes/mail-view.html` (breadcrumbs, headers, action buttons, content tabs, and empty state placeholders).
- **Administration Views**: Localize `admin/users.html` (and `user-table.html`), `admin/domains.html` (and `domain-table.html`), `admin/transactions.html` (and `transaction-table.html`), and `admin/statistics.html`.
- **Error Pages**: Localize `error/404.html`, `error/500.html`, and `error.html`.
- **Testing Coverage**: Add comprehensive tests across the testing pyramid (unit tests for message bundle parity and completeness, controller/integration tests verifying rendered localized content for German and English across happy paths, error cases, and special cases).
- **Code Standards**: Maintain Java/template best practices with `final` wherever possible, Javadoc, and OWASP-compliant escaping.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `web-console-ui`: Adds comprehensive internationalization requirements for all web console views and fragments, requiring that all user-facing labels, titles, table headers, buttons, placeholders, confirmation dialogs, status badges, and flash messages render dynamically according to the active locale (English or German).

## Impact

- **Templates**: All Thymeleaf templates under `xcmailr-webapp/src/main/resources/templates/`.
- **Message Bundles**: `xcmailr-webapp/src/main/resources/conf/messages.properties` and `xcmailr-webapp/src/main/resources/conf/messages_de.properties`.
- **Controllers & View Models**: Ensure controllers pass necessary parameters or rely on Spring's active `Locale` for any server-formatted message strings.
- **Backward Compatibility**: Fully backward-compatible; existing legacy message keys will be preserved to prevent regressions.

