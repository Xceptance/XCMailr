# Design

## Context

The web console UI currently resolves localized strings across navigation, table headers, forms, and alerts using Spring's `MessageSource` via Thymeleaf `#{...}` syntax. However, several input placeholders, pattern validation tooltips, and screen reader ARIA landmarks remain hardcoded English HTML attributes. Furthermore, when form submissions fail server-side in `ProfileController` and `WebAuthController`, error alerts are passed to views as static English strings via `model.addAttribute("errorMessage", "...")`.

See `proposal.md` for background and motivation.

## Goals / Non-Goals

**Goals:**
- Provide 100% localization parity across English (`en`) and German (`de`) for all form input placeholders, pattern validation tooltips, timestamp fallback text, and accessibility landmarks.
- Localize server-side form validation error alerts in `ProfileController` and `WebAuthController` using Spring's native `MessageSource` and request `Locale`.
- Extend `PasswordValidationResult` to supply structured message keys while preserving existing string error messages for full backward compatibility.
- Enforce strict 1:1 message key parity between `conf/messages.properties` and `conf/messages_de.properties`.
- Provide testing pyramid test coverage (unit, controller, integration, OWASP XSS safety) with `final` modifiers, comprehensive Javadoc, and clear code comments.

**Non-Goals:**
- No custom validation frameworks, complex error-code class hierarchies, or architectural overengineering.
- No changes to REST API JSON schemas or database entity models.
- No changes to Thymeleaf template error display markup (templates continue rendering `<span th:text="${errorMessage}">`).

## Decisions

### Decision 1: Leverage Spring MVC Native `Locale` and `MessageSource` in Controllers
**Choice**: Inject Spring's standard `MessageSource` into `ProfileController` and `WebAuthController`, and add `final Locale locale` as a method argument to form-handling `@PostMapping` methods.
**Rationale**: Spring MVC automatically resolves the active user or session locale from the `LocaleResolver` and supplies it to controller handler arguments without any custom interceptors. When adding `errorMessage` to the `Model`, the controller calls `messageSource.getMessage(code, args, defaultMessage, locale)`.
**Alternatives Considered**:
- *Passing message keys directly to templates (`th:text="#{${errorKey}}"`)*: Rejected because some errors contain dynamic parameters or originate from legacy validators; pre-resolving in the controller maintains 100% template simplicity and compatibility.
- *Creating a custom `@ControllerAdvice` error translation filter*: Rejected as unnecessary overengineering for two controllers.

### Decision 2: Backward-Compatible Extension of `PasswordValidationResult`
**Choice**: Extend the `PasswordValidationResult` record to include `String messageKey`:
```java
public record PasswordValidationResult(boolean isValid, String errorMessage, String messageKey)
```
Provide overloaded factory methods `invalid(final String errorMessage, final String messageKey)` and `invalid(final String errorMessage)` that defaults `messageKey` to `null`.
**Rationale**: Existing unit tests that inspect `result.errorMessage()` continue to pass without modification, while controllers can check `result.messageKey()` and resolve the localized message via `messageSource.getMessage(result.messageKey(), null, result.errorMessage(), locale)`.
**Alternatives Considered**:
- *Replacing `errorMessage` with an enum*: Rejected because it would require rewriting all existing assertions in `PasswordValidatorTest`.

### Decision 3: Standardized Translation Taxonomy
**Choice**: Group all new keys under established namespaces:
- Form Placeholders: `auth.password.placeholder.*`, `auth.register.placeholder.*`
- Inline Fallbacks: `profile.token.recently`
- Tooltips & ARIA: `admin.domains.title.pattern`, `common.aria.pagination.*`, `common.aria.toolbar.*`, `common.aria.nav.*`
- Profile Controller Errors: `profile.error.password.*`, `profile.error.delete.*`
- Auth Controller Errors: `auth.error.register.*`, `auth.error.resetPassword.*`
- Password Quality Rules: `auth.password.error.*`

### Decision 4: Testing Strategy & OWASP Security
**Choice**: Verify localization across all levels of the test pyramid:
1. **Unit Tests**: `MessageBundleParityTest` validates key parity, placeholder alignment, and non-empty translations. `PasswordValidatorTest` verifies that message keys are accurately populated for each rule violation.
2. **Controller Tests**: `ProfileControllerTest` and `WebAuthControllerTest` verify with `MockMvc` that error banners render in English by default and in German when `Locale.GERMAN` is specified.
3. **Integration / Template Contract Tests**: Verify that rendered HTML pages contain localized `placeholder`, `title`, and `aria-label` attributes.
4. **OWASP ASVS / XSS Defense**: Verify that Thymeleaf automatically applies context-aware HTML entity escaping on localized message text.

## Risks / Trade-offs

- **[Risk: Missing Translation Key at Runtime]** → *Mitigation*: Always pass the English text as the `defaultMessage` parameter in `messageSource.getMessage(...)`, and enforce 100% key parity via `MessageBundleParityTest`.
- **[Risk: Existing Controller Tests Breaking]** → *Mitigation*: Unscoped requests continue to use default English, keeping existing assertions identical while new dedicated tests assert German behavior.
- **[Risk: Stale Session or Locale Desynchronization]** → *Mitigation*: Spring's `LocaleResolver` evaluates the authenticated user's profile setting and `Accept-Language` header consistently.

