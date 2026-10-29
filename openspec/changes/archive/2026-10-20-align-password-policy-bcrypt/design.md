# Design

## Context

When a user submitted a 128-character or 129-character password during registration (or password reset / profile change), the application responded with an unhandled HTTP 500 Internal Server Error.

Investigation identified the root cause in the interaction between `PasswordValidator` and Spring Security's `BCryptPasswordEncoder`:
- In commit `787d189`, `PasswordValidator.MAX_LENGTH` was set to 128 characters based on OWASP ASVS guidelines.
- `SecurityConfig` configures `BCryptPasswordEncoder`, which delegates to `org.springframework.security.crypto.bcrypt.BCrypt.hashpw(byte[], String)`.
- The BCrypt Blowfish key schedule strictly limits input to 72 bytes:
  ```java
  if (bytes.length > 72) {
      throw new IllegalArgumentException("password cannot be more than 72 bytes");
  }
  ```
- When a 128-character password was submitted, it passed `PasswordValidator.validate()`, but `passwordEncoder.encode(password)` threw an uncaught `IllegalArgumentException`, resulting in an HTTP 500.
- When a 129-character password was entered into the browser, the client-side `<input maxlength="128">` truncated it to 128 characters, which then failed identically with an HTTP 500.

## Root Cause Analysis: Why Automated Tests Missed This

The previous test suite had 311 passing tests, yet this bug slipped through. The failure occurred because of a classic test pyramid isolation gap:

1. **Isolated Unit Test Layer**: `PasswordValidatorTest` tested `PasswordValidator.validate()` in complete isolation without invoking `BCryptPasswordEncoder`. It asserted that a 128-character string returned `true` (valid), fulfilling its unit-level requirement without testing the downstream cryptographic consumer.
2. **Homogeneous Controller Slice Test Layer**: `WebAuthControllerTest` and `ProfileControllerTest` tested registration, password reset, and password changes, but used only short, typical passwords (such as `"SecureSecret123!"`, ~15 characters). Neither controller test ever submitted boundary passwords (e.g. 72, 73, 128, or 129 characters) or multibyte characters.
3. **Missing Integration Boundary Test**: No test in the test suite ever took a password of maximum allowed length (128 characters) and passed it through the full pipeline: Controller -> `PasswordValidator` -> `BCryptPasswordEncoder.encode()` -> Repository persistence.

As a result, the incompatibility between the validator's maximum permitted length (128 characters) and BCrypt's hard limit (72 bytes) remained completely undetected until manually tested.

## Testing Strategy: The Testing Pyramid

To ensure that boundary conditions, error cases, and special cases are thoroughly tested and that no regression or unhandled exception can ever reach production, this change implements a strict **Testing Pyramid**:

```
                       / \
                      /   \
                     / Reg \   Regression & System Tests
                    /------- \  - Full test suite (311+ tests)
                   /  Slice   \ - Backward compatibility for DB hashes
                  / Controller \ Controller Slice Tests (MockMvc)
                 /--------------\ - Full pipeline: HTTP POST -> Validator -> BCrypt -> DB
                /  Unit Tests    \ - Happy path (72 chars), Error (73, 128, 129), Multibyte
               /  (Fast, Isolated)\ Unit Tests (PasswordValidatorTest)
              /--------------------\ - Character & byte boundaries, error msgs, user context
```

### Layer 1: Unit Tests (`PasswordValidatorTest`)
Fast, isolated unit tests validating every validation rule, boundary, and error message:

- **Happy Path Tests**:
  - `testValidate_MinLength_Boundary`: Exactly 10 characters accepted (e.g. `"P@ssw0rd10"`).
  - `testValidate_TypicalPassphrase`: Typical 20-50 character passphrases with spaces and symbols accepted.
  - `testValidate_MaxLength_Boundary`: Exactly 72 characters ASCII (single-byte UTF-8) accepted.
  - `testValidate_ValidMultibyte_WithinByteLimit`: Valid UTF-8 strings containing German umlauts (`äöü`) or emojis where `getBytes(UTF-8).length <= 72` accepted (e.g. 18 emojis x 4 bytes = 72 bytes).
