# Tasks

## 1. Configuration, Documentation, and Coding Standards

- [x] 1.1 Add `requireConfirmation` boolean property (default `true`) to `XcmailrProperties.AppProperties` in `XcmailrProperties.java` using `final` method parameters, full Javadoc, and property binding.
- [x] 1.2 Add `xcmailr.app.require-confirmation: ${APP_REQUIRE_CONFIRMATION:true}` to `application.yml` with detailed comments explaining default behavior and offline development usage.
- [x] 1.3 Document the email confirmation toggle, environment variable `APP_REQUIRE_CONFIRMATION`, and security implications in `README.md` under the Configuration section.

## 2. Authentication and Web Flow Implementation

- [x] 2.1 Update alert banners in `login.html` to provide accurate messaging for `param.registered` (check email), `param.ready` (immediate login), `param.unconfirmed` (activation required), `param.invalidToken` (invalid link), and `param.expiredToken` (expired link).
- [x] 2.2 Implement password-first authentication provider and custom failure handler in `SecurityConfig.java` to prevent user enumeration (CWE-204) and route inactive valid-password users to `/login?unconfirmed`, using `final` modifiers, full Javadocs, and security rationale comments.
- [x] 2.3 Update `WebAuthController.registerSubmit` and `confirmAccount` using `final` modifiers, complete Javadoc, and explanatory comments: branch on `requireConfirmation`, handle invalid/expired tokens gracefully, and strictly avoid logging tokens or URLs (CWE-532).

## 3. Testing Pyramid - Unit Tests (Happy Paths, Error Paths & Edge Cases)

- [x] 3.1 Create `AuthenticationProviderUnitTest` using isolated Mockito mocks with `final` modifiers and Javadocs:
  - Verify valid credentials on active account return authenticated token.
  - Verify invalid credentials on active account throw `BadCredentialsException`.
  - Verify invalid credentials on inactive account throw `BadCredentialsException` (anti-enumeration check).
  - Verify non-existent user throws `BadCredentialsException` or `UsernameNotFoundException`.
  - Verify valid credentials on inactive account throw `DisabledException`.
- [x] 3.2 Create or expand `WebAuthControllerUnitTest` with isolated Mockito mocks using `final` modifiers and Javadocs:
  - Verify registration branching when `requireConfirmation` is `true` (inactive, token set, email sent).
  - Verify registration branching when `requireConfirmation` is `false` (active, no token, no email).
  - Verify password confirmation mismatch triggers validation error.
  - Verify duplicate email registration (case-insensitive) triggers duplicate error message.
  - Verify confirmation with invalid or non-existent token redirects to `/login?invalidToken`.
  - Verify confirmation with expired token timestamp redirects to `/login?expiredToken`.
- [x] 3.3 Add unit test in `WebAuthControllerUnitTest` verifying token redaction in logs during registration and confirmation processing.

## 4. Testing Pyramid - Integration & Roundtrip Verification

- [x] 4.1 Implement complete roundtrip integration test in `WebAuthControllerTest` for email confirmation flow: register $\to$ assert blocked login with `/login?unconfirmed` $\to$ confirm via `/confirm/{token}` $\to$ assert successful login and authenticated session at `/`.
- [x] 4.2 Implement roundtrip integration test in `WebAuthControllerTest` for auto-activation flow: register with `requireConfirmation = false` $\to$ assert redirect to `/login?ready` $\to$ assert immediate successful login at `/`.
- [x] 4.3 Implement negative and edge case integration tests in `WebAuthControllerTest`:
  - Anti-enumeration: wrong password for unconfirmed user redirects to `/login?error`.
  - Invalid confirmation token: `/confirm/invalid-token-xyz` redirects to `/login?invalidToken` without activating user.
  - Expired confirmation token: `/confirm/{expiredToken}` redirects to `/login?expiredToken` without activating user.
  - Case-insensitive login: registration with lowercase, login with uppercase credentials.
  - Duplicate registration: duplicate submission fails with appropriate validation feedback.
- [x] 4.4 Execute full project test suite (`mvn clean test`) to confirm zero regressions across all test classes.
