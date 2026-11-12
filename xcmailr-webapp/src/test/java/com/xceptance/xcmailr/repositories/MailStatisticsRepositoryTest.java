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
package com.xceptance.xcmailr.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.xceptance.xcmailr.dto.DomainStatisticsDto;
import com.xceptance.xcmailr.models.MailStatistics;
import com.xceptance.xcmailr.models.MailStatisticsKey;
import com.xceptance.xcmailr.repositories.MailStatisticsRepository.DailyTrafficStatEntry;
import com.xceptance.xcmailr.repositories.MailStatisticsRepository.QuarterHourTrafficStatEntry;

/**
 * Repository slice and integration tests for {@link MailStatisticsRepository} domain aggregation query.
 * Verifies domain aggregation, volume ordering descending, date boundary filtering, and COALESCE
 * handling of zero counts.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MailStatisticsRepositoryTest
{
    @Autowired
    private MailStatisticsRepository mailStatisticsRepository;

    private Date today;
    private Date yesterday;

    @BeforeEach
    public void setup()
    {
        today = Date.valueOf(LocalDate.now());
        yesterday = Date.valueOf(LocalDate.now().minusDays(1));
        mailStatisticsRepository.deleteAll();
    }

    @Test
    @DisplayName("Happy Path: Aggregate drop, forward, and store counts by domain and order by volume descending")
    public void testHappyPathDomainAggregationAndVolumeOrdering()
    {
        // High volume domain: across two 15-minute slots on today
        final MailStatistics highSlot1 = new MailStatistics(
            new MailStatisticsKey(today, 10, "highvolume.org", "xcmailr.test"), 5, 10, 15);
        final MailStatistics highSlot2 = new MailStatistics(
            new MailStatisticsKey(today, 11, "highvolume.org", "xcmailr.test"), 2, 3, 5);

        // Medium volume domain: single slot on today
        final MailStatistics medSlot1 = new MailStatistics(
            new MailStatisticsKey(today, 10, "medvolume.org", "xcmailr.test"), 1, 10, 0);

        // Low volume domain: single slot on yesterday (within range when querying from yesterday)
        final MailStatistics lowSlot1 = new MailStatistics(
            new MailStatisticsKey(yesterday, 12, "lowvolume.org", "xcmailr.test"), 2, 1, 0);

        mailStatisticsRepository.saveAll(List.of(highSlot1, highSlot2, medSlot1, lowSlot1));

        // Query since yesterday: all three domains should appear, ordered by total volume descending
        final List<DomainStatisticsDto> results = mailStatisticsRepository.findDomainStatisticsSince(yesterday);
        assertNotNull(results, "Aggregation results must not be null");
        assertEquals(3, results.size(), "All three sender domains within date window must be returned");

        // 1st: highvolume.org -> drops: 7, forwards: 13, stores: 20 (total: 40)
        final DomainStatisticsDto first = results.get(0);
        assertEquals("highvolume.org", first.domain());
        assertEquals(7L, first.dropCount());
        assertEquals(13L, first.forwardCount());
        assertEquals(20L, first.storeCount());
        assertEquals(40L, first.getTotalCount());

        // 2nd: medvolume.org -> drops: 1, forwards: 10, stores: 0 (total: 11)
        final DomainStatisticsDto second = results.get(1);
        assertEquals("medvolume.org", second.domain());
        assertEquals(1L, second.dropCount());
        assertEquals(10L, second.forwardCount());
        assertEquals(0L, second.storeCount());
        assertEquals(11L, second.getTotalCount());

        // 3rd: lowvolume.org -> drops: 2, forwards: 1, stores: 0 (total: 3)
        final DomainStatisticsDto third = results.get(2);
        assertEquals("lowvolume.org", third.domain());
        assertEquals(2L, third.dropCount());
        assertEquals(1L, third.forwardCount());
        assertEquals(0L, third.storeCount());
        assertEquals(3L, third.getTotalCount());
    }

    @Test
    @DisplayName("Boundary Case: Query with start date after existing records returns empty collection")
    public void testBoundaryCaseNoRecordsBeforeStartDate()
    {
        final Date tenDaysAgo = Date.valueOf(LocalDate.now().minusDays(10));
        final MailStatistics oldEntry = new MailStatistics(
            new MailStatisticsKey(tenDaysAgo, 20, "ancient.org", "xcmailr.test"), 10, 10, 10);
        mailStatisticsRepository.save(oldEntry);

        // Querying for today should filter out records from ten days ago
        final List<DomainStatisticsDto> results = mailStatisticsRepository.findDomainStatisticsSince(today);
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Entries older than startDate must be excluded");
    }

    @Test
    @DisplayName("Special Case: Zero counts handled cleanly via COALESCE without returning null values")
    public void testSpecialCaseZeroCountsCoalesceWithoutNulls()
    {
        // Entry with only store count (drops=0, forwards=0)
        final MailStatistics storeOnly = new MailStatistics(
            new MailStatisticsKey(today, 15, "storeonly.org", "xcmailr.test"), 0, 0, 8);
        mailStatisticsRepository.save(storeOnly);

        final List<DomainStatisticsDto> results = mailStatisticsRepository.findDomainStatisticsSince(today);
        assertEquals(1, results.size());

        final DomainStatisticsDto dto = results.get(0);
        assertEquals("storeonly.org", dto.domain());
        assertEquals(0L, dto.dropCount(), "Drop count must be 0L and not null");
        assertEquals(0L, dto.forwardCount(), "Forward count must be 0L and not null");
        assertEquals(8L, dto.storeCount(), "Store count must be 8L");
        assertEquals(8L, dto.getTotalCount(), "Total count must equal 8L");
    }

    @Test
    @DisplayName("Happy Path: findDailyDomainStatisticsSince groups records by calendar date and domain, ordered by date DESC then total DESC")
    public void testFindDailyDomainStatisticsSinceGroupsByDateAndDomain()
    {
        final Date twoDaysAgo = Date.valueOf(LocalDate.now().minusDays(2));

        // Two entries for example.com on today across different quarter-hours
        final MailStatistics today1 = new MailStatistics(
            new MailStatisticsKey(today, 10, "example.com", "xcmailr.test"), 1, 2, 3);
        final MailStatistics today2 = new MailStatistics(
            new MailStatisticsKey(today, 20, "example.com", "xcmailr.test"), 4, 5, 6);
        // One entry for external.com on today with higher volume
        final MailStatistics todayExternal = new MailStatistics(
            new MailStatisticsKey(today, 15, "external.com", "xcmailr.test"), 10, 20, 30);

        // One entry for example.com on yesterday
        final MailStatistics yesterdayExample = new MailStatistics(
            new MailStatisticsKey(yesterday, 5, "example.com", "xcmailr.test"), 2, 0, 1);

        // One entry for old.com on twoDaysAgo
        final MailStatistics twoDaysAgoOld = new MailStatistics(
            new MailStatisticsKey(twoDaysAgo, 8, "old.com", "xcmailr.test"), 5, 5, 5);

        mailStatisticsRepository.saveAll(List.of(today1, today2, todayExternal, yesterdayExample, twoDaysAgoOld));

        final List<DomainStatisticsDto> dailyResults = mailStatisticsRepository.findDailyDomainStatisticsSince(yesterday);
        assertNotNull(dailyResults);
        assertEquals(3, dailyResults.size(), "Should return 3 daily domain entries for today and yesterday, excluding twoDaysAgo");

        // 1st: today, external.com (total: 60)
        final DomainStatisticsDto r1 = dailyResults.get(0);
        assertEquals(today, r1.date());
        assertEquals(today, r1.getDate());
        assertEquals("external.com", r1.domain());
        assertEquals(60L, r1.totalCount());

        // 2nd: today, example.com (drops: 1+4=5, forwards: 2+5=7, stores: 3+6=9, total: 21)
        final DomainStatisticsDto r2 = dailyResults.get(1);
        assertEquals(today, r2.date());
        assertEquals("example.com", r2.domain());
        assertEquals(5L, r2.dropCount());
        assertEquals(7L, r2.forwardCount());
        assertEquals(9L, r2.storeCount());
        assertEquals(21L, r2.totalCount());

        // 3rd: yesterday, example.com (total: 3)
        final DomainStatisticsDto r3 = dailyResults.get(2);
        assertEquals(yesterday, r3.date());
        assertEquals("example.com", r3.domain());
        assertEquals(3L, r3.totalCount());
    }

    @Test
    @DisplayName("Happy Path: findDailyTrafficStatsSince aggregates drop, forward, and store totals across all domains ordered by date ASC")
    public void testFindDailyTrafficStatsSinceAggregatesAcrossDomains()
    {
        final Date twoDaysAgo = Date.valueOf(LocalDate.now().minusDays(2));

        // Today: domain A has (10 drops, 20 forwards, 30 stores), domain B has (5 drops, 15 forwards, 25 stores)
        final MailStatistics todayA = new MailStatistics(
            new MailStatisticsKey(today, 10, "doma.com", "xcmailr.test"), 10, 20, 30);
        final MailStatistics todayB = new MailStatistics(
            new MailStatisticsKey(today, 20, "domb.com", "xcmailr.test"), 5, 15, 25);

        // Yesterday: domain A has (2 drops, 4 forwards, 6 stores)
        final MailStatistics yesterdayA = new MailStatistics(
            new MailStatisticsKey(yesterday, 10, "doma.com", "xcmailr.test"), 2, 4, 6);

        // Older than cutoff: should be excluded
        final MailStatistics oldEntry = new MailStatistics(
            new MailStatisticsKey(twoDaysAgo, 10, "old.com", "xcmailr.test"), 100, 100, 100);

        mailStatisticsRepository.saveAll(List.of(todayA, todayB, yesterdayA, oldEntry));

        final List<DailyTrafficStatEntry> results = mailStatisticsRepository.findDailyTrafficStatsSince(yesterday);
        assertNotNull(results);
        assertEquals(2, results.size(), "Should return exactly 2 daily entries (yesterday and today)");

        // 1st entry: yesterday
        final DailyTrafficStatEntry day1 = results.get(0);
        assertEquals(yesterday, day1.getDate());
        assertEquals(4L, day1.getForwardCount());
        assertEquals(6L, day1.getStoreCount());
        assertEquals(2L, day1.getDropCount());

        // 2nd entry: today (aggregated across doma.com and domb.com)
        final DailyTrafficStatEntry day2 = results.get(1);
        assertEquals(today, day2.getDate());
        assertEquals(35L, day2.getForwardCount(), "Forwards: 20 + 15 = 35");
        assertEquals(55L, day2.getStoreCount(), "Stores: 30 + 25 = 55");
        assertEquals(15L, day2.getDropCount(), "Drops: 10 + 5 = 15");
    }

    @Test
    @DisplayName("Happy Path: findQuarterHourTrafficStatsSince aggregates totals by date and quarter-hour slot across domains")
    public void testFindQuarterHourTrafficStatsSinceAggregatesAcrossDomains()
    {
        // Slot 12 on today: domain A (1 drop, 2 forwards, 3 stores), domain B (4 drops, 5 forwards, 6 stores)
        final MailStatistics slot12A = new MailStatistics(
            new MailStatisticsKey(today, 12, "doma.com", "xcmailr.test"), 1, 2, 3);
        final MailStatistics slot12B = new MailStatistics(
            new MailStatisticsKey(today, 12, "domb.com", "xcmailr.test"), 4, 5, 6);

        // Slot 13 on today: domain A (10 drops, 20 forwards, 30 stores)
        final MailStatistics slot13A = new MailStatistics(
            new MailStatisticsKey(today, 13, "doma.com", "xcmailr.test"), 10, 20, 30);

        mailStatisticsRepository.saveAll(List.of(slot12A, slot12B, slot13A));

        final List<QuarterHourTrafficStatEntry> results = mailStatisticsRepository.findQuarterHourTrafficStatsSince(today);
        assertNotNull(results);
        assertEquals(2, results.size(), "Should return 2 quarter-hour intervals");

        // 1st entry: slot 12
        final QuarterHourTrafficStatEntry entry1 = results.get(0);
        assertEquals(today, entry1.getDate());
        assertEquals(12, entry1.getQuarterHour());
        assertEquals(7L, entry1.getForwardCount(), "Forwards: 2 + 5 = 7");
        assertEquals(9L, entry1.getStoreCount(), "Stores: 3 + 6 = 9");
        assertEquals(5L, entry1.getDropCount(), "Drops: 1 + 4 = 5");

        // 2nd entry: slot 13
        final QuarterHourTrafficStatEntry entry2 = results.get(1);
        assertEquals(today, entry2.getDate());
        assertEquals(13, entry2.getQuarterHour());
        assertEquals(20L, entry2.getForwardCount());
        assertEquals(30L, entry2.getStoreCount());
        assertEquals(10L, entry2.getDropCount());
    }
}