- **Error Cases**:
  - `testValidate_MinLength_Exceeded`: 9 characters rejected with message: `"Password must be at least 10 characters long."`.
  - `testValidate_MaxLength_Exceeded`: 73 characters rejected with message: `"Password cannot exceed 72 characters."`.
  - `testValidate_ManualTest128Chars_Rejected`: The user's exact 128-character test string rejected with message: `"Password cannot exceed 72 characters."`.
  - `testValidate_ManualTest129Chars_Rejected`: The user's exact 129-character test string rejected with message: `"Password cannot exceed 72 characters."`.
- **Special Cases**:
  - `testValidate_Multibyte_ByteOverflow_Rejected`: String with fewer than 72 characters but exceeding 72 UTF-8 bytes (e.g. 25 emojis = 100 bytes, or 40 German umlauts = 80 bytes) rejected with distinct actionable message: `"Password cannot exceed 72 bytes (special or non-Latin characters use multiple bytes)."`.
  - `testValidate_UserContext_Rejected`: Passwords containing forename, surname, or username (case-insensitive, 3+ characters) rejected.
  - `testValidate_SingleRepeatedCharacter_Rejected`: Passwords consisting of a single repeated character (e.g. `"aaaaaaaaaa"`) rejected.
  - `testValidate_NullAndBlank_Rejected`: Null, empty, and all-whitespace passwords rejected safely without NullPointerException.

### Layer 2: Controller Slice Tests (`WebAuthControllerTest`, `ProfileControllerTest`)
Spring MVC slice tests executing the entire HTTP request pipeline through real controllers, real `PasswordValidator`, real `BCryptPasswordEncoder`, and mocked/real repositories. These tests directly address the manual failure:

- **Happy Path Tests**:
  - `testRegisterSubmit_Boundary72Chars_Success`: POST `/register` with a valid 72-character password.
    - Asserts: HTTP 302 Redirect to `/confirm`, user persisted, `passwordEncoder.encode()` hashes the 72-character password cleanly without throwing `IllegalArgumentException`.
  - `testResetPasswordSubmit_Boundary72Chars_Success`: POST `/reset-password` with 72-character password successfully updates credential.
  - `testChangePasswordSubmit_Boundary72Chars_Success`: POST `/profile/change-password` with 72-character password successfully updates credential.
- **Error Cases**:
  - `testRegisterSubmit_73Chars_RejectedWithValidationFeedback`: POST `/register` with a 73-character password.
    - Asserts: HTTP 200 OK (re-renders `auth/register` view), model contains password validation error ("Password cannot exceed 72 characters"), HTTP status is NOT 500, and `passwordEncoder.encode()` is never reached with invalid input.
  - `testRegisterSubmit_Manual128Chars_RejectedWithout500`: POST `/register` with the user's exact 128-character password.
    - Asserts: HTTP 200 OK, form validation error displayed, NEVER HTTP 500.
  - `testRegisterSubmit_Manual129Chars_RejectedWithout500`: POST `/register` with the user's exact 129-character password.
    - Asserts: HTTP 200 OK, form validation error displayed, NEVER HTTP 500.
  - `testProfileChangePassword_ExceedingLength_RejectedWithout500`: POST `/profile/change-password` with 73, 128, and 129 characters rejected with HTTP 200 and validation feedback, never HTTP 500.
- **Special Cases**:
  - `testRegisterSubmit_MultibyteByteOverflow_RejectedWithout500`: POST `/register` with 25 emojis (char count 25, byte count 100) rejected with HTTP 200 and byte-limit error message, never HTTP 500.
  - `testRegisterSubmit_EncoderException_HandledDefensively`: Simulates or verifies defensive catch in `WebAuthController` and `ProfileController`: if an `IllegalArgumentException` were thrown during encoding, the controller catches it, logs a warning, and returns HTTP 200 with user-friendly error message rather than bubbling up as an uncaught 500.
  - `testTemplatesRenderMaxlength72AndGuidance`: GET `/register`, GET `/reset-password`, and GET `/profile` render HTML containing `<input ... maxlength="72" minlength="10">` and guidance text `"At least 10 characters (up to 72)"`.

