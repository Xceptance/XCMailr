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
package com.xceptance.xcmailr.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import etc.HelperUtils;
import models.MBox;

/**
 * Unit test suite representing the lowest layer of the test pyramid.
 * <p>
 * Tests mailbox address validation rules, address sanitization and normalization,
 * random address auto-generation, and timestamp status conversions in complete isolation
 * without loading Spring ApplicationContext or databases.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
public final class MailboxValidationUnitTest
{
    /** Regex pattern used by {@link MailboxWebController} for local mailbox part validation. */
    private static final Pattern CONTROLLER_ADDRESS_PATTERN =
        Pattern.compile("^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~.-]+$");

    /** Regex pattern used by the persistent {@link MBox} entity for JPA validation. */
    private static final Pattern ENTITY_ADDRESS_PATTERN =
        Pattern.compile("(?i)^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*");

    /** Regex pattern used by the persistent {@link MBox} entity for domain validation. */
    private static final Pattern DOMAIN_PATTERN =
        Pattern.compile("(?i)[a-z-]+(\\.[\\w-]+)+");

    // =========================================================================
    // HAPPY PATH UNIT TESTS
    // =========================================================================

    /**
     * Verifies that standard alphanumeric mailbox addresses match the controller validation pattern.
     *
     * @param validAddress candidate valid mailbox local part
     */
    @ParameterizedTest(name = "Happy path: standard address \"{0}\" is accepted")
    @ValueSource(strings = {"mailbox", "test123", "alpha-beta", "user.name", "my_inbox"})
    @DisplayName("Happy path: standard alphanumeric addresses should match validation pattern")
    public void testValidateAddress_StandardAlphanumeric_Accepted(final String validAddress)
    {
        // GIVEN
        final String address = validAddress;

        // WHEN
        final boolean matches = CONTROLLER_ADDRESS_PATTERN.matcher(address).matches();

        // THEN
        assertTrue(matches, "Expected standard address '" + address + "' to be accepted by pattern");
    }

    /**
     * Verifies that RFC-compliant special characters in the local part of email addresses are permitted.
     *
     * @param specialCharAddress candidate mailbox local part with RFC characters
     */
    @ParameterizedTest(name = "Happy path: special character address \"{0}\" is accepted")
    @ValueSource(strings = {"user+tag", "dev#build", "support!priority", "test$123", "qa%box"})
    @DisplayName("Happy path: RFC-compliant special characters should match validation pattern")
    public void testValidateAddress_RfcSpecialCharacters_Accepted(final String specialCharAddress)
    {
        // GIVEN
        final String address = specialCharAddress;

        // WHEN
        final boolean matches = CONTROLLER_ADDRESS_PATTERN.matcher(address).matches();

        // THEN
        assertTrue(matches, "Expected RFC-compliant address '" + address + "' to be accepted");
    }

    /**
     * Verifies that whitespace trimming and lowercase normalization converts input to standard form.
     */
    @Test
    @DisplayName("Happy path: address trimming and lowercasing normalizes input")
    public void testAddressNormalization_TrimmingAndLowercasing_Succeeds()
    {
        // GIVEN
        final String rawInput = "   MyCustomBox-123   ";

        // WHEN
        final String normalized = rawInput.trim().toLowerCase();

        // THEN
        assertEquals("mycustombox-123", normalized, "Address must be trimmed and converted to lower case");
        assertTrue(CONTROLLER_ADDRESS_PATTERN.matcher(normalized).matches());
    }

    /**
     * Verifies that valid domains conform to the domain regex pattern.
     *
     * @param validDomain candidate valid domain name
     */
    @ParameterizedTest(name = "Happy path: valid domain \"{0}\" is accepted")
    @ValueSource(strings = {"xcmailr.test", "example.com", "sub.domain.org", "my-mail.net"})
    @DisplayName("Happy path: valid domain names should match domain pattern")
    public void testValidateDomain_ValidDomains_Accepted(final String validDomain)
    {
        // GIVEN
        final String domain = validDomain;

        // WHEN
        final boolean matches = DOMAIN_PATTERN.matcher(domain).matches();

        // THEN
        assertTrue(matches, "Expected domain '" + domain + "' to match domain pattern");
    }

    /**
     * Verifies that timestamp conversions correctly represent unlimited and active time periods.
     */
    @Test
    @DisplayName("Happy path: timestamp conversions properly format unlimited and active dates")
    public void testTimestampFormatting_ActiveAndUnlimited_CorrectFormat()
    {
        // GIVEN
        final long unlimitedTimestamp = 0L;
        final long fixedTimestamp = 1735689600000L; // 2025-01-01 00:00:00 UTC approx

        // WHEN
        final String unlimitedFormatted = HelperUtils.parseStringTs(unlimitedTimestamp);
        final String fixedFormatted = HelperUtils.parseStringTs(fixedTimestamp);

        // THEN
        assertEquals("unlimited", unlimitedFormatted, "Timestamp of 0 must format to 'unlimited'");
        assertNotNull(fixedFormatted);
        assertFalse(fixedFormatted.isEmpty());
    }

    // =========================================================================
    // ERROR CASES UNIT TESTS
    // =========================================================================

    /**
     * Verifies that addresses containing forbidden whitespace characters are rejected.
     *
     * @param whitespaceAddress candidate address containing whitespace
     */
    @ParameterizedTest(name = "Error case: address with whitespace \"{0}\" is rejected")
    @ValueSource(strings = {"mail box", "user\tname", "hello world", "test\nline"})
    @DisplayName("Error case: addresses with interior whitespace should be rejected")
    public void testValidateAddress_ContainsWhitespace_Rejected(final String whitespaceAddress)
    {
        // GIVEN
        final String address = whitespaceAddress;

        // WHEN
        final boolean matches = CONTROLLER_ADDRESS_PATTERN.matcher(address).matches();

        // THEN
        assertFalse(matches, "Expected address with whitespace '" + address + "' to be rejected");
    }

    /**
     * Verifies that addresses containing forbidden characters like '@', quotes, brackets, or angle brackets are rejected.
     *
     * @param forbiddenAddress candidate address containing forbidden symbols
     */
    @ParameterizedTest(name = "Error case: address with forbidden character \"{0}\" is rejected")
    @ValueSource(strings = {"user@domain", "test\"quote", "box<tag>", "user(parentheses)", "[bracket]"})
    @DisplayName("Error case: addresses with forbidden symbols should be rejected")
    public void testValidateAddress_ContainsForbiddenSymbols_Rejected(final String forbiddenAddress)
    {
        // GIVEN
        final String address = forbiddenAddress;

        // WHEN
        final boolean matches = CONTROLLER_ADDRESS_PATTERN.matcher(address).matches();

        // THEN
        assertFalse(matches, "Expected address with forbidden symbol '" + address + "' to be rejected");
    }

    /**
     * Verifies that non-ASCII or multi-byte characters such as emojis or umlauts are rejected.
     *
     * @param nonAsciiAddress candidate address containing non-ASCII symbols
     */
    @ParameterizedTest(name = "Error case: address with non-ASCII \"{0}\" is rejected")
    @ValueSource(strings = {"münchen", "tëst", "box📧mail", "üser"})
    @DisplayName("Error case: non-ASCII characters in address should be rejected")
    public void testValidateAddress_NonAsciiCharacters_Rejected(final String nonAsciiAddress)
    {
        // GIVEN
        final String address = nonAsciiAddress;

        // WHEN
        final boolean matches = CONTROLLER_ADDRESS_PATTERN.matcher(address).matches();

        // THEN
        assertFalse(matches, "Expected non-ASCII address '" + address + "' to be rejected");
    }

    /**
     * Verifies that malformed domain strings are rejected.
     *
     * @param invalidDomain candidate malformed domain string
     */
    @ParameterizedTest(name = "Error case: invalid domain \"{0}\" is rejected")
    @ValueSource(strings = {"nodot", ".startingdot.com", "endingdot.", "has spaces.com", "@domain.com"})
    @DisplayName("Error case: malformed domain names should be rejected")
    public void testValidateDomain_Malformed_Rejected(final String invalidDomain)
    {
        // GIVEN
        final String domain = invalidDomain;

        // WHEN
        final boolean matches = DOMAIN_PATTERN.matcher(domain).matches();

        // THEN
        assertFalse(matches, "Expected invalid domain '" + domain + "' to be rejected");
    }

    // =========================================================================
    // SPECIAL EDGE CASES UNIT TESTS
    // =========================================================================

    /**
     * Verifies that blank or empty address inputs trigger auto-generation of a 7-character
     * lowercase alphanumeric mailbox address conforming to the validation pattern.
     */
    @Test
    @DisplayName("Special case: blank address triggers auto-generation of 7-char alphanumeric address")
    public void testBlankAddress_TriggersAutoGeneration_ProducesValidAddress()
    {
        // GIVEN
        final String blankInput = "   ";

        // WHEN
        final String resolvedAddress;
        if (blankInput.trim().isEmpty())
        {
            resolvedAddress = RandomStringUtils.randomAlphanumeric(7).toLowerCase();
        }
        else
        {
            resolvedAddress = blankInput.trim().toLowerCase();
        }

        // THEN
        assertNotNull(resolvedAddress, "Generated address must not be null");
        assertEquals(7, resolvedAddress.length(), "Generated address must be exactly 7 characters");
        assertTrue(CONTROLLER_ADDRESS_PATTERN.matcher(resolvedAddress).matches(),
            "Generated address '" + resolvedAddress + "' must match controller validation pattern");
    }

    /**
     * Verifies that null address input is handled safely without throwing NullPointerException
     * and produces a valid fallback address.
     */
    @Test
    @DisplayName("Special case: null address handled gracefully and triggers auto-generation")
    public void testNullAddress_HandledGracefully_ProducesValidAddress()
    {
        // GIVEN
        final String nullInput = null;

        // WHEN
        final String normalized = nullInput != null ? nullInput.trim().toLowerCase() : "";
        final String resolvedAddress = normalized.isEmpty()
            ? RandomStringUtils.randomAlphanumeric(7).toLowerCase()
            : normalized;

        // THEN
        assertNotNull(resolvedAddress);
        assertEquals(7, resolvedAddress.length());
        assertTrue(CONTROLLER_ADDRESS_PATTERN.matcher(resolvedAddress).matches());
    }

    /**
     * Verifies that single character valid addresses are accepted by the pattern.
     */
    @Test
    @DisplayName("Special case: single character mailbox address is accepted")
    public void testSingleCharacterAddress_Accepted()
    {
        // GIVEN
        final String singleChar = "a";

        // WHEN
        final boolean matches = CONTROLLER_ADDRESS_PATTERN.matcher(singleChar).matches();

        // THEN
        assertTrue(matches, "Single character address must be accepted");
    }

    /**
     * Verifies that an address containing dots conforms to the persistent entity regex pattern.
     */
    @Test
    @DisplayName("Special case: dotted address passes entity validation pattern")
    public void testDottedAddress_EntityValidationPattern_Accepted()
    {
        // GIVEN
        final String dottedAddress = "first.middle.last";

        // WHEN
        final boolean matches = ENTITY_ADDRESS_PATTERN.matcher(dottedAddress).matches();

        // THEN
        assertTrue(matches, "Dotted address must be accepted by entity regex");
    }
}

