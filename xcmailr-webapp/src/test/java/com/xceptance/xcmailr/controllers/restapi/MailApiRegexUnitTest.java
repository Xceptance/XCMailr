/*
 * Copyright (c) 2013-2026 Xceptance Software Technologies GmbH
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
package com.xceptance.xcmailr.controllers.restapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.xceptance.xcmailr.controllers.restapi.MailApiController.RegexValidationException;

/**
 * Unit tests verifying regular expression pre-validation, ReDoS rejection heuristics,
 * length constraints, and pattern compilation behavior in {@link MailApiController#compileRegex}.
 * <p>
 * Structured according to the testing pyramid with fast, isolated unit tests covering
 * happy paths, error conditions, and boundary/special cases.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
public class MailApiRegexUnitTest
{
    /**
     * Happy path test cases verifying legitimate search expressions and omitted filters.
     */
    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests
    {
        /**
         * Verifies that null, empty, or whitespace-only inputs return null without throwing.
         */
        @Test
        @DisplayName("compileRegex returns null for null, empty, or blank inputs")
        public void testBlankOrNullRegexReturnsNull()
        {
            assertNull(MailApiController.compileRegex("subject", null),
                "Null input should return null");
            assertNull(MailApiController.compileRegex("subject", ""),
                "Empty string input should return null");
            assertNull(MailApiController.compileRegex("subject", "   "),
                "Whitespace-only input should return null");
            assertNull(MailApiController.compileRegex("subject", "\t\n"),
                "Tab and newline input should return null");
        }

        /**
         * Verifies that standard literal search strings compile and match properly.
         */
        @Test
        @DisplayName("compileRegex compiles simple literal pattern")
        public void testSimpleLiteralPattern()
        {
            final Pattern pattern = MailApiController.compileRegex("subject", "Invoice");

            assertNotNull(pattern, "Compiled pattern should not be null");
            assertTrue(pattern.matcher("Invoice #12345").find(), "Pattern should match target text");
        }

        /**
         * Verifies that typical automated test patterns with wildcards and digit matchers compile.
         */
        @Test
        @DisplayName("compileRegex compiles typical automated verification token pattern")
        public void testVerificationTokenPattern()
        {
            final Pattern pattern = MailApiController.compileRegex("textContent",
                ".*Your activation code is: \\d{6}.*");

            assertNotNull(pattern, "Compiled pattern should not be null");
            assertTrue(pattern.matcher("Hello! Your activation code is: 849201. Thank you.").matches(),
                "Pattern should match activation message");
        }

        /**
         * Verifies that safe alternations and character sets compile without triggering ReDoS heuristics.
         */
        @Test
        @DisplayName("compileRegex compiles safe non-nested grouping and character classes")
        public void testSafeGroupingAndCharacterClasses()
        {
            final Pattern pattern = MailApiController.compileRegex("from",
                "^(user|admin)@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

            assertNotNull(pattern, "Compiled pattern should not be null");
            assertTrue(pattern.matcher("admin@xceptance.de").matches(), "Pattern should match valid address");
        }
    }

    /**
     * Error condition test cases verifying length violations, ReDoS patterns, and syntax errors.
     */
    @Nested
    @DisplayName("Error Condition Tests")
    class ErrorConditionTests
    {
        /**
         * Verifies that expressions exceeding 255 characters are rejected with appropriate error info.
         */
        @Test
        @DisplayName("compileRegex rejects pattern exceeding 255 characters")
        public void testRegexExceedingMaxLength()
        {
            final String longPattern = "a".repeat(256);

            final RegexValidationException ex = assertThrows(RegexValidationException.class,
                () -> MailApiController.compileRegex("textContent", longPattern),
                "Should throw RegexValidationException when length exceeds 255");

            assertEquals("textContent", ex.getParameterName(), "Exception should identify the parameter name");
            assertTrue(ex.getMessage().contains("exceeds maximum allowed length"),
                "Exception message should mention maximum allowed length");
        }

        /**
         * Verifies that nested quantifier patterns prone to exponential ReDoS are rejected.
         */
        @Test
        @DisplayName("compileRegex rejects dangerous nested quantifier pattern (a+)+")
        public void testRejectNestedQuantifierPatternPlus()
        {
            final RegexValidationException ex = assertThrows(RegexValidationException.class,
                () -> MailApiController.compileRegex("subject", "(a+)+"),
                "Should throw RegexValidationException for (a+)+");

            assertEquals("subject", ex.getParameterName(), "Exception should identify the parameter name");
            assertTrue(ex.getMessage().contains("nested repetition"),
                "Exception message should indicate unsafe nested repetition");
        }

        /**
         * Verifies rejection of star-nested quantifier pattern {@code (x*)*}.
         */
        @Test
        @DisplayName("compileRegex rejects nested star repetition (x*)*")
        public void testRejectNestedQuantifierPatternStar()
        {
            final RegexValidationException ex = assertThrows(RegexValidationException.class,
                () -> MailApiController.compileRegex("from", "(x*)*"),
                "Should throw RegexValidationException for (x*)*");

            assertEquals("from", ex.getParameterName(), "Exception should identify the parameter name");
            assertTrue(ex.getMessage().contains("nested repetition"),
                "Exception message should indicate unsafe nested repetition");
        }

        /**
         * Verifies rejection of quantified character group with outer quantifier {@code ([0-9]+)+}.
         */
        @Test
        @DisplayName("compileRegex rejects nested repetition on character classes ([0-9]+)+")
        public void testRejectNestedQuantifierCharacterClass()
        {
            final RegexValidationException ex = assertThrows(RegexValidationException.class,
                () -> MailApiController.compileRegex("mailHeader", "([0-9]+)+"),
                "Should throw RegexValidationException for ([0-9]+)+");

            assertEquals("mailHeader", ex.getParameterName(), "Exception should identify the parameter name");
            assertTrue(ex.getMessage().contains("nested repetition"),
                "Exception message should indicate unsafe nested repetition");
        }

        /**
         * Verifies rejection of bounded nested quantifier {@code (a{2,}){2,}}.
         */
        @Test
        @DisplayName("compileRegex rejects nested curly bracket quantifier (a{2,}){2,}")
        public void testRejectNestedCurlyQuantifier()
        {
            final RegexValidationException ex = assertThrows(RegexValidationException.class,
                () -> MailApiController.compileRegex("htmlContent", "(a{2,}){2,}"),
                "Should throw RegexValidationException for nested curly brackets");

            assertEquals("htmlContent", ex.getParameterName(), "Exception should identify parameter name");
            assertTrue(ex.getMessage().contains("nested repetition"),
                "Exception message should indicate unsafe nested repetition");
        }

        /**
         * Verifies that syntactically malformed regular expressions are caught and reported.
         */
        @Test
        @DisplayName("compileRegex rejects syntactically invalid pattern (unclosed bracket)")
        public void testRejectSyntaxInvalidPattern()
        {
            final RegexValidationException ex = assertThrows(RegexValidationException.class,
                () -> MailApiController.compileRegex("subject", "[unclosed-bracket"),
                "Should throw RegexValidationException for invalid syntax");

            assertEquals("subject", ex.getParameterName(), "Exception should identify parameter name");
            assertTrue(ex.getMessage().contains("Invalid regular expression"),
                "Exception message should indicate invalid syntax");
        }

        /**
         * Verifies that unmatched parentheses are caught as syntax errors.
         */
        @Test
        @DisplayName("compileRegex rejects unclosed grouping parenthesis")
        public void testRejectUnclosedParenthesis()
        {
            final RegexValidationException ex = assertThrows(RegexValidationException.class,
                () -> MailApiController.compileRegex("subject", "(unclosed"),
                "Should throw RegexValidationException for unclosed parenthesis");

            assertEquals("subject", ex.getParameterName(), "Exception should identify parameter name");
            assertTrue(ex.getMessage().contains("Invalid regular expression"),
                "Exception message should indicate invalid syntax");
        }
    }

    /**
     * Special and boundary test cases verifying boundary lengths and regex matching flags.
     */
    @Nested
    @DisplayName("Special Case Tests")
    class SpecialCaseTests
    {
        /**
         * Verifies that a valid pattern of exactly 255 characters is accepted and compiles.
         */
        @Test
        @DisplayName("compileRegex accepts valid pattern of exactly 255 characters")
        public void testExactMaxLengthPattern()
        {
            final String boundaryPattern = "a".repeat(MailApiController.MAX_REGEX_LENGTH);
            assertEquals(255, boundaryPattern.length(), "Test string must be exactly 255 characters");

            final Pattern pattern = MailApiController.compileRegex("subject", boundaryPattern);

            assertNotNull(pattern, "Pattern of exactly 255 chars should compile successfully");
            assertTrue(pattern.matcher(boundaryPattern).matches(), "Compiled pattern should match the string");
        }

        /**
         * Verifies that a pattern of 256 characters (off-by-one boundary) is rejected.
         */
        @Test
        @DisplayName("compileRegex rejects boundary pattern of exactly 256 characters")
        public void testOffByOneMaxLengthPattern()
        {
            final String boundaryPattern = "a".repeat(MailApiController.MAX_REGEX_LENGTH + 1);
            assertEquals(256, boundaryPattern.length(), "Test string must be exactly 256 characters");

            final RegexValidationException ex = assertThrows(RegexValidationException.class,
                () -> MailApiController.compileRegex("subject", boundaryPattern));

            assertEquals("subject", ex.getParameterName());
            assertTrue(ex.getMessage().contains("exceeds maximum allowed length"));
        }

        /**
         * Verifies that MULTILINE and DOTALL flags are enabled by default on compiled patterns.
         */
        @Test
        @DisplayName("compileRegex enables MULTILINE and DOTALL flags")
        public void testMultilineAndDotAllFlags()
        {
            final Pattern pattern = MailApiController.compileRegex("textContent", "^Line 2: (.*)$");

            assertNotNull(pattern, "Compiled pattern should not be null");
            final String multilineText = "Line 1: Header\nLine 2: Important Value\nLine 3: Footer";

            // MULTILINE allows ^ and $ to match within lines
            assertTrue(pattern.matcher(multilineText).find(),
                "Pattern should match within multiline text using MULTILINE flag");

            // DOTALL allows . to match across newline
            final Pattern dotAllPattern = MailApiController.compileRegex("textContent", "Start.*End");
            assertNotNull(dotAllPattern, "Compiled pattern should not be null");
            assertTrue(dotAllPattern.matcher("Start\nMiddle\nEnd").matches(),
                "Pattern should match across newlines using DOTALL flag");
        }
    }
}

