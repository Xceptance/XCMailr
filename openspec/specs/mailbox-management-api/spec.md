# Mailbox Management API Specification

## Purpose

Provides a token-authenticated RESTful API allowing programmatic creation, inspection, updating, and deletion of mailboxes and stored emails.

## Requirements

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

### Requirement: REST Mail Inspection and Attachment Download
The system SHALL provide REST endpoints under `/api/v1/mails` for querying received emails and downloading file attachments, validating any client-supplied regular expression query filters against length, structural ReDoS, and syntax constraints.

#### Scenario: List Mails for Mailbox
- **WHEN** an authenticated user sends a GET request to `/api/v1/mails` filtered by mailbox address
- **THEN** the system returns HTTP 200 with metadata of all matching received emails

#### Scenario: Retrieve Raw Mail and Attachments
- **WHEN** an authenticated user requests a specific email or attachment by identifier
- **THEN** the system streams the content with appropriate MIME content-type headers and returns HTTP 200

#### Scenario: Filter Mails with Valid Regex Pattern
- **WHEN** an authenticated user supplies valid regular expression filter parameters (such as `from`, `subject`, `textContent`, `htmlContent`, or `mailHeader`) within 255 characters
- **THEN** the system compiles the expressions, matches them against the mailbox's stored emails, and returns HTTP 200 with matching emails

#### Scenario: List Mails with Blank or Omitted Regex Filters
- **WHEN** an authenticated user queries emails with null, empty, or whitespace-only regex query parameters
- **THEN** the system ignores the blank filters without compiling regular expressions and returns all mailbox emails with HTTP 200

#### Scenario: Reject Regex Exceeding Maximum Length Limit
- **WHEN** an authenticated user supplies any regex filter parameter exceeding 255 characters
- **THEN** the system rejects the request immediately with HTTP 400 Bad Request and an error identifying the invalid parameter

#### Scenario: Reject Dangerous Catastrophic Backtracking Regex Pattern
- **WHEN** an authenticated user supplies a regex filter containing nested repetition constructs (such as `(a+)+`, `(x*)*`, or `([0-9]+)+`)
- **THEN** the system aborts processing before compilation using a linear-time scanner and returns HTTP 400 Bad Request indicating a potential ReDoS pattern

#### Scenario: Linear-Time Evaluation of Unclosed Parentheses Pattern
- **WHEN** an authenticated user supplies a regex filter containing repeated unclosed parentheses (e.g. `((((((...`)
- **THEN** the system validates the string in deterministic linear time $\mathcal{O}(n)$ without polynomial regex backtracking and rejects it with HTTP 400 Bad Request

#### Scenario: Reject Syntax Invalid Regex Pattern
- **WHEN** an authenticated user supplies a syntactically invalid regular expression (such as unclosed brackets or mismatched parentheses)
- **THEN** the system catches the syntax violation and returns HTTP 400 Bad Request identifying the parameter and error message

#### Scenario: Filter Mails with Maximum Length Valid Regex Pattern
- **WHEN** an authenticated user supplies a safe, syntactically valid regular expression filter of exactly 255 characters
- **THEN** the system accepts the filter, compiles it successfully, and returns matching emails with HTTP 200
