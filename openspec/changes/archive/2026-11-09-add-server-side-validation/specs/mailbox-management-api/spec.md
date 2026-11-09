# Mailbox Management API Specification Delta

## MODIFIED Requirements

### Requirement: REST Mailbox Lifecycle Management
The system SHALL provide REST endpoints under `/api/v1/mailboxes` for mailbox creation, retrieval, updates, and deletion, treating a `deactivationTime` of 0 or omitted as a permanent, non-expiring mailbox.

#### Scenario: Create Custom or Random Mailbox
- **WHEN** an authenticated user posts a valid mailbox payload with custom address or random address request and a future deactivation timestamp
- **THEN** the system generates the mailbox, sets expiration timestamp and forwarding rules, marks the mailbox active (`expired = false`), and returns HTTP 201 with mailbox details

#### Scenario: Create Permanent Mailbox with Omitted or Zero Deactivation Time
- **WHEN** an authenticated user posts a mailbox creation request with `deactivationTime` set to `0` or omitted
- **THEN** the system creates an active mailbox with `ts_Active = 0` and `expired = false` that does not expire automatically

#### Scenario: List Mailboxes with Pagination
- **WHEN** an authenticated user sends a GET request to `/api/v1/mailboxes` with pagination and filter parameters
- **THEN** the system returns HTTP 200 containing a page of matching mailboxes belonging to the user

#### Scenario: Update Mailbox Settings
- **WHEN** an authenticated user issues a PUT request to update expiration date or forwarding status
- **THEN** the system updates the mailbox attributes and returns HTTP 200

#### Scenario: Update Mailbox to Permanent Status with Zero Deactivation Time
- **WHEN** an authenticated user updates a mailbox setting `deactivationTime` to `0`
- **THEN** the system sets `ts_Active` to `0`, clears expired status (`expired = false`), and returns HTTP 200

#### Scenario: Delete Mailbox
- **WHEN** an authenticated user issues a DELETE request for their mailbox
- **THEN** the system removes the mailbox and its associated mails, returning HTTP 200

#### Scenario: Server-Side Validation Rejection of Malformed Email Address (Error Case)
- **WHEN** an authenticated client sends a POST or PUT request to `/api/v1/mailboxes` with a missing, blank, or malformed email address in the request body
- **THEN** the server-side bean validator rejects the request immediately and returns HTTP 400 Bad Request with an `ApiErrors` object identifying the `address` field

#### Scenario: Server-Side Validation Rejection of Comment Exceeding 255 Characters (Boundary / Error Case)
- **WHEN** an authenticated client sends a POST or PUT request to `/api/v1/mailboxes` with a comment exceeding 255 characters
- **THEN** the server-side bean validator rejects the request and returns HTTP 400 Bad Request with an `ApiErrors` object identifying the `comment` field

#### Scenario: Server-Side Validation Rejection of Disallowed Domain (Error Case)
- **WHEN** an authenticated client sends a POST or PUT request to `/api/v1/mailboxes` specifying a domain that is not present in the domain whitelist
- **THEN** the system rejects the request with HTTP 403 Forbidden and an `ApiErrors` object identifying the `address` or `mailboxAddress` field
