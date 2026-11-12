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
package com.xceptance.xcmailr.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.xceptance.xcmailr.dto.ChartDataDto;
import com.xceptance.xcmailr.repositories.MailStatisticsRepository;
import com.xceptance.xcmailr.repositories.MailStatisticsRepository.DailyTrafficStatEntry;
import com.xceptance.xcmailr.repositories.MailStatisticsRepository.QuarterHourTrafficStatEntry;

/**
 * Unit tests for {@link EmailStatisticsService} following the test pyramid.
 * Verifies happy path aggregation, zero-fill continuity, and boundary handling from {@code MAIL_STATISTICS}.
 *
 * @author Xceptance Software Technologies GmbH
 */
class EmailStatisticsServiceTest
{
    private static final ZoneId UTC = ZoneOffset.UTC;

    private MailStatisticsRepository mailStatisticsRepository;
    private EmailStatisticsService statisticsService;

    @BeforeEach
    void setUp()
    {
        mailStatisticsRepository = mock(MailStatisticsRepository.class);
        statisticsService = new EmailStatisticsService(mailStatisticsRepository, UTC);
    }

    private static DailyTrafficStatEntry createDailyEntry(final Date date, final long forwarded, final long stored, final long dropped)
    {
        return new DailyTrafficStatEntry()
        {
            @Override
            public Date getDate()
            {
                return date;
            }

            @Override
            public long getForwardCount()
            {
                return forwarded;
            }

            @Override
            public long getStoreCount()
            {
                return stored;
            }

            @Override
            public long getDropCount()
            {
                return dropped;
            }
        };
    }

    private static QuarterHourTrafficStatEntry createQuarterHourEntry(final Date date, final int quarterHour, final long forwarded, final long stored, final long dropped)
    {
        return new QuarterHourTrafficStatEntry()
        {
            @Override
            public Date getDate()
            {
                return date;
            }

            @Override
            public int getQuarterHour()
            {
                return quarterHour;
            }

            @Override
            public long getForwardCount()
            {
                return forwarded;
            }

            @Override
            public long getStoreCount()
            {
                return stored;
            }

            @Override
            public long getDropCount()
            {
                return dropped;
            }
        };
    }

    @Test
    @DisplayName("Happy Path: Last 24 Hours aggregates quarter-hour forwarded, stored, and dropped counts into correct hourly buckets")
    void testGetLast24HoursChartData_HappyPath()
    {
        // Fixed anchor time: 2026-11-10T15:30:00Z
        final ZonedDateTime nowZdt = ZonedDateTime.of(2026, 11, 10, 15, 30, 0, 0, UTC);
        final long now = nowZdt.toInstant().toEpochMilli();

        // 24 hours window starts at 2026-11-09T16:00:00Z
        final Date yesterday = Date.valueOf(LocalDate.of(2026, 11, 9));
        final Date today = Date.valueOf(LocalDate.of(2026, 11, 10));

        final List<QuarterHourTrafficStatEntry> entries = new ArrayList<>();
        // Bucket 0: 2026-11-09 16:00 (quarterHour 64) -> 1 forwarded, 1 stored, 1 dropped
        entries.add(createQuarterHourEntry(yesterday, 64, 1, 1, 1));
        // Bucket 0: 2026-11-09 16:15 (quarterHour 65) -> 1 forwarded, 0 stored, 0 dropped
        entries.add(createQuarterHourEntry(yesterday, 65, 1, 0, 0));

        // Bucket 12: 12 hours after start (2026-11-10 04:00, quarterHour 16) -> 2 forwarded, 1 stored, 0 dropped
        entries.add(createQuarterHourEntry(today, 16, 2, 1, 0));

        // Bucket 23 (current hour): 2026-11-10 15:00 (quarterHour 60) -> 0 forwarded, 0 stored, 1 dropped
        entries.add(createQuarterHourEntry(today, 60, 0, 0, 1));

        when(mailStatisticsRepository.findQuarterHourTrafficStatsSince(any(Date.class))).thenReturn(entries);

        final ChartDataDto dto = statisticsService.getLast24HoursChartData(now);

        assertNotNull(dto);
        assertEquals(24, dto.labels().size());
        assertEquals(24, dto.forwarded().size());
        assertEquals(24, dto.stored().size());
        assertEquals(24, dto.dropped().size());

        // First label is 16:00, last label is 15:00
        assertEquals("16:00", dto.labels().get(0));
        assertEquals("15:00", dto.labels().get(23));

        // Bucket 0: 1+1=2 forwarded, 1 stored, 1 dropped
        assertEquals(2L, dto.forwarded().get(0));
        assertEquals(1L, dto.stored().get(0));
        assertEquals(1L, dto.dropped().get(0));

        // Bucket 12
        assertEquals(2L, dto.forwarded().get(12));
        assertEquals(1L, dto.stored().get(12));
        assertEquals(0L, dto.dropped().get(12));

        // Bucket 23
        assertEquals(0L, dto.forwarded().get(23));
        assertEquals(0L, dto.stored().get(23));
        assertEquals(1L, dto.dropped().get(23));
    }

