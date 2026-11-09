# Proposal

## Why

Inbound HTTP endpoints across `xcmailr-webapp` currently rely predominantly on client-side HTML5 attributes (`required`, `type="email"`, `minlength="10"`) rather than server-side validation boundaries. When clients bypass browser validation (via direct HTTP requests, Postman, curl, or automated scripts) with missing, blank, or malformed data, controllers pass unvalidated inputs to the persistence layer, where Hibernate JPA throws uncaught `ConstraintViolationException` errors resulting in unhandled HTTP 500 application crashes.

Existing automated test suites tested happy paths and standard browser payloads, leaving direct controller-level boundary failures undetected. Implementing declarative, fail-fast server-side validation across web forms and REST endpoints ensures that all inbound data is verified at the controller boundary before business logic or database persistence, returning clean HTTP 400 Bad Request responses or localized form validation feedback.

## What Changes

- **Web Registration Server-Side Validation**: Replace loose `@RequestParam` parameters in `WebAuthController.registerSubmit` with a validated `RegistrationForm` backing bean enforcing `@NotBlank`, `@Email`, and `@Size(max = 255)` constraints.
- **Two-Stage Password Validation Pipeline**: Integrate declarative length bounds (`@Size(min = 10, max = 72)`) with server-side confirmation matching (`password.equals(confirmPassword)`) and contextual OWASP ASVS v4.0 / NIST SP 800-63B rules via `PasswordValidator`, rejecting identity-containing or repeating passwords with localized error messages.
- **Web Profile Update Server-Side Validation**: Replace loose `@RequestParam` parameters in `ProfileController.updateProfile` with a validated `ProfileUpdateForm` backing bean enforcing `@NotBlank` and `@Size(max = 255)` on forename and surname.
- **REST Mailbox API Declarative Validation**: Annotate `@RequestBody MailboxData mailboxData` with `@Valid` on `POST` and `PUT /api/v1/mailboxes`, enforce `@Size(max = 255)` on `MailboxData.comment`, and introduce a `@RestControllerAdvice` or `@ExceptionHandler` for `MethodArgumentNotValidException` that outputs standard `ApiErrors` (HTTP 400 Bad Request), preserving full backward compatibility with `xcmailr-client`.
- **Internationalized Error Bundles**: Define clean, localized validation message keys in both `messages.properties` (English) and `messages_de.properties` (German) for all form field errors.
- **Test-Driven Test Pyramid Verification**: Introduce comprehensive integration tests (`MockMvc`) and unit tests written *prior* to implementation (TDD) that initially fail against the unvalidated endpoints and subsequently pass once validation is active. The test suite covers happy paths, validation error cases, and special boundary cases (whitespace-only strings, boundary lengths, multi-byte UTF-8, script tags / HTML injection, and duplicate constraints).
- **Code Quality**: Enforce `final` on all method parameters, local variables, and fields, complete Javadoc documentation, and informative inline comments across all new and modified components.

## Capabilities

### Modified Capabilities
- `web-console-ui`: Enforce server-side form backing validation on user registration, password changes, and profile update views, catching blank/malformed/out-of-bounds submissions and re-rendering forms with localized error feedback rather than triggering HTTP 500 errors.
- `mailbox-management-api`: Enforce declarative `@Valid` Bean Validation on mailbox creation and modification payloads, returning HTTP 400 Bad Request with structured `ApiErrors` when email address or comment constraints are violated.

## Impact

- **Affected Code**:
  - `com.xceptance.xcmailr.controllers.WebAuthController` (registration form binding and error handling)
  - `com.xceptance.xcmailr.controllers.ProfileController` (profile update form binding and error handling)
  - `com.xceptance.xcmailr.controllers.restapi.MailboxApiController` (`@Valid` on request bodies and exception handling)
  - `com.xceptance.xcmailr.controllers.restapi.MailboxData` (constraint annotations)
  - New form backing beans: `RegistrationForm`, `ProfileUpdateForm`
  - New advice: `RestValidationExceptionHandler` (translates `MethodArgumentNotValidException` to `ApiErrors`)
  - Resource bundles: `conf/messages.properties`, `conf/messages_de.properties`
  - Templates: `templates/auth/register.html`, `templates/user/profile.html` (binding result error display)
- **API & Client Compatibility**: 100% backward compatible with `xcmailr-client`. HTTP status codes and JSON error response structures (`ApiErrors` / `ApiError`) remain identical.
- **Dependencies**: No new external dependencies required; uses existing `spring-boot-starter-validation` (`jakarta.validation`).
