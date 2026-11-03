# Proposal: Enforce OWASP Password Policy for Credential Creation

## Why

XCMailr currently enforces a rudimentary 6-character minimum check for passwords across registration, password reset, and profile updates. This does not meet modern security baselines (such as OWASP ASVS v4.0 and NIST SP 800-63B) and leaves accounts susceptible to trivial or context-based passwords (e.g. passwords containing the user's name or email). At the same time, modernized instances must remain fully backward-compatible with existing production databases, allowing legacy accounts to authenticate seamlessly without forced resets or login disruption.

## What Changes

- Introduce a dedicated, offline `PasswordValidator` component enforcing modern OWASP / NIST SP 800-63B standards:
  - Minimum length of 10 characters and maximum length of 128 characters (passphrase friendly, hash DoS resilient).
  - Rejection of passwords containing or matching user-specific context (forename, surname, or email username/local part, case-insensitively).
  - Rejection of trivial repeated character sequences (e.g., `aaaaaaaaaa`).
  - Full character set freedom without restrictive composition rules (allowing spaces, unicode, punctuation).
- Wire `PasswordValidator` into credential creation and modification flows:
  - User registration (`WebAuthController.registerSubmit`)
  - Password reset (`WebAuthController.resetPasswordSubmit`)
  - Profile password change (`ProfileController.changePassword`)
- Update Thymeleaf templates (`register.html`, `reset-password.html`, `profile.html`) to clearly present password rules to users:
  - In-form guidance listing minimum length, context restrictions, and passphrase support.
  - HTML5 client validation attributes (`minlength="10"`, `maxlength="128"`).
  - Clear, user-actionable server-side validation error messages.
- Ensure strict backward compatibility for authentication:
  - Existing database credentials continue to authenticate normally via `PasswordFirstAuthenticationProvider` without encountering creation policy re-validation.
- Comprehensive test coverage following the test pyramid:
  - Unit tests for `PasswordValidator` covering happy paths, length boundary conditions, user context matching, single-character repeats, and edge cases.
  - Web MVC controller tests verifying form rejection and error messaging on policy violations.
  - End-to-end integration tests verifying registration, reset, and backward-compatible authentication.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `security-hardening`: Add requirement for OWASP-aligned password policy enforcement on credential creation and modification, with explicit guarantee of backward compatibility for existing credentials during authentication.

## Impact

- **Backend Code**:
  - New class: `com.xceptance.xcmailr.security.PasswordValidator` (and validation result record / model).
  - Modified controllers: `WebAuthController`, `ProfileController`.
- **Views**:
  - `templates/auth/register.html`
  - `templates/auth/reset-password.html`
  - `templates/user/profile.html`
- **Tests**:
  - `PasswordValidatorTest` (unit tests).
  - `WebAuthControllerTest`, `ProfileControllerTest` (MVC/controller slice tests).
  - Integration tests verifying existing password authentication and new password validation.
- **Dependencies**: Zero new external dependencies. Standard JDK and existing Spring Boot dependencies only.

