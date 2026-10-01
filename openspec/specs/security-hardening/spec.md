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
