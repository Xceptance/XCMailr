# Tasks

## 1. PasswordValidator Alignment and Unit Tests

- [x] 1.1 Update `PasswordValidator` constants (`MAX_LENGTH = 72`, `MAX_BYTES = 72`) and implement UTF-8 byte checking logic with distinct error messages for character count versus byte limit. Ensure `final` modifiers on all variables and parameters, comprehensive Javadoc, and inline comments. Verify compilation via `mvn test-compile -pl xcmailr-webapp`.
- [x] 1.2 Implement Unit Tests in `PasswordValidatorTest` following the testing pyramid:
  - **Happy path**: 10 characters (minimum boundary), 20-50 characters (typical passphrase), 72 characters (maximum boundary), and valid UTF-8 strings with emojis/umlauts <= 72 bytes.
  - **Error cases**: 9 characters (below minimum), 73 characters (just above maximum), 128 characters (user manual test case), and 129 characters (user manual test case).
  - **Special cases**: Multibyte strings < 72 characters but > 72 bytes (asserting distinct byte limit error message), user context injection (forename, surname, email local-part), single repeated character, and null/blank safety.
  - Verify via `mvn test -pl xcmailr-webapp -Dtest=PasswordValidatorTest`.

## 2. Controller Defensive Guarding and Slice Tests

- [x] 2.1 Update `WebAuthController` (`registerSubmit` and `resetPasswordSubmit`) to wrap `passwordEncoder.encode(password)` in a `try-catch` catching `IllegalArgumentException`, logging a warning and returning the form view with an actionable error message. Ensure `final` modifiers, Javadoc, and inline comments. Verify via `mvn test-compile -pl xcmailr-webapp`.
- [x] 2.2 Update `ProfileController` (`changePasswordSubmit`) to wrap `passwordEncoder.encode(newPassword)` in a `try-catch` catching `IllegalArgumentException`, logging a warning and returning the profile view with an actionable error message. Ensure `final` modifiers, Javadoc, and inline comments. Verify via `mvn test-compile -pl xcmailr-webapp`.
- [x] 2.3 Implement Controller Slice Tests in `WebAuthControllerTest` and `ProfileControllerTest` following the testing pyramid:
  - **Happy path**: Submit 72-character valid password via POST `/register`, POST `/reset-password`, and POST `/profile/change-password`, asserting HTTP 302 redirect / success, proper entity persistence, and that `passwordEncoder.encode()` processes 72 bytes without error.
  - **Error cases**: Submit 73-character, 128-character, and 129-character passwords via POST `/register`, asserting HTTP 200 OK (re-renders form with error), model error message presence, and strictly avoiding HTTP 500; submit 73, 128, 129 characters to `/profile/change-password` asserting clean form error.
  - **Special cases**: Submit multibyte strings > 72 bytes (e.g. 25 emojis) asserting HTTP 200 and byte error message; test defensive catch guard ensuring any unexpected `IllegalArgumentException` during encoding renders HTTP 200 with error feedback instead of uncaught HTTP 500.
  - Verify via `mvn test -pl xcmailr-webapp -Dtest=WebAuthControllerTest,ProfileControllerTest`.

## 3. Thymeleaf Template Updates and UI Verification

- [x] 3.1 Update `auth/register.html` password and confirmPassword inputs to `maxlength="72"` and update requirement guidance text to "At least 10 characters (up to 72)".
- [x] 3.2 Update `auth/reset-password.html` password and confirmPassword inputs to `maxlength="72"` and update requirement guidance text to "At least 10 characters (up to 72)".
- [x] 3.3 Update `user/profile.html` newPassword and confirmNewPassword inputs to `maxlength="72"` and update requirement guidance text to "At least 10 characters (up to 72)".
- [x] 3.4 Verify UI template rendering via `WebAuthControllerTest` and `ProfileControllerTest` asserting `maxlength="72"`, `minlength="10"`, and helper text "At least 10 characters (up to 72)" are present in rendered HTML.

## 4. Verification and Regression Testing

- [x] 4.1 Run full project test suite (`mvn test -pl xcmailr-webapp`) verifying all 311+ tests pass cleanly without errors.
- [x] 4.2 Verify backward-compatible authentication: ensure existing database user credentials hashed with BCrypt continue to authenticate cleanly via `/login`.
