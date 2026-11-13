# Spec Delta

## ADDED Requirements

### Requirement: Comprehensive Web Console Internationalization
The system SHALL render all user-facing web console views, layouts, navigation menus, modals, form controls, status banners, table headers, and error pages dynamically using message bundles corresponding to the active user's locale (`en` or `de`), falling back to default English when a key is absent, while strictly escaping localized text to prevent Cross-Site Scripting (XSS).

#### Scenario: Layout and Navigation Localized Rendering (Happy Path)
- **WHEN** an authenticated user with German (`de`) language preference navigates to any application view
- **THEN** the header navigation links, offcanvas drawer titles and navigation items, user dropdown menu, theme switcher tooltips, and footer notices render in German
- **AND** when the user switches their language preference to English (`en`), all navigation and layout components render in English

#### Scenario: Authentication and Onboarding Localized Rendering (Happy Path)
- **WHEN** an unauthenticated visitor accesses `/login`, `/register`, `/forgot-password`, or `/reset-password` with a German request locale (`Accept-Language: de`)
- **THEN** form titles, field labels, placeholders, password requirement rules, submit buttons, and status/redirect feedback banners render in German

#### Scenario: User Profile and Account Management Localized Rendering (Happy Path)
- **WHEN** an authenticated user with German language preference navigates to `/profile`
- **THEN** personal information headers and labels, password change guidance, API token descriptions and action buttons, danger zone warnings, and the account deletion confirmation modal render in German

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
- **WHEN** a template attempts to resolve a translation key that is not present in `messages_de.properties`
- **THEN** the system falls back to the default message definition in `messages.properties` without throwing server exceptions or breaking template rendering

#### Scenario: OWASP Cross-Site Scripting Prevention in Localized Messages (Security Case)
- **WHEN** localized messages or message parameters containing HTML special characters (e.g., `<`, `>`, `&`, `"`) are rendered by Thymeleaf
- **THEN** standard XML/HTML escaping is applied automatically, preventing script injection or DOM manipulation attacks

