# Design

## Context

When an authenticated user updates their language in the profile settings, the change is saved to the database via `userRepository.save(user)`. However, `UserPreferenceLocaleResolver.resolveLocale(...)` historically checked the in-session `UserPrincipal` before querying `UserRepository`. Because `ProfileController.updateProfile(...)` only updated the database entity and did not update the `UserPrincipal` snapshot stored in Spring Security's `SecurityContext`, `UserPreferenceLocaleResolver` continued reading the stale login-time language preference.

See `proposal.md` for background motivation and `specs/web-console-ui/spec.md` for behavioral requirements.

## Goals / Non-Goals

**Goals:**
- Guarantee immediate locale synchronization across all web pages and HTMX partial requests as soon as a user updates their language in `/profile`, without requiring re-authentication or page reloads.
- Establish the database as the authoritative single source of truth for user locale preferences across multiple browser tabs, concurrent sessions, and administrative updates.
- Keep in-memory session state consistent by synchronizing the user reference within `UserPrincipal` during profile updates.
- Preserve backward-compatible fallback for unauthenticated requests and isolated unit tests where `UserRepository` may be absent.
- Enforce OWASP security principles: prevent broken access control, avoid privilege escalation, and provide injection-resilient locale parsing.
- Provide comprehensive test coverage across the testing pyramid (unit and WebMvc integration tests) covering happy paths, error cases, and special/security cases.
- Enforce strict coding standards: `final` on all parameters/variables/fields, complete Javadoc, and clear inline comments.

**Non-Goals:**
- Re-architecting Spring Security authentication tokens or session replication mechanisms.
- Introducing client-side cookie or query parameter-based locale switching (locale remains strictly tied to user profile preference, with HTTP `Accept-Language` header fallback for anonymous requests).
- Modifying SMTP inbound engine, REST API, or CLI behavior.

## Decisions

### Decision 1: Option 3 (Defense-in-Depth) Architecture
We adopt a two-layer defense-in-depth approach combining DB-first locale resolution with in-session principal synchronization.

- **Layer 1: Database-First Locale Resolution in `UserPreferenceLocaleResolver`**:
  ```java
  if (userRepository != null) {
      final Optional<User> userOpt = userRepository.findByMailIgnoreCase(auth.getName());
      if (userOpt.isPresent()) {
          final String lang = userOpt.get().getLanguage();
          if (StringUtils.hasText(lang)) {
              return Locale.forLanguageTag(lang);
          }
      }
  }
  // Fallback to UserPrincipal snapshot if userRepository is null or user not found
  if (auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
      final User user = userPrincipal.getUser();
      if (user != null && StringUtils.hasText(user.getLanguage())) {
          return Locale.forLanguageTag(user.getLanguage());
      }
  }
  ```
- **Layer 2: In-Session `UserPrincipal` Synchronization in `ProfileController.updateProfile`**:
  ```java
  if (principal instanceof Authentication auth && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
      final User sessionUser = userPrincipal.getUser();
      if (sessionUser != null) {
          sessionUser.setForename(user.getForename());
          sessionUser.setSurname(user.getSurname());
          sessionUser.setLanguage(user.getLanguage());
      }
  }
  ```

*Rationale*:
- Querying the database first makes the persistent store the single authoritative source of truth. Any profile update in one tab or device immediately affects all other tabs and sessions.
- Synchronizing the in-memory `UserPrincipal` ensures that any existing or future component that inspects the session principal directly (e.g. headers, audit logs) receives current data rather than stale login-time data.
- Maintaining the fallback in `UserPreferenceLocaleResolver` guarantees that standalone unit tests (which may construct a resolver with a null repository or test mock principals) continue to pass without regression.

*Alternatives Considered*:
- *Option 1 (Resolver DB-first only)*: Leaves `UserPrincipal` stale in session memory, creating subtle desynchronization bugs if other controllers or security filters read `UserPrincipal.getUser()`.
- *Option 2 (ProfileController session sync only)*: Fails if the user updates profile settings in another tab or device, or if an administrator updates user preferences directly in the database.

### Decision 2: Fallback Hierarchy and Case-Insensitive User Lookup
In `UserPreferenceLocaleResolver`, the lookup order is:
1. Authenticated user → `userRepository.findByMailIgnoreCase(auth.getName())`
2. Fallback → `UserPrincipal.getUser().getLanguage()` (if repository is null or returns empty)
3. Fallback → Default Spring MVC request locale (honoring HTTP `Accept-Language` headers for anonymous/unauthenticated requests)
4. Fallback → Application default (`Locale.ENGLISH`)

*Rationale*: Case-insensitive lookup matches the authentication and repository convention throughout XCMailr (`findByMailIgnoreCase`).

## OWASP Security Principles & Threat Analysis

1. **A01: Broken Access Control & Tenant Isolation**:
   - The user identity is determined strictly from the cryptographically verified Spring Security `Authentication.getName()` principal.
   - The resolver never inspects client-controlled parameters or unauthenticated session cookies to determine user identity. Cross-tenant locale probing or manipulation is impossible.
