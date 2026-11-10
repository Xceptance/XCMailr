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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.xceptance.xcmailr.dto.ChartDataDto;
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailTransactionRepository.TransactionStatEntry;

/**
 * Unit tests for {@link EmailStatisticsService} following the test pyramid.
 * Verifies happy path aggregation, zero-fill continuity, and edge case boundary clamping.
 *
 * @author Xceptance Software Technologies GmbH
 */
class EmailStatisticsServiceTest
{
    private static final ZoneId UTC = ZoneOffset.UTC;

    private MailTransactionRepository transactionRepository;
    private EmailStatisticsService statisticsService;

    @BeforeEach
    void setUp()
    {
        transactionRepository = mock(MailTransactionRepository.class);
        statisticsService = new EmailStatisticsService(transactionRepository, UTC);
    }

    private static TransactionStatEntry createEntry(final long ts, final int status)
    {
        return new TransactionStatEntry()
        {
            @Override
            public Long getTs()
            {
                return ts;
            }

            @Override
            public int getStatus()
            {
                return status;
            }
        };
    }

    @Test
    @DisplayName("Happy Path: Last 24 Hours aggregates forwarded and dropped counts into correct hourly buckets")
    void testGetLast24HoursChartData_HappyPath()
    {
        // Fixed anchor time: 2026-11-10T15:30:00Z
        final ZonedDateTime nowZdt = ZonedDateTime.of(2026, 11, 10, 15, 30, 0, 0, UTC);
        final long now = nowZdt.toInstant().toEpochMilli();

        // 24 hours window starts at 2026-11-09T16:00:00Z
        final ZonedDateTime startZdt = nowZdt.truncatedTo(ChronoUnit.HOURS).minusHours(23);
        final long startEpoch = startZdt.toInstant().toEpochMilli();

        final List<TransactionStatEntry> entries = new ArrayList<>();
        // Bucket 0: 2026-11-09T16:15:00Z -> 1 forwarded (300), 1 dropped (100)
        entries.add(createEntry(startEpoch + 15 * 60_000L, 300));
        entries.add(createEntry(startEpoch + 20 * 60_000L, 100));

        // Bucket 12: 12 hours after start -> 2 forwarded (300)
        entries.add(createEntry(startEpoch + 12 * 3600_000L + 5 * 60_000L, 300));
        entries.add(createEntry(startEpoch + 12 * 3600_000L + 10 * 60_000L, 300));

        // Bucket 23 (current hour): 2026-11-10T15:10:00Z -> 1 dropped (400)
        entries.add(createEntry(nowZdt.truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli() + 10 * 60_000L, 400));

        when(transactionRepository.findStatsSince(anyLong())).thenReturn(entries);

        final ChartDataDto dto = statisticsService.getLast24HoursChartData(now);

        assertNotNull(dto);
        assertEquals(24, dto.labels().size());
        assertEquals(24, dto.forwarded().size());
        assertEquals(24, dto.dropped().size());

        // First label is 16:00, last label is 15:00
        assertEquals("16:00", dto.labels().get(0));
        assertEquals("15:00", dto.labels().get(23));

        // Bucket 0
        assertEquals(1L, dto.forwarded().get(0));
        assertEquals(1L, dto.dropped().get(0));

        // Bucket 12
        assertEquals(2L, dto.forwarded().get(12));
        assertEquals(0L, dto.dropped().get(12));

        // Bucket 23
        assertEquals(0L, dto.forwarded().get(23));
        assertEquals(1L, dto.dropped().get(23));
    }

    @Test
    @DisplayName("Special Case: Zero-fill continuity ensures continuous zero baseline when no transactions exist")
    void testGetLast24HoursChartData_ZeroFillWhenEmpty()
    {
        final long now = ZonedDateTime.of(2026, 11, 10, 12, 0, 0, 0, UTC).toInstant().toEpochMilli();
        when(transactionRepository.findStatsSince(anyLong())).thenReturn(List.of());

        final ChartDataDto dto = statisticsService.getLast24HoursChartData(now);

        assertNotNull(dto);
        assertEquals(24, dto.labels().size());
        for (int i = 0; i < 24; i++)
        {
            assertEquals(0L, dto.forwarded().get(i), "Forwarded count must be zero for bucket " + i);
            assertEquals(0L, dto.dropped().get(i), "Dropped count must be zero for bucket " + i);
        }
    }

    @Test
    @DisplayName("Edge Case: Boundary clamping handles transactions at exact boundary, past now, or before window")
    void testGetLast24HoursChartData_BoundaryClamping()
    {
        final ZonedDateTime nowZdt = ZonedDateTime.of(2026, 11, 10, 10, 15, 0, 0, UTC);
        final long now = nowZdt.toInstant().toEpochMilli();
        final ZonedDateTime startZdt = nowZdt.truncatedTo(ChronoUnit.HOURS).minusHours(23);
        final long startEpoch = startZdt.toInstant().toEpochMilli();

        final List<TransactionStatEntry> entries = new ArrayList<>();
        // Before window (should be ignored)
        entries.add(createEntry(startEpoch - 1000L, 300));

        // Exact start boundary
        entries.add(createEntry(startEpoch, 300));

        // In the future / beyond now (clamped to bucket 23)
        entries.add(createEntry(now + 3600_000L, 200));

        when(transactionRepository.findStatsSince(anyLong())).thenReturn(entries);

        final ChartDataDto dto = statisticsService.getLast24HoursChartData(now);

        assertEquals(1L, dto.forwarded().get(0), "Exact start boundary should fall in bucket 0");
        assertEquals(1L, dto.dropped().get(23), "Future transaction should be defensively clamped to bucket 23");
    }

