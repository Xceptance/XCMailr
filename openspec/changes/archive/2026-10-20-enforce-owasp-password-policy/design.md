# Technical Design: OWASP Password Policy Enforcement

## Context

See `proposal.md` for background and motivation.

Currently, XCMailr validates passwords using duplicated `password == null || password.length() < 6` checks inside:
1. `WebAuthController.registerSubmit`
2. `WebAuthController.resetPasswordSubmit`
3. `ProfileController.changePassword`

Spring Security authenticates users via `PasswordFirstAuthenticationProvider` which delegates solely to `PasswordEncoder.matches(rawPassword, storedHash)`. Existing database accounts have valid BCrypt hashes and do not undergo validation on login.

## Goals / Non-Goals

**Goals:**
- Provide a clean, zero-dependency Spring `@Component` `PasswordValidator` implementing modern OWASP ASVS v4.0 and NIST SP 800-63B standards.
- Enforce length constraints (minimum 10 characters, maximum 128 characters).
- Enforce user context checks (reject passwords containing user's email local part, forename, or surname).
- Enforce repetition defense (reject passwords consisting solely of a single repeated character).
- Support full character freedom (spaces, passphrases, Unicode, punctuation) without artificial composition rules.
- Display visible password requirements on all password forms (`register.html`, `reset-password.html`, `profile.html`) along with HTML5 attributes (`minlength="10"`, `maxlength="128"`).
- Provide distinct, user-actionable error messages for each rule violation.
- Preserve 100% backward compatibility for existing database credentials during login.
- Adhere to strict code quality: `final` wherever possible, comprehensive Javadoc, and detailed code comments.
- Follow the testing pyramid with unit, controller/slice, and integration tests covering happy paths, edge cases, and error conditions.

**Non-Goals:**
- Outbound network breach checks (e.g. HaveIBeenPwned API calls via `HaveIBeenPwnedRestApiPasswordChecker`), keeping the application self-contained, fast, and fully operable offline.
- Legacy password expiration or forced mass password resets for existing production database users.
- Arbitrary composition rules (e.g. requiring 1 uppercase, 1 symbol, 1 digit) which OWASP explicitly advises against.

## Decisions

### Decision 1: Dedicated Spring Component `PasswordValidator` with Result Record
Instead of using an external library (such as Passay) or scattered controller checks, create a focused Spring component `PasswordValidator`.

```
+---------------------------------------------------------------------------------+
|                       com.xceptance.xcmailr.security                            |
+---------------------------------------------------------------------------------+
|                                                                                 |
|   +--------------------------+           +----------------------------------+   |
|   | PasswordValidationResult | <-------  |        PasswordValidator         |   |
|   +--------------------------+           +----------------------------------+   |
|   | boolean valid            |           | validate(password, context)      |   |
|   | String errorMessage      |           +----------------------------------+   |
|   +--------------------------+                            ^                     |
|                                                           |                     |
|                   +---------------------------------------+                     |
|                   |                                       |                     |
|         +-------------------+                   +-------------------+           |
|         | WebAuthController |                   | ProfileController |           |
|         +-------------------+                   +-------------------+           |
+---------------------------------------------------------------------------------+
```

- **Validation Record**:
  ```java
  public record PasswordValidationResult(boolean isValid, String errorMessage) {
      public static PasswordValidationResult valid() {
          return new PasswordValidationResult(true, null);
      }
      public static PasswordValidationResult invalid(final String message) {
          return new PasswordValidationResult(false, message);
      }
  }
  ```
- **Context Holder**:
  `PasswordContext` record containing `forename`, `surname`, and `mail` to supply contextual identity data for validation.
- **Alternatives Considered**:
  - *Passay Library*: Rejected because it adds external dependencies, unneeded complexity, and potential CVEs for logic that takes ~70 lines of clean Java.
  - *Jakarta Validation Custom Annotation (`@ValidPassword`)*: While viable, the controllers currently accept discrete form parameters (`@RequestParam final String password`) rather than form command objects. A direct validator bean invocation fits existing controller signatures naturally.

### Decision 2: User Context Extraction and Matching
- Extract terms:
  - `forename.trim().toLowerCase(Locale.ROOT)`
  - `surname.trim().toLowerCase(Locale.ROOT)`
  - `mail` local-part: substring before `@`, stripped of separators (`.` or `+`), lowercased.
- Minimum term length threshold: Only check terms with length $\ge 3$ characters to prevent false positives with short initials or abbreviations (e.g., "Al", "Ed").
- Password matching: Normalize password to lowercase and check `passwordLower.contains(term)`.

### Decision 3: Repetition Defense
- Check if all characters in the password are identical:
  ```java
  private boolean isSingleRepeatedCharacter(final String password) {
      final char first = password.charAt(0);
      for (int i = 1; i < password.length(); i++) {
          if (password.charAt(i) != first) {
              return false;
          }
      }
      return true;
  }
  ```

### Decision 4: Preserving Legacy Authentication
- Spring Security's `PasswordFirstAuthenticationProvider` will not be altered. Authentication performs BCrypt verification (`passwordEncoder.matches`) and never triggers `PasswordValidator`. This guarantees existing production credentials continue to log in without friction.

### Decision 5: UI Presentation and Actionable Error Messaging
- In `register.html`, `reset-password.html`, and `profile.html`:
  - Add Bootstrap 5 form helper blocks below the password field describing the requirements (10+ characters, up to 128, no personal info, passphrases and spaces welcomed).
  - Add `minlength="10"`, `maxlength="128"`, and descriptive `placeholder` attributes on password input elements.
  - Controllers map `PasswordValidationResult.errorMessage()` directly to the model's `errorMessage` attribute rendered in the alert banner.

## Risks / Trade-offs

- **[Risk] Existing test fixtures failing due to new length requirement**: Existing tests (such as `WebAuthControllerTest`) creating new accounts in tests using `"secret"` or `"123456"` will be rejected by registration endpoints.
  - *Mitigation*: Update test cases that exercise registration and password reset to use compliant test passwords (e.g., `"Correct-Horse-Battery-2026!"`), while adding specific tests proving that legacy short passwords stored directly in the database can still authenticate successfully.
- **[Risk] False-positive context rejection for short names**:
  - *Mitigation*: Ignore context terms shorter than 3 characters so short 1- or 2-letter names/initials do not block valid passwords.
- **[Risk] High CPU consumption on extremely long passwords**:
  - *Mitigation*: Reject passwords $> 128$ characters before any hashing occurs.

## Testing Strategy (Pyramid)

1. **Unit Tests (`PasswordValidatorTest`)**:
   - Happy paths: standard compliant password, passphrases with spaces, unicode characters, symbols.
   - Boundaries: 9 chars (reject), 10 chars (accept), 128 chars (accept), 129 chars (reject).
   - Error cases: null password, empty password, single repeated character (e.g. `aaaaaaaaaa`, `1111111111`).
   - Context cases: password containing forename, surname, email local part (exact match, case variations, substring inside password), terms $< 3$ characters ignored.
2. **Controller Slice Tests (`WebAuthControllerTest`, `ProfileControllerTest`)**:
   - Form submission with non-compliant password returns HTTP 200, re-renders view, and displays appropriate error message.
   - Form submission with matching vs. non-matching password confirmation.
   - Successful form submission redirects with flash message.
3. **Integration Tests**:
   - Complete registration and password reset flows with compliant passwords.
   - Verifying that an existing user pre-populated in database with a legacy 6-character password can log in without error.