    @Test
    @DisplayName("Special Case: Zero-fill continuity ensures continuous zero baseline when no statistics exist")
    void testGetLast24HoursChartData_ZeroFillWhenEmpty()
    {
        final long now = ZonedDateTime.of(2026, 11, 10, 12, 0, 0, 0, UTC).toInstant().toEpochMilli();
        when(mailStatisticsRepository.findQuarterHourTrafficStatsSince(any(Date.class))).thenReturn(List.of());

        final ChartDataDto dto = statisticsService.getLast24HoursChartData(now);

        assertNotNull(dto);
        assertEquals(24, dto.labels().size());
        for (int i = 0; i < 24; i++)
        {
            assertEquals(0L, dto.forwarded().get(i), "Forwarded count must be zero for bucket " + i);
            assertEquals(0L, dto.stored().get(i), "Stored count must be zero for bucket " + i);
            assertEquals(0L, dto.dropped().get(i), "Dropped count must be zero for bucket " + i);
        }
    }

    @Test
    @DisplayName("Edge Case: Boundary handling ignores quarter hours before window start, beyond current hour, or invalid entries")
    void testGetLast24HoursChartData_BoundaryHandling()
    {
        final ZonedDateTime nowZdt = ZonedDateTime.of(2026, 11, 10, 10, 15, 0, 0, UTC);
        final long now = nowZdt.toInstant().toEpochMilli();
        // Window: 2026-11-09 11:00 (quarterHour 44) to 2026-11-10 10:00 (quarterHour 40)
        final Date yesterday = Date.valueOf(LocalDate.of(2026, 11, 9));
        final Date today = Date.valueOf(LocalDate.of(2026, 11, 10));

        final List<QuarterHourTrafficStatEntry> entries = new ArrayList<>();
        // Before window (10:45 yesterday, quarterHour 43) -> ignored
        entries.add(createQuarterHourEntry(yesterday, 43, 10, 0, 0));

        // Exact start boundary (11:00 yesterday, quarterHour 44) -> bucket 0
        entries.add(createQuarterHourEntry(yesterday, 44, 5, 2, 1));

        // Beyond window (11:00 today, quarterHour 44) -> ignored
        entries.add(createQuarterHourEntry(today, 44, 15, 0, 0));

        // Invalid quarterHour (<0 or >95) or null date -> ignored
        entries.add(createQuarterHourEntry(today, -1, 1, 0, 0));
        entries.add(createQuarterHourEntry(today, 96, 1, 0, 0));
        entries.add(createQuarterHourEntry(null, 20, 1, 0, 0));

        when(mailStatisticsRepository.findQuarterHourTrafficStatsSince(any(Date.class))).thenReturn(entries);

        final ChartDataDto dto = statisticsService.getLast24HoursChartData(now);

        assertEquals(5L, dto.forwarded().get(0), "Exact start boundary should fall in bucket 0");
        assertEquals(2L, dto.stored().get(0));
        assertEquals(1L, dto.dropped().get(0));
    }

