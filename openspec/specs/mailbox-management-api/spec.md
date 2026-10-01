# Mailbox Management API Specification

## Purpose

Provides a token-authenticated RESTful API allowing programmatic creation, inspection, updating, and deletion of mailboxes and stored emails.

## Requirements

### Requirement: REST Mailbox Lifecycle Management
The system SHALL provide REST endpoints under `/api/v1/mailboxes` for mailbox creation, retrieval, updates, and deletion.

#### Scenario: Create Custom or Random Mailbox
- **WHEN** an authenticated user posts a valid mailbox payload with custom address or random address request
- **THEN** the system generates the mailbox, sets expiration and forwarding rules, and returns HTTP 201 with mailbox details
 
#### Scenario: List Mailboxes with Pagination
- **WHEN** an authenticated user sends a GET request to `/api/v1/mailboxes` with pagination and filter parameters
- **THEN** the system returns HTTP 200 containing a page of matching mailboxes belonging to the user

#### Scenario: Update Mailbox Settings
- **WHEN** an authenticated user issues a PUT request to update expiration date or forwarding status
- **THEN** the system updates the mailbox attributes and returns HTTP 200

#### Scenario: Delete Mailbox
- **WHEN** an authenticated user issues a DELETE request for their mailbox
- **THEN** the system removes the mailbox and its associated mails, returning HTTP 200

### Requirement: REST Mail Inspection and Attachment Download
The system SHALL provide REST endpoints under `/api/v1/mails` for querying received emails and downloading file attachments.

#### Scenario: List Mails for Mailbox
- **WHEN** an authenticated user sends a GET request to `/api/v1/mails` filtered by mailbox address
- **THEN** the system returns HTTP 200 with metadata of all matching received emails

#### Scenario: Retrieve Raw Mail and Attachments
- **WHEN** an authenticated user requests a specific email or attachment by identifier
- **THEN** the system streams the content with appropriate MIME content-type headers and returns HTTP 200
