# Proposal

## Why

When new users register an account, they currently encounter a misleading banner ("Registration successful! You may now log in.") and are immediately blocked upon trying to log in with a generic "Invalid email address or password" error. This occurs because the account is created with `active = false` pending email confirmation, but Spring Security masks the disabled account status as bad credentials, and default development environments lack an outbound SMTP server to deliver the activation link. Furthermore, there is zero end-to-end roundtrip test coverage connecting the registration, activation, and login lifecycle, nor adequate test coverage for negative paths and edge cases (such as token expiration, invalid tokens, duplicate emails, and case sensitivity).

This change fixes the authentication and registration lifecycle by aligning it with OWASP anti-enumeration standards and the proven pre-migration behavior: verifying credentials before revealing account activation state, correcting deceptive UI copy, introducing a configurable confirmation requirement toggle for offline/development environments, documenting the configuration in `application.yml` and `README.md`, strictly preventing bearer token leakage in logs (CWE-532), adhering to strict Java coding standards (`final` modifiers everywhere possible, full Javadoc, and inline code comments), and establishing a comprehensive testing pyramid covering happy paths, negative error conditions, and security edge cases across unit, component slice, and roundtrip integration tests.

## What Changes

- **Accurate Registration Messaging**: Update `login.html` and registration redirects so users are explicitly instructed to check their inbox when email confirmation is active, eliminating the false promise of immediate login.
- **Secure Password-First Status Validation**: Reorder Spring Security authentication checks so passwords are verified *before* account activation status is checked. Unauthenticated attackers receive generic "Invalid email address or password" errors (preventing user enumeration under CWE-204), while authentic users with inactive accounts receive clear, actionable guidance ("Your account has not been activated yet. Please check your email.").
- **Configurable Confirmation Toggle (`xcmailr.app.require-confirmation`)**: Introduce a boolean configuration toggle (default `true`) allowing offline, testing, or self-hosted environments without an outbound SMTP relay to automatically activate accounts upon registration.
- **Documentation**: Document the `require-confirmation` configuration toggle, its security implications, and default behaviors in `application.yml` and `README.md`.
- **Strict Bearer Token Log Hygiene (CWE-532)**: Confirm that confirmation tokens and activation URLs are never printed to log files or standard output, maintaining proof-of-inbox integrity.
- **Code Standards & Clean Architecture**: Enforce `final` modifiers on all method parameters, local variables, and class fields; provide comprehensive Javadocs on all classes and methods; and include informative inline code comments explaining security decisions.
- **Comprehensive Testing Pyramid Suite (Happy Paths, Error Paths & Edge Cases)**:
  - **Unit Tests**: Isolated unit tests with mocked dependencies testing controller logic, property bindings, authentication decisions, registration validation failures (passwords mismatch, duplicate email), and token handling.
  - **Component / Slice Tests**: Controller and security filter tests verifying request mapping, status codes, and security redirections for valid logins, disabled accounts, and invalid credentials.
  - **Integration / Roundtrip Tests**: Full end-to-end tests validating the complete user journey under both confirmation-required and auto-activated modes, as well as edge cases: expired tokens, invalid tokens, duplicate token usage, case-insensitive email matching, and wrong passwords on inactive accounts.

## Capabilities

### Modified Capabilities
- `web-console-ui`: Refines the `User Authentication and Self-Service` requirement to specify accurate registration messaging, activation guidance on valid-password login, support for the configurable confirmation toggle, and robust handling of registration validation and token error states.
- `security-hardening`: Adds the `Account Enumeration Prevention and Inactive Account Handling` requirement to ensure passwords are validated prior to account status disclosure, error cases do not leak user existence, and bearer tokens are never logged.

## Impact

- Affected Code:
  - `xcmailr-webapp/src/main/resources/templates/auth/login.html`: Banner copy updates for `?registered`, `?ready`, `?unconfirmed`, `?invalidToken`, and `?expiredToken`.
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/config/XcmailrProperties.java`: Add `requireConfirmation` to `AppProperties` with `final` accessors and Javadoc.
  - `xcmailr-webapp/src/main/resources/application.yml`: Configuration property definition and documentation comment.
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/controllers/WebAuthController.java`: Conditional activation check, input error handling, and non-leaking logs with `final` modifiers and Javadocs.
  - `xcmailr-webapp/src/main/java/com/xceptance/xcmailr/security/SecurityConfig.java`: Password-first authentication provider and failure handler routing with `final` modifiers and Javadocs.
  - `README.md`: Document `xcmailr.app.require-confirmation` / `APP_REQUIRE_CONFIRMATION`.
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/WebAuthControllerUnitTest.java`: Isolated unit tests for registration branching, password mismatches, duplicate emails, and token error handling.
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/security/AuthenticationProviderUnitTest.java`: Unit tests verifying password validation executes before disabled checks, covering bad credentials, inactive accounts, and user not found.
  - `xcmailr-webapp/src/test/java/com/xceptance/xcmailr/controllers/WebAuthControllerTest.java`: Complete roundtrip integration tests for happy paths, negative error conditions, and edge cases.

