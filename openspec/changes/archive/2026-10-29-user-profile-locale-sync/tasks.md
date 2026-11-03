# Tasks

## 1. Database-First Locale Resolution

- [x] 1.1 Update `UserPreferenceLocaleResolver.resolveLocale` to query `userRepository.findByMailIgnoreCase(...)` first, falling back to `UserPrincipal` snapshot only if `userRepository` is null or returns empty, enforcing `final` on all parameters and local variables, complete Javadoc, and clear inline comments.
- [x] 1.2 Implement unit tests in `UserPreferenceLocaleResolverTest` following the testing pyramid: happy path tests (DB lookup precedence over stale `UserPrincipal`, valid `de`/`en` tags), error cases (null language, empty string, malformed injection payloads), and special cases (null repository, user not found, unauthenticated header fallback); verify with `mvn test -Dtest=UserPreferenceLocaleResolverTest`.

## 2. In-Session Principal Synchronization in Profile Controller

- [x] 2.1 Update `ProfileController.updateProfile` to synchronize in-session `UserPrincipal`'s mutable user fields (`setForename`, `setSurname`, `setLanguage`) while strictly preventing privilege escalation on security fields (`roles`, `admin`), maintaining `final` keywords on all parameters and local variables, complete Javadoc, and clear inline comments.
- [x] 2.2 Add WebMvc integration tests in `ProfileControllerTest` covering happy path profile language updates, error cases (validation failures leaving DB and session untouched), and special/security cases (CSRF protection, role immutability, unauthenticated access); verify with `mvn test -Dtest=ProfileControllerTest`.

## 3. End-to-End Integration, Security, and Verification

- [x] 3.1 Add integration tests in `MailboxWebControllerTest` covering happy path (profile language update from German to English immediately reflected in full page and HTMX pagination range indicators), error cases, and special cases (tenant isolation, preserving sort/filter params across localized swaps); verify with `mvn test -Dtest=MailboxWebControllerTest`.
- [x] 3.2 Audit code for OWASP principles (tenant isolation via `Authentication.getName()`, parameterized repository queries, injection-safe locale parsing, privilege escalation protection) and ensure `final` keywords and Javadoc are strictly applied across all touched files.
- [x] 3.3 Run the full test suite (`mvn clean test`) to ensure zero regressions across all application modules.
- [x] 3.4 Validate the OpenSpec change using `openspec validate "user-profile-locale-sync" --strict` to ensure spec compliance.
