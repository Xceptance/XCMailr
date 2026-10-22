# Spec Delta: web-console-ui

## ADDED Requirements

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