2. **A03: Injection Resilience**:
   - Database queries use Spring Data JPA parameterized queries (`findByMailIgnoreCase`), precluding SQL injection.
   - Language tags from the database or principal are resolved safely using standard Java `Locale.forLanguageTag(...)` without reflection, dynamic script evaluation, or file system lookups. Malformed or hostile strings (such as CRLF headers or path traversal sequences) resolve safely to root/fallback locales without error or header pollution.
3. **A04: Insecure Design & Privilege Escalation Prevention**:
   - When updating the session `UserPrincipal`, only explicit non-security fields (`forename`, `surname`, `language`) are copied.
   - Security-critical properties (`roles`, `admin` flag, password hash, account status) are never touched or exposed to mutation during profile update synchronization, completely preventing privilege escalation attacks.
4. **A05: Security Misconfiguration & Error Handling**:
   - In any failure condition (e.g. user not found in database, empty language field, unexpected exception), the resolver fails closed to a safe fallback (`Locale.ENGLISH`) without leaking stack traces or internal entity metadata.
5. **A07: Identification and Authentication Failures**:
   - Existing session lifecycle and CSRF protections on `/profile` are preserved completely without invalidating active security credentials or introducing session fixation vulnerabilities.

## Coding Standards

- **Immutability & Finality**: Use `final` keywords on all method parameters, local variables, and field definitions across all production and test classes.
- **Documentation**: Provide clear, comprehensive Javadoc on all modified classes and methods, explaining purpose, `@param`, and `@return` contracts.
- **Code Comments**: Add concise inline comments explaining the DB-first rationale, fallback branches, and in-memory session synchronization steps.

## Risks / Trade-offs

- **[Risk] Additional database query on every HTTP request during locale resolution.**
  → *Mitigation*: The lookup `userRepository.findByMailIgnoreCase(...)` queries the unique indexed email column on the `users` table. In Postgres/H2, this is an indexed point-lookup taking < 0.1ms, already cached in the buffer pool. Furthermore, all main web controllers (`MailboxWebController`, `ProfileController`) already perform this lookup on every request. There is no measurable performance impact.
- **[Risk] Null `userRepository` in existing unit tests.**
  → *Mitigation*: Defensive null check ensures tests instantiating `UserPreferenceLocaleResolver(null)` or with empty repositories fall back cleanly to `UserPrincipal` or default request locales without throwing `NullPointerException`.
- **[Risk] Session synchronization when `principal` is not an `Authentication` or not a `UserPrincipal`.**
  → *Mitigation*: Safe pattern matching (`principal instanceof Authentication auth && auth.getPrincipal() instanceof UserPrincipal userPrincipal`) and null checks on `userPrincipal.getUser()`.

## Testing Plan (Testing Pyramid)

### Tier 1: Unit Tests (`UserPreferenceLocaleResolverTest`)
- **Happy Path**:
  - Resolver returns `Locale.GERMAN` when database contains `language = "de"`.
  - Resolver returns `Locale.ENGLISH` when database contains `language = "en"`.
  - Precedence verification: Resolver returns DB language when DB has `en` while `UserPrincipal` contains stale `de`.
- **Error Cases**:
  - Persisted user language is empty string (`""`) → falls back to default request locale.
  - Persisted user language is unsupported/invalid tag (e.g. `"xyz"`) → falls back to standard locale without exception.
  - Malformed language tag with special characters or injection payloads → handles safely and falls back cleanly.
- **Special / Fallback Cases**:
  - `userRepository` is null (isolated unit test mode) → falls back to `UserPrincipal.getUser().getLanguage()`.
  - User not found in DB (`Optional.empty()`) → falls back to `UserPrincipal`.
  - Unauthenticated request with `Accept-Language: de` → resolves German from request headers.
  - Unauthenticated request with missing `Accept-Language` → falls back to application default (`Locale.ENGLISH`).

### Tier 2: WebMvc Integration Tests (`ProfileControllerTest`, `MailboxWebControllerTest`)
- **Happy Path**:
  - Profile update lifecycle: Authenticated user switches language from German to English in `/profile`, verifying DB entity save and in-session `UserPrincipal` synchronization.
  - Immediate view update: Subsequent GET `/mailboxes` full page load resolves English and renders `Showing 1–15 of 17 mailboxes`.
  - HTMX partial swap: Subsequent GET `/mailboxes` with `HX-Request: true` returns the table fragment localized in English.
- **Error Cases**:
  - Form validation failure on `/profile` (e.g. invalid input): rejects update, retains original locale in DB and session, and renders error messages in the active language.
- **Special / Security Cases (OWASP)**:
  - CSRF protection: POST `/profile` without CSRF token returns HTTP 403 Forbidden without modifying user data.
  - Tenant isolation: User cannot modify another user's profile or locale.
  - Privilege escalation check: Verify user's admin status and roles remain unchanged after profile update.
  - Unauthenticated access: GET/POST `/profile` without authentication redirects to `/login`.
