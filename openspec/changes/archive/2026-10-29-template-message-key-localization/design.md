# Design: Template-Driven Message Key Error Localization

## Context

An exhaustive audit of all web and REST controllers across the application (`AdminWebController`, `MailWebController`, `MailboxWebController`, `ProfileController`, `WebAuthController`, `MailApiController`, `MailboxApiController`) confirmed that `ProfileController` and `WebAuthController` are the only controllers injecting Spring's `MessageSource` and accepting `final Locale locale` parameters in `@PostMapping` action methods to translate error strings in Java.

Other controllers either rely entirely on redirect query flags (e.g., `?updated`, `?error=invalidDomain`) resolved directly in Thymeleaf templates via `param.*`, or return JSON representations in the REST layer without internationalization coupling.

While `UserPreferenceLocaleResolver` already dynamically determines the user's preferred locale (`en` or `de`) from their database entity (for authenticated users) or the HTTP `Accept-Language` header (for unauthenticated visitors), performing message resolution in Java creates unnecessary coupling, parameter clutter, and potential state desynchronization.

See [proposal.md](proposal.md) for background and motivation.

## Goals / Non-Goals

**Goals:**
- Pass raw message keys (`errorMessageKey`) from controllers to the view model and flash attributes instead of pre-rendered message strings.
- Remove `MessageSource` injection and `final Locale locale` parameters from `ProfileController` and `WebAuthController`.
- Support backward-compatible template error display with `${errorMessageKey != null ? #messages.msg(errorMessageKey) : errorMessage}` in Thymeleaf templates.
- Preserve submitted `language` selection upon validation failure in the registration form.
- Ensure 100% backward compatibility with the JSON REST API (`/api/v1/**`).

**Non-Goals:**
- Modifying REST API controllers (`MailApiController`, `MailboxApiController`) or JSON error payload structures (`ApiError`).
- Changing how `UserPreferenceLocaleResolver` determines locale or altering the database schema for user preferences.
- Replacing Thymeleaf standard message expression mechanisms (`#messages.msg(...)` / `#{...}`).

## Decisions

### 1. Template Error Evaluation: Dual Support with Fallback
- **Choice**: In Thymeleaf templates, inspect `errorMessageKey` first, resolving it via `#messages.msg(errorMessageKey)`, and fall back to `errorMessage`:
  ```html
  <div th:if="${errorMessageKey != null or errorMessage != null}" class="alert alert-danger" role="alert"
       th:text="${errorMessageKey != null ? #messages.msg(errorMessageKey) : errorMessage}">
  </div>
  ```
- **Rationale**: This allows web controllers to simply provide semantic message keys while maintaining graceful fallback if any controller or integration test injects a raw `errorMessage`.
- **Alternatives Considered**:
  - *Replace `errorMessage` completely without fallback*: Breaks existing tests and any custom error handlers that supply dynamic, non-keyed strings.
  - *Custom Thymeleaf dialect or tag processor*: Overengineering for a small set of form templates.

### 2. Complete Elimination of `MessageSource` and `Locale` from Controller Signatures
- **Choice**: Remove `MessageSource` fields and constructors from `ProfileController` and `WebAuthController`. Remove `final Locale locale` parameters from `@PostMapping` methods across the entire codebase.
- **Rationale**:
  - Spring MVC controllers should be thin coordinators. Message localization is a view-layer presentation concern.
  - Passing `final Locale locale` into controller action methods is completely unnecessary when passing message keys: Thymeleaf already resolves the user's active locale automatically at render time via Spring's `UserPreferenceLocaleResolver` (which queries `user.language` from the database for authenticated users, and the `Accept-Language` header for unauthenticated visitors).
  - Removing `Locale` eliminates conceptual confusion and duplication with form fields (e.g. `@RequestParam("language")`), preventing language desynchronization on validation re-renders.
  - After this change, zero web controllers in the application will require `MessageSource` or `Locale` parameters.
- **Alternatives Considered**:
  - *Keep `Locale` in controller signatures*: Redundant dead parameter since the controller does no translation.
  - *Keep `MessageSource` in controllers as an optional fallback*: Retains unwanted boilerplate and prevents clean separation of concerns.

### 3. Password Validation Integration
- **Choice**: Use `validationResult.messageKey()` directly when adding errors to the model: `model.addAttribute("errorMessageKey", validationResult.messageKey())`.
- **Rationale**: `PasswordValidationResult` already contains `messageKey()` from the OWASP ASVS v4.0 password quality enhancement, matching the exact keys in `messages.properties` and `messages_de.properties`.
- **Alternatives Considered**:
  - *Map validation errors to new enum*: Unnecessary layer of indirection since standard property keys are already established.

### 4. Registration Form Language State Preservation
- **Choice**: In `WebAuthController.populateForm(...)`, accept and pass the submitted `language` parameter back into the model: `model.addAttribute("language", language)`. In `register.html`, update `<select name="language">` to prioritize `language` over `#locale.language`:
  ```html
  <select class="form-select" id="language" name="language">
      <option value="en" th:selected="${(language != null ? language : #locale.language) == 'en'}" th:text="#{auth.register.language.en}">English</option>
      <option value="de" th:selected="${(language != null ? language : #locale.language) == 'de'}" th:text="#{auth.register.language.de}">Deutsch</option>
  </select>
  ```
- **Rationale**: When a user selects a language different from their browser default (e.g. English browser visitor selecting Deutsch) and validation fails, the dropdown would otherwise reset to the browser's default. Passing `language` back preserves user input.
- **Alternatives Considered**:
  - *Do not preserve language selection*: Leads to poor user experience if validation fails on other fields.

## Risks / Trade-offs

- **[Risk] Missing message key in properties files causes Thymeleaf rendering exception or raw key output**:
  → *Mitigation*: Spring's message source fallback guarantees resolution to default messages from `messages.properties` if missing in `messages_de.properties`. All message keys (`profile.error.*`, `auth.error.*`) already exist in both files. Unit tests will assert key presence.
- **[Risk] Controller unit/integration tests expecting `errorMessage` in model fail**:
  → *Mitigation*: Update assertions in `ProfileControllerTest` and `WebAuthControllerTest` to verify `model().attribute("errorMessageKey", ...)` or verify rendered HTML content containing the localized strings.

## Migration Plan

1. Update templates (`user/profile.html`, `auth/register.html`, `auth/reset-password.html`, `layout/base.html`) to support `errorMessageKey` with fallback.
2. Refactor `ProfileController` and `WebAuthController` to remove `MessageSource` and `Locale locale`, passing `errorMessageKey` instead.
3. Update unit and integration tests to verify model attributes and HTML rendering.
4. Run full test suite (`mvn clean test`) to verify zero regressions.

