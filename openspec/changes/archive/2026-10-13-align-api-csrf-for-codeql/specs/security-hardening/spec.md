# Spec Delta: security-hardening

## MODIFIED Requirements

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
