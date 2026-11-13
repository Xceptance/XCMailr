# Spec Delta

## ADDED Requirements

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