    @Test
    @DisplayName("Happy Path: Last 7 Days aggregates forwarded and dropped counts across daily buckets")
    void testGetLast7DaysChartData_HappyPath()
    {
        // Anchor: 2026-11-10T14:00:00Z (Tuesday)
        final ZonedDateTime nowZdt = ZonedDateTime.of(2026, 11, 10, 14, 0, 0, 0, UTC);
        final long now = nowZdt.toInstant().toEpochMilli();

        // 7 days: 2026-11-04 to 2026-11-10
        final LocalDate startDay = LocalDate.of(2026, 11, 4);
        final long day0Epoch = startDay.atStartOfDay(UTC).toInstant().toEpochMilli();

        final List<TransactionStatEntry> entries = new ArrayList<>();
        // Day 0 (2026-11-04): 3 forwarded, 1 dropped
        entries.add(createEntry(day0Epoch + 3600_000L, 300));
        entries.add(createEntry(day0Epoch + 7200_000L, 300));
        entries.add(createEntry(day0Epoch + 10800_000L, 300));
        entries.add(createEntry(day0Epoch + 14400_000L, 500)); // dropped

        // Day 6 (Today, 2026-11-10): 2 forwarded, 0 dropped
        entries.add(createEntry(now - 1000L, 300));
        entries.add(createEntry(now, 300));

        when(transactionRepository.findStatsSince(anyLong())).thenReturn(entries);

        final ChartDataDto dto = statisticsService.getLast7DaysChartData(now);

        assertNotNull(dto);
        assertEquals(7, dto.labels().size());
        assertEquals("2026-11-04", dto.labels().get(0));
        assertEquals("2026-11-10", dto.labels().get(6));

        // Day 0
        assertEquals(3L, dto.forwarded().get(0));
        assertEquals(1L, dto.dropped().get(0));

        // Day 1 to 5: zero-filled
        for (int i = 1; i <= 5; i++)
        {
            assertEquals(0L, dto.forwarded().get(i), "Day " + i + " must be zero-filled");
            assertEquals(0L, dto.dropped().get(i), "Day " + i + " must be zero-filled");
        }

        // Day 6
        assertEquals(2L, dto.forwarded().get(6));
        assertEquals(0L, dto.dropped().get(6));
    }

    @Test
    @DisplayName("Special Case: Last 7 Days zero-fill continuity when no transactions exist")
    void testGetLast7DaysChartData_ZeroFillWhenEmpty()
    {
        final long now = ZonedDateTime.of(2026, 11, 10, 12, 0, 0, 0, UTC).toInstant().toEpochMilli();
        when(transactionRepository.findStatsSince(anyLong())).thenReturn(List.of());

        final ChartDataDto dto = statisticsService.getLast7DaysChartData(now);

        assertNotNull(dto);
        assertEquals(7, dto.labels().size());
        for (int i = 0; i < 7; i++)
        {
            assertEquals(0L, dto.forwarded().get(i));
            assertEquals(0L, dto.dropped().get(i));
        }
    }

    @Test
    @DisplayName("Edge Case: Last 7 Days handles entries before start day or clamped to end day")
    void testGetLast7DaysChartData_BoundaryClamping()
    {
        final ZonedDateTime nowZdt = ZonedDateTime.of(2026, 11, 10, 12, 0, 0, 0, UTC);
        final long now = nowZdt.toInstant().toEpochMilli();
        final LocalDate startDay = LocalDate.of(2026, 11, 4);
        final long startEpoch = startDay.atStartOfDay(UTC).toInstant().toEpochMilli();

        final List<TransactionStatEntry> entries = new ArrayList<>();
        // Before start epoch
        entries.add(createEntry(startEpoch - 1000L, 300));
        // Future date beyond 7 days
        final long futureEpoch = LocalDate.of(2026, 11, 12).atStartOfDay(UTC).toInstant().toEpochMilli();
        entries.add(createEntry(futureEpoch, 100));

        when(transactionRepository.findStatsSince(anyLong())).thenReturn(entries);

        final ChartDataDto dto = statisticsService.getLast7DaysChartData(now);

        assertEquals(0L, dto.forwarded().get(0), "Transaction before start epoch should be ignored");
        assertEquals(1L, dto.dropped().get(6), "Transaction on future date should be defensively clamped to bucket 6");
    }

    @Test
    @DisplayName("Special Case: Default no-arg methods execute without throwing")
    void testDefaultNoArgMethods()
    {
        when(transactionRepository.findStatsSince(anyLong())).thenReturn(List.of());

        final ChartDataDto h24 = statisticsService.getLast24HoursChartData();
        final ChartDataDto d7 = statisticsService.getLast7DaysChartData();

        assertNotNull(h24);
        assertEquals(24, h24.labels().size());
        assertNotNull(d7);
        assertEquals(7, d7.labels().size());
    }
}
