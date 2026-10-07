# Design

## Context

XCMailr's web application modernizes legacy Ninja framework controllers to Spring Boot 4 and Spring Security 7. In the legacy framework, authentication checked credentials *before* evaluating whether an account was active, ensuring that attackers guessing passwords received a generic error, while authentic users were notified to activate their accounts. In the modernized application, default Spring Security architecture checks `isEnabled()` (i.e. `user.isActive()`) in `preAuthenticationChecks` *before* the password check. Because Spring Security masks this as a generic failure to prevent user enumeration, and `login.html` told users they could log in immediately, users who register cannot log in and are misled by an "Invalid email address or password" error. Furthermore, development environments without outbound SMTP have no mechanism to bypass email confirmation, and error cases (expired tokens, duplicate emails, password mismatches) lack systematic testing.

See `proposal.md` for motivation and high-level scope.

## Goals / Non-Goals

**Goals:**
- Eliminate user enumeration (CWE-204) by validating the user's password *before* disclosing account activation status.
- Provide clear, actionable feedback to users who enter valid credentials for an unactivated account.
- Fix misleading UI banners on `/login` to reflect actual account state, including token validation error states (`?invalidToken`, `?expiredToken`).
- Support an optional `xcmailr.app.require-confirmation` configuration toggle for offline, local development, and self-hosted environments.
- Document configuration and security considerations in `application.yml` and `README.md`.
- Guarantee that bearer confirmation and reset tokens are never written to application logs (CWE-532).
- Adhere strictly to clean code standards: `final` on all parameters, local variables, and fields; comprehensive Javadoc; and meaningful inline comments.
- Establish comprehensive testing pyramid coverage across happy paths, negative error conditions, and security edge cases:
  - **Unit Tests**: Mockito-based tests for controllers and authentication providers isolated from Spring context and database.
  - **Component / Slice Tests**: Web layer and security filter tests verifying HTTP parameter routing and exception handling.
  - **Integration / Roundtrip Tests**: End-to-end user journeys verifying registration, confirmation, and login under both confirmation modes, plus token failure and anti-enumeration edge cases.

**Non-Goals:**
- Modifying the password reset token generation or expiration logic.
- Introducing multi-factor authentication (MFA) or OAuth2 providers.
- Changing REST API token authentication (which operates on active bearer tokens).

## Code Quality and Java Standards

All Java code introduced or modified in this change must strictly adhere to the following standards:
1. **Immutability and `final` Modifiers**:
   - Every method parameter MUST be declared `final` (e.g., `public void authenticate(final Authentication auth)`).
   - Every local variable MUST be declared `final` (e.g., `final User user = userRepository.findByMailIgnoreCase(...)`).
   - All class fields MUST be declared `final` wherever mutable state is not required (e.g., dependency injection fields, constants).
2. **Comprehensive Javadoc**:
   - Every new or modified class, interface, method, and constructor must have full Javadoc explaining its purpose, parameters (`@param`), return values (`@return`), and thrown exceptions (`@throws`).
3. **Intentional Code Comments**:
   - Complex or security-sensitive sections (such as password-first authentication and timing mitigation) must have clear inline comments explaining *why* the code is written that way, citing CWEs and security principles.

## Decisions

### Decision 1: Password-First Credential Validation in Spring Security

- **Choice**: Implement an `AuthenticationProvider` (customizing or extending `DaoAuthenticationProvider`) where password validation is performed *prior* to evaluating `user.isActive()`.
  - When credentials are invalid or the user does not exist: throw `BadCredentialsException`.
  - When credentials are valid, but `user.isActive() == false`: throw `DisabledException`.
- **Rationale**: If `DisabledException` is thrown before verifying the password, any request can determine whether an account exists simply by submitting a dummy password (CWE-204 user enumeration). Verifying the password first guarantees that only a requester in possession of the correct password learns the activation state of that account.
- **Alternatives Considered**: 
  - *Standard `DaoAuthenticationProvider` with generic failure*: Fails user experience; legitimate users have no idea why their login failed.
  - *Disclosing inactive status on all requests*: Creates an immediate account enumeration vulnerability.

