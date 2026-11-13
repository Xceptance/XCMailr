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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import com.xceptance.xcmailr.config.XcmailrProperties;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailStatisticsRepository;
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import com.xceptance.xcmailr.models.MBox;
import com.xceptance.xcmailr.models.Mail;
import com.xceptance.xcmailr.models.MailStatistics;
import com.xceptance.xcmailr.models.MailStatisticsKey;
import com.xceptance.xcmailr.models.MailTransaction;
import com.xceptance.xcmailr.models.User;

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
    private MailTransactionQueueService transactionQueueService;

    @Autowired
    private XcmailrProperties properties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private User testUser;

    @BeforeEach
    public void setup()
    {
        transactionQueueService.clear();
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

    /**
     * Verifies the happy path for unconfirmed registration cleanup: genuinely abandoned
     * unconfirmed registrations ({@code active = false}, non-null {@code confirmation},
     * and {@code ts_confirm > 0} older than the cutoff) are purged during scheduled
     * maintenance runs.
     */
    @Test
    @DisplayName("Happy Path: Should purge expired unconfirmed user registrations")
    public void testPurgeExpiredUnconfirmedUsersHappyPath()
    {
        final long now = System.currentTimeMillis();

        // Expired unconfirmed registration (24 hours past confirmation cutoff)
        final User expiredUnconfirmedUser = new User("Unconfirmed", "Expired", "unconfirmed_expired@xcmailr.test", "pass", "en");
        expiredUnconfirmedUser.setActive(false);
        expiredUnconfirmedUser.setConfirmation("reg-token-456");
        expiredUnconfirmedUser.setTs_confirm(now - (24L * 3_600_000L));
        userRepository.save(expiredUnconfirmedUser);

        // Execute scheduled maintenance tasks
        expirationService.runMaintenanceTasks();

        // Verify that the expired unconfirmed registration was purged
        assertFalse(userRepository.findById(expiredUnconfirmedUser.getId()).isPresent(),
                    "Expired unconfirmed registration must be purged");
    }

    /**
     * Verifies the legacy production database safeguard after the {@code V6} Flyway migration:
     * an already-deactivated legacy user who owned mailboxes and carried a historical 2019
     * {@code confirmation} token has their token and timestamp cleared to {@code null} by
     * {@code V6__Clean_Legacy_Confirmation_Tokens.sql}, and is strictly preserved along with
     * all of their mailboxes across scheduled maintenance runs.
     *
     * @throws Exception if executing the V6 migration SQL fails
     */
    @Test
    @DisplayName("Special Case: Should preserve V6-migrated legacy deactivated user who owns mailboxes")
    public void testLegacyDeactivatedUserWithMailboxesPreserved() throws Exception
    {
        final long now = System.currentTimeMillis();

        // Legacy deactivated user (e.g. m.pfotenhauer+hb@xceptance.com) with 2019 confirmation token and mailboxes
        final User legacyDeactivatedWithBoxes = new User("Legacy", "Deactivated", "legacy_deactivated@xcmailr.test", "pass", "en");
        legacyDeactivatedWithBoxes.setActive(false);
        legacyDeactivatedWithBoxes.setConfirmation("legacy-2019-token");
        legacyDeactivatedWithBoxes.setTs_confirm(now - (365L * 86_400_000L));
        userRepository.saveAndFlush(legacyDeactivatedWithBoxes);

        final MBox userBox = new MBox("legacybox", "xcmailr.test", 0L, false, legacyDeactivatedWithBoxes);
        mailboxRepository.saveAndFlush(userBox);

        // Apply V6 data migration to sanitize legacy tokens on active users and mailbox owners
        final ClassPathResource v6Resource = new ClassPathResource("db/migration/V6__Clean_Legacy_Confirmation_Tokens.sql");
        final String v6Sql = StreamUtils.copyToString(v6Resource.getInputStream(), StandardCharsets.UTF_8);
        jdbcTemplate.execute(v6Sql);
        entityManager.flush();
        entityManager.clear();

        // Execute scheduled maintenance task
        expirationService.runMaintenanceTasks();

        final User preservedUser = userRepository.findById(legacyDeactivatedWithBoxes.getId()).orElseThrow();
        assertNull(preservedUser.getConfirmation(), "Migrated legacy deactivated user must have null confirmation token");
        assertNull(preservedUser.getTs_confirm(), "Migrated legacy deactivated user must have null ts_confirm");
        assertTrue(mailboxRepository.findById(userBox.getId()).isPresent(),
                   "Mailboxes owned by legacy deactivated user must remain intact");
    }

    /**
     * Verifies that administrator-deactivated accounts (whose confirmation tokens are null),
     * V6-migrated active accounts (whose historical confirmation tokens were cleared to null),
     * and active accounts are preserved across maintenance runs.
     */
    @Test
    @DisplayName("Special Case: Should preserve admin-deactivated users without tokens and active users")
    public void testAdminDeactivatedAndActiveLegacyUsersPreserved()
    {
        final long now = System.currentTimeMillis();

        // Admin-deactivated user (tokens cleared on toggle or V6 migration, 0 mailboxes)
        final User adminDeactivatedUser = new User("Admin", "Deactivated", "admin_deactivated@xcmailr.test", "pass", "en");
        adminDeactivatedUser.setActive(false);
        adminDeactivatedUser.setConfirmation(null);
        adminDeactivatedUser.setTs_confirm(null);
        userRepository.save(adminDeactivatedUser);

        // Active user (protected by u.active = false predicate even if a token is present)
        final User activeUser = new User("Active", "Legacy", "active_legacy@xcmailr.test", "pass", "en");
        activeUser.setActive(true);
        activeUser.setConfirmation("legacy-active-token");
        activeUser.setTs_confirm(now - (100L * 86_400_000L));
        userRepository.save(activeUser);

        expirationService.runMaintenanceTasks();

        assertTrue(userRepository.findById(adminDeactivatedUser.getId()).isPresent(),
                    "Admin-deactivated user without confirmation token must remain in repository");
        assertTrue(userRepository.findById(activeUser.getId()).isPresent(),
                    "Active user must remain in repository");
    }

    /**
     * Verifies boundary and error conditions for unconfirmed user cleanup:
     * users within the confirmation grace period, users with non-positive or null {@code ts_confirm},
     * and runs when {@code confirmationPeriodHours <= 0} must not delete user accounts.
     */
    @Test
    @DisplayName("Boundary & Error Cases: Should preserve unconfirmed users within grace period, non-positive timestamps, or when disabled")
    public void testUnconfirmedUserBoundaryAndErrorCases()
    {
        final long now = System.currentTimeMillis();

        // 1. Fresh unconfirmed registration still within grace period (ts_confirm in the future)
        final User freshUnconfirmed = new User("Fresh", "Signup", "fresh_signup@xcmailr.test", "pass", "en");
        freshUnconfirmed.setActive(false);
        freshUnconfirmed.setConfirmation("fresh-token-111");
        freshUnconfirmed.setTs_confirm(now + 1_800_000L);
        userRepository.save(freshUnconfirmed);

        // 2. Inactive user with ts_confirm == 0 (legacy default for unset primitive long)
        final User zeroTsUser = new User("Zero", "Timestamp", "zero_ts@xcmailr.test", "pass", "en");
        zeroTsUser.setActive(false);
        zeroTsUser.setConfirmation("some-token-222");
        zeroTsUser.setTs_confirm(0L);
        userRepository.save(zeroTsUser);

        // 3. Inactive user with negative ts_confirm
        final User negativeTsUser = new User("Negative", "Timestamp", "neg_ts@xcmailr.test", "pass", "en");
        negativeTsUser.setActive(false);
        negativeTsUser.setConfirmation("some-token-333");
        negativeTsUser.setTs_confirm(-500L);
        userRepository.save(negativeTsUser);

        // 4. Inactive user with null ts_confirm
        final User nullTsUser = new User("Null", "Timestamp", "null_ts@xcmailr.test", "pass", "en");
        nullTsUser.setActive(false);
        nullTsUser.setConfirmation("some-token-444");
        nullTsUser.setTs_confirm(null);
        userRepository.save(nullTsUser);

        expirationService.purgeExpiredInactiveUsers(now);

        assertTrue(userRepository.findById(freshUnconfirmed.getId()).isPresent(),
                   "Fresh unconfirmed user within grace period must be preserved");
        assertTrue(userRepository.findById(zeroTsUser.getId()).isPresent(),
                   "User with ts_confirm == 0 must be preserved by ts_confirm > 0 guard");
        assertTrue(userRepository.findById(negativeTsUser.getId()).isPresent(),
                   "User with negative ts_confirm must be preserved by ts_confirm > 0 guard");
        assertTrue(userRepository.findById(nullTsUser.getId()).isPresent(),
                   "User with null ts_confirm must be preserved");

        // 5. Verify configuration guard when confirmationPeriodHours <= 0
        final User expiredButDisabled = new User("Disabled", "Purge", "disabled_purge@xcmailr.test", "pass", "en");
        expiredButDisabled.setActive(false);
        expiredButDisabled.setConfirmation("expired-token-555");
        expiredButDisabled.setTs_confirm(now - (48L * 3_600_000L));
        userRepository.save(expiredButDisabled);

        final int originalHours = properties.getApp().getConfirmationPeriodHours();
        try
        {
            properties.getApp().setConfirmationPeriodHours(0);
            expirationService.purgeExpiredInactiveUsers(now);
            assertTrue(userRepository.findById(expiredButDisabled.getId()).isPresent(),
                       "Expired unconfirmed user must not be purged when confirmationPeriodHours <= 0");
        }
        finally
        {
            properties.getApp().setConfirmationPeriodHours(originalHours);
        }
    }

    /**
     * Verifies the simplified repository query {@link UserRepository#findExpiredInactiveUsers(Long)}
     * directly: returns inactive users with non-null confirmation tokens and positive expired
     * timestamps ({@code 0 < ts_confirm < cutoff}) without performing a runtime {@code u.boxes IS EMPTY}
     * subquery check (since legacy tokens are cleaned up at startup by the {@code V6} Flyway migration),
     * while excluding boundary timestamps ({@code ts_confirm == cutoff}) and migrated/deactivated users
     * with {@code null} confirmation tokens.
     */
    @Test
    @DisplayName("Repository: findExpiredInactiveUsers evaluates token and timestamp without runtime mailbox filter")
    public void testFindExpiredInactiveUsersRepositoryQuery()
    {
        final long now = System.currentTimeMillis();
        final long cutoff = now - 3_600_000L;

        // Eligible 1: inactive, token != null, ts_confirm > 0 && < cutoff, no mailboxes
        final User eligibleNoBox = new User("Eligible", "NoBox", "eligible_repo@xcmailr.test", "pass", "en");
        eligibleNoBox.setActive(false);
        eligibleNoBox.setConfirmation("eligible-token-1");
        eligibleNoBox.setTs_confirm(cutoff - 1_000L);
        userRepository.save(eligibleNoBox);

        // Eligible 2: proves runtime query no longer filters on u.boxes IS EMPTY
        final User eligibleWithBox = new User("Eligible", "WithBox", "withbox_repo@xcmailr.test", "pass", "en");
        eligibleWithBox.setActive(false);
        eligibleWithBox.setConfirmation("eligible-token-2");
        eligibleWithBox.setTs_confirm(cutoff - 10_000L);
        userRepository.save(eligibleWithBox);
        mailboxRepository.save(new MBox("repobox", "xcmailr.test", 0L, false, eligibleWithBox));

        // Ineligible 1: exact boundary (ts_confirm == cutoff)
        final User onBoundary = new User("Boundary", "User", "boundary_repo@xcmailr.test", "pass", "en");
        onBoundary.setActive(false);
        onBoundary.setConfirmation("boundary-token");
        onBoundary.setTs_confirm(cutoff);
        userRepository.save(onBoundary);

        // Ineligible 2: V6-migrated / admin-deactivated user (confirmation == null, ts_confirm == null)
        final User migratedDeactivated = new User("Migrated", "Deactivated", "migrated_repo@xcmailr.test", "pass", "en");
        migratedDeactivated.setActive(false);
        migratedDeactivated.setConfirmation(null);
        migratedDeactivated.setTs_confirm(null);
        userRepository.save(migratedDeactivated);

        final List<User> found = userRepository.findExpiredInactiveUsers(cutoff);
        assertEquals(2, found.size(),
                     "Both inactive users with non-null expired confirmation tokens must be returned without runtime mailbox filtering");
        assertTrue(found.stream().anyMatch(u -> u.getId() == eligibleNoBox.getId()));
        assertTrue(found.stream().anyMatch(u -> u.getId() == eligibleWithBox.getId()));
    }

    @Test
    @DisplayName("Unit: Should calculate quarter-hour slot index (0..95) and handle boundary conditions")
    public void testQuarterHourCalculationAndBoundaries()
    {
        final Calendar cal = Calendar.getInstance();

        // 00:00:00.000 -> slot 0
        cal.set(2026, Calendar.NOVEMBER, 11, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final long t00_00 = cal.getTimeInMillis();
        assertEquals(0, expirationService.getQuarterHour(t00_00), "00:00:00.000 must map to slot 0");

        // 00:14:59.999 -> slot 0
        cal.set(2026, Calendar.NOVEMBER, 11, 0, 14, 59);
        cal.set(Calendar.MILLISECOND, 999);
        final long t00_14 = cal.getTimeInMillis();
        assertEquals(0, expirationService.getQuarterHour(t00_14), "00:14:59.999 must map to slot 0");

        // 00:15:00.000 -> slot 1
        cal.set(2026, Calendar.NOVEMBER, 11, 0, 15, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final long t00_15 = cal.getTimeInMillis();
        assertEquals(1, expirationService.getQuarterHour(t00_15), "00:15:00.000 must map to slot 1");

        // 10:14:00.000 -> slot 40 ((10 * 4) + (14 / 15) = 40)
        cal.set(2026, Calendar.NOVEMBER, 11, 10, 14, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final long t10_14 = cal.getTimeInMillis();
        assertEquals(40, expirationService.getQuarterHour(t10_14), "10:14:00.000 must map to slot 40");

        // 10:16:00.000 -> slot 41 ((10 * 4) + (16 / 15) = 41)
        cal.set(2026, Calendar.NOVEMBER, 11, 10, 16, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final long t10_16 = cal.getTimeInMillis();
        assertEquals(41, expirationService.getQuarterHour(t10_16), "10:16:00.000 must map to slot 41");

        // 23:59:59.999 -> slot 95 ((23 * 4) + (59 / 15) = 92 + 3 = 95)
        cal.set(2026, Calendar.NOVEMBER, 11, 23, 59, 59);
        cal.set(Calendar.MILLISECOND, 999);
        final long t23_59 = cal.getTimeInMillis();
        assertEquals(95, expirationService.getQuarterHour(t23_59), "23:59:59.999 must map to slot 95");

        // Special case: Negative timestamp safely clamped to 0
        assertEquals(0, expirationService.getQuarterHour(-1000L), "Negative timestamp should return slot 0");
    }

    @Test
    @DisplayName("Happy Path: Multiple incoming transactions within the same 15-minute slot increment a single row")
    public void testHappyPathSameSlotAggregation()
    {
        final Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.NOVEMBER, 11, 10, 5, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final long now = cal.getTimeInMillis();

        // Status 300: Forwarded mail
        final MailTransaction txForward = new MailTransaction(300, "alice@sender.org", "box@xcmailr.test", "real@user.test");
        txForward.setTs(now);
        transactionQueueService.enqueue(txForward);

        // Status 250: Stored mail 1
        final MailTransaction txStore1 = new MailTransaction(250, "bob@sender.org", "box@xcmailr.test", null);
        txStore1.setTs(now + 60_000L); // 1 minute later, still slot 40
        transactionQueueService.enqueue(txStore1);

        // Status 250: Stored mail 2
        final MailTransaction txStore2 = new MailTransaction(250, "carol@sender.org", "box@xcmailr.test", null);
        txStore2.setTs(now + 120_000L); // 2 minutes later, still slot 40
        transactionQueueService.enqueue(txStore2);

        // Status 100: Dropped mail
        final MailTransaction txDrop = new MailTransaction(100, "spammer@sender.org", "box@xcmailr.test", null);
        txDrop.setTs(now + 180_000L); // 3 minutes later, still slot 40
        transactionQueueService.enqueue(txDrop);

        // Run batch aggregation
        expirationService.aggregateAndCleanTransactions(now + 300_000L);

        // Assert queue is drained
        assertTrue(transactionQueueService.isEmpty(), "Transaction queue must be drained after aggregation");

        // Assert all 4 transactions persisted to mailtransactions
        final List<MailTransaction> persistedTx = mailTransactionRepository.findAll();
        assertEquals(4, persistedTx.size(), "All 4 transactions must be batch persisted to mailtransactions table");

        // Assert exactly 1 aggregated row in MAIL_STATISTICS
        final List<MailStatistics> allStats = mailStatisticsRepository.findAll();
        assertEquals(1, allStats.size(), "All transactions in the same slot and domain pair must aggregate into 1 row");

        final MailStatistics stat = allStats.get(0);
        assertEquals(40, stat.getKey().getQuarterHour(), "Quarter-hour slot must be 40");
        assertEquals("sender.org", stat.getKey().getFromDomain(), "From domain must match sender.org");
        assertEquals("xcmailr.test", stat.getKey().getTargetDomain(), "Target domain must match xcmailr.test");
        assertEquals(1, stat.getForwardCount(), "Forward count must be 1");
        assertEquals(2, stat.getStoreCount(), "Store count must be 2");
        assertEquals(1, stat.getDropCount(), "Drop count must be 1");
    }

    @Test
    @DisplayName("Happy Path: Transactions in consecutive 15-minute intervals create separate sequential rows")
    public void testHappyPathConsecutiveSlotRowInsertion()
    {
        final Calendar cal = Calendar.getInstance();

        // 10:14 -> Slot 40
        cal.set(2026, Calendar.NOVEMBER, 11, 10, 14, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final long t10_14 = cal.getTimeInMillis();

        // 10:16 -> Slot 41
        cal.set(2026, Calendar.NOVEMBER, 11, 10, 16, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final long t10_16 = cal.getTimeInMillis();

        final MailTransaction txSlot40 = new MailTransaction(300, "user1@sender.org", "box@xcmailr.test", "dest@user.test");
        txSlot40.setTs(t10_14);
        transactionQueueService.enqueue(txSlot40);

        final MailTransaction txSlot41 = new MailTransaction(250, "user2@sender.org", "box@xcmailr.test", null);
        txSlot41.setTs(t10_16);
        transactionQueueService.enqueue(txSlot41);

        expirationService.aggregateAndCleanTransactions(t10_16 + 1000L);

        final List<MailStatistics> stats = mailStatisticsRepository.findAll();
        assertEquals(2, stats.size(), "Consecutive slots must generate two distinct rows");

        final MailStatistics stat40 = stats.stream().filter(s -> s.getKey().getQuarterHour() == 40).findFirst().orElseThrow();
        assertEquals(1, stat40.getForwardCount(), "Slot 40 forward count must be 1");
        assertEquals(0, stat40.getStoreCount(), "Slot 40 store count must be 0");

        final MailStatistics stat41 = stats.stream().filter(s -> s.getKey().getQuarterHour() == 41).findFirst().orElseThrow();
        assertEquals(0, stat41.getForwardCount(), "Slot 41 forward count must be 0");
        assertEquals(1, stat41.getStoreCount(), "Slot 41 store count must be 1");
    }

    @Test
    @DisplayName("Special Case: Multi-domain partitioning creates separate rows for different domains in same slot")
    public void testSpecialCaseMultiDomainPartitioningInSameSlot()
    {
        final Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.NOVEMBER, 11, 14, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final long ts = cal.getTimeInMillis();

        // Domain A -> Target 1 (forward)
        final MailTransaction tx1 = new MailTransaction(300, "user@domain-a.org", "box@target-one.test", "fwd@target.test");
        tx1.setTs(ts);
        transactionQueueService.enqueue(tx1);

        // Domain B -> Target 1 (store)
        final MailTransaction tx2 = new MailTransaction(250, "user@domain-b.org", "box@target-one.test", null);
        tx2.setTs(ts);
        transactionQueueService.enqueue(tx2);

        // Domain A -> Target 2 (drop)
        final MailTransaction tx3 = new MailTransaction(100, "user@domain-a.org", "box@target-two.test", null);
        tx3.setTs(ts);
        transactionQueueService.enqueue(tx3);

        expirationService.aggregateAndCleanTransactions(ts + 1000L);

        final List<MailStatistics> stats = mailStatisticsRepository.findAll();
        assertEquals(3, stats.size(), "Three distinct domain pairs must create 3 distinct statistics rows");

        final MailStatistics statA1 = stats.stream()
            .filter(s -> "domain-a.org".equals(s.getKey().getFromDomain()) && "target-one.test".equals(s.getKey().getTargetDomain()))
            .findFirst().orElseThrow();
        assertEquals(1, statA1.getForwardCount());
        assertEquals(0, statA1.getStoreCount());

        final MailStatistics statB1 = stats.stream()
            .filter(s -> "domain-b.org".equals(s.getKey().getFromDomain()) && "target-one.test".equals(s.getKey().getTargetDomain()))
            .findFirst().orElseThrow();
        assertEquals(0, statB1.getForwardCount());
        assertEquals(1, statB1.getStoreCount());

        final MailStatistics statA2 = stats.stream()
            .filter(s -> "domain-a.org".equals(s.getKey().getFromDomain()) && "target-two.test".equals(s.getKey().getTargetDomain()))
            .findFirst().orElseThrow();
        assertEquals(1, statA2.getDropCount());
    }

    @Test
    @DisplayName("Special Case: Second scheduled batch run updates existing row without primary key conflicts")
    public void testSpecialCaseMultiBatchConsecutiveRunsUpdateExistingRow()
    {
        final Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.NOVEMBER, 11, 15, 2, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final long batch1Time = cal.getTimeInMillis();

        // First batch: Tx forward
        final MailTransaction tx1 = new MailTransaction(300, "sender@example.com", "box@xcmailr.test", "fwd@xcmailr.test");
        tx1.setTs(batch1Time);
        transactionQueueService.enqueue(tx1);

        expirationService.aggregateAndCleanTransactions(batch1Time + 10_000L);

        assertEquals(1, mailStatisticsRepository.findAll().size(), "First batch should insert 1 row");
        final MailStatistics afterBatch1 = mailStatisticsRepository.findAll().get(0);
        assertEquals(1, afterBatch1.getForwardCount());
        assertEquals(0, afterBatch1.getStoreCount());

        // Second batch: Tx store in same 15-minute slot (15:08)
        cal.set(Calendar.MINUTE, 8);
        final long batch2Time = cal.getTimeInMillis();
        final MailTransaction tx2 = new MailTransaction(250, "sender@example.com", "box@xcmailr.test", null);
        tx2.setTs(batch2Time);
        transactionQueueService.enqueue(tx2);

        expirationService.aggregateAndCleanTransactions(batch2Time + 10_000L);

        assertEquals(1, mailStatisticsRepository.findAll().size(), "Second batch in same slot must UPDATE existing row, not duplicate");
        final MailStatistics afterBatch2 = mailStatisticsRepository.findAll().get(0);
        assertEquals(1, afterBatch2.getForwardCount(), "Forward count must remain 1");
        assertEquals(1, afterBatch2.getStoreCount(), "Store count must be incremented to 1");
    }

    @Test
    @DisplayName("Special Case: Idempotency with empty queue and graceful shutdown flush via @PreDestroy")
    public void testSpecialCaseIdempotencyAndGracefulShutdownFlush()
    {
        final Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.NOVEMBER, 11, 16, 10, 0);
        final long ts = cal.getTimeInMillis();

        // Step 1: Enqueue and process initial transaction
        final MailTransaction txInitial = new MailTransaction(250, "client@company.org", "inbox@xcmailr.test", null);
        txInitial.setTs(ts);
        transactionQueueService.enqueue(txInitial);
        expirationService.aggregateAndCleanTransactions(ts + 1000L);

        assertEquals(1, mailStatisticsRepository.findAll().size());
        assertEquals(1, mailStatisticsRepository.findAll().get(0).getStoreCount());

        // Step 2: Idempotent run with empty queue
        expirationService.aggregateAndCleanTransactions(ts + 60_000L);
        assertEquals(1, mailStatisticsRepository.findAll().size(), "Empty queue run must not insert or alter rows");
        assertEquals(1, mailStatisticsRepository.findAll().get(0).getStoreCount(), "Store count must remain strictly 1");

        // Step 3: Shutdown hook flushes buffered transaction
        final MailTransaction txShutdown = new MailTransaction(250, "client@company.org", "inbox@xcmailr.test", null);
        txShutdown.setTs(ts + 120_000L);
        transactionQueueService.enqueue(txShutdown);
        assertFalse(transactionQueueService.isEmpty(), "Queue should hold pending transaction before shutdown");

        expirationService.onShutdown();

        assertTrue(transactionQueueService.isEmpty(), "Queue must be empty after shutdown hook execution");
        assertEquals(1, mailStatisticsRepository.findAll().size(), "Row count must remain 1 for same slot");
        assertEquals(2, mailStatisticsRepository.findAll().get(0).getStoreCount(), "Shutdown flush must increment store count to 2");
    }

    @Test
    @DisplayName("Error Case: Transactions with malformed or missing domains are saved to audit log but bypassed for statistics")
    public void testErrorCaseMalformedDomainHandlingSafelyBypassed()
    {
        final long now = System.currentTimeMillis();

        // Missing source address
        final MailTransaction txNullSource = new MailTransaction(250, null, "box@xcmailr.test", null);
        txNullSource.setTs(now);
        transactionQueueService.enqueue(txNullSource);

        // Missing relay address
        final MailTransaction txNullRelay = new MailTransaction(250, "sender@external.org", null, null);
        txNullRelay.setTs(now);
        transactionQueueService.enqueue(txNullRelay);

        // Malformed address without '@'
        final MailTransaction txNoAtSign = new MailTransaction(100, "malformed-address", "box@xcmailr.test", null);
        txNoAtSign.setTs(now);
        transactionQueueService.enqueue(txNoAtSign);

        // Blank address
        final MailTransaction txBlank = new MailTransaction(200, "   ", "box@xcmailr.test", null);
        txBlank.setTs(now);
        transactionQueueService.enqueue(txBlank);

        // Execute batch aggregation - must not throw NullPointerException
        expirationService.aggregateAndCleanTransactions(now + 1000L);

        // Verify all 4 are saved to audit log table
        final List<MailTransaction> transactions = mailTransactionRepository.findAll();
        assertEquals(4, transactions.size(), "All transactions regardless of validity must be saved to audit log");

        // Verify statistics repository has zero rows (safely bypassed)
        final List<MailStatistics> stats = mailStatisticsRepository.findAll();
        assertTrue(stats.isEmpty(), "Malformed domain transactions must be bypassed without creating statistics entries");
    }
}
