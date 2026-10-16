# Tasks

## 1. Security Configuration Update

- [x] 1.1 Update `SecurityConfig.java` to configure `.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))` on `apiSecurityFilterChain` and remove unused `AbstractHttpConfigurer` import; verify the project compiles with `mvn test-compile -pl xcmailr-webapp`.

## 2. Unit & Security Slice Tests (Testing Pyramid)

- [x] 2.1 Add happy-path slice tests in `SecurityConfigTest.java` verifying mutating REST API requests (`POST`, `PUT`, `DELETE` to `/api/v1/**`) with valid Bearer token succeed without CSRF tokens, and web POST requests with valid CSRF tokens pass security; verify with `mvn test -pl xcmailr-webapp -Dtest=SecurityConfigTest#testApiMutatingRequestsExemptFromCsrf,SecurityConfigTest#testWebPostWithValidCsrf`.
- [x] 2.2 Add error-case slice tests in `SecurityConfigTest.java` verifying web requests missing CSRF tokens or supplying invalid CSRF tokens are rejected with HTTP 403 Forbidden, and mutating API requests without valid Bearer tokens are rejected with HTTP 401 Unauthorized; verify with `mvn test -pl xcmailr-webapp -Dtest=SecurityConfigTest#testWebPostMissingCsrfRejected,SecurityConfigTest#testWebPostInvalidCsrfRejected,SecurityConfigTest#testApiMutatingRequestUnauthorizedWithoutToken`.
- [x] 2.3 Add special-case slice tests in `SecurityConfigTest.java` verifying that API endpoints ignore spurious/malformed CSRF headers when a valid Bearer token is provided, and non-API path boundaries outside `/api/**` strictly enforce CSRF; verify with `mvn test -pl xcmailr-webapp -Dtest=SecurityConfigTest#testApiIgnoresMalformedCsrfHeader,SecurityConfigTest#testNonApiPathEnforcesCsrf`.

## 3. Integration & Regression Verification

- [x] 3.1 Run end-to-end REST API integration tests verifying full CRUD operations succeed without CSRF tokens and invalid tokens are rejected with 401; verify with `mvn test -pl xcmailr-webapp -Dtest=MailboxApiIT,MailApiIT`.
- [x] 3.2 Run the combined security and integration test suite to ensure zero regressions across web and API security filter chains; verify with `mvn test -pl xcmailr-webapp -Dtest=SecurityConfigTest,MailboxApiIT,MailApiIT`.
