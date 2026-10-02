# Spec Delta

## MODIFIED Requirements

### Requirement: User Authentication and Self-Service
The system SHALL provide web views for user registration, email verification, session login, password reset, and profile management, handling both success and error conditions gracefully.

#### Scenario: User Login and Session Management
- **WHEN** a user enters valid email and password credentials for an active account on the login page
- **THEN** the system creates an authenticated HTTP session and redirects to the mailbox dashboard

#### Scenario: User Registration and Email Activation
- **WHEN** a new user completes the registration form and email confirmation is required
- **THEN** an activation token is generated, confirmation email is dispatched, account remains inactive, and the user is redirected to login with instructions to check their email

#### Scenario: User Registration with Immediate Activation
- **WHEN** a new user completes the registration form and email confirmation is disabled via configuration
- **THEN** the account is activated immediately upon creation without generating confirmation tokens or sending activation emails, allowing immediate login

#### Scenario: Unactivated Account Login Notification
- **WHEN** a user enters valid email and password credentials for an inactive account
- **THEN** the system prevents login and displays clear feedback that the account has not been activated yet

#### Scenario: Registration Password Mismatch Validation
- **WHEN** a user submits the registration form with mismatched password and confirm password fields
- **THEN** the system rejects registration, preserves entered non-sensitive fields, and displays a validation error message

#### Scenario: Registration Duplicate Email Prevention
- **WHEN** a user attempts to register with an email address that is already registered (case-insensitive)
- **THEN** the system prevents account creation and displays a user-friendly error message

#### Scenario: Invalid or Nonexistent Confirmation Token
- **WHEN** an unauthenticated request attempts to confirm an account with an invalid or non-existent token
- **THEN** the system redirects to the login view with an invalid token error alert without activating any account

#### Scenario: Expired Confirmation Token Handling
- **WHEN** an unauthenticated request attempts to confirm an account using a token whose expiration timestamp has passed
- **THEN** the system rejects activation and redirects to the login view with an expired token notification

#### Scenario: Case-Insensitive Email Authentication
- **WHEN** a user logs in using an email address whose casing differs from the registration record
- **THEN** the system normalizes the email casing and successfully authenticates the user
