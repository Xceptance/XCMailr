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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.xceptance.xcmailr.models.User;

/**
 * Integration tests for {@link UserRepository} metric counting and query methods.
 * Verifies role partitioning, zero-boundary states, and total sum consistency.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserRepositoryTest
{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

    @BeforeEach
    public void setUp()
    {
        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("countByAdmin returns zero when user repository is empty")
    public void testCountByAdminEmpty()
    {
        assertEquals(0L, userRepository.countByAdmin(true), "Admin count must be 0 for empty database");
        assertEquals(0L, userRepository.countByAdmin(false), "Standard user count must be 0 for empty database");
        assertEquals(0L, userRepository.count(), "Total count must be 0");
    }

    @Test
    @DisplayName("countByAdmin correctly partitions administrator and standard user accounts")
    public void testCountByAdminPartitioning()
    {
        final User admin1 = new User("Admin", "One", "admin1@example.com", "secret", "en");
        admin1.setAdmin(true);

        final User admin2 = new User("Admin", "Two", "admin2@example.com", "secret", "de");
        admin2.setAdmin(true);

        final User standard1 = new User("User", "One", "user1@example.com", "secret", "en");
        standard1.setAdmin(false);

        final User standard2 = new User("User", "Two", "user2@example.com", "secret", "de");
        standard2.setAdmin(false);

        final User standard3 = new User("User", "Three", "user3@example.com", "secret", "en");
        standard3.setAdmin(false);

        userRepository.saveAll(List.of(admin1, admin2, standard1, standard2, standard3));

        final long adminCount = userRepository.countByAdmin(true);
        final long standardCount = userRepository.countByAdmin(false);
        final long totalCount = userRepository.count();

        assertEquals(2L, adminCount, "Admin count must accurately reflect 2 admin accounts");
        assertEquals(3L, standardCount, "Standard user count must accurately reflect 3 standard accounts");
        assertEquals(5L, totalCount, "Total users count must equal sum of admin and standard users");
        assertEquals(totalCount, adminCount + standardCount, "Admin and standard counts must sum to total");
    }
}
