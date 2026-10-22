/*
 * Copyright (c) Xceptance Software Technologies GmbH
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import models.MBox;
import models.User;

/**
 * Unit and integration tests for {@link MailboxRepository} batch methods and tenant boundaries.
 * Follows the testing pyramid covering happy paths, tenant security boundaries, and edge cases.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MailboxRepositoryTest
{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

    private User primaryUser;
    private User foreignUser;

    @BeforeEach
    public void setUp()
    {
        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();

        primaryUser = new User("Alice", "Tester", "alice@example.com", "secret", "en");
        userRepository.save(primaryUser);

        foreignUser = new User("Bob", "Other", "bob@example.com", "secret", "en");
        userRepository.save(foreignUser);
    }

    @Test
    @DisplayName("findByUsrAndIdIn returns only mailboxes belonging to the requesting user")
    public void testFindByUsrAndIdInTenantIsolation()
    {
        final MBox box1 = new MBox("alice1", "xcmailr.test", 0L, false, primaryUser);
        final MBox box2 = new MBox("alice2", "xcmailr.test", 0L, false, primaryUser);
        final MBox foreignBox = new MBox("bob1", "xcmailr.test", 0L, false, foreignUser);

        mailboxRepository.save(box1);
        mailboxRepository.save(box2);
        mailboxRepository.save(foreignBox);

        final List<Long> requestedIds = List.of(box1.getId(), box2.getId(), foreignBox.getId());
        final List<MBox> aliceBoxes = mailboxRepository.findByUsrAndIdIn(primaryUser, requestedIds);

        assertEquals(2, aliceBoxes.size(), "Should only return mailboxes owned by primaryUser");
        assertTrue(aliceBoxes.stream().anyMatch(b -> b.getId() == box1.getId()));
        assertTrue(aliceBoxes.stream().anyMatch(b -> b.getId() == box2.getId()));
        assertFalse(aliceBoxes.stream().anyMatch(b -> b.getId() == foreignBox.getId()));
    }

    @Test
    @DisplayName("findByUsrAndIdIn returns empty list when given empty ID collection")
    public void testFindByUsrAndIdInEmptyList()
    {
        final MBox box = new MBox("alice1", "xcmailr.test", 0L, false, primaryUser);
        mailboxRepository.save(box);

        final List<MBox> result = mailboxRepository.findByUsrAndIdIn(primaryUser, Collections.emptyList());
        assertTrue(result.isEmpty(), "Empty ID list query must return empty list");
    }

    @Test
    @DisplayName("resetCountersByUsrAndIds resets counters for specified user mailboxes only")
    public void testResetCountersByUsrAndIds()
    {
        final MBox box1 = new MBox("alice1", "xcmailr.test", 0L, false, primaryUser);
        box1.setForwards(10);
        box1.setSuppressions(5);
        mailboxRepository.save(box1);

        final MBox box2 = new MBox("alice2", "xcmailr.test", 0L, false, primaryUser);
        box2.setForwards(3);
        box2.setSuppressions(2);
        mailboxRepository.save(box2);

        final MBox foreignBox = new MBox("bob1", "xcmailr.test", 0L, false, foreignUser);
        foreignBox.setForwards(20);
        foreignBox.setSuppressions(8);
        mailboxRepository.save(foreignBox);

        final List<Long> idsToReset = List.of(box1.getId(), foreignBox.getId());
        final int updatedCount = mailboxRepository.resetCountersByUsrAndIds(primaryUser, idsToReset);

        assertEquals(1, updatedCount, "Only the primaryUser's mailbox should have been updated");

        final MBox reloadedBox1 = mailboxRepository.findById(box1.getId()).orElseThrow();
        assertEquals(0, reloadedBox1.getForwards());
        assertEquals(0, reloadedBox1.getSuppressions());

        final MBox reloadedBox2 = mailboxRepository.findById(box2.getId()).orElseThrow();
        assertEquals(3, reloadedBox2.getForwards(), "Untargeted box counters must remain unchanged");
        assertEquals(2, reloadedBox2.getSuppressions());

        final MBox reloadedForeignBox = mailboxRepository.findById(foreignBox.getId()).orElseThrow();
        assertEquals(20, reloadedForeignBox.getForwards(), "Foreign user box counters must remain unchanged");
        assertEquals(8, reloadedForeignBox.getSuppressions());
    }

    @Test
    @DisplayName("findByUsrAndExpired accurately partitions active vs expired mailboxes")
    public void testFindByUsrAndExpired()
    {
        final MBox activeBox1 = new MBox("active1", "xcmailr.test", 0L, false, primaryUser);
        activeBox1.setExpired(false);
        mailboxRepository.save(activeBox1);

        final MBox activeBox2 = new MBox("active2", "xcmailr.test", 0L, false, primaryUser);
        activeBox2.setExpired(false);
        mailboxRepository.save(activeBox2);

        final MBox expiredBox = new MBox("expired1", "xcmailr.test", 0L, false, primaryUser);
        expiredBox.setExpired(true);
        mailboxRepository.save(expiredBox);

        final List<MBox> activeList = mailboxRepository.findByUsrAndExpired(primaryUser, false);
        assertEquals(2, activeList.size());
        assertTrue(activeList.stream().noneMatch(MBox::isExpired));

        final List<MBox> expiredList = mailboxRepository.findByUsrAndExpired(primaryUser, true);
        assertEquals(1, expiredList.size());
        assertTrue(expiredList.get(0).isExpired());
    }
}
