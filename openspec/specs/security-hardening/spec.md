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
The system SHALL require valid Anti-CSRF tokens on all state-changing web and session-based HTTP requests including HTMX AJAX calls, while explicitly exempting stateless Bearer-authenticated API endpoints from CSRF token verification.

#### Scenario: Valid CSRF on HTMX Post
- **WHEN** an HTMX request includes the CSRF header matching the active session token
- **THEN** the server processes the request and responds with the requested fragment

#### Scenario: Rejected Missing CSRF Token
- **WHEN** an untrusted state-changing request is submitted without a valid CSRF token
- **THEN** the server aborts processing and responds with HTTP 403 Forbidden

#### Scenario: Stateless Bearer API Mutating Requests Exempt from CSRF
- **WHEN** a client submits a state-changing request (POST, PUT, DELETE, PATCH) to an endpoint matching `/api/**` with a valid Bearer API token
- **THEN** the server processes the request without requiring an Anti-CSRF token or header

#### Scenario: Rejected Invalid or Tampered CSRF Token on Web Requests
- **WHEN** a state-changing web request is submitted with an invalid, expired, or tampered CSRF token
- **THEN** the server aborts processing and responds with HTTP 403 Forbidden

#### Scenario: Mutating API Requests Reject Missing or Invalid Authentication
- **WHEN** a client submits a state-changing request (POST, PUT, DELETE) to `/api/**` without a Bearer token or with an invalid Bearer token
- **THEN** the server rejects the request with HTTP 401 Unauthorized regardless of CSRF exemption

#### Scenario: API Endpoints Disregard Spurious or Malformed CSRF Tokens
- **WHEN** a client submits a state-changing request to `/api/**` with a valid Bearer token and includes an invalid, empty, or expired CSRF token header or parameter
- **THEN** the server disregards the CSRF token entirely, evaluates only the Bearer token, and allows execution

#### Scenario: Non-API Endpoint Path Boundaries Strictly Enforce CSRF
- **WHEN** a client submits a state-changing request without a CSRF token to a path outside `/api/**` (such as `/api-docs`, `/login`, or `/confirm`)
- **THEN** the server does not apply the `/api/**` CSRF exemption and rejects the request with HTTP 403 Forbidden

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

### Requirement: Secure Embedded Servlet Container Baseline
The system SHALL bundle embedded servlet container dependencies free from known critical authentication bypass and authorization security vulnerabilities.

#### Scenario: Remediated container authentication and authorization vulnerabilities
- **WHEN** the embedded servlet container dependencies are analyzed for security vulnerabilities
- **THEN** all bundled Apache Tomcat embedded libraries are at version 11.0.25 or higher, remediating CVE-2026-68525, CVE-2026-43512, and CVE-2026-65182

### Requirement: OWASP Password Policy for Credential Creation
The system SHALL enforce modern OWASP / NIST SP 800-63B password policy rules exclusively upon credential creation and modification (user registration, password reset, and profile password changes) while strictly preserving authentication validity for existing credentials in the database.

#### Scenario: Successful Registration with Compliant Password
- **WHEN** a user registers with a password between 10 and 72 characters (and at most 72 bytes) that does not contain their name or email and is not a single repeated character
- **THEN** the system accepts the password, hashes it using the configured password encoder, and proceeds with account creation

#### Scenario: Rejected Password Shorter Than 10 Characters
- **WHEN** a user attempts to register, reset their password, or change their password with a password under 10 characters in length
- **THEN** the system rejects the submission with a clear error message stating that the password must be at least 10 characters long

#### Scenario: Rejected Password Exceeding 128 Characters
- **WHEN** a user submits a password longer than 128 characters (or 128 characters)
- **THEN** the system rejects the submission with an error message indicating the password cannot exceed 72 characters and returns an HTTP 200 form view rather than an HTTP 500 server error

#### Scenario: Rejected Password Exceeding 72 Characters
- **WHEN** a user submits a password longer than 72 characters (such as 73 characters)
- **THEN** the system rejects the submission with an error message indicating the password cannot exceed 72 characters without throwing a server error

#### Scenario: Rejected Password Exceeding 72 Bytes in UTF-8
- **WHEN** a user submits a password of 72 or fewer characters whose UTF-8 encoding exceeds 72 bytes (such as strings containing multiple multibyte Unicode characters or emojis)
- **THEN** the system rejects the submission with an actionable error message indicating the password cannot exceed 72 bytes

#### Scenario: Rejected Password Containing User Context
- **WHEN** a user submits a password that contains their forename, surname, or email username (local part before @) case-insensitively for terms of 3 or more characters
- **THEN** the system rejects the submission with a clear error message indicating that the password cannot contain personal identity information

#### Scenario: Rejected Single Repeated Character Password
- **WHEN** a user submits a password consisting solely of a single repeated character (such as "aaaaaaaaaa")
- **THEN** the system rejects the submission with an error message indicating that the password cannot consist of a single repeated character

#### Scenario: Allowed Passphrases and Varied Character Sets
- **WHEN** a user submits a password within 10 to 72 characters and at most 72 bytes that includes whitespace, numbers, symbols, or Unicode characters and meets all policy rules
- **THEN** the system accepts and securely hashes the password without restriction

#### Scenario: Form Requirement Presentation and Browser Validation
- **WHEN** an unauthenticated or authenticated user views registration, password reset, or profile password forms
- **THEN** the form displays clear guidance of password requirements (stating 10 to 72 characters) and specifies HTML5 minlength="10" and maxlength="72" validation attributes on password input fields

#### Scenario: Defensive Server Handling for Password Encoding Faults
- **WHEN** a controller receives a password that passes initial validation but cannot be processed by the password encoder due to an unexpected encoding failure
- **THEN** the controller catches the exception gracefully, logs a warning, and renders the form with a user-facing error message instead of an unhandled HTTP 500 error

#### Scenario: Backward-Compatible Authentication for Legacy Passwords
- **WHEN** an existing user authenticates at /login using their valid existing password stored in the database, regardless of length or complexity
- **THEN** the system verifies the password hash and authenticates the user successfully without subjecting the existing password to creation policy rules