### Decision 2: Distinct Authentication Failure Routing

- **Choice**: Configure a custom `AuthenticationFailureHandler` in `SecurityConfig.webSecurityFilterChain`.
  - If exception is `DisabledException`: redirect to `/login?unconfirmed`.
  - For all other authentication failures: redirect to `/login?error`.
- **Rationale**: Preserves standard Spring Security form login architecture while cleanly passing distinct query parameters for Thymeleaf rendering.
- **Alternatives Considered**:
  - *Flash attributes / Session state*: More complex to manage across redirects and potential browser back-button navigations. Query parameters match the existing `?registered`, `?confirmed`, `?error`, and `?logout` design.

### Decision 3: Configurable Email Confirmation Toggle

- **Choice**: Add `requireConfirmation` (default `true`) to `XcmailrProperties.AppProperties` bound to `xcmailr.app.require-confirmation` and environment variable `APP_REQUIRE_CONFIRMATION`.
  - When `true` (production default): registration sets `user.setActive(false)`, generates a token, dispatches email, and redirects to `/login?registered`.
  - When `false` (development / offline): registration sets `user.setActive(true)`, generates no token, dispatches no email, and redirects to `/login?ready`.
- **Rationale**: Allows developers and self-hosters without an active outbound SMTP relay to test and use XCMailr locally without being locked out or requiring manual database interventions.
- **Alternatives Considered**:
  - *Logging the activation link in console*: Violates CWE-532 by exposing secret bearer tokens to log aggregators and terminal buffers.
  - *Always requiring email confirmation*: Breaks local development when SMTP is not configured.

### Decision 4: Thymeleaf UI Banner Alignment and Token Error States

- **Choice**: Update `src/main/resources/templates/auth/login.html` alert banners:
  - `param.registered != null`: *"Registration successful! Please check your email to activate your account before logging in."* (info alert).
  - `param.ready != null`: *"Registration successful! You may now log in."* (success alert for auto-activated accounts).
  - `param.unconfirmed != null`: *"Your account has not been activated yet. Please check your email for the confirmation link."* (warning alert).
  - `param.invalidToken != null`: *"The confirmation link is invalid or has already been used."* (danger alert).
  - `param.expiredToken != null`: *"The confirmation link has expired. Please register again or request a new link."* (danger alert).
  - `param.error != null`: *"Invalid email address or password. Please try again."* (danger alert).

### Decision 5: Documentation in Configuration and README

- **Choice**:
  - Document `xcmailr.app.require-confirmation` in `application.yml` with comments explaining its default value (`true`) and development/self-hosting use cases.
  - Add a dedicated subsection in `README.md` under "Configuration" detailing `APP_REQUIRE_CONFIRMATION` alongside existing security configurations.

## Testing Pyramid Architecture

```
                    / \
                   /   \
                  /     \
                 /  E2E  \       Roundtrip & Edge Case Tests (WebAuthControllerTest)
                / Integr. \      - Happy path journey: register -> confirm -> login
               /-----------\     - Auto-activation journey (requireConfirmation=false)
              /  Component  \    - Negative journeys: invalid/expired tokens, anti-enumeration
             /  MVC Filters  \   Security & MVC Slice Tests
            /-----------------\  - Failure handler routing (/login?unconfirmed vs ?error)
           /    Unit Tests     \ Isolated Unit Tests (WebAuthControllerUnitTest,
          /  Fast, Pure Mocks   \ AuthenticationProviderUnitTest)
         /-----------------------\
```

