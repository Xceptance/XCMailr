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

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xceptance.xcmailr.config.XcmailrProperties;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailStatisticsRepository;
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import etc.HelperUtils;
import models.MBox;
import models.MailStatistics;
import models.MailStatisticsKey;
import models.MailTransaction;
import models.User;

/**
 * Scheduled background service for mailbox expiration, retention cleanup, API token expiry,
 * and statistical aggregation.
 *
 * @author Xceptance Software Technologies GmbH
 */
@Service
public class ExpirationScheduledService
{
    private static final Logger LOG = LoggerFactory.getLogger(ExpirationScheduledService.class);

    private final XcmailrProperties properties;
    private final MailboxRepository mailboxRepository;
    private final MailRepository mailRepository;
    private final UserRepository userRepository;
    private final MailTransactionRepository mailTransactionRepository;
    private final MailStatisticsRepository mailStatisticsRepository;

    /**
     * Constructs the expiration and maintenance scheduled service.
     *
     * @param properties application configuration
     * @param mailboxRepository repository for virtual mailboxes
     * @param mailRepository repository for emails
     * @param userRepository repository for users
     * @param mailTransactionRepository repository for transaction audit logs
     * @param mailStatisticsRepository repository for aggregate email statistics
     */
    public ExpirationScheduledService(final XcmailrProperties properties,
                                      final MailboxRepository mailboxRepository,
                                      final MailRepository mailRepository,
                                      final UserRepository userRepository,
                                      final MailTransactionRepository mailTransactionRepository,
                                      final MailStatisticsRepository mailStatisticsRepository)
    {
        this.properties = properties;
        this.mailboxRepository = mailboxRepository;
        this.mailRepository = mailRepository;
        this.userRepository = userRepository;
        this.mailTransactionRepository = mailTransactionRepository;
        this.mailStatisticsRepository = mailStatisticsRepository;
    }

