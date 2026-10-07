# Security Hardening Specification

## Purpose

Enforces OWASP Top 10 security standards across web, API, and SMTP layers to eliminate vulnerabilities such as XSS, CSRF, header injection, and broken access control.

## Requirements

### Requirement: Email Body HTML Sanitization
The system SHALL sanitize raw email HTML content before rendering in the web console to eliminate Cross-Site Scripting (XSS) vectors.

#### Scenario: Stripping Malicious Script Tags
- **WHEN** a received email containing embedded `<script>`, `<iframe>`, or javascript URI payloads is viewed in the webmail viewer
- **THEN** all executable elements are stripped and safe HTML markup is rendered

### Requirement: CSRF Protection for Dynamic HTMX Requests
The system SHALL require valid Anti-CSRF tokens on all state-changing HTTP requests, including HTMX AJAX calls.

#### Scenario: Valid CSRF on HTMX Post
- **WHEN** an HTMX request includes the CSRF header matching the active session token
- **THEN** the server processes the request and responds with the requested fragment

#### Scenario: Rejected Missing CSRF Token
- **WHEN** an untrusted state-changing request is submitted without a valid CSRF token
- **THEN** the server aborts processing and responds with HTTP 403 Forbidden

### Requirement: Header Injection and Anti-Relay Safeguards
The system SHALL sanitize all email headers against CRLF injection and strictly isolate user resource ownership.

#### Scenario: CRLF Header Sanitization
- **WHEN** input fields intended for email subject, recipient, or sender contain carriage return or newline characters
- **THEN** CRLF characters are removed or rejected before composing outbound emails

#### Scenario: Cross-User Resource Access Denial
- **WHEN** an authenticated user attempts to access or modify a mailbox or email owned by another user
- **THEN** the system denies access with HTTP 403 Forbidden or HTTP 404 Not Found

### Requirement: Non-Destructive Administrative Account Bootstrapping and Recovery
The system SHALL bootstrap configured administrator credentials only when valid credentials are provided and the account does not exist in the database, and SHALL strictly preserve existing user credentials and permissions without alteration during startup.

#### Scenario: Missing or Blank Credentials Safeguard
- **WHEN** the application starts up and no administrator address or password is provided in configuration or environment variables
- **THEN** the system skips administrative bootstrapping immediately without querying the database or performing any entity mutations

#### Scenario: Fresh Database Bootstrap
- **WHEN** the application starts up with valid administrator credentials configured and the administrator account does not exist in the database
- **THEN** the system creates the administrator account with active status and admin privileges, and seeds the default domain if absent

#### Scenario: Existing Administrative Account Preservation
- **WHEN** the application starts up with administrator credentials configured and an account with that administrator address already exists in the database
- **THEN** the system leaves the account completely unmodified, preserving all existing passwords, active states, and permissions

#### Scenario: Disaster Recovery for Deleted Administrator
- **WHEN** an administrator account was previously removed from the database and the application restarts with valid administrator credentials configured
- **THEN** the system detects the absence of the account and safely recreates it with administrator privileges

### Requirement: Account Enumeration Prevention and Inactive Account Handling
The system SHALL verify credentials prior to disclosing account activation status and SHALL NOT disclose sensitive bearer tokens in logs or standard output.

#### Scenario: Generic Authentication Rejection for Invalid Credentials
- **WHEN** an unauthenticated requester submits invalid credentials or a non-existent email address
- **THEN** the system rejects authentication with a generic invalid credentials response without disclosing account existence

#### Scenario: Inactive Account Notification on Valid Credentials
- **WHEN** a user submits valid email and password credentials for an inactive or unconfirmed account
- **THEN** the system rejects session creation and explicitly instructs the user that their account requires activation

#### Scenario: Inactive Account Wrong Password Anti-Enumeration
- **WHEN** an unauthenticated requester submits an invalid password for an unconfirmed or inactive account
- **THEN** the system returns the identical generic invalid credentials error, strictly concealing that the account exists or is unactivated

#### Scenario: Bearer Token Redaction in Logs
- **WHEN** account confirmation tokens or password reset tokens are generated and processed
- **THEN** the system dispatches the token exclusively via email without printing the token or full verification URL in application logs
