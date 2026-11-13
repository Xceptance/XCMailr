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
package com.xceptance.xcmailr.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MailStatistics} and {@link MailStatisticsKey}.
 * Tests property access, equality, hashing, and null safety across
 * happy paths, special cases, and error cases adhering to the testing pyramid.
 *
 * @author Xceptance Software Technologies GmbH
 */
public class MailStatisticsTest
{
    @Test
    @DisplayName("Happy Path: Constructor and property getters/setters")
    public void testPropertiesAndConstructor()
    {
        final Date date = Date.valueOf("2026-11-11");
        final MailStatisticsKey key = new MailStatisticsKey(date, 42, "sender.test", "recipient.test");
        final MailStatistics stats = new MailStatistics(key, 5, 10, 15);

        assertEquals(key, stats.getKey(), "Key should match constructor argument");
        assertEquals(5, stats.getDropCount(), "Drop count should match");
        assertEquals(10, stats.getForwardCount(), "Forward count should match");
        assertEquals(15, stats.getStoreCount(), "Store count should match");

        stats.setDropCount(20);
        stats.setForwardCount(30);
        stats.setStoreCount(40);

        assertEquals(20, stats.getDropCount());
        assertEquals(30, stats.getForwardCount());
        assertEquals(40, stats.getStoreCount());
    }

    @Test
    @DisplayName("Special Case: Equals and HashCode contract")
    public void testEqualsAndHashCode()
    {
        final Date date = Date.valueOf("2026-11-11");
        final MailStatisticsKey key1 = new MailStatisticsKey(date, 10, "alpha.test", "xcmailr.test");
        final MailStatisticsKey key2 = new MailStatisticsKey(date, 10, "ALPHA.TEST", "XCMAILR.TEST");

        final MailStatistics stats1 = new MailStatistics(key1, 1, 2, 3);
        final MailStatistics stats2 = new MailStatistics(key2, 1, 2, 3);

        // Reflexive & symmetric
        assertEquals(stats1, stats1);
        assertEquals(stats1, stats2);
        assertEquals(stats2, stats1);
        assertEquals(stats1.hashCode(), stats2.hashCode());

        // Different store count
        final MailStatistics differentStore = new MailStatistics(key1, 1, 2, 99);
        assertNotEquals(stats1, differentStore);
        assertNotEquals(stats1.hashCode(), differentStore.hashCode());

        // Different forward count
        final MailStatistics differentForward = new MailStatistics(key1, 1, 99, 3);
        assertNotEquals(stats1, differentForward);

        // Different drop count
        final MailStatistics differentDrop = new MailStatistics(key1, 99, 2, 3);
        assertNotEquals(stats1, differentDrop);

        // Different key
        final MailStatisticsKey keyDifferentSlot = new MailStatisticsKey(date, 11, "alpha.test", "xcmailr.test");
        final MailStatistics differentKey = new MailStatistics(keyDifferentSlot, 1, 2, 3);
        assertNotEquals(stats1, differentKey);
    }

    @Test
    @DisplayName("Special Case: Null-safe key handling in equals and hashCode")
    public void testNullKeyHandling()
    {
        final MailStatistics statsNullKey1 = new MailStatistics(null, 1, 2, 3);
        final MailStatistics statsNullKey2 = new MailStatistics(null, 1, 2, 3);
        final MailStatisticsKey validKey = new MailStatisticsKey(Date.valueOf("2026-11-11"), 10, "a.test", "b.test");
        final MailStatistics statsValidKey = new MailStatistics(validKey, 1, 2, 3);

        assertEquals(statsNullKey1, statsNullKey2);
        assertEquals(statsNullKey1.hashCode(), statsNullKey2.hashCode());

        assertNotEquals(statsNullKey1, statsValidKey);
        assertNotEquals(statsValidKey, statsNullKey1);
    }

    @Test
    @DisplayName("Error Case: Incompatible type comparison and null equals")
    public void testIncompatibleComparison()
    {
        final MailStatistics stats = new MailStatistics(null, 1, 2, 3);

        assertFalse(stats.equals(null), "Comparison with null must return false");
        assertFalse(stats.equals("some string"), "Comparison with incompatible type must return false");
    }

    @Test
    @DisplayName("Happy Path: toString and number formatting")
    public void testToStringAndFormat()
    {
        final Date date = Date.valueOf("2026-11-11");
        final MailStatisticsKey key = new MailStatisticsKey(date, 4, "sender.test", "xcmailr.test");
        final MailStatistics stats = new MailStatistics(key, 10, 20, 30);

        final String str = stats.toString();
        assertNotNull(str);
        assertTrue(str.contains("storeCount=30"), "toString output must include storeCount");

        final String formatted = MailStatistics.format(1234567);
        assertEquals("1 234 567", formatted, "Number format must use space separator");
    }
}