### Layer 3: Regression and System Tests
- `mvn test -pl xcmailr-webapp`: Executes all existing 311+ tests across authentication, API, SMTP, mailboxes, and security layers to ensure zero regressions.
- Verify backward compatibility: Existing users in database with BCrypt hashes (regardless of original length) authenticate successfully via `/login`.

## Goals / Non-Goals

**Goals:**
- Enforce a strict 72-character maximum and 72-byte UTF-8 maximum in `PasswordValidator` during credential creation and modification.
- Return descriptive, user-friendly validation messages when passwords exceed character or byte limits rather than crashing with an HTTP 500.
- Synchronize HTML5 client attributes (`maxlength="72"`) and helper text in all credential forms (`register.html`, `reset-password.html`, `profile.html`).
- Add defensive exception handling in controllers (`WebAuthController`, `ProfileController`) around `passwordEncoder.encode(...)` as a defense-in-depth measure.
- Maintain 100% backward compatibility with all existing database passwords and legacy BCrypt hashes.
- Implement complete testing pyramid coverage ensuring all happy paths, error boundaries, and special multibyte cases are verified automatically.

**Non-Goals:**
- Pre-hashing passwords with SHA-256 before BCrypt (out of scope to avoid breaking existing database credentials or introducing complex migration schemes).
- Changing password hashing algorithms (Argon2, PBKDF2) or altering the database schema.

## Decisions

### 1. Dual Constraint: Character Length (72) and UTF-8 Byte Length (72)
- **Decision**: Define `MAX_LENGTH = 72` and `MAX_BYTES = 72` in `PasswordValidator`. Evaluate both `password.length() > MAX_LENGTH` and `password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES`.
- **Rationale**: Single-byte ASCII characters map 1:1 between characters and bytes, but UTF-8 encodes non-ASCII characters (such as German umlauts `ä, ö, ü` at 2 bytes or emojis `🚀` at 4 bytes) across multiple bytes. Without an explicit byte length check, a 30-character passphrase containing emojis would pass character validation but crash `BCrypt.hashpw()`.
- **Alternatives Considered**:
  - *Only check character length (72)*: Would crash when users submit short passphrases composed of multiple emojis or extended Unicode.
  - *Pre-hash with SHA-256*: Would permit arbitrary password lengths (e.g. up to 128+ chars) by compressing the input to 32 bytes prior to BCrypt. Rejected because it alters the cryptographic digest and immediately invalidates all existing passwords in the database unless a complex dual-authenticator migration path is built.

### 2. Actionable Validation Messages
- **Decision**: Return specific feedback depending on which threshold was exceeded:
  - Exceeding characters: `"Password cannot exceed 72 characters."`
  - Exceeding bytes: `"Password cannot exceed 72 bytes (special or non-Latin characters use multiple bytes)."`
- **Rationale**: Informs the user clearly why their password was rejected and how to fix it.

### 3. Client-Side Synchronization in Thymeleaf Templates
- **Decision**: Update `maxlength="72"` on password input fields and adjust helper text from "up to 128" to "up to 72" in:
  - `auth/register.html`
  - `auth/reset-password.html`
  - `user/profile.html`
- **Rationale**: Provides immediate browser feedback while matching server-side validation rules.

### 4. Controller Defensive Catch Guard
- **Decision**: Wrap `passwordEncoder.encode(password)` in `WebAuthController` (`registerSubmit`, `resetPasswordSubmit`) and `ProfileController` (`changePasswordSubmit`) in a `try-catch` block catching `IllegalArgumentException`. If triggered, log a warning and return the form view with an appropriate error message instead of bubbling up as an unhandled HTTP 500 error.
- **Rationale**: Defense-in-depth ensuring that no unexpected encoder runtime exception can ever produce an unhandled 500 error for users.

## Risks / Trade-offs

- **[Risk] Maximum length reduced from 128 to 72 characters** → **Mitigation**: 72 characters exceeds OWASP ASVS v4.0 Requirement 2.1.2 (which requires permitting at least 64 characters). 72 characters accommodates 6-8 random passphrase words with ample entropy (>256 bits).
- **[Risk] Multibyte characters exceed byte limit before character limit** → **Mitigation**: The explicit byte check flags this gracefully with actionable user messaging, and `maxlength="72"` prevents oversized ASCII input in standard browsers.
