# Spec Delta

## MODIFIED Requirements

### Requirement: Unified Cross-Mailbox Inbox View
The system SHALL provide an interactive, paginated unified inbox view at `/mails` displaying emails across all mailboxes owned by the authenticated user using lightweight metadata projections that exclude raw message BLOB payloads from memory, indicating recipient mailbox addresses, providing HTMX-driven partial updates and page navigation, ensuring strict tenant data isolation so users cannot view emails belonging to other users, preventing SQL injection on all input vectors, and adapting responsively across viewport sizes.

#### Scenario: Full Page Load of Unified Inbox (Happy Path)
- **WHEN** an authenticated user navigates to `/mails`
- **THEN** the system renders a complete HTML page containing the paginated list of received emails across all owned mailboxes, each showing received timestamp, sender, subject, and the recipient mailbox address badge
- **AND** the response body is non-empty and includes breadcrumb navigation, pagination controls, and the message viewer container `#mail-viewer-pane`

#### Scenario: Paginated Navigation via HTMX (Happy Path)
- **WHEN** an authenticated user clicks a pagination link (such as "Next" or a specific page number) on `/mails`
- **THEN** HTMX requests `/mails` with parameters `page` and `size` and header `HX-Request: true`
- **AND** the system responds with only the updated `mailList` fragment markup containing the requested page of emails and refreshed pagination controls without reloading the entire page

#### Scenario: Projection-Only Fetching (Memory Protection)
- **WHEN** the system queries emails for the unified inbox list
- **THEN** the persistence layer executes a projection query selecting only `id`, `mailbox`, `sender`, `subject`, and `receiveTime`
- **AND** the full raw email MIME payload (`message` BLOB column) is excluded from the query, preventing excessive heap allocation and dirty-checking snapshot cloning

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

#### Scenario: Out of Range Page Index Handling (Edge Case)
- **WHEN** a user or client requests `/mails` with a negative page number (e.g. `page=-1`)
- **THEN** the system normalizes the page parameter to 0 and renders the first page of results
- **WHEN** a client requests a page number beyond the total number of available pages
- **THEN** the system returns an empty result page with consistent pagination controls without throwing an exception

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
- **THEN** the system resolves only mailboxes owned by User A and executes the query strictly constrained to User A's mailbox IDs
- **AND** User A sees only emails destined for mailboxes owned by User A, and zero emails, subjects, senders, or IDs belonging to User B are exposed in the HTML output, model, or database query results

#### Scenario: SQL Injection Prevention in Pagination Parameters (Security Case)
- **WHEN** a client submits a request to `/mails` with SQL injection payloads in the `page` or `size` query parameters (e.g. `page=1' OR '1'='1`, `size=10; DROP TABLE mail;--`)
- **THEN** Spring MVC type conversion rejects the non-numeric parameter and returns HTTP 400 Bad Request before invoking controller logic or database queries
- **AND** when a client passes arbitrary sort parameters attempting SQL injection (e.g. `sort=receiveTime; DROP TABLE mail`), the sort parameter is ignored because sorting is statically defined at compile time within the controller

#### Scenario: Disallowed HTTP Methods (Error Case)
- **WHEN** a client submits a non-GET HTTP request (e.g. POST, PUT, DELETE) directly to `/mails`
- **THEN** the system rejects the request with HTTP 405 Method Not Allowed

### Requirement: Mailbox Email Inspection View
The system SHALL provide an interactive, paginated email inspection view for individual mailboxes at `/mailboxes/{id}/mails`, listing received messages using lightweight metadata projections that exclude raw message BLOB payloads, providing HTMX pagination navigation, enforcing tenant authorization so users cannot inspect mailboxes belonging to other users, preventing SQL injection on path variables and query parameters, and loading message previews asynchronously into `#mail-viewer-pane` upon row selection, rendering UTF-8 plain text and sanitized HTML bodies accurately without mojibake regardless of whether Content-Type headers or charset parameters are present.

#### Scenario: Mailbox Email List Full Page Load (Happy Path)
- **WHEN** an authenticated user navigates to `/mailboxes/{id}/mails` for a mailbox they own
- **THEN** the system renders the paginated email list table, pagination controls, breadcrumbs, search/filter controls, and the `#mail-viewer-pane` placeholder

#### Scenario: Mailbox Email List Pagination Navigation (Happy Path)
- **WHEN** an authenticated user clicks a pagination control within `/mailboxes/{id}/mails`
- **THEN** HTMX requests `/mailboxes/{id}/mails` with parameters `page` and `size` and header `HX-Request: true`
- **AND** the system responds with only the updated `mailList` fragment markup containing the requested page of emails and refreshed pagination controls without reloading the entire page

#### Scenario: Mailbox Email Row Selection and Preview Loading (Happy Path)
- **WHEN** an authenticated user clicks an email row in the mailbox email list
- **THEN** HTMX issues an asynchronous GET request with `HX-Request: true` to `/mailboxes/{id}/mails/{mailId}`
- **AND** the system responds with the email preview fragment (`mailboxes/fragments/mail-detail :: detail`)
- **AND** HTMX swaps the preview markup into `#mail-viewer-pane` without reloading the page

#### Scenario: Email Preview Rendering with Untyped UTF-8 Body
- **WHEN** an authenticated user loads the preview of an email whose plain text or HTML body was transmitted as raw UTF-8 without a Content-Type header or without an explicit charset parameter
- **THEN** the plain text and HTML preview panes display the text accurately with correct Unicode characters and without mojibake (e.g. displaying `üüü` rather than `Ã¼Ã¼Ã¼`)

#### Scenario: Empty Mailbox State (Edge Case)
- **WHEN** an authenticated user navigates to `/mailboxes/{id}/mails` for a mailbox containing zero received messages
- **THEN** the system renders an empty state notice indicating no messages exist for this mailbox, preserving table headers and action controls

#### Scenario: Out of Range Page Index Handling for Mailbox (Edge Case)
- **WHEN** a client requests `/mailboxes/{id}/mails` with a page number below 0 or beyond the total available pages
- **THEN** negative page numbers are normalized to 0, and out-of-range page requests return an empty page with valid pagination controls without throwing server exceptions

#### Scenario: Unauthorized Access to Other Users Mailbox (Security Case)
- **WHEN** an authenticated user attempts to access `/mailboxes/{id}/mails` for a mailbox owned by another user
- **THEN** the system rejects the request with HTTP 403 Forbidden, preserving tenant data isolation and preventing inspection of other users' emails

#### Scenario: Non-Existent Mailbox Access (Security Case)
- **WHEN** an authenticated user attempts to access `/mailboxes/{id}/mails` for a non-existent mailbox ID
- **THEN** the system rejects the request with HTTP 404 Not Found

#### Scenario: SQL Injection Prevention in Mailbox Endpoint (Security Case)
- **WHEN** a client submits a request to `/mailboxes/{id}/mails` with SQL injection payloads in the `id` path variable (e.g. `/mailboxes/1' OR '1'='1/mails`)
- **THEN** Spring MVC type conversion rejects the non-numeric path variable with HTTP 400 Bad Request before any database query is formulated
