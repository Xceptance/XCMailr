# Spec Delta

## ADDED Requirements

### Requirement: Hypermedia Request Security and Asynchronous Event Handling
The system SHALL secure all asynchronous client hypermedia operations against cross-site request forgery by attaching valid CSRF tokens to mutating requests and enforcing strict token validation and error response handling.

#### Scenario: Asynchronous Request With Valid CSRF Token
- **WHEN** an authenticated user triggers an asynchronous hypermedia mutation (POST, PUT, or DELETE) through the web console
- **THEN** the client event listener extracts the CSRF token from page metadata and attaches it to the request headers
- **AND** the server accepts the request and executes the corresponding action successfully

#### Scenario: Asynchronous Request With Missing CSRF Token
- **WHEN** an asynchronous hypermedia mutation request is transmitted to any protected endpoint without a CSRF token header
- **THEN** the server denies the request immediately with an HTTP 403 Forbidden response without modifying any application state

#### Scenario: Asynchronous Request With Invalid or Forged CSRF Token
- **WHEN** an asynchronous hypermedia mutation request is transmitted with an unrecognized, expired, or forged CSRF token value
- **THEN** the server rejects the request with an HTTP 403 Forbidden response and preserves all data untouched

#### Scenario: Asynchronous Error Response Handling
- **WHEN** an asynchronous hypermedia request receives a 4xx or 5xx error status code from the server
- **THEN** the client hypermedia engine suppresses automatic DOM replacement of target elements with unformatted error bodies, preserving existing UI context