    /**
     * Periodic scheduled runner executing all maintenance, expiration, and cleanup tasks.
     * Executes at a configurable interval (default every minute).
     */
    @Scheduled(fixedRateString = "${xcmailr.mbox.interval-minutes:1}", timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void runMaintenanceTasks()
    {
        final long now = System.currentTimeMillis();
        LOG.debug("Starting scheduled expiration and maintenance run at epoch {}", now);

        expireMailboxes(now);
        purgeExpiredMails(now);
        expireUserTokens(now);
        purgeExpiredInactiveUsers(now);
        aggregateAndCleanTransactions(now);
        purgeOldStatistics();

        LOG.debug("Finished scheduled maintenance run");
    }

    /**
     * Marks mailboxes whose expiration timestamp has passed as expired and inactive.
     *
     * @param now current epoch milliseconds
     */
    public void expireMailboxes(final long now)
    {
        final List<MBox> expiringBoxes = mailboxRepository.findActiveExpiredBoxes(now);
        for (final MBox box : expiringBoxes)
        {
            box.setActive(false);
            box.setExpired(true);
            mailboxRepository.save(box);
            LOG.info("Mailbox '{}' has expired and was disabled", box.getFullAddress());
        }
    }

    /**
     * Deletes stored email records older than the configured mail retention period.
     *
     * @param now current epoch milliseconds
     */
    public void purgeExpiredMails(final long now)
    {
        final int retentionMinutes = properties.getMbox().getRetentionPeriodMinutes();
        if (retentionMinutes > 0)
        {
            final long cutoff = now - (retentionMinutes * 60_000L);
            mailRepository.deleteByReceiveTimeLessThan(cutoff);
            LOG.debug("Purged emails received before epoch {}", cutoff);
        }
    }

    /**
     * Clears user API tokens that have passed the token expiration limit.
     *
     * @param now current epoch milliseconds
     */
    public void expireUserTokens(final long now)
    {
        final int tokenDays = properties.getApi().getTokenExpirationDays();
        if (tokenDays > 0)
        {
            final long cutoff = now - (tokenDays * 86_400_000L);
            final List<User> expiredUsers = userRepository.findUsersWithExpiredApiTokens(cutoff);
            for (final User user : expiredUsers)
            {
                user.setApiToken(null);
                user.setApiTokenCreationTimestamp(0L);
                userRepository.save(user);
                LOG.info("Revoked expired API token for user '{}'", user.getMail());
            }
        }
    }

    /**
     * Deletes inactive user registrations whose confirmation period has expired.
     *
     * @param now current epoch milliseconds
     */
    public void purgeExpiredInactiveUsers(final long now)
    {
        final int confirmHours = properties.getApp().getConfirmationPeriodHours();
        if (confirmHours > 0)
        {
            final long cutoff = now - (confirmHours * 3600_000L);
            final List<User> expiredUsers = userRepository.findExpiredInactiveUsers(cutoff);
            if (!expiredUsers.isEmpty())
            {
                userRepository.deleteAll(expiredUsers);
                LOG.info("Purged {} unconfirmed user registrations older than {} hours", expiredUsers.size(), confirmHours);
            }
        }
    }

    /**
     * Aggregates SMTP transaction logs into {@link MailStatistics} and cleans old transaction rows.
     *
     * @param now current epoch milliseconds
     */
    public void aggregateAndCleanTransactions(final long now)
    {
        final int maxAgeHours = properties.getMailTransaction().getMaxAgeHours();
        if (maxAgeHours <= 0)
        {
            return;
        }

        // Aggregate statistics for un-aggregated transactions
        final List<MailTransaction> transactions = mailTransactionRepository.findAll();
        final Map<MailStatisticsKey, int[]> statsMap = new HashMap<>();

        for (final MailTransaction tx : transactions)
        {
            final int status = tx.getStatus();
            if (status == 100 || status == 300) // 100: dropped, 300: forwarded
            {
                final MailStatisticsKey key = createStatisticsKey(tx);
                if (key != null)
                {
                    final int[] counts = statsMap.computeIfAbsent(key, k -> new int[2]); // [drops, forwards]
                    if (status == 100)
                    {
                        counts[0]++;
                    }
                    else
                    {
                        counts[1]++;
                    }
                }
            }
        }

        // Persist aggregated statistics
        for (final Map.Entry<MailStatisticsKey, int[]> entry : statsMap.entrySet())
        {
            final MailStatisticsKey key = entry.getKey();
            final int drops = entry.getValue()[0];
            final int forwards = entry.getValue()[1];

            final Optional<MailStatistics> existing = mailStatisticsRepository.findById(key);
            if (existing.isPresent())
            {
                final MailStatistics stat = existing.get();
                stat.setDropCount(stat.getDropCount() + drops);
                stat.setForwardCount(stat.getForwardCount() + forwards);
                mailStatisticsRepository.save(stat);
            }
            else
            {
                final MailStatistics stat = new MailStatistics();
                stat.setKey(key);
                stat.setDropCount(drops);
                stat.setForwardCount(forwards);
                mailStatisticsRepository.save(stat);
            }
        }

        // Delete old transactions past retention window
        final long cutoff = now - (maxAgeHours * 3600_000L);
        mailTransactionRepository.deleteByTsBefore(cutoff);
    }

    /**
     * Deletes statistical entries older than 30 days.
     */
    public void purgeOldStatistics()
    {
        final Date cutoff = Date.valueOf(LocalDate.now().minusDays(30));
        mailStatisticsRepository.deleteByKeyDateBefore(cutoff);
    }

    /**
     * Builds a {@link MailStatisticsKey} from a transaction record.
     *
     * @param tx the mail transaction
     * @return key, or null if source or target domain is missing
     */
    private MailStatisticsKey createStatisticsKey(final MailTransaction tx)
    {
        final String sourceDomain = getDomain(tx.getSourceaddr());
        final String targetDomain = getDomain(tx.getRelayaddr());
        if (sourceDomain == null || targetDomain == null)
        {
            return null;
        }

        final Date date = new Date(tx.getTs());
        final int quarterHour = getQuarterHour(tx.getTs());
        return new MailStatisticsKey(date, quarterHour, sourceDomain, targetDomain);
    }

    /**
     * Extracts domain name from an email address.
     *
     * @param email full email address
     * @return domain name or null
     */
    private String getDomain(final String email)
    {
        final String[] parts = HelperUtils.splitMailAddress(email);
        return (parts != null && parts.length > 1) ? parts[1].toLowerCase() : null;
    }

    /**
     * Computes the 15-minute bucket index of the day (0-95).
     *
     * @param timestamp epoch timestamp
     * @return bucket index (0..95)
     */
    private int getQuarterHour(final long timestamp)
    {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return (cal.get(Calendar.HOUR_OF_DAY) * 4) + (cal.get(Calendar.MINUTE) / 15);
    }
}
