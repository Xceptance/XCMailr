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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that Flyway auto-migration executes cleanly against existing
 * {@code V1}, {@code V2}, and {@code V3} database migration scripts and validates
 * that all migrations are in a {@link MigrationState#SUCCESS} state.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
public class FlywayMigrationTest
{
    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("Verify Flyway migrations V1, V2, V3 are applied and valid")
    public void testFlywayMigrationsAppliedSuccessfully()
    {
        assertNotNull(flyway, "Flyway bean must be configured and available in Spring context");

        final MigrationInfo[] appliedMigrations = flyway.info().applied();
        assertNotNull(appliedMigrations, "Applied migrations must not be null");
        assertTrue(appliedMigrations.length >= 3, "At least 3 migrations (V1, V2, V3) must be applied");

        for (final MigrationInfo info : appliedMigrations)
        {
            assertEquals(MigrationState.SUCCESS, info.getState(),
                         "Migration " + info.getVersion() + " (" + info.getDescription() + ") must be in SUCCESS state");
        }

        // Validate that current schema matches all migration checksums
        flyway.validate();
    }
}
