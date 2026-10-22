/*
 * Copyright (c) Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xceptance.xcmailr.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PasswordValidator} following the testing pyramid.
 * <p>
 * Evaluates happy paths, boundary conditions, error handling, and contextual identity
 * matching according to OWASP ASVS v4.0 and NIST SP 800-63B guidelines.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
class PasswordValidatorTest
{
    private PasswordValidator validator;

    @BeforeEach
    void setUp()
    {
        validator = new PasswordValidator();
    }

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests
    {
        @Test
        @DisplayName("Accepts compliant standard password meeting length threshold")
        void acceptsCompliantPassword()
        {
            final PasswordValidationResult result = validator.validate("StrongPass123!");
            assertTrue(result.isValid());
            assertNull(result.errorMessage());
        }

        @Test
        @DisplayName("Accepts multi-word passphrases with whitespace")
        void acceptsPassphraseWithSpaces()
        {
            final PasswordValidationResult result = validator.validate("correct horse battery staple");
            assertTrue(result.isValid());
            assertNull(result.errorMessage());
        }

        @Test
        @DisplayName("Accepts passwords containing diverse Unicode characters and emojis")
        void acceptsUnicodeAndEmojiPassword()
        {
            final PasswordValidationResult result = validator.validate("SüßesPasswörtchen🚀2026!");
            assertTrue(result.isValid());
            assertNull(result.errorMessage());
        }

        @Test
        @DisplayName("Accepts valid password when evaluated against non-matching user context")
        void acceptsValidPasswordWithContext()
        {
            final PasswordContext context = PasswordContext.of("John", "Doe", "johndoe@example.com");
            final PasswordValidationResult result = validator.validate("MountainBreeze44!", context);
            assertTrue(result.isValid());
            assertNull(result.errorMessage());
        }
        @Test
        @DisplayName("Accepts password of exactly 10 characters (boundary min length)")
        void acceptsTenCharacters()
        {
            final PasswordValidationResult result = validator.validate("1234567890");
            assertTrue(result.isValid());
            assertNull(result.errorMessage());
        }

        @Test
        @DisplayName("Accepts password of exactly 72 characters (boundary max length)")
        void acceptsSeventyTwoCharacters()
        {
            final String maxPassword = "A".repeat(36) + "B".repeat(36);
            final PasswordValidationResult result = validator.validate(maxPassword);
            assertTrue(result.isValid());
            assertNull(result.errorMessage());
        }
    }

    @Nested
    @DisplayName("Boundary and Error Tests")
    class BoundaryAndErrorTests
    {
        @Test
        @DisplayName("Rejects null password candidate")
        void rejectsNullPassword()
        {
            final PasswordValidationResult result = validator.validate(null);
            assertFalse(result.isValid());
            assertNotNull(result.errorMessage());
            assertTrue(result.errorMessage().contains("empty"));
        }

        @Test
        @DisplayName("Rejects empty string password candidate")
        void rejectsEmptyPassword()
        {
            final PasswordValidationResult result = validator.validate("");
            assertFalse(result.isValid());
            assertNotNull(result.errorMessage());
            assertTrue(result.errorMessage().contains("empty"));
        }

        @Test
        @DisplayName("Rejects password of 9 characters (boundary under min length)")
        void rejectsNineCharacters()
        {
            final PasswordValidationResult result = validator.validate("123456789");
            assertFalse(result.isValid());
            assertTrue(result.errorMessage().contains("at least 10 characters"));
        }

        @Test
        @DisplayName("Rejects password of 73 characters (boundary exceeding max length)")
        void rejectsSeventyThreeCharacters()
        {
            final String tooLongPassword = "A".repeat(36) + "B".repeat(37);
            final PasswordValidationResult result = validator.validate(tooLongPassword);
            assertFalse(result.isValid());
            assertTrue(result.errorMessage().contains("cannot exceed 72 characters"));
        }

        @Test
        @DisplayName("Rejects user manual test case of 128 characters")
        void rejectsManualTest128Characters()
        {
            final String userPass128 = "RfYfJm8wVe3jJzOVTwsL0ibnuyWmJthLPhNYe42pJmvdxL4mAY59AofqPCnTp0RDQt1rwI0hYOBrjueAGlYm1Nnsmc98vQH19mt93jyY5zJzU1MRAieuJVdUeu5X2cmN";
            final PasswordValidationResult result = validator.validate(userPass128);
            assertFalse(result.isValid());
            assertTrue(result.errorMessage().contains("cannot exceed 72 characters"));
        }

        @Test
        @DisplayName("Rejects user manual test case of 129 characters")
        void rejectsManualTest129Characters()
        {
            final String userPass129 = "RfYfJm8wVe3jJzOVTwsL0ibnuyWmJthLPhNYe42pJmvdxL4mAY59AofqPCnTp0RDQt1rwI0hYOBrjueAGlYm1Nnsmc98vQH19mt93jyY5zJzU1MRAieuJVdUeu5X2cmNQ";
            final PasswordValidationResult result = validator.validate(userPass129);
            assertFalse(result.isValid());
            assertTrue(result.errorMessage().contains("cannot exceed 72 characters"));
        }

        @Test
        @DisplayName("Rejects multibyte emojis exceeding 72 bytes even when character count is small")
        void rejectsMultibyteEmojisExceedingByteLimit()
        {
            // 25 emojis = 25 characters, but 25 * 4 bytes = 100 bytes (> 72 bytes)
            final String emojiPass = "🚀".repeat(25);
            final PasswordValidationResult result = validator.validate(emojiPass);
            assertFalse(result.isValid());
            assertTrue(result.errorMessage().contains("cannot exceed 72 bytes"));
        }

        @Test
        @DisplayName("Rejects multibyte umlauts exceeding 72 bytes even when character count is below 72")
        void rejectsMultibyteUmlautsExceedingByteLimit()
        {
            // 40 umlauts = 40 characters (< 72), but 40 * 2 bytes = 80 bytes (> 72 bytes)
            final String umlautPass = "ä".repeat(40);
            final PasswordValidationResult result = validator.validate(umlautPass);
            assertFalse(result.isValid());
            assertTrue(result.errorMessage().contains("cannot exceed 72 bytes"));
        }

        @Test
        @DisplayName("Rejects password consisting solely of a single repeated letter")
        void rejectsSingleRepeatedLetter()
        {
            final PasswordValidationResult result = validator.validate("aaaaaaaaaa");
            assertFalse(result.isValid());
            assertTrue(result.errorMessage().contains("single repeated character"));
        }

        @Test
        @DisplayName("Rejects password consisting solely of a single repeated digit")
        void rejectsSingleRepeatedDigit()
        {
            final PasswordValidationResult result = validator.validate("99999999999");
            assertFalse(result.isValid());
            assertTrue(result.errorMessage().contains("single repeated character"));
        }

        @Test
        @DisplayName("Rejects password consisting solely of a single repeated symbol")
        void rejectsSingleRepeatedSymbol()
        {
            final PasswordValidationResult result = validator.validate("!!!!!!!!!!");
            assertFalse(result.isValid());
            assertTrue(result.errorMessage().contains("single repeated character"));
        }
    }

    @Nested
    @DisplayName("Contextual Identity and Edge Case Tests")
    class ContextualIdentityTests
    {
        @Test
        @DisplayName("Rejects password containing user forename (case-insensitive)")
        void rejectsPasswordContainingForename()
        {
            final PasswordContext context = PasswordContext.of("Alexander", "Smith", "alex@example.com");
            final PasswordValidationResult result = validator.validate("MyalexanderPass123", context);
            assertFalse(result.isValid());
            assertTrue(result.errorMessage().contains("name or email address"));
        }

        @Test
        @DisplayName("Rejects password containing user surname (case-insensitive)")
        void rejectsPasswordContainingSurname()
        {
            final PasswordContext context = PasswordContext.of("Jane", "Doeckmann", "jane@example.com");
            final PasswordValidationResult result = validator.validate("SecureDOECKMANN99", context);
            assertFalse(result.isValid());
            assertTrue(result.errorMessage().contains("name or email address"));
        }

        @Test
        @DisplayName("Rejects password containing user email local part")
        void rejectsPasswordContainingEmailLocalPart()
        {
            final PasswordContext context = PasswordContext.of("Robert", "Johnson", "rjohnson@example.com");
            final PasswordValidationResult result = validator.validate("Secret_rjohnson_99", context);
            assertFalse(result.isValid());
            assertTrue(result.errorMessage().contains("name or email address"));
        }

        @Test
        @DisplayName("Rejects password containing segmented email local-part token")
        void rejectsPasswordContainingSegmentedEmailToken()
        {
            final PasswordContext context = PasswordContext.of(null, null, "max.mustermann@example.com");
            final PasswordValidationResult result = validator.validate("Pass_mustermann_123", context);
            assertFalse(result.isValid());
            assertTrue(result.errorMessage().contains("name or email address"));
        }

        @Test
        @DisplayName("Ignores contextual identity terms shorter than 3 characters to avoid false positives")
        void ignoresShortIdentityTerms()
        {
            // Context has 2-letter initials
            final PasswordContext context = PasswordContext.of("Al", "Li", "al@example.com");
            // "AlLiSecurePassphrase2026" contains "al" and "li", but they are < 3 characters
            final PasswordValidationResult result = validator.validate("AlLiSecurePassphrase2026", context);
            assertTrue(result.isValid());
            assertNull(result.errorMessage());
        }

        @Test
        @DisplayName("Handles empty and null context gracefully")
        void handlesNullAndEmptyContext()
        {
            final PasswordValidationResult nullContextResult = validator.validate("ValidLongPassword123!", null);
            assertTrue(nullContextResult.isValid());

            final PasswordValidationResult emptyContextResult = validator.validate("ValidLongPassword123!", PasswordContext.EMPTY);
            assertTrue(emptyContextResult.isValid());
        }
    }
}

