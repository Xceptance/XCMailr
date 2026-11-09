# Tasks

## 1. TDD Phase 1: Author Failing Integration Tests

- [x] 1.1 Author failing MockMvc integration tests in `WebAuthControllerValidationTest` covering user registration boundary cases (blank forename, blank surname, malformed email, fields > 255 chars, password mismatch, OWASP password rules) and verify via `mvn test -Dtest=WebAuthControllerValidationTest` that tests fail against unvalidated endpoints
- [x] 1.2 Author failing MockMvc integration tests in `ProfileControllerValidationTest` covering profile update boundary cases (blank forename, blank surname, fields > 255 chars) and verify via `mvn test -Dtest=ProfileControllerValidationTest` that tests fail against unvalidated endpoints
- [x] 1.3 Author failing MockMvc integration tests in `MailboxApiControllerValidationTest` covering REST API mailbox creation and update boundary cases (missing email, invalid email, comment > 255 chars) and verify via `mvn test -Dtest=MailboxApiControllerValidationTest` that tests fail against unvalidated endpoints

## 2. Web Authentication Form Validation Implementation

- [x] 2.1 Add localized validation error message keys in `conf/messages.properties` and `conf/messages_de.properties` for required names, email format, field lengths, and password rules
- [x] 2.2 Create `RegistrationForm` backing bean with Jakarta Bean Validation annotations (`@NotBlank`, `@Email`, `@Size`), strict `final` modifiers, full Javadoc, and inline comments
- [x] 2.3 Refactor `WebAuthController.registerSubmit` to accept `@Valid @ModelAttribute("form") RegistrationForm form` and `BindingResult`, integrate two-stage password verification with `PasswordValidator`, and update `templates/auth/register.html` to bind field error feedback
- [x] 2.4 Re-run `WebAuthControllerValidationTest` via `mvn test -Dtest=WebAuthControllerValidationTest` and verify all tests turn green

## 3. User Profile Form Validation Implementation

- [x] 3.1 Create `ProfileUpdateForm` backing bean with Jakarta Bean Validation annotations (`@NotBlank`, `@Size`), strict `final` modifiers, full Javadoc, and inline comments
- [x] 3.2 Refactor `ProfileController.updateProfile` to accept `@Valid @ModelAttribute("profileForm") ProfileUpdateForm form` and `BindingResult`, preserve in-session `UserPrincipal` synchronization, and update `templates/user/profile.html` to display validation error feedback
- [x] 3.3 Re-run `ProfileControllerValidationTest` via `mvn test -Dtest=ProfileControllerValidationTest` and verify all tests turn green

## 4. REST API Mailbox Validation Implementation

- [x] 4.1 Update `MailboxData` to add `@Size(max = 255)` constraint on `comment` with comprehensive Javadoc and inline comments
- [x] 4.2 Create `RestValidationExceptionHandler` (`@RestControllerAdvice`) to intercept `MethodArgumentNotValidException` and transform field errors into standard `ApiErrors` (HTTP 400 Bad Request) with strict `final` modifiers and Javadoc
- [x] 4.3 Annotate `@RequestBody MailboxData mailboxData` with `@Valid` on `POST` and `PUT /api/v1/mailboxes` in `MailboxApiController`
- [x] 4.4 Re-run `MailboxApiControllerValidationTest` via `mvn test -Dtest=MailboxApiControllerValidationTest` and verify all tests turn green

## 5. Test Pyramid & Regression Verification

- [x] 5.1 Implement special case and boundary tests across unit and integration test suites covering whitespace-only strings, UTF-8 multi-byte characters, XSS/HTML injection strings (`<script>`), SQL control characters (`' OR 1=1;--`), and exact boundary lengths (255 vs 256 chars)
- [x] 5.2 Execute complete test suite via `mvn clean test` across all reactor modules, verifying that all tests pass with zero failures and zero regressions
