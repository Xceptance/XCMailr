# Spec Delta

## MODIFIED Requirements

### Requirement: User Authentication and Self-Service
The system SHALL provide web views for user registration, email verification, session login, password reset, and profile management including self-service account deletion, handling both success and error conditions gracefully.

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

#### Scenario: Self-Service Account Deletion with Valid Password (Happy Path)
- **WHEN** an authenticated user opens the account deletion modal on their profile page, enters their correct current password, and submits the form
- **THEN** the system transactionally removes all stored emails for the user's mailboxes, removes all mailboxes owned by the user, and deletes the user record
- **AND** the system invalidates the current HTTP session, clears security credentials, and redirects to `/login?accountDeleted` displaying an informative confirmation banner

#### Scenario: Self-Service Account Deletion with Incorrect Password (Error Case)
- **WHEN** an authenticated user attempts to delete their account but submits an incorrect password
- **THEN** the system rejects the deletion, retains all user mailboxes, emails, and account records unaltered, and reloads the profile page with a clear error alert indicating the password was incorrect

#### Scenario: Self-Service Account Deletion with Blank Password (Error Case)
- **WHEN** an authenticated user submits the account deletion request with a blank or whitespace-only password
- **THEN** the system rejects the deletion and reloads the profile page with an error notification requiring password entry

#### Scenario: Sole Administrator Account Deletion Prevention (Special Case / Security)
- **WHEN** an authenticated administrator attempts to delete their own account and they are the only remaining active administrator in the system
- **THEN** the system blocks the deletion, keeps all administrator and mailbox data intact, and reloads the profile page displaying an error message stating that the last administrator account cannot be deleted

#### Scenario: Administrator Self-Service Deletion When Multiple Administrators Exist (Happy Path)
- **WHEN** an authenticated administrator enters their valid password to delete their account and at least one other active administrator account exists
- **THEN** the system permits the deletion, cascades data cleanup, terminates the session, and redirects to `/login?accountDeleted`

#### Scenario: Unauthenticated Account Deletion Attempt (Security Case)
- **WHEN** an unauthenticated request attempts to invoke `POST /profile/delete`
- **THEN** the system rejects the request and redirects to `/login` without modifying any user records

#### Scenario: Account Deletion Attempt With Missing CSRF Token (Security Case)
- **WHEN** an account deletion request is transmitted without a CSRF token
- **THEN** the system rejects the request immediately with an HTTP 403 Forbidden response without modifying any user data

#### Scenario: Account Deletion Attempt With Invalid CSRF Token (Security Case)
- **WHEN** an authenticated user submits an account deletion request with an invalid or forged CSRF token
- **THEN** the system rejects the request with an HTTP 403 Forbidden response and preserves all account data untouched
