# Tasks

## 1. Message Bundles & Validation Result Model

- [x] 1.1 Add all required translation keys for placeholders, fallback strings, tooltips, ARIA landmarks, controller errors, and password validator rules to `conf/messages.properties` and `conf/messages_de.properties` with 1:1 parity, and verify using `mvn test -pl xcmailr-webapp -Dtest=MessageBundleParityTest`.
- [x] 1.2 Extend `PasswordValidationResult` record with `String messageKey` and backward-compatible overloaded static factory methods (`final` modifiers and Javadocs), verifying zero regressions in `PasswordValidatorTest`.
- [x] 1.3 Update `PasswordValidator` rule failure returns to include corresponding `auth.password.error.*` message keys (`final` modifiers and Javadoc), and verify in `PasswordValidatorTest`.

## 2. Template Localization Polish

- [x] 2.1 Add dynamic `th:placeholder` attributes for password and confirm-password fields in `user/profile.html`, `auth/reset-password.html`, and `auth/register.html`, verifying template syntax and bundle key resolution.
- [x] 2.2 Add dynamic `th:placeholder` attributes for forename, surname, and target forwarding email in `auth/register.html`, verifying template syntax.
- [x] 2.3 Replace hardcoded `'Recently'` fallback string in `user/profile.html` with dynamic `#messages.msg('profile.token.recently')`, verifying fallback rendering when timestamp is absent.
- [x] 2.4 Add dynamic `th:title` in `admin/domains.html` and dynamic `th:aria-label` attributes across pagination controls, batch toolbars, duration preset groups, and mobile offcanvas navigation toggles, verifying rendered accessibility markup.

## 3. Controller Error Message Localization

- [x] 3.1 Inject `MessageSource` into `ProfileController` and update `@PostMapping` handler methods (`/change-password` and `/delete`) to accept request `Locale` and resolve localized `errorMessage` strings (`final` parameters, comprehensive Javadoc, and OWASP safety), verifying via `ProfileControllerTest`.
- [x] 3.2 Inject `MessageSource` into `WebAuthController` and update `@PostMapping` handler methods (`/register` and `/reset-password`) to accept request `Locale` and resolve localized `errorMessage` strings (`final` parameters, comprehensive Javadoc, and OWASP safety), verifying via `WebAuthControllerTest`.

## 4. Verification & Testing Pyramid

- [x] 4.1 Add happy path, error case, and boundary tests in `ProfileControllerTest` and `WebAuthControllerTest` verifying that error alerts render in German when requested with `Locale.GERMAN` and in English by default.
- [x] 4.2 Verify OWASP ASVS / XSS security standards by asserting automatic HTML entity escaping on rendered localized placeholders and error text.
- [x] 4.3 Execute full multi-module build and test suite (`mvn clean test`) to confirm zero regressions and verify all tests pass.

