# Design

## Context

Inbound HTTP endpoints currently accept unvalidated loose `@RequestParam` parameters or unannotated `@RequestBody` DTOs. When clients submit blank or malformed data that bypasses browser HTML5 validation attributes, the data reaches Hibernate JPA entities, where entity-level constraints (`@NotEmpty`, `@Email`) fail at transaction flush time and throw uncaught `ConstraintViolationException` errors, resulting in HTTP 500 crashes.

See `proposal.md` for motivation and background.

## Goals / Non-Goals

**Goals:**
- Provide declarative, fail-fast server-side input validation at the Spring MVC controller boundary for all state-changing endpoints in `xcmailr-webapp`.
- Translate Bean Validation failures cleanly into localized form error messages for web views (`auth/register`, `user/profile`) and structured JSON `ApiErrors` (HTTP 400 Bad Request) for REST endpoints.
- Seamlessly integrate the existing OWASP/NIST `PasswordValidator` component into the Spring MVC form binding pipeline.
- Implement strictly test-driven (TDD): author failing integration and unit tests before implementing validation, confirming that bypass attempts are caught.
- Enforce clean code standards: `final` on all parameters, local variables, and fields, complete Javadoc, and detailed inline comments.

**Non-Goals:**
- Modifying underlying database schema or Flyway migrations (entity column definitions already exist).
- Altering the REST API contract or response payload structure of `ApiErrors` / `ApiError` (full backward compatibility with `xcmailr-client` is preserved).
- Replacing Spring Security authentication filter logic or session management.

## Decisions

### 1. Form Backing DTOs vs. Procedural Controller Guards

**Decision:** Create dedicated form backing classes (`RegistrationForm`, `ProfileUpdateForm`) annotated with Jakarta Bean Validation annotations (`@NotBlank`, `@Email`, `@Size`) and bind them using `@Valid @ModelAttribute` and `BindingResult`.

**Rationale:**
- Declarative annotations separate format/length constraints from controller business logic.
- Spring MVC binds and preserves form input automatically across validation failures, eliminating manual `populateForm(model, ...)` boilerplate.
- Spring's `BindingResult` enables clean integration with Thymeleaf field error tags (`th:errors="*{forename}"`).

**Alternatives Considered:**
- *Procedural `if (StringUtils.isBlank(...))` in controller:* Kept loose `@RequestParam` parameters, but leads to repetitive boilerplate, fragile maintenance, and manual model attribute repopulation.

### 2. Two-Stage Password Validation Pipeline

**Decision:** Enforce basic structural constraints via `@NotBlank` and `@Size(min = 10, max = 72)` on `RegistrationForm`, followed by server-side confirmation equality check and contextual evaluation using `PasswordValidator.validate(password, context)`.

**Rationale:**
- OWASP password rules require identity context (checking if the password contains the user's forename, surname, or email username).
- Decoupling basic bounds (`@Size`) from contextual rules ensures fast-fail on trivial inputs while binding contextual violations directly to `bindingResult.rejectValue("password", result.messageKey())`.

```
Inbound Registration Form Submit
              |
              v
[1. Jakarta Bean Validation] ---> Fail? ---> Return form view with field error
              |
            Pass
              v
[2. Password Confirmation Match] -> Mismatch? -> Reject with auth.error.register.passwordMismatch
              |
            Match
              v
[3. PasswordValidator (OWASP)] ---> Violation? -> Reject with specific password rule key
              |
            Valid
              v
[4. Email Uniqueness Check] ------> Exists? ----> Reject with auth.error.register.emailExists
              |
            Unique
              v
[5. Construct & Save User Entity]
```

### 3. REST API Validation & Global Exception Translation

**Decision:** Add `@Valid` to `@RequestBody MailboxData mailboxData` in `MailboxApiController`, add `@Size(max = 255)` to `MailboxData.comment`, and implement a `@RestControllerAdvice` (`RestValidationExceptionHandler`) handling `MethodArgumentNotValidException`.

**Rationale:**
- Spring MVC throws `MethodArgumentNotValidException` when an `@Valid @RequestBody` argument fails validation.
- The exception advice maps each `FieldError` into an `ApiError(field, defaultMessage)` wrapped in `ApiErrors`, returning HTTP 400 Bad Request.
- Existing API clients like `xcmailr-client` expect this exact JSON structure and HTTP status code.

**Alternatives Considered:**
- *Injecting `BindingResult` into `@RestController` methods:* Requires manual error extraction and response building in every endpoint. A centralized `@RestControllerAdvice` guarantees consistent error formatting across all present and future REST endpoints.

### 4. Test-Driven Development (TDD) & Test Pyramid Strategy

**Decision:** Author tests across the pyramid *before* applying validation fixes:
1. **MockMvc Integration Tests (`@WebMvcTest` / `@SpringBootTest`):**
   - Author tests submitting blank names, malformed emails, over-length strings, and invalid passwords directly via HTTP POST.
   - Run tests to confirm they fail (exposing missing validation / HTTP 500 errors).
   - Implement form validation and verify tests turn green (HTTP 400 or form re-rendering with localized errors).
2. **Test Pyramid Coverage:**
   - *Happy Path:* Valid submissions succeed and redirect/return 200/201.
   - *Validation Error Cases:* Blank fields, missing fields, malformed emails, passwords < 10 chars, passwords > 72 chars, mismatched confirmation, comments > 255 chars.
   - *Special Cases & Boundaries:* Whitespace-only strings, exactly 255-character fields, exactly 256-character fields, UTF-8 umlauts, XSS/HTML control characters (`<script>`), SQL injection strings (`' OR 1=1;--`).

### 5. Code Quality & Modifiers

**Decision:** Enforce `final` on all method parameters, local variables, and class fields across all new and touched classes. Provide complete Javadoc class-level and method-level documentation.

## Risks / Trade-offs

- **[Risk] REST client breakage if error payload format changes:**
  - *Mitigation:* `RestValidationExceptionHandler` strictly outputs `com.xceptance.xcmailr.controllers.restapi.ApiErrors`, preserving identical JSON keys (`errors[i].parameter`, `errors[i].message`).
- **[Risk] Thymeleaf template binding errors if field names diverge:**
  - *Mitigation:* `RegistrationForm` and `ProfileUpdateForm` use exact matching property names (`forename`, `surname`, `mail`, `password`, `confirmPassword`, `language`).
- **[Risk] In-memory session principal desynchronization on profile update:**
  - *Mitigation:* Preserve existing defense-in-depth logic in `ProfileController.updateProfile` that updates `SecurityContextHolder` session principal.
