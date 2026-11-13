# Spec Delta

## ADDED Requirements

### Requirement: REST Mailbox Comment Support
The system SHALL support an optional `comment` field of up to 255 characters on mailbox creation and update operations under `/api/v1/mailboxes`, returning the comment in all mailbox response payloads and preserving full backward compatibility when the comment is omitted.

#### Scenario: Create Mailbox with Comment and Special Characters (Happy Path)
- **WHEN** an authenticated user issues a POST request to `/api/v1/mailboxes` with a valid payload containing a comment with UTF-8 characters and German umlauts (e.g. `Projekt Überprüfung & Tests`)
- **THEN** the system persists the comment exactly as provided and returns HTTP 201 with the comment included in the response body

#### Scenario: Create Mailbox with Omitted Comment Preserves Backward Compatibility (Happy Path)
- **WHEN** an authenticated user or existing API client issues a POST request to `/api/v1/mailboxes` without a `comment` field in the JSON payload
- **THEN** the system creates the mailbox successfully with a null comment and returns HTTP 201 with `comment: null` or omitted, preserving existing API behavior

#### Scenario: Update Mailbox Comment (Happy Path)
- **WHEN** an authenticated user issues a PUT request to `/api/v1/mailboxes/{mailboxAddress}` providing a modified comment string
- **THEN** the system updates the stored mailbox comment and returns HTTP 200 with the updated comment

#### Scenario: Clear Mailbox Comment (Happy Path)
- **WHEN** an authenticated user issues a PUT request to `/api/v1/mailboxes/{mailboxAddress}` with an empty or null comment
- **THEN** the system clears the stored comment and returns HTTP 200 with `comment: null`

#### Scenario: Reject Comment Exceeding Maximum Length (Error Case)
- **WHEN** an authenticated user issues a POST or PUT request with a comment exceeding 255 characters
- **THEN** the system rejects the request with HTTP 400 Bad Request indicating the comment exceeds the maximum length constraint

#### Scenario: SQL Injection Prevention in REST Comment Payloads (Special Case)
- **WHEN** an authenticated user supplies a comment containing SQL control characters or injection payloads (such as `' OR 1=1;--` or `'; DROP TABLE mailboxes;--`) in a REST request
- **THEN** the system persists the comment strictly as parameterized data via JPA prepared statements without evaluating SQL commands or modifying query logic

