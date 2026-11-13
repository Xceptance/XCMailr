# Tasks

## 1. Thymeleaf Template Message Key Support

- [x] 1.1 Update `xcmailr-webapp/src/main/resources/templates/user/profile.html` to support `errorMessageKey` via `th:text="${errorMessageKey != null ? #messages.msg(errorMessageKey) : errorMessage}"`, and verify Thymeleaf syntax
- [x] 1.2 Update `xcmailr-webapp/src/main/resources/templates/auth/register.html` to support `errorMessageKey` with fallback and preserve submitted `language` selection via `th:selected="${(language != null ? language : #locale.language) == 'en'}"` (and `'de'`), and verify Thymeleaf syntax
- [x] 1.3 Update `xcmailr-webapp/src/main/resources/templates/auth/reset-password.html` and `xcmailr-webapp/src/main/resources/templates/layout/base.html` to support `errorMessageKey` with fallback, and verify Thymeleaf syntax

## 2. Controller Refactoring & Unit Verification

- [x] 2.1 Refactor `ProfileController` to completely eliminate `MessageSource` injection, constructors, helper `resolveMessage(...)`, and `Locale locale` method parameters (`/change-password` and `/delete`), setting `model.addAttribute("errorMessageKey", ...)` for password mismatch, validation error, encoding error, and delete-account checks, and update `ProfileControllerTest` to verify `errorMessageKey` model attribute assertions without `Locale` parameters
- [x] 2.2 Refactor `WebAuthController` to completely eliminate `MessageSource` injection, constructors, helper `resolveMessage(...)`, and `Locale locale` method parameters (`/register` and `/pwreset/{token}`), setting `model.addAttribute("errorMessageKey", ...)` in `registerSubmit` and `resetPasswordSubmit`, passing `language` in `populateForm(...)`, and update `WebAuthControllerTest` and `WebAuthControllerUnitTest` to verify `errorMessageKey` assertions and `language` retention without `Locale` parameters

## 3. Integration & Testing Pyramid Verification

- [x] 3.1 Add/update integration tests in `WebAuthControllerTest` and `ProfileControllerTest` verifying localized German and English HTML error alert rendering under respective `Locale` / `Accept-Language` headers
- [x] 3.2 Execute complete test suite (`mvn clean test`) to verify zero regressions across web controllers, Thymeleaf rendering, security validators, and the REST API (`/api/v1/**`)

