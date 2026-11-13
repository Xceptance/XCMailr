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
package com.xceptance.xcmailr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import jakarta.persistence.EntityManager;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import com.xceptance.xcmailr.models.MBox;
import com.xceptance.xcmailr.models.User;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

/**
 * Verifies that Flyway auto-migration executes cleanly against existing
 * {@code V1} through {@code V6} database migration scripts, validates that all
 * migrations are in a {@link MigrationState#SUCCESS} state, and tests the
 * {@code V6__Clean_Legacy_Confirmation_Tokens.sql} data migration across happy paths,
 * special cases, and boundary/idempotency cases.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FlywayMigrationTest
{
    private static final String V6_MIGRATION_RESOURCE = "db/migration/V6__Clean_Legacy_Confirmation_Tokens.sql";

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * Verifies that all Flyway migrations from {@code V1} through {@code V6} are discovered,
     * applied in {@link MigrationState#SUCCESS} state, and pass checksum validation.
     */
    @Test
    @DisplayName("Verify Flyway migrations V1 through V6 are applied and valid")
    public void testFlywayMigrationsAppliedSuccessfully()
    {
        assertNotNull(flyway, "Flyway bean must be configured and available in Spring context");

        final MigrationInfo[] appliedMigrations = flyway.info().applied();
        assertNotNull(appliedMigrations, "Applied migrations must not be null");
        assertTrue(appliedMigrations.length >= 6,
                   "At least 6 migrations (V1, V2, V3, V4, V5, V6) must be applied");

        for (final MigrationInfo info : appliedMigrations)
        {
            assertEquals(MigrationState.SUCCESS, info.getState(),
                         "Migration " + info.getVersion() + " (" + info.getDescription() + ") must be in SUCCESS state");
        }

        final boolean hasV6 = Arrays.stream(appliedMigrations)
            .anyMatch(info -> info.getVersion() != null && "6".equals(info.getVersion().getVersion()));
        assertTrue(hasV6, "Migration V6 (Clean Legacy Confirmation Tokens) must be applied");

        // Validate that current schema matches all migration checksums
        flyway.validate();
    }

    /**
     * Verifies the happy path for {@code V6__Clean_Legacy_Confirmation_Tokens.sql}:
     * active legacy users ({@code active = true}) who still carry historical confirmation
     * tokens and timestamps have both {@code confirmation} and {@code ts_confirm} cleared to {@code null}.
     *
     * @throws Exception if executing the migration script fails
     */
    @Test
    @DisplayName("Happy Path: V6 migration clears confirmation tokens and timestamps for active legacy users")
    public void testV6MigrationClearsActiveLegacyUserTokens() throws Exception
    {
        // Seed an active legacy user who still has a historical confirmation token from 2015
        final User activeLegacyUser = new User("Legacy", "Active", "v6_active_legacy@xcmailr.test", "pass", "en");
        activeLegacyUser.setActive(true);
        activeLegacyUser.setConfirmation("legacy-2015-token");
        activeLegacyUser.setTs_confirm(1_435_701_600_000L);
        userRepository.saveAndFlush(activeLegacyUser);

        executeV6MigrationScript();

        final User reloaded = userRepository.findById(activeLegacyUser.getId()).orElseThrow();
        assertTrue(reloaded.isActive(), "Active legacy user must remain active");
        assertNull(reloaded.getConfirmation(), "V6 migration must clear confirmation token on active user");
        assertNull(reloaded.getTs_confirm(), "V6 migration must clear ts_confirm timestamp on active user");
    }

    /**
     * Verifies the special cases for {@code V6__Clean_Legacy_Confirmation_Tokens.sql}:
     * an already-deactivated legacy user ({@code active = false}) who owns one or more mailboxes
     * has their historical {@code confirmation} token and {@code ts_confirm} timestamp cleared to {@code null},
     * whereas a genuinely unconfirmed inactive user ({@code active = false}) with zero mailboxes retains
     * their confirmation token and timestamp.
     *
     * @throws Exception if executing the migration script fails
     */
    @Test
    @DisplayName("Special Case: V6 migration clears tokens on deactivated mailbox owners while preserving unconfirmed signups")
    public void testV6MigrationClearsDeactivatedMailboxOwnerAndPreservesUnconfirmedSignup() throws Exception
    {
        // 1. Deactivated legacy user who owns a mailbox (e.g., m.pfotenhauer+hb@xceptance.com)
        final User deactivatedWithMailbox = new User("Legacy", "Deactivated", "v6_deactivated_boxes@xcmailr.test", "pass", "en");
        deactivatedWithMailbox.setActive(false);
        deactivatedWithMailbox.setConfirmation("legacy-2019-token");
        deactivatedWithMailbox.setTs_confirm(1_559_668_265_392L);
        userRepository.saveAndFlush(deactivatedWithMailbox);

        final MBox ownedBox = new MBox("v6legacybox", "xcmailr.test", 0L, false, deactivatedWithMailbox);
        mailboxRepository.saveAndFlush(ownedBox);

        // 2. Genuinely unconfirmed registration (active = false, zero mailboxes)
        final User unconfirmedSignup = new User("Pending", "Signup", "v6_pending_signup@xcmailr.test", "pass", "en");
        unconfirmedSignup.setActive(false);
        unconfirmedSignup.setConfirmation("pending-signup-token");
        unconfirmedSignup.setTs_confirm(1_700_000_000_000L);
        userRepository.saveAndFlush(unconfirmedSignup);

        executeV6MigrationScript();

        final User reloadedDeactivated = userRepository.findById(deactivatedWithMailbox.getId()).orElseThrow();
        assertFalse(reloadedDeactivated.isActive(), "Deactivated legacy user must remain inactive");
        assertNull(reloadedDeactivated.getConfirmation(),
                   "V6 migration must clear confirmation token on deactivated user who owns mailboxes");
        assertNull(reloadedDeactivated.getTs_confirm(),
                   "V6 migration must clear ts_confirm timestamp on deactivated user who owns mailboxes");

        final User reloadedUnconfirmed = userRepository.findById(unconfirmedSignup.getId()).orElseThrow();
        assertFalse(reloadedUnconfirmed.isActive(), "Unconfirmed signup must remain inactive");
        assertEquals("pending-signup-token", reloadedUnconfirmed.getConfirmation(),
                     "V6 migration must preserve confirmation token on unconfirmed user with zero mailboxes");
        assertEquals(1_700_000_000_000L, reloadedUnconfirmed.getTs_confirm(),
                     "V6 migration must preserve ts_confirm timestamp on unconfirmed user with zero mailboxes");
    }

    /**
     * Verifies boundary and idempotency cases for {@code V6__Clean_Legacy_Confirmation_Tokens.sql}:
     * users whose {@code confirmation} and {@code ts_confirm} are already {@code null} remain
     * unchanged, and executing the migration multiple times is idempotent.
     *
     * @throws Exception if executing the migration script fails
     */
    @Test
    @DisplayName("Boundary & Idempotency Case: V6 migration safely handles already-null tokens and repeated execution")
    public void testV6MigrationBoundaryAndIdempotency() throws Exception
    {
        // Active user already having null confirmation fields
        final User cleanActiveUser = new User("Clean", "Active", "v6_clean_active@xcmailr.test", "pass", "en");
        cleanActiveUser.setActive(true);
        cleanActiveUser.setConfirmation(null);
        cleanActiveUser.setTs_confirm(null);
        userRepository.saveAndFlush(cleanActiveUser);

        // Inactive admin-deactivated user already having null confirmation fields and zero mailboxes
        final User cleanInactiveUser = new User("Clean", "Inactive", "v6_clean_inactive@xcmailr.test", "pass", "en");
        cleanInactiveUser.setActive(false);
        cleanInactiveUser.setConfirmation(null);
        cleanInactiveUser.setTs_confirm(null);
        userRepository.saveAndFlush(cleanInactiveUser);

        // Execute V6 migration twice to verify idempotency
        executeV6MigrationScript();
        executeV6MigrationScript();

        final User reloadedActive = userRepository.findById(cleanActiveUser.getId()).orElseThrow();
        assertTrue(reloadedActive.isActive());
        assertNull(reloadedActive.getConfirmation());
        assertNull(reloadedActive.getTs_confirm());

        final User reloadedInactive = userRepository.findById(cleanInactiveUser.getId()).orElseThrow();
        assertFalse(reloadedInactive.isActive());
        assertNull(reloadedInactive.getConfirmation());
        assertNull(reloadedInactive.getTs_confirm());
    }

    /**
     * Executes {@code V6__Clean_Legacy_Confirmation_Tokens.sql} against the current test transaction
     * and clears the JPA persistence context so subsequent repository queries read fresh database state.
     *
     * @throws Exception if the SQL resource cannot be read or executed
     */
    private void executeV6MigrationScript() throws Exception
    {
        final ClassPathResource resource = new ClassPathResource(V6_MIGRATION_RESOURCE);
        assertTrue(resource.exists(), "Migration script " + V6_MIGRATION_RESOURCE + " must exist on classpath");
        final String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        jdbcTemplate.execute(sql);
        entityManager.flush();
        entityManager.clear();
    }
}
