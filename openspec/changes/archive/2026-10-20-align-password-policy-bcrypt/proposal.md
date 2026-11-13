# Proposal

## Why

When users attempt to register, reset, or change a password with a length exceeding 72 bytes (such as 128-character passphrases or multibyte Unicode strings), Spring Security's `BCryptPasswordEncoder` throws an unhandled `IllegalArgumentException: password cannot be more than 72 bytes`, causing an HTTP 500 Internal Server Error. 

Aligning the maximum password length to 72 characters and enforcing a strict 72-byte UTF-8 limit prevents runtime crashes while maintaining compliance with OWASP ASVS v4.0 (which mandates permitting at least 64 characters) and preserving 100% backward compatibility for existing credentials in the database.

### Why Previous Automated Tests Missed This

During manual testing, submitting 128-character and 129-character passwords resulted in an unexpected HTTP 500. This bug escaped the existing automated test suite (311 passing tests) because of an isolation gap in the test pyramid:
1. `PasswordValidatorTest` validated 128 characters in pure isolation without invoking `BCryptPasswordEncoder.encode()`.
2. `WebAuthControllerTest` and `ProfileControllerTest` only tested short sample passwords (~15 characters).
3. No slice or integration test passed boundary passwords (72, 73, 128, 129 characters or multibyte strings) through the full controller-to-encoder pipeline.

## What Changes

- **Update Maximum Password Length**: Lower `PasswordValidator.MAX_LENGTH` from 128 to 72 characters.
- **Enforce UTF-8 Byte Limit**: Add validation in `PasswordValidator` ensuring password byte length in UTF-8 does not exceed 72 bytes, providing user-friendly validation messages for multibyte characters (e.g. emojis or extended Unicode) rather than triggering unhandled server exceptions.
- **Update UI Form Attributes and Guidance**: Update `maxlength` attributes from `128` to `72` and adjust helper text from "up to 128" to "up to 72" in `register.html`, `reset-password.html`, and `profile.html`.
- **Defensive Controller Exception Handling**: Ensure controllers handling password updates (`WebAuthController`, `ProfileController`) defensively catch potential encoding argument errors (`IllegalArgumentException`) and present clean validation feedback instead of raw 500 pages.
- **Testing Pyramid Test Suite**:
  - **Unit Tests (`PasswordValidatorTest`)**: Happy path (10 chars, typical passphrases, 72 chars, valid multibyte <= 72 bytes), error cases (9 chars, 73 chars, manual 128/129 chars), and special cases (multibyte overflow > 72 bytes, user context, repeated chars, null/blank).
  - **Controller Slice Tests (`WebAuthControllerTest`, `ProfileControllerTest`)**: Full pipeline MockMvc tests verifying 72-character acceptance into BCrypt, 73/128/129-character rejections returning HTTP 200 with form errors (never HTTP 500), multibyte overflow rejections, and defensive exception handling.
  - **Regression Tests**: Full test suite execution ensuring all existing functionality and legacy database credential authentication remain 100% intact.

## Capabilities

### New Capabilities

*(None)*

### Modified Capabilities

- `security-hardening`: Update the password creation policy requirements to specify a 72-character and 72-byte maximum length constraint aligned with the BCrypt cryptographic key schedule instead of 128 characters.

## Impact

- **Affected Code**:
  - `com.xceptance.xcmailr.security.PasswordValidator`
  - `com.xceptance.xcmailr.controllers.WebAuthController`
  - `com.xceptance.xcmailr.controllers.ProfileController`
  - `templates/auth/register.html`
  - `templates/auth/reset-password.html`
  - `templates/user/profile.html`
- **Tests**:
  - `com.xceptance.xcmailr.security.PasswordValidatorTest`
  - `com.xceptance.xcmailr.controllers.WebAuthControllerTest`
  - `com.xceptance.xcmailr.controllers.ProfileControllerTest`
- **APIs/Dependencies**: No new dependencies or breaking API contract changes; existing database hashes remain completely valid.
