# Proposal

## Why

While core page labels and navigation menus are internationalized in English and German, several user-facing elements still render hardcoded English strings when navigating in German (`de`). Specifically:
1. Form input placeholders for new passwords and password confirmations remain static English text across profile, registration, and reset-password forms (`"At least 10 characters"`, `"Repeat new password"`).
2. Registration input placeholders for names and target email addresses remain hardcoded English examples.
3. Fallback timestamp text for API token creation dates defaults to hardcoded `'Recently'`.
4. Input pattern validation tooltips (`title`) and screen reader pagination/action landmarks (`aria-label`) lack dynamic localization bindings.
5. Server-side form validation error banners in `ProfileController` and `WebAuthController` (such as password mismatch or password quality violations) are returned as hardcoded English strings.

Resolving these gaps completes the end-to-end localization of the web console UI, ensuring consistent English and German experiences without architectural overengineering.

## What Changes

- **Password Form Input Placeholders**: Add dynamic `th:placeholder` attributes referencing localized message bundle keys for all password and password confirmation inputs in `user/profile.html`, `auth/reset-password.html`, and `auth/register.html`.
- **Registration Example Placeholders**: Add dynamic `th:placeholder` attributes for forename, surname, and target forwarding email in `auth/register.html`.
- **Profile Timestamp Fallback**: Replace hardcoded inline `'Recently'` fallback in `user/profile.html` with localized `#messages.msg('profile.token.recently')`.
- **Validation Pattern Tooltips**: Bind `th:title` on domain input validation in `admin/domains.html`.
- **Screen Reader ARIA Labels**: Add dynamic `th:aria-label` attributes for pagination navigation and toolbar landmarks in admin and mailbox table fragments and modals.
- **Controller Error Message Localization**: Inject Spring's `MessageSource` and accept request `Locale` in `ProfileController` and `WebAuthController` to resolve localized error banner strings for password mismatches, credential mismatches, duplicate emails, and sole administrator deletion protection.
- **Password Validator Result Message Keys**: Extend `PasswordValidationResult` with an optional `messageKey` while preserving `errorMessage` as fallback, allowing controllers to resolve localized password complexity failure feedback.
- **Message Bundle Parity**: Add all new translation keys to both `conf/messages.properties` and `conf/messages_de.properties`, verified via `MessageBundleParityTest`.
- **Testing Pyramid Coverage**: Add unit, controller, and integration tests verifying both English (`en`) and German (`de`) rendering for happy paths, validation error banners, boundary cases, and OWASP XSS safety.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `web-console-ui`: Updates requirement `Comprehensive Web Console Internationalization` to mandate localized form input placeholders, fallback text, tooltips, ARIA landmarks, and controller-generated validation error messages across German and English locales.

## Impact

- **Templates**: `xcmailr-webapp/src/main/resources/templates/{user/profile.html, auth/register.html, auth/reset-password.html, admin/domains.html, admin/fragments/*.html, mailboxes/fragments/*.html, layout/base.html}`.
- **Controllers & Security**: `ProfileController.java`, `WebAuthController.java`, `PasswordValidationResult.java`, and `PasswordValidator.java`.
- **Properties**: `conf/messages.properties` and `conf/messages_de.properties`.
- **Tests**: `MessageBundleParityTest.java`, `ProfileControllerTest.java`, `WebAuthControllerTest.java`, `PasswordValidatorTest.java`.
- **APIs & Database**: No schema or REST API changes; strictly web console presentation and localized controller error feedback.