1. **Unit Testing Layer (Base of the Pyramid)**:
   - `WebAuthControllerUnitTest`: Uses isolated Mockito mocks (`UserRepository`, `OutboundMailService`, `XcmailrProperties`, `PasswordEncoder`) without launching Spring context:
     - **Happy Paths**:
       - Registration branching when `requireConfirmation` is `true` (sets inactive, token generated, email sent, redirects to `/login?registered`).
       - Registration branching when `requireConfirmation` is `false` (sets active, no token, no email, redirects to `/login?ready`).
       - Confirmation with valid token activates user, clears token, and redirects to `/login?confirmed`.
     - **Error & Edge Cases**:
       - Password confirmation mismatch returns validation error with HTTP 200/form view.
       - Duplicate email registration (case-insensitive) returns error banner.
       - Blank/whitespace-only email or password rejected by validation.
       - Confirmation with null or unknown token redirects to `/login?invalidToken`.
       - Confirmation with expired timestamp redirects to `/login?expiredToken`.
       - Confirmation for already active user handles gracefully.
     - **Log Hygiene Test**:
       - Verifies that `LOG.info` calls do not log token values or URLs.
   - `AuthenticationProviderUnitTest`:
     - **Happy Path**: Valid credentials on active user return fully authenticated token with user authorities.
     - **Error & Edge Cases**:
       - Invalid password on active account throws `BadCredentialsException`.
       - Non-existent email throws `BadCredentialsException` or `UsernameNotFoundException` (never `DisabledException`).
       - Valid password on inactive account throws `DisabledException`.
       - **Anti-Enumeration Edge Case**: Invalid password on inactive account throws `BadCredentialsException` (MUST NOT throw `DisabledException`).
       - Case-insensitive email lookup verification.

2. **Component / Slice Testing Layer**:
   - Tests `AuthenticationFailureHandler` redirects:
     - `DisabledException` maps to `/login?unconfirmed`.
     - `BadCredentialsException` maps to `/login?error`.
     - `UsernameNotFoundException` maps to `/login?error`.

3. **Integration / Roundtrip Testing Layer (`WebAuthControllerTest`)**:
   - **Happy Path Journeys**:
     - *User Journey 1 (Email Confirmation Required)*: Register user $\to$ verify DB state (`active=false`, token set) $\to$ attempt login [assert redirected to `/login?unconfirmed`] $\to$ call `/confirm/{token}` [assert redirected to `/login?confirmed`] $\to$ login with password [assert redirected to `/` with authenticated session].
     - *User Journey 2 (Auto-Activation / Offline)*: Dynamic property override `requireConfirmation = false` $\to$ register user $\to$ assert redirected to `/login?ready` $\to$ login immediately $\to$ assert authenticated session at `/`.
   - **Negative & Edge Case Journeys**:
     - *Anti-Enumeration Check*: Submit bad password for unconfirmed user $\to$ assert redirected to `/login?error` (no leak of unconfirmed status).
     - *Invalid Confirmation Token*: Request `/confirm/invalid-token-uuid-1234` $\to$ assert redirected to `/login?invalidToken` and user remains inactive.
     - *Expired Confirmation Token*: Seed user with expired `ts_confirm` in the past $\to$ request `/confirm/{token}` $\to$ assert redirected to `/login?expiredToken` and user remains inactive.
     - *Case-Insensitive Login*: Register `test.case@example.com` $\to$ confirm $\to$ submit login with `TEST.CASE@EXAMPLE.COM` $\to$ assert successful authentication.
     - *Duplicate Registration Attempt*: Register user with email $\to$ attempt second registration with same email (different casing) $\to$ assert rejected with duplicate email feedback.

## Risks / Trade-offs

- **[Risk: Timing Attacks on Missing Users]** → **Mitigation**: BCrypt computation duration naturally mitigates timing differences; for non-existent users, Spring Security's `PasswordEncoder` performs a dummy hash comparison to equalize execution time.
- **[Risk: Accidental Production Inactivity Enforcement Bypass]** → **Mitigation**: `requireConfirmation` defaults to `true`. Clearly document in `README.md` that setting `APP_REQUIRE_CONFIRMATION=false` should only be used in trusted, local, or isolated test environments.
- **[Risk: Stale Tokens]** → **Mitigation**: Confirmation tokens retain an expiration timestamp (`ts_confirm`); expired tokens are rejected on `/confirm/{token}` with `/login?expiredToken`.