    @Test
    @DisplayName("Happy Path: Last 7 Days aggregates forwarded, stored, and dropped counts across daily buckets")
    void testGetLast7DaysChartData_HappyPath()
    {
        // Anchor: 2026-11-10T14:00:00Z (Tuesday)
        final ZonedDateTime nowZdt = ZonedDateTime.of(2026, 11, 10, 14, 0, 0, 0, UTC);
        final long now = nowZdt.toInstant().toEpochMilli();

        // 7 days: 2026-11-04 to 2026-11-10
        final Date day0 = Date.valueOf(LocalDate.of(2026, 11, 4));
        final Date day6 = Date.valueOf(LocalDate.of(2026, 11, 10));

        final List<DailyTrafficStatEntry> entries = new ArrayList<>();
        // Day 0 (2026-11-04): 3 forwarded, 1 stored, 1 dropped
        entries.add(createDailyEntry(day0, 3, 1, 1));

        // Day 6 (Today, 2026-11-10): 2 forwarded, 1 stored, 0 dropped
        entries.add(createDailyEntry(day6, 2, 1, 0));

        when(mailStatisticsRepository.findDailyTrafficStatsSince(any(Date.class))).thenReturn(entries);

        final ChartDataDto dto = statisticsService.getLast7DaysChartData(now);

        assertNotNull(dto);
        assertEquals(7, dto.labels().size());
        assertEquals(7, dto.forwarded().size());
        assertEquals(7, dto.stored().size());
        assertEquals(7, dto.dropped().size());
        assertEquals("2026-11-04", dto.labels().get(0));
        assertEquals("2026-11-10", dto.labels().get(6));

        // Day 0
        assertEquals(3L, dto.forwarded().get(0));
        assertEquals(1L, dto.stored().get(0));
        assertEquals(1L, dto.dropped().get(0));

        // Day 1 to 5: zero-filled
        for (int i = 1; i <= 5; i++)
        {
            assertEquals(0L, dto.forwarded().get(i), "Day " + i + " must be zero-filled");
            assertEquals(0L, dto.stored().get(i), "Day " + i + " must be zero-filled");
            assertEquals(0L, dto.dropped().get(i), "Day " + i + " must be zero-filled");
        }

        // Day 6
        assertEquals(2L, dto.forwarded().get(6));
        assertEquals(1L, dto.stored().get(6));
        assertEquals(0L, dto.dropped().get(6));
    }

    @Test
    @DisplayName("Special Case: Last 7 Days zero-fill continuity when no statistics exist")
    void testGetLast7DaysChartData_ZeroFillWhenEmpty()
    {
        final long now = ZonedDateTime.of(2026, 11, 10, 12, 0, 0, 0, UTC).toInstant().toEpochMilli();
        when(mailStatisticsRepository.findDailyTrafficStatsSince(any(Date.class))).thenReturn(List.of());

        final ChartDataDto dto = statisticsService.getLast7DaysChartData(now);

        assertNotNull(dto);
        assertEquals(7, dto.labels().size());
        for (int i = 0; i < 7; i++)
        {
            assertEquals(0L, dto.forwarded().get(i));
            assertEquals(0L, dto.stored().get(i));
            assertEquals(0L, dto.dropped().get(i));
        }
    }

    @Test
    @DisplayName("Edge Case: Last 7 Days ignores entries before start date or beyond current date")
    void testGetLast7DaysChartData_BoundaryHandling()
    {
        final ZonedDateTime nowZdt = ZonedDateTime.of(2026, 11, 10, 12, 0, 0, 0, UTC);
        final long now = nowZdt.toInstant().toEpochMilli();

        final Date pastDate = Date.valueOf(LocalDate.of(2026, 11, 3));
        final Date startDate = Date.valueOf(LocalDate.of(2026, 11, 4));
        final Date futureDate = Date.valueOf(LocalDate.of(2026, 11, 11));

        final List<DailyTrafficStatEntry> entries = new ArrayList<>();
        // Before start date
        entries.add(createDailyEntry(pastDate, 10, 10, 10));
        // Valid start date
        entries.add(createDailyEntry(startDate, 1, 2, 3));
        // Future date beyond 7 days
        entries.add(createDailyEntry(futureDate, 5, 5, 5));
        // Null date entry
        entries.add(createDailyEntry(null, 1, 1, 1));

        when(mailStatisticsRepository.findDailyTrafficStatsSince(any(Date.class))).thenReturn(entries);

        final ChartDataDto dto = statisticsService.getLast7DaysChartData(now);

        assertEquals(1L, dto.forwarded().get(0));
        assertEquals(2L, dto.stored().get(0));
        assertEquals(3L, dto.dropped().get(0));
    }

    @Test
    @DisplayName("Special Case: Default no-arg methods execute without throwing")
    void testDefaultNoArgMethods()
    {
        when(mailStatisticsRepository.findQuarterHourTrafficStatsSince(any(Date.class))).thenReturn(List.of());
        when(mailStatisticsRepository.findDailyTrafficStatsSince(any(Date.class))).thenReturn(List.of());

        final ChartDataDto h24 = statisticsService.getLast24HoursChartData();
        final ChartDataDto d7 = statisticsService.getLast7DaysChartData();

        assertNotNull(h24);
        assertEquals(24, h24.labels().size());
        assertNotNull(d7);
        assertEquals(7, d7.labels().size());
    }
}
