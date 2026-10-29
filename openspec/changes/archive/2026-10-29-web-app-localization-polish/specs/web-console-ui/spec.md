# Spec Delta

## MODIFIED Requirements

### Requirement: Comprehensive Web Console Internationalization
The system SHALL render all user-facing web console views, layouts, navigation menus, modals, form controls, placeholders, tooltips, ARIA accessibility landmarks, status banners, validation error messages, table headers, and error pages dynamically using message bundles corresponding to the active user's locale (`en` or `de`), falling back to default English when a key is absent, while strictly escaping localized text to prevent Cross-Site Scripting (XSS).

#### Scenario: Layout and Navigation Localized Rendering (Happy Path)
- **WHEN** an authenticated user with German (`de`) language preference navigates to any application view
- **THEN** the header navigation links, offcanvas drawer titles and navigation items, user dropdown menu, theme switcher tooltips, and footer notices render in German
- **AND** when the user switches their language preference to English (`en`), all navigation and layout components render in English

#### Scenario: Authentication and Onboarding Localized Rendering (Happy Path)
- **WHEN** an unauthenticated visitor accesses `/login`, `/register`, `/forgot-password`, or `/reset-password` with a German request locale (`Accept-Language: de`)
- **THEN** form titles, field labels, input placeholders (including forename, surname, target email, password min-length guidance, and password confirmation), password requirement rules, submit buttons, and status/redirect feedback banners render in German

#### Scenario: User Profile and Account Management Localized Rendering (Happy Path)
- **WHEN** an authenticated user with German language preference navigates to `/profile`
- **THEN** personal information headers and labels, password change guidance and input placeholders (such as `"Mindestens 10 Zeichen"` and `"Neues Passwort wiederholen"`), API token descriptions, creation date fallbacks (`"Kürzlich"`), action buttons, danger zone warnings, and the account deletion confirmation modal render in German

#### Scenario: Server-Side Form Validation Error Localization (Error Case)
- **WHEN** a user submits an authentication or profile form with validation errors (such as password mismatch, incorrect current password, duplicate registration email, or sole administrator account deletion attempt) under German locale
- **THEN** the controller resolves and displays the corresponding localized error banner (`errorMessage`) in German matching the user's active locale

#### Scenario: Password Quality Validation Error Localization (Error Case)
- **WHEN** a user submits a candidate password failing OWASP/NIST validation rules (such as minimum length violation, maximum length violation, single repeated character, or identity term inclusion) under German locale
- **THEN** the validation result message is resolved and displayed in German matching the specific rule violated

#### Scenario: Accessibility and Tooltip Localization (Special Case)
- **WHEN** an authenticated or unauthenticated user navigates web views containing input pattern validation tooltips or screen reader landmarks under German locale
- **THEN** input pattern titles (`th:title`) and navigation/toolbar landmarks (`th:aria-label` on pagination and batch action controls) render localized German descriptions

#### Scenario: Mailbox Dashboard and Modal Localized Rendering (Happy Path)
- **WHEN** an authenticated user with German language preference navigates to `/mailboxes`
- **THEN** the mailbox search placeholder, status filter dropdown options, table headers, sorting titles, status badges (`Aktiv`, `Abgelaufen`, `Unbegrenzt`), kebab action options, pagination buttons (`Zurück`, `Weiter`), deletion confirmation prompts, and floating batch bar action buttons render in German

#### Scenario: Unified Inbox and Email Detail Localized Rendering (Happy Path)
- **WHEN** an authenticated user with German language preference navigates to `/mails` or `/mailboxes/{id}/mails`
- **THEN** breadcrumbs, inbox total counters, refresh buttons, content viewer tabs (`HTML`, `Klartext`, `Header`), download action buttons, and empty selection placeholders render in German

#### Scenario: Administration Views Localized Rendering (Happy Path)
- **WHEN** an authenticated administrator with German language preference accesses `/admin/users`, `/admin/domains`, `/admin/transactions`, or `/admin/statistics`
- **THEN** administrative page titles, search placeholders, action buttons (e.g. `Aktivieren`, `Deaktivieren`, `Zum Admin ernennen`), status overview pills, audit table headers, and log purge dialogs render in German

#### Scenario: Error Pages Localized Rendering (Happy Path)
- **WHEN** a client encountering an HTTP 404 or HTTP 500 error view has German language preference
- **THEN** the error title, explanation message, and dashboard navigation buttons render in German

#### Scenario: Translation Key Missing Fallback (Special / Edge Case)
- **WHEN** a template or controller attempts to resolve a translation key that is not present in `messages_de.properties`
- **THEN** the system falls back to the default message definition in `messages.properties` without throwing server exceptions or breaking template rendering

#### Scenario: OWASP Cross-Site Scripting Prevention in Localized Messages (Security Case)
- **WHEN** localized messages or message parameters containing HTML special characters (e.g., `<`, `>`, `&`, `"`) are rendered by Thymeleaf
- **THEN** standard XML/HTML escaping is applied automatically, preventing script injection or DOM manipulation attacks

