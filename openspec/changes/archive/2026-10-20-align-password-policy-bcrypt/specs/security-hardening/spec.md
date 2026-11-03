# Spec Delta

## MODIFIED Requirements

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
