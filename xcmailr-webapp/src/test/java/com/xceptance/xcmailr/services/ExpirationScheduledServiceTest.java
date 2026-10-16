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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.xceptance.xcmailr.config.XcmailrProperties;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailStatisticsRepository;
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.MBox;
import models.Mail;
import models.MailStatistics;
import models.MailTransaction;
import models.User;

/**
 * Integration and unit tests for {@link ExpirationScheduledService}.
 * Verifies mailbox expiration, retention cleanup, API token expiry,
 * unconfirmed user cleanup, and transaction aggregation.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ExpirationScheduledServiceTest
{
    @Autowired
    private ExpirationScheduledService expirationService;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailTransactionRepository mailTransactionRepository;

    @Autowired
    private MailStatisticsRepository mailStatisticsRepository;

    @Autowired
    private XcmailrProperties properties;

    private User testUser;

    @BeforeEach
    public void setup()
    {
        testUser = new User("Sched", "User", "sched_user@xcmailr.test", "password", "en");
        testUser.setActive(true);
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should mark active mailboxes with expired timestamps as inactive and expired")
    public void testMailboxExpiration()
    {
        final long now = System.currentTimeMillis();

        // Create mailbox expired 5 minutes ago
        final MBox expiredBox = new MBox("expiredbox", "xcmailr.test", now - 300_000L, false, testUser);
        expiredBox.setActive(true);
        mailboxRepository.save(expiredBox);

        // Create active mailbox valid for 1 hour
        final MBox activeBox = new MBox("activebox", "xcmailr.test", now + 3600_000L, false, testUser);
        activeBox.setActive(true);
        mailboxRepository.save(activeBox);

        // Run expiration task
        expirationService.expireMailboxes(now);

        final MBox reloadedExpired = mailboxRepository.findById(expiredBox.getId()).orElseThrow();
        assertFalse(reloadedExpired.isActive(), "Expired mailbox should no longer be active");
        assertTrue(reloadedExpired.isExpired(), "Expired mailbox should have expired flag set to true");

        final MBox reloadedActive = mailboxRepository.findById(activeBox.getId()).orElseThrow();
        assertTrue(reloadedActive.isActive(), "Active mailbox should remain active");
        assertFalse(reloadedActive.isExpired(), "Active mailbox should not have expired flag set");
    }

    @Test
    @DisplayName("Should purge emails older than the retention period")
    public void testMailRetentionPurge()
    {
        final long now = System.currentTimeMillis();
        final MBox box = new MBox("retentionbox", "xcmailr.test", 0L, false, testUser);
        mailboxRepository.save(box);

        // Email older than 10 minutes (configured default retention is 10 min)
        final Mail oldMail = new Mail();
        oldMail.setMailbox(box);
        oldMail.setSender("old@example.com");
        oldMail.setSubject("Old Subject");
        oldMail.setMessage("Old Body".getBytes());
        oldMail.setReceiveTime(now - 15 * 60_000L);
        oldMail.setUuid("uuid-old-mail");
        mailRepository.save(oldMail);

        // Fresh email received just now
        final Mail freshMail = new Mail();
        freshMail.setMailbox(box);
        freshMail.setSender("fresh@example.com");
        freshMail.setSubject("Fresh Subject");
        freshMail.setMessage("Fresh Body".getBytes());
        freshMail.setReceiveTime(now - 60_000L);
        freshMail.setUuid("uuid-fresh-mail");
        mailRepository.save(freshMail);

        expirationService.purgeExpiredMails(now);

        assertFalse(mailRepository.findByUuid("uuid-old-mail").isPresent(), "Old mail should be purged");
        assertTrue(mailRepository.findByUuid("uuid-fresh-mail").isPresent(), "Fresh mail should remain in repository");
    }

    @Test
    @DisplayName("Should clear API token for users past the expiration threshold")
    public void testUserApiTokenExpiration()
    {
        final long now = System.currentTimeMillis();
        final User userWithExpiredToken = new User("Expired", "User", "expired_token@xcmailr.test", "pass", "en");
        userWithExpiredToken.setActive(true);
        userWithExpiredToken.setApiToken("expired-token-12345");
        // Created 40 days ago (threshold is 30 days)
        userWithExpiredToken.setApiTokenCreationTimestamp(now - (40L * 86_400_000L));
        userRepository.save(userWithExpiredToken);

        final User userWithActiveToken = new User("Active", "User", "active_token@xcmailr.test", "pass", "en");
        userWithActiveToken.setActive(true);
        userWithActiveToken.setApiToken("active-token-67890");
        // Created 2 days ago
        userWithActiveToken.setApiTokenCreationTimestamp(now - (2L * 86_400_000L));
        userRepository.save(userWithActiveToken);

        expirationService.expireUserTokens(now);

        final User reloadedExpired = userRepository.findById(userWithExpiredToken.getId()).orElseThrow();
        assertNull(reloadedExpired.getApiToken(), "Expired API token should be set to null");
        assertEquals(0L, reloadedExpired.getApiTokenCreationTimestamp(), "Timestamp should be reset to 0");

        final User reloadedActive = userRepository.findById(userWithActiveToken.getId()).orElseThrow();
        assertEquals("active-token-67890", reloadedActive.getApiToken(), "Active API token should remain intact");
    }

    @Test
    @DisplayName("Should aggregate drop and forward transactions into MailStatistics")
    public void testStatisticalAggregation()
    {
        final long now = System.currentTimeMillis();

        // Status 100: dropped mail
        final MailTransaction txDrop = new MailTransaction(100, "spammer@external.test", "victim@xcmailr.test", null);
        txDrop.setTs(now);
        mailTransactionRepository.save(txDrop);

        // Status 300: forwarded mail
        final MailTransaction txForward = new MailTransaction(300, "friend@external.test", "user@xcmailr.test", "real@user.test");
        txForward.setTs(now);
        mailTransactionRepository.save(txForward);

        expirationService.aggregateAndCleanTransactions(now);

        final List<MailStatistics> allStats = mailStatisticsRepository.findAll();
        assertFalse(allStats.isEmpty(), "Statistics entries should have been created");

        boolean dropFound = false;
        boolean forwardFound = false;
        for (final MailStatistics stat : allStats)
        {
            if (stat.getDropCount() > 0)
            {
                dropFound = true;
            }
            if (stat.getForwardCount() > 0)
            {
                forwardFound = true;
            }
        }

        assertTrue(dropFound, "Aggregated drop count should be found in statistics");
        assertTrue(forwardFound, "Aggregated forward count should be found in statistics");
    }
}
