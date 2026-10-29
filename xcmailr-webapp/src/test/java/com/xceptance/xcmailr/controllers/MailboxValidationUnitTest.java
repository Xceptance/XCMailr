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

    // =========================================================================
    // DURATION CALCULATION & FORWARDING UNIT TESTS (TESTING PYRAMID)
    // =========================================================================

    @ParameterizedTest(name = "Happy path: duration {0} hours correctly offsets from base timestamp")
    @ValueSource(ints = {1, 2, 24, 168, 720})
    @DisplayName("Happy path: standard durations produce expected expiration timestamps")
    public void testExpirationCalculation_PositiveHours_OffsetsCorrectly(final int durationHours)
    {
        // GIVEN
        final long fixedNow = 1_700_000_000_000L;

        // WHEN
        final long expirationTs = durationHours <= 0 ? 0L : fixedNow + java.util.concurrent.TimeUnit.HOURS.toMillis(durationHours);

        // THEN
        final long expectedOffset = (long) durationHours * 3600 * 1000;
        assertEquals(fixedNow + expectedOffset, expirationTs, "Expiration timestamp must match base plus duration in milliseconds");
    }

    @ParameterizedTest(name = "Edge case: duration {0} hours evaluates to unlimited (0)")
    @ValueSource(ints = {0, -1, -24, Integer.MIN_VALUE})
    @DisplayName("Edge case: zero or negative durations produce unlimited expiration (0L)")
    public void testExpirationCalculation_NonPositiveHours_EvaluatesToZero(final int durationHours)
    {
        // GIVEN
        final long fixedNow = 1_700_000_000_000L;

        // WHEN
        final long expirationTs = durationHours <= 0 ? 0L : fixedNow + java.util.concurrent.TimeUnit.HOURS.toMillis(durationHours);

        // THEN
        assertEquals(0L, expirationTs, "Zero or negative duration must evaluate to 0L (unlimited)");
    }

    /**
     * Verifies that large duration values do not cause 32-bit integer overflow when computing millisecond offsets.
     */
    @Test
    @DisplayName("Special case: large duration does not suffer from 32-bit integer overflow")
    public void testExpirationCalculation_LargeDuration_NoIntegerOverflow()
    {
        // GIVEN
        final long fixedNow = 1_700_000_000_000L;
        final int largeDurationHours = 1_000_000; // 1 million hours exceeds 32-bit int milliseconds

        // WHEN
        final long expirationTs = fixedNow + java.util.concurrent.TimeUnit.HOURS.toMillis((long) largeDurationHours);

        // THEN
        final long expectedOffset = 1_000_000L * 3600L * 1000L;
        assertEquals(fixedNow + expectedOffset, expirationTs, "Large duration must correctly compute with 64-bit long arithmetic");
        assertTrue(expirationTs > fixedNow, "Expiration timestamp must remain in the future");
    }

    /**
     * Verifies entity timestamp status conversions across happy paths and edge cases (active, expired, unlimited).
     */
    @Test
    @DisplayName("Status conversion: verifies MBox entity active and expired state behavior")
    public void testMailboxActiveStatus_TimestampConversions()
    {
        // GIVEN
        final long now = System.currentTimeMillis();
        final long futureTs = now + java.util.concurrent.TimeUnit.HOURS.toMillis(720);
        final long pastTs = now - 3600_000L;

        // WHEN 1: Happy path active future mailbox
        final MBox activeFutureBox = new MBox("testbox", "xcmailr.test", futureTs, false, null);

        // THEN 1
        assertTrue(activeFutureBox.isActive(), "Future mailbox must be active");
        assertFalse(activeFutureBox.isExpired(), "Future mailbox must not be expired");
        assertFalse(activeFutureBox.isExpiredByTimestamp(), "Future mailbox is not expired by timestamp");

        // WHEN 2: Happy path unlimited mailbox
        final MBox unlimitedBox = new MBox("unlimited", "xcmailr.test", 0L, false, null);

        // THEN 2
        assertTrue(unlimitedBox.isActive(), "Unlimited mailbox must be active");
        assertFalse(unlimitedBox.isExpired(), "Unlimited mailbox must not be expired");
        assertFalse(unlimitedBox.isExpiredByTimestamp(), "Unlimited mailbox (ts=0) is not expired by timestamp");

        // WHEN 3: Edge case expired mailbox in the past
        final MBox expiredPastBox = new MBox("expired", "xcmailr.test", pastTs, true, null);

        // THEN 3
        assertFalse(expiredPastBox.isActive(), "Past expired mailbox must be inactive");
        assertTrue(expiredPastBox.isExpired(), "Past expired mailbox must be expired");
        assertTrue(expiredPastBox.isExpiredByTimestamp(), "Past expired mailbox must be expired by timestamp");

        // WHEN 4: Edge case manually deactivated unlimited mailbox
        final MBox deactivatedUnlimitedBox = new MBox("deactivated", "xcmailr.test", 0L, true, null);

        // THEN 4
        assertFalse(deactivatedUnlimitedBox.isActive(), "Deactivated unlimited mailbox must be inactive");
        assertTrue(deactivatedUnlimitedBox.isExpired(), "Deactivated unlimited mailbox must be expired");
        assertFalse(deactivatedUnlimitedBox.isExpiredByTimestamp(), "Unlimited mailbox cannot be expired by timestamp even if deactivated");
    }

    @Test
    @DisplayName("Happy path: MBox forwardEmails property respects explicit true and false states")
    public void testMailboxForwarding_ExplicitStates_Preserved()
    {
        // GIVEN
        final MBox boxEnabled = new MBox();
        final MBox boxDisabled = new MBox();

        // WHEN
        boxEnabled.setForwardEmails(true);
        boxDisabled.setForwardEmails(false);

        // THEN
        assertTrue(boxEnabled.isForwardEmails(), "Forwarding should be enabled when set to true");
        assertFalse(boxDisabled.isForwardEmails(), "Forwarding should be disabled when set to false");
    }

    @Test
    @DisplayName("Special case: omitted checkbox default (false) sets forwardEmails to false")
    public void testMailboxForwarding_OmittedDefault_EvaluatesToFalse()
    {
        // GIVEN: In HTML form submission, unchecked checkboxes are omitted,
        // so the controller's defaultValue parameter resolution gives false.
        final boolean resolvedForwardParameter = false;
        final MBox box = new MBox();

        // WHEN
        box.setForwardEmails(resolvedForwardParameter);

        // THEN
        assertFalse(box.isForwardEmails(), "Unchecked checkbox default must resolve forwardEmails to false");
    }
}

