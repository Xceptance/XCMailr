# Spec Delta: web-console-ui

## MODIFIED Requirements

### Requirement: Unified Cross-Mailbox Inbox View
The system SHALL provide an interactive, unified inbox view at `/mails` displaying emails across all mailboxes owned by the authenticated user, indicating recipient mailbox addresses, providing HTMX-driven partial updates, ensuring strict tenant data isolation, and adapting responsively across viewport sizes.

#### Scenario: Full Page Load of Unified Inbox (Happy Path)
- **WHEN** an authenticated user navigates to `/mails`
- **THEN** the system renders a complete HTML page containing the list of received emails across all owned mailboxes, each showing received timestamp, sender, subject, and the recipient mailbox address badge
- **AND** the response body is non-empty and includes breadcrumb navigation and the message viewer container `#mail-viewer-pane`

#### Scenario: Asynchronous Refresh via HTMX (Happy Path)
- **WHEN** the user triggers an inbox refresh on the `/mails` page
- **THEN** HTMX requests `/mails` with header `HX-Request: true`
- **AND** the system responds with only the `mailList` fragment markup containing the updated email items, without the full page layout shell

#### Scenario: Email Row Selection and Detail Preview Loading (Happy Path)
- **WHEN** an authenticated user clicks an email row within the unified inbox message list
- **THEN** HTMX issues an asynchronous GET request for the email detail fragment (`/mailboxes/{mailboxId}/mails/{mailId}`)
- **AND** the system responds with the email preview fragment (`mailboxes/fragments/mail-detail :: detail`)
- **AND** HTMX swaps the detail markup into `#mail-viewer-pane` without reloading the page

#### Scenario: Multi-Mailbox Disambiguation (Happy Path)
- **WHEN** an authenticated user owns multiple distinct mailboxes and receives emails to each
- **THEN** the unified inbox renders every email with a badge explicitly reflecting the corresponding destination mailbox address

#### Scenario: Empty Inbox State (Edge Case)
- **WHEN** an authenticated user with zero received emails across all mailboxes accesses `/mails`
- **THEN** the system displays a clear, user-friendly empty state indicator stating that no messages have been received yet, rather than rendering an empty table or broken layout

#### Scenario: Resilient Rendering with Missing Mailbox Metadata (Edge Case)
- **WHEN** an email record exists whose associated mailbox ID is not present in `mailboxAddressMap`
- **THEN** the system renders the email row gracefully using a null-safe fallback label (such as `Unknown Mailbox`) without throwing a template evaluation exception

#### Scenario: Responsive Mobile Layout on Small Screens (Edge Case / UX)
- **WHEN** a user accesses the unified inbox view on a mobile viewport narrower than 768px
- **THEN** the message list and message preview pane stack vertically (`col-12`), secondary table elements collapse into compact sublines, and email selection targets maintain accessible touch padding (`>= 44px`) without horizontal clipping

#### Scenario: Unauthenticated Access Restriction (Security Case)
- **WHEN** an unauthenticated visitor attempts to access `/mails`
- **THEN** the system rejects the request and redirects the user to the `/login` page with HTTP 302 Found

#### Scenario: Cross-User Tenant Isolation (Security Case)
- **WHEN** User A and User B both own mailboxes with received emails in the system
- **AND** User A navigates to `/mails`
- **THEN** User A only sees emails destined for mailboxes owned by User A, and zero emails, subjects, or addresses belonging to User B are exposed in the HTML output or model

#### Scenario: Disallowed HTTP Methods (Error Case)
- **WHEN** a client submits a non-GET HTTP request (e.g. POST, PUT, DELETE) directly to `/mails`
- **THEN** the system rejects the request with HTTP 405 Method Not Allowed

### Requirement: Hypermedia Request Security and Asynchronous Event Handling
The system SHALL secure all asynchronous client hypermedia operations against cross-site request forgery by attaching valid CSRF tokens to mutating requests, enforcing strict token validation, suppressing automatic error swaps, and operating under standard explicit inheritance without unintended trigger leakage.

#### Scenario: Asynchronous Request With Valid CSRF Token
- **WHEN** an authenticated user triggers an asynchronous hypermedia mutation (POST, PUT, or DELETE) through the web console
- **THEN** the client event listener extracts the CSRF token from page metadata and attaches it to the request headers
- **AND** the server accepts the request and executes the corresponding action successfully

#### Scenario: Asynchronous Request With Missing CSRF Token
- **WHEN** an asynchronous hypermedia mutation request is transmitted to any protected endpoint without a CSRF token header
- **THEN** the server denies the request immediately with an HTTP 403 Forbidden response without modifying any application state

#### Scenario: Asynchronous Request With Invalid or Forged CSRF Token
- **WHEN** an asynchronous hypermedia mutation request is transmitted with an unrecognized, expired, or forged CSRF token value
- **THEN** the server rejects the request with an HTTP 403 Forbidden response and preserves all data untouched

#### Scenario: Asynchronous Error Response Handling
- **WHEN** an asynchronous hypermedia request receives a 4xx or 5xx error status code from the server
- **THEN** the client hypermedia engine suppresses automatic DOM replacement of target elements with unformatted error bodies, preserving existing UI context

#### Scenario: Standard Explicit Inheritance Without Trigger Leakage
- **WHEN** an asynchronous hypermedia interaction occurs on any view containing nested containers with event triggers (such as `mailListChanged from:body`)
- **THEN** the client hypermedia engine operates under standard explicit inheritance (`implicitInheritance` disabled)
- **AND** child interactive elements do not inadvertently inherit ancestor triggers, retaining their default or explicitly declared triggers (such as `click`)

## ADDED Requirements

### Requirement: Mailbox Email Inspection View
The system SHALL provide an interactive email inspection view for individual mailboxes at `/mailboxes/{id}/mails`, listing received messages and loading message previews asynchronously into `#mail-viewer-pane` upon row selection.

#### Scenario: Mailbox Email List Full Page Load (Happy Path)
- **WHEN** an authenticated user navigates to `/mailboxes/{id}/mails` for a mailbox they own
- **THEN** the system renders the email list table, breadcrumbs, search/filter controls, and the `#mail-viewer-pane` placeholder

#### Scenario: Mailbox Email Row Selection and Preview Loading (Happy Path)
- **WHEN** an authenticated user clicks an email row in the mailbox email list
- **THEN** HTMX issues an asynchronous GET request with `HX-Request: true` to `/mailboxes/{id}/mails/{mailId}`
- **AND** the system responds with the email preview fragment (`mailboxes/fragments/mail-detail :: detail`)
- **AND** HTMX swaps the preview markup into `#mail-viewer-pane` without reloading the page

