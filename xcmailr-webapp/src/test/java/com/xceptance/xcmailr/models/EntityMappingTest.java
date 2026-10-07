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
package com.xceptance.xcmailr.models;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import models.Domain;
import models.MBox;
import models.Mail;
import models.MailStatistics;
import models.MailStatisticsKey;
import models.MailTransaction;
import models.User;

/**
 * Verifies standard Spring Data JPA mapping integrity for all entity classes:
 * {@link User}, {@link MBox}, {@link Mail}, {@link MailTransaction},
 * {@link MailStatistics}, and {@link Domain}.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class EntityMappingTest
{
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Verify Domain entity mapping integrity")
    public void testDomainMapping()
    {
        final Domain domain = new Domain("testdomain.org");
        entityManager.persist(domain);
        entityManager.flush();
        entityManager.clear();

        final Domain found = entityManager.find(Domain.class, domain.getId());
        assertNotNull(found, "Persisted domain must be found by ID");
        assertEquals("testdomain.org", found.getDomainname(), "Domain name must match");
    }

    @Test
    @DisplayName("Verify User and MBox relationship mapping integrity")
    public void testUserAndMBoxMapping()
    {
        final User user = new User("Jane", "Doe", "jane.doe@xcmailr.test", "secret123", "en");
        user.setActive(true);
        entityManager.persist(user);

        final MBox box = new MBox("testbox", "xcmailr.test", System.currentTimeMillis() + 3600_000L, false, user);
        entityManager.persist(box);
        entityManager.flush();
        entityManager.clear();

        final User foundUser = entityManager.find(User.class, user.getId());
        assertNotNull(foundUser, "User must be found");
        assertEquals("Jane", foundUser.getForename());
        assertEquals("jane.doe@xcmailr.test", foundUser.getMail());

        final MBox foundBox = entityManager.find(MBox.class, box.getId());
        assertNotNull(foundBox, "MBox must be found");
        assertEquals("testbox", foundBox.getAddress());
        assertEquals(foundUser.getId(), foundBox.getUsr().getId());
    }

    @Test
    @DisplayName("Verify Mail entity mapping integrity with BLOB payload")
    public void testMailMapping()
    {
        final byte[] payload = "Subject: Test\r\n\r\nBody text".getBytes(StandardCharsets.UTF_8);
        final Mail mail = new Mail();
        mail.setSender("sender@example.com");
        mail.setSubject("Test Subject");
        mail.setReceiveTime(System.currentTimeMillis());
        mail.setMessage(payload);
        mail.setMailboxId(999L);
        mail.setUuid("uuid-12345");

        entityManager.persist(mail);
        entityManager.flush();
        entityManager.clear();

        final Mail found = entityManager.find(Mail.class, mail.getId());
        assertNotNull(found, "Mail must be found");
        assertEquals("sender@example.com", found.getSender());
        assertArrayEquals(payload, found.getMessage(), "BLOB message payload must be preserved");
        assertEquals(999L, found.getMailboxId());
        assertEquals("uuid-12345", found.getUuid());
    }

    @Test
    @DisplayName("Verify MailTransaction entity mapping integrity")
    public void testMailTransactionMapping()
    {
        final MailTransaction tx = new MailTransaction(300, "from@example.com", "relay@xcmailr.test", "to@example.com");
        entityManager.persist(tx);
        entityManager.flush();
        entityManager.clear();

        final MailTransaction found = entityManager.find(MailTransaction.class, tx.getId());
        assertNotNull(found, "MailTransaction must be found");
        assertEquals(300, found.getStatus());
        assertEquals("from@example.com", found.getSourceaddr());
        assertEquals("relay@xcmailr.test", found.getRelayaddr());
        assertEquals("to@example.com", found.getTargetaddr());
    }

    @Test
    @DisplayName("Verify MailStatistics composite key mapping integrity")
    public void testMailStatisticsMapping()
    {
        final Date sqlDate = Date.valueOf(LocalDate.now());
        final MailStatisticsKey key = new MailStatisticsKey(sqlDate, 12, "example.com", "xcmailr.test");
        final MailStatistics stats = new MailStatistics();
        stats.setKey(key);
        stats.setDropCount(5);
        stats.setForwardCount(10);

        entityManager.persist(stats);
        entityManager.flush();
        entityManager.clear();

        final MailStatistics found = entityManager.find(MailStatistics.class, key);
        assertNotNull(found, "MailStatistics must be found by composite key");
        assertEquals(5, found.getDropCount());
        assertEquals(10, found.getForwardCount());
        assertEquals(12, found.getKey().getQuarterHour());
    }
}
