# Spec Delta

## MODIFIED Requirements

### Requirement: Mailbox Email Inspection View
The system SHALL provide an interactive email inspection view for individual mailboxes at `/mailboxes/{id}/mails`, listing received messages and loading message previews asynchronously into `#mail-viewer-pane` upon row selection, rendering UTF-8 plain text and sanitized HTML bodies accurately without mojibake regardless of whether Content-Type headers or charset parameters are present.

#### Scenario: Mailbox Email List Full Page Load (Happy Path)
- **WHEN** an authenticated user navigates to `/mailboxes/{id}/mails` for a mailbox they own
- **THEN** the system renders the email list table, breadcrumbs, search/filter controls, and the `#mail-viewer-pane` placeholder

#### Scenario: Mailbox Email Row Selection and Preview Loading (Happy Path)
- **WHEN** an authenticated user clicks an email row in the mailbox email list
- **THEN** HTMX issues an asynchronous GET request with `HX-Request: true` to `/mailboxes/{id}/mails/{mailId}`
- **AND** the system responds with the email preview fragment (`mailboxes/fragments/mail-detail :: detail`)
- **AND** HTMX swaps the preview markup into `#mail-viewer-pane` without reloading the page

#### Scenario: Email Preview Rendering with Untyped UTF-8 Body
- **WHEN** an authenticated user loads the preview of an email whose plain text or HTML body was transmitted as raw UTF-8 without a Content-Type header or without an explicit charset parameter
- **THEN** the plain text and HTML preview panes display the text accurately with correct Unicode characters and without mojibake (e.g. displaying `üüü` rather than `Ã¼Ã¼Ã¼`)

