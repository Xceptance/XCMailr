# Proposal: Template-Driven Message Key Error Localization

## Why

In `WebAuthController` and `ProfileController`, form error messages are currently translated in Java before being passed to views. This couples controllers to Spring's `MessageSource` and forces `final Locale locale` into controller method signatures, creating conceptual duplication with user preference parameters (`@RequestParam("language")`) and leading to potential language desynchronization on registration error re-renders.

Decoupling controllers from message resolution and passing semantic message keys (e.g. `errorMessageKey`) directly to Thymeleaf views aligns with modern Spring MVC best practices, eliminates controller boilerplate, and ensures Thymeleaf natively resolves messages in the user's preferred locale while preserving form state and maintaining complete backward compatibility with the existing REST API.

## What Changes

- **Controller Message Key Handing**: Update `ProfileController` and `WebAuthController` to pass translation message keys (`errorMessageKey`) to the template model / flash attributes instead of translating messages in Java.
- **Remove Controller Boilerplate**: Completely eliminate `MessageSource` injection, constructors, and `final Locale locale` method parameters from `ProfileController` and `WebAuthController` form submission handlers. No controller across the entire application will require `Locale` or `MessageSource` dependencies.
- **Thymeleaf Template Error Resolution**: Update error banner fragments in `user/profile.html`, `auth/register.html`, and `auth/reset-password.html` to evaluate `errorMessageKey` using `#{${errorMessageKey}}`, falling back gracefully to `errorMessage` for backward compatibility.
- **Registration Form State Preservation**: Update `populateForm(...)` in `WebAuthController` to pass the submitted `language` parameter back into the model so the `<select name="language">` dropdown retains the user's selection on validation errors.
- **REST API Untouched**: Explicitly guarantee that the programmatic JSON REST API (`/api/v1/**` via `MailApiController` and `MailboxApiController`) remains 100% untouched and backward-compatible.
- **Testing Pyramid Verification**: Update existing controller and integration tests to verify both key-based resolution and localized alert rendering across English and German locales.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-console-ui`: Refines the server-side form validation and password error scenarios to specify template-driven message key resolution based on the active user/request locale.

## Impact

- **Affected Code**: `WebAuthController.java`, `ProfileController.java`, `user/profile.html`, `auth/register.html`, `auth/reset-password.html`.
- **API Impact**: None. The REST API (`/api/v1/**`) remains completely unaffected.
- **Dependencies**: No external dependencies added.

