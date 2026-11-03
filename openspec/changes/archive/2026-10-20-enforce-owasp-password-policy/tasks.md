# Tasks

## 1. Core Validator Component & Unit Tests

- [x] 1.1 Create `PasswordValidationResult` and `PasswordContext` records in `com.xceptance.xcmailr.security` using `final` modifier wherever possible, full Javadoc, and detailed code comments. Verify classes compile cleanly.
- [x] 1.2 Implement `PasswordValidator` Spring `@Component` in `com.xceptance.xcmailr.security` enforcing: length boundaries (10 to 128 characters), case-insensitive user context exclusion (terms >= 3 chars for forename, surname, and email local-part), single-character repetition defense, and open character set acceptance. Use `final` parameters/variables, full Javadoc, and inline rationale comments.
- [x] 1.3 Implement unit tests in `PasswordValidatorTest` following the testing pyramid:
  - Happy path tests: standard compliant passwords, multi-word passphrases with spaces, punctuation, numbers, and Unicode/emojis.
  - Error boundary tests: null, empty string, 9 characters (boundary rejection), 10 characters (boundary acceptance), 128 characters (boundary acceptance), 129 characters (boundary rejection), and single repeated characters (e.g. `aaaaaaaaaa`, `1111111111`).
  - Special & context cases: password containing forename, surname, or email username (exact match, embedded substring, case insensitivity), short identity terms (< 3 chars) ignored to prevent false positives.
  Verify execution with `mvn test -Dtest=PasswordValidatorTest`.

## 2. Controller Integration & Web MVC Slice Tests

- [x] 2.1 Integrate `PasswordValidator` into `WebAuthController.registerSubmit`, replacing legacy 6-char check with validator result evaluation and user-friendly error messages. Use `final` variables, Javadoc updates, and clear code comments.
- [x] 2.2 Integrate `PasswordValidator` into `WebAuthController.resetPasswordSubmit`, validating new password candidates against the target user's context.
- [x] 2.3 Integrate `PasswordValidator` into `ProfileController.changePassword`, validating new password candidates against the authenticated user's context.
- [x] 2.4 Update and extend controller tests in `WebAuthControllerTest` and `ProfileControllerTest` covering:
  - Happy path registration, password reset, and profile change with valid 10+ char passwords.
  - Validation failure cases for length (< 10 or > 128), context matching, and repetition, verifying appropriate view re-rendering and error message mapping.
  - Mismatched confirmation password handling.
  Verify execution with `mvn test -Dtest=WebAuthControllerTest,ProfileControllerTest`.

## 3. UI Presentation & Browser Validation

- [x] 3.1 Update `templates/auth/register.html` to add password requirements guidance block, `minlength="10"`, `maxlength="128"`, and updated placeholder text.
- [x] 3.2 Update `templates/auth/reset-password.html` to add password requirements guidance block, `minlength="10"`, `maxlength="128"`, and updated placeholder text.
- [x] 3.3 Update `templates/user/profile.html` to add password requirements guidance block, `minlength="10"`, `maxlength="128"`, and updated placeholder text.
- [x] 3.4 Verify view rendering by running web slice tests or inspecting rendered output.

## 4. Integration Verification & Backward Compatibility

- [x] 4.1 Create integration test `LegacyPasswordAuthenticationIntegrationTest` verifying that existing users pre-seeded in the database with short legacy passwords (< 10 chars) authenticate successfully via `/login` without encountering creation policy rejection.
- [x] 4.2 Run full test suite (`mvn clean test` and failsafe verification) to ensure zero regressions across unit, web slice, and integration test layers.

