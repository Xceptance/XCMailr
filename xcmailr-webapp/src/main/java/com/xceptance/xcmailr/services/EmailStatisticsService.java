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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xceptance.xcmailr.dto.ChartDataDto;
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailTransactionRepository.TransactionStatEntry;

/**
 * Service providing statistical time-series aggregations of inbound and relay email
 * transactions for administrative dashboard charts.
 * <p>
 * Aggregates email delivery metrics into continuous time buckets with automatic
 * zero-filling for inactive intervals and defensive boundary clamping.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@Service
@Transactional(readOnly = true)
public class EmailStatisticsService
{
    private static final Logger LOG = LoggerFactory.getLogger(EmailStatisticsService.class);

    /** HTTP / SMTP status code indicating successful email forwarding. */
    public static final int STATUS_FORWARDED = 300;

    /** Number of hours in the 24-hour activity window. */
    public static final int HOURS_IN_DAY = 24;

    /** Number of days in the 7-day activity window. */
    public static final int DAYS_IN_WEEK = 7;

    private static final DateTimeFormatter HOURLY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:00");
    private static final DateTimeFormatter DAILY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MailTransactionRepository transactionRepository;
    private final ZoneId zoneId;

    /**
     * Constructs the statistics service using system default time zone.
     *
     * @param transactionRepository the repository providing transaction audit records
     */
    @Autowired
    public EmailStatisticsService(final MailTransactionRepository transactionRepository)
    {
        this(transactionRepository, ZoneId.systemDefault());
    }

    /**
     * Constructs the statistics service with an explicit time zone for deterministic testing.
     *
     * @param transactionRepository the repository providing transaction audit records
     * @param zoneId the time zone for date truncation and bucket boundary calculations
     */
    public EmailStatisticsService(final MailTransactionRepository transactionRepository, final ZoneId zoneId)
    {
        this.transactionRepository = transactionRepository;
        this.zoneId = zoneId != null ? zoneId : ZoneId.systemDefault();
    }

    /**
     * Computes the 24-hour email activity chart dataset for the current system time.
     *
     * @return time-series chart data with 24 continuous hourly buckets
     */
    public ChartDataDto getLast24HoursChartData()
    {
        return getLast24HoursChartData(System.currentTimeMillis());
    }

    /**
     * Computes the 24-hour email activity chart dataset ending at the given epoch millisecond.
     * Generates 24 continuous hourly buckets ending at the hour of {@code now}.
     *
     * @param now epoch milliseconds representing the current time
     * @return time-series chart data with 24 continuous hourly buckets
     */
    public ChartDataDto getLast24HoursChartData(final long now)
    {
        final ZonedDateTime nowZdt = Instant.ofEpochMilli(now).atZone(zoneId).truncatedTo(ChronoUnit.HOURS);
        final ZonedDateTime startZdt = nowZdt.minusHours(HOURS_IN_DAY - 1);
        final long startEpochMs = startZdt.toInstant().toEpochMilli();

        final List<String> labels = new ArrayList<>(HOURS_IN_DAY);
        for (int i = 0; i < HOURS_IN_DAY; i++)
        {
            final ZonedDateTime bucketTime = startZdt.plusHours(i);
            labels.add(bucketTime.format(HOURLY_LABEL_FORMATTER));
        }

        final long[] forwarded = new long[HOURS_IN_DAY];
        final long[] dropped = new long[HOURS_IN_DAY];

        final List<TransactionStatEntry> entries = transactionRepository.findStatsSince(startEpochMs);
        for (final TransactionStatEntry entry : entries)
        {
            final long ts = entry.getTs();
            if (ts < startEpochMs)
            {
                continue;
            }

            int bucketIndex = (int) ((ts - startEpochMs) / 3600_000L);
            if (bucketIndex < 0)
            {
                continue;
            }
            if (bucketIndex >= HOURS_IN_DAY)
            {
                bucketIndex = HOURS_IN_DAY - 1;
            }

            if (entry.getStatus() == STATUS_FORWARDED)
            {
                forwarded[bucketIndex]++;
            }
            else
            {
                dropped[bucketIndex]++;
            }
        }

        return new ChartDataDto(labels, toLongList(forwarded), toLongList(dropped));
    }

    /**
     * Computes the 7-day email activity chart dataset for the current system time.
     *
     * @return time-series chart data with 7 continuous daily buckets
     */
    public ChartDataDto getLast7DaysChartData()
    {
        return getLast7DaysChartData(System.currentTimeMillis());
    }

    /**
     * Computes the 7-day email activity chart dataset ending at the given epoch millisecond.
     * Generates 7 continuous daily buckets ending on the local date of {@code now}.
     *
     * @param now epoch milliseconds representing the current time
     * @return time-series chart data with 7 continuous daily buckets
     */
    public ChartDataDto getLast7DaysChartData(final long now)
    {
        final LocalDate today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate();
        final LocalDate startDay = today.minusDays(DAYS_IN_WEEK - 1);
        final ZonedDateTime startZdt = startDay.atStartOfDay(zoneId);
        final long startEpochMs = startZdt.toInstant().toEpochMilli();

        final List<String> labels = new ArrayList<>(DAYS_IN_WEEK);
        for (int i = 0; i < DAYS_IN_WEEK; i++)
        {
            final LocalDate bucketDay = startDay.plusDays(i);
            labels.add(bucketDay.format(DAILY_LABEL_FORMATTER));
        }

        final long[] forwarded = new long[DAYS_IN_WEEK];
        final long[] dropped = new long[DAYS_IN_WEEK];

        final List<TransactionStatEntry> entries = transactionRepository.findStatsSince(startEpochMs);
        for (final TransactionStatEntry entry : entries)
        {
            final long ts = entry.getTs();
            if (ts < startEpochMs)
            {
                continue;
            }

            final LocalDate entryDate = Instant.ofEpochMilli(ts).atZone(zoneId).toLocalDate();
            int bucketIndex = (int) ChronoUnit.DAYS.between(startDay, entryDate);
            if (bucketIndex < 0)
            {
                continue;
            }
            if (bucketIndex >= DAYS_IN_WEEK)
            {
                bucketIndex = DAYS_IN_WEEK - 1;
            }

            if (entry.getStatus() == STATUS_FORWARDED)
            {
                forwarded[bucketIndex]++;
            }
            else
            {
                dropped[bucketIndex]++;
            }
        }

        return new ChartDataDto(labels, toLongList(forwarded), toLongList(dropped));
    }

    private static List<Long> toLongList(final long[] array)
    {
        final List<Long> list = new ArrayList<>(array.length);
        for (final long val : array)
        {
            list.add(val);
        }
        return list;
    }
}
