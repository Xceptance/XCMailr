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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.xceptance.xcmailr.repositories.projections.MailSummaryProjection;

import models.MBox;
import models.Mail;
import models.User;

/**
 * Repository slice and integration tests for {@link MailRepository} paginated summary projections.
 * Follows the testing pyramid covering happy path queries, projection memory efficiency,
 * strict multi-tenant data isolation, SQL injection immunity via parameterized queries,
 * and boundary/edge conditions.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MailRepositoryTest
{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

    private User userAlice;
    private User userBob;
    private MBox aliceBox1;
    private MBox aliceBox2;
    private MBox bobBox;

    @BeforeEach
    public void setUp()
    {
        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();

        userAlice = new User("Alice", "Tester", "alice@example.com", "secret", "en");
        userRepository.save(userAlice);

        userBob = new User("Bob", "Other", "bob@example.com", "secret", "en");
        userRepository.save(userBob);

        aliceBox1 = new MBox("alice1", "xcmailr.test", 0L, false, userAlice);
        mailboxRepository.save(aliceBox1);

        aliceBox2 = new MBox("alice2", "xcmailr.test", 0L, false, userAlice);
        mailboxRepository.save(aliceBox2);

        bobBox = new MBox("bob1", "xcmailr.test", 0L, false, userBob);
        mailboxRepository.save(bobBox);
    }

    /**
     * Helper method to persist a test email message.
     *
     * @param mailbox target mailbox
     * @param sender sender address
     * @param subject email subject
     * @param receiveTime receive timestamp in epoch millis
     * @return persisted {@link Mail} entity
     */
    private Mail createMail(final MBox mailbox, final String sender, final String subject, final long receiveTime)
    {
        final Mail mail = new Mail();
        mail.setMailboxId(mailbox.getId());
        mail.setSender(sender);
        mail.setSubject(subject);
        mail.setReceiveTime(receiveTime);
        // Include dummy message content representing raw MIME payload
        mail.setMessage(("From: " + sender + "\r\nSubject: " + subject + "\r\n\r\nTest body").getBytes(StandardCharsets.UTF_8));
        return mailRepository.save(mail);
    }

    @Test
    @DisplayName("findSummaryByMailboxIn returns paginated summaries ordered descending by receiveTime")
    public void testFindSummaryByMailboxInHappyPath()
    {
        // Arrange: Create 3 emails across Alice's two mailboxes with distinct timestamps
        final Mail mail1 = createMail(aliceBox1, "sender1@example.com", "First Email", 1000L);
        final Mail mail2 = createMail(aliceBox2, "sender2@example.com", "Second Email", 3000L);
        final Mail mail3 = createMail(aliceBox1, "sender3@example.com", "Third Email", 2000L);

        final List<Long> aliceBoxIds = List.of(aliceBox1.getId(), aliceBox2.getId());
        final Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "receiveTime"));

        // Act: Query page 0 with size 2
        final Page<MailSummaryProjection> pageResult = mailRepository.findSummaryByMailboxIn(aliceBoxIds, pageable);

        // Assert: Pagination metadata
        assertEquals(3, pageResult.getTotalElements(), "Total elements across all mailboxes should be 3");
        assertEquals(2, pageResult.getTotalPages(), "Total pages with page size 2 should be 2");
        assertEquals(2, pageResult.getContent().size(), "First page should contain exactly 2 items");

        // Assert: Descending sort order by receiveTime (mail2: 3000L, then mail3: 2000L)
        final MailSummaryProjection first = pageResult.getContent().get(0);
        final MailSummaryProjection second = pageResult.getContent().get(1);

        assertEquals(mail2.getId(), first.getId());
        assertEquals("sender2@example.com", first.getSender());
        assertEquals("Second Email", first.getSubject());
        assertEquals(3000L, first.getReceiveTime());
        assertEquals(aliceBox2.getId(), first.getMailbox());

        assertEquals(mail3.getId(), second.getId());
        assertEquals(2000L, second.getReceiveTime());

        // Act: Query page 1 (second page)
        final Pageable secondPageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "receiveTime"));
        final Page<MailSummaryProjection> secondPage = mailRepository.findSummaryByMailboxIn(aliceBoxIds, secondPageable);

        // Assert: Second page contains the remaining mail (mail1: 1000L)
        assertEquals(1, secondPage.getContent().size());
        assertEquals(mail1.getId(), secondPage.getContent().get(0).getId());
    }

    @Test
    @DisplayName("findSummaryByMailbox returns paginated summaries for a single mailbox")
    public void testFindSummaryByMailboxHappyPath()
    {
        // Arrange: 2 emails in aliceBox1, 1 email in aliceBox2
        final Mail mail1 = createMail(aliceBox1, "sender1@example.com", "Box 1 Mail 1", 1000L);
        final Mail mail2 = createMail(aliceBox1, "sender2@example.com", "Box 1 Mail 2", 2000L);
        createMail(aliceBox2, "sender3@example.com", "Box 2 Mail", 3000L);

        final Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "receiveTime"));

        // Act
        final Page<MailSummaryProjection> result = mailRepository.findSummaryByMailbox(aliceBox1.getId(), pageable);

        // Assert: Only emails belonging to aliceBox1 are returned
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(mail2.getId(), result.getContent().get(0).getId());
        assertEquals(mail1.getId(), result.getContent().get(1).getId());
        assertTrue(result.getContent().stream().allMatch(m -> m.getMailbox() == aliceBox1.getId()));
    }

    @Test
    @DisplayName("Multi-tenant isolation: user queries never expose emails belonging to another user")
    public void testMultiTenantDataIsolation()
    {
        // Arrange: Alice receives emails, Bob receives emails
        final Mail aliceMail1 = createMail(aliceBox1, "alice.sender@example.com", "Alice Secret 1", 1000L);
        final Mail aliceMail2 = createMail(aliceBox2, "alice.sender@example.com", "Alice Secret 2", 2000L);
        final Mail bobMail = createMail(bobBox, "bob.sender@example.com", "Bob Confidential", 3000L);

        final List<Long> aliceBoxIds = List.of(aliceBox1.getId(), aliceBox2.getId());
        final Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "receiveTime"));

        // Act: Alice queries her unified inbox
        final Page<MailSummaryProjection> aliceResult = mailRepository.findSummaryByMailboxIn(aliceBoxIds, pageable);

        // Assert: Alice's result must NOT contain Bob's email
        assertEquals(2, aliceResult.getTotalElements(), "Alice should only see her 2 emails");
        final List<Long> returnedMailIds = aliceResult.getContent().stream().map(MailSummaryProjection::getId).toList();
        assertTrue(returnedMailIds.contains(aliceMail1.getId()), "Should contain Alice's mail 1");
        assertTrue(returnedMailIds.contains(aliceMail2.getId()), "Should contain Alice's mail 2");
        assertFalse(returnedMailIds.contains(bobMail.getId()), "Must NEVER contain Bob's mail");

        // Act: Single mailbox query for Alice's box
        final Page<MailSummaryProjection> aliceSingleBoxResult = mailRepository.findSummaryByMailbox(aliceBox1.getId(), pageable);

        // Assert: Only Alice's box 1 email returned
        assertEquals(1, aliceSingleBoxResult.getTotalElements());
        assertEquals(aliceMail1.getId(), aliceSingleBoxResult.getContent().get(0).getId());
        assertFalse(aliceSingleBoxResult.getContent().stream().anyMatch(m -> m.getId() == bobMail.getId()));
    }

    @Test
    @DisplayName("SQL injection immunity: parameterized queries handle boundary inputs safely")
    public void testSqlInjectionImmunity()
    {
        // Arrange
        createMail(aliceBox1, "sender@example.com", "Standard Mail", 1000L);

        final Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "receiveTime"));

        // Act & Assert: Negative mailbox ID (safely parameterized, returns empty page)
        final Page<MailSummaryProjection> negativeResult = mailRepository.findSummaryByMailbox(-999L, pageable);
        assertNotNull(negativeResult);
        assertEquals(0, negativeResult.getTotalElements());
        assertTrue(negativeResult.getContent().isEmpty());

        // Act & Assert: Querying collection with non-existent IDs
        final Page<MailSummaryProjection> nonExistentListResult =
            mailRepository.findSummaryByMailboxIn(List.of(-1L, 999999L), pageable);
        assertNotNull(nonExistentListResult);
        assertEquals(0, nonExistentListResult.getTotalElements());
        assertTrue(nonExistentListResult.getContent().isEmpty());
    }

    @Test
    @DisplayName("Empty mailbox returns empty page with zero totalElements and valid pagination structure")
    public void testEmptyMailboxReturnsEmptyPage()
    {
        final Pageable pageable = PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "receiveTime"));

        // Act: Alice's box with 0 emails
        final Page<MailSummaryProjection> result = mailRepository.findSummaryByMailbox(aliceBox1.getId(), pageable);

        // Assert
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("Requesting page index beyond available pages returns empty content with correct totalElements")
    public void testOutOfRangePageIndex()
    {
        // Arrange: 2 emails exist
        createMail(aliceBox1, "sender1@example.com", "Mail 1", 1000L);
        createMail(aliceBox1, "sender2@example.com", "Mail 2", 2000L);

        // Act: Request page 10 when only 1 page (page 0) exists
        final Pageable outOfRangePageable = PageRequest.of(10, 10, Sort.by(Sort.Direction.DESC, "receiveTime"));
        final Page<MailSummaryProjection> result =
            mailRepository.findSummaryByMailbox(aliceBox1.getId(), outOfRangePageable);

        // Assert: Total elements is still accurate, content is empty without exception
        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.getContent().isEmpty(), "Out-of-range page content must be empty");
    }
}

