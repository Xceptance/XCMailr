# Spec Delta

## MODIFIED Requirements

### Requirement: User Authentication and Self-Service
The system SHALL provide web views for user registration, email verification, session login, password reset, and profile management including language preference updates and self-service account deletion, handling both success and error conditions gracefully and ensuring that profile language updates take immediate effect across subsequent views and asynchronous HTMX requests without requiring re-authentication.

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

#### Scenario: Profile Language Update Takes Immediate Effect Across Views (Happy Path)
- **WHEN** an authenticated user updates their profile language preference (e.g. from German `de` to English `en` or vice versa) and submits the profile form
- **THEN** the system persists the change to the database, updates the active session principal, and immediately resolves the new language for subsequent full-page and asynchronous HTMX requests (such as mailbox pagination summaries) without requiring re-authentication

#### Scenario: Dynamic Locale Resolution Prioritizes Persisted User Preference Over Login Snapshot (Happy Path)
- **WHEN** an authenticated user accesses localized views or partial fragments
- **THEN** the locale resolver queries the persisted user record to obtain the current language preference, prioritizing it over any static login-time authentication snapshot
- **AND** falls back to the in-memory principal or default request locale only if database lookup is not possible

#### Scenario: Profile Update Preserves In-Memory Session Principal Consistency (Happy Path)
- **WHEN** an authenticated user updates their profile (forename, surname, language preference)
- **THEN** the system updates the in-memory user entity within the active session's authentication principal to match the newly persisted values

#### Scenario: Locale Resolution Fallback on Malformed or Unsupported Language Preference (Error / Fallback Case)
- **WHEN** an authenticated user's persisted language preference is null, blank, malformed, or an unsupported language code
- **THEN** the locale resolver safely falls back to standard default English localization without throwing exceptions or generating invalid HTTP response headers

#### Scenario: Profile Language Update Validation Failure Preserves Existing Locale (Error Case)
- **WHEN** a profile update request contains invalid input fields or validation errors
- **THEN** the profile changes are rejected, the database and session principal remain unaltered, and the error response renders in the user's previously active locale

#### Scenario: OWASP Access Control and Tenant Isolation in Locale Resolution (Security Case)
- **WHEN** an authenticated request triggers locale resolution
- **THEN** the system resolves user identity strictly from the cryptographically verified `Authentication.getName()` security principal and never from untrusted client-supplied headers or request parameters, enforcing strict tenant isolation and preventing identity spoofing

#### Scenario: Privilege Escalation Prevention During Session Principal Synchronization (Security Case)
- **WHEN** the system synchronizes the in-session `UserPrincipal` upon profile update
- **THEN** only non-security profile fields (`forename`, `surname`, `language`) are copied, while roles, authorities, admin privileges, password hashes, and account activation states remain strictly immutable and protected against tampering
