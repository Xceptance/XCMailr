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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import models.Domain;
import models.MBox;
import models.Mail;
import models.MailTransaction;
import models.User;

/**
 * Verification test for Spring Data JPA repositories.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RepositoryTests
{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private MailTransactionRepository mailTransactionRepository;

    @Autowired
    private DomainRepository domainRepository;

    @BeforeEach
    public void setup()
    {
        mailRepository.deleteAll();
        mailTransactionRepository.deleteAll();
        mailboxRepository.deleteAll();
        domainRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Verify UserRepository CRUD and queries")
    public void testUserRepository()
    {
        final User user = new User("John", "Doe", "john@example.com", "secret", "en");
        user.setApiToken("test-api-token-123");
        user.setApiTokenCreationTimestamp(System.currentTimeMillis());
        userRepository.save(user);

        assertTrue(userRepository.existsByMailIgnoreCase("JOHN@EXAMPLE.COM"));
        final Optional<User> byMail = userRepository.findByMailIgnoreCase("John@Example.com");
        assertTrue(byMail.isPresent());
        assertEquals("John", byMail.get().getForename());

        final Optional<User> byToken = userRepository.findByApiToken("test-api-token-123");
        assertTrue(byToken.isPresent());
        assertEquals("john@example.com", byToken.get().getMail());
    }

    @Test
    @DisplayName("Verify DomainRepository CRUD and ordering")
    public void testDomainRepository()
    {
        domainRepository.save(new Domain("b-domain.com"));
        domainRepository.save(new Domain("a-domain.com"));

        assertTrue(domainRepository.existsByDomainnameIgnoreCase("A-DOMAIN.COM"));
        final List<Domain> ordered = domainRepository.findAllByOrderByDomainnameAsc();
        assertEquals(2, ordered.size());
        assertEquals("a-domain.com", ordered.get(0).getDomainname());
        assertEquals("b-domain.com", ordered.get(1).getDomainname());
    }

    @Test
    @DisplayName("Verify MailboxRepository search and queries")
    public void testMailboxRepository()
    {
        final User user = new User("Jane", "Doe", "jane@example.com", "secret", "en");
        userRepository.save(user);

        final MBox box1 = new MBox("temp1", "xcmailr.test", 0L, false, user);
        final MBox box2 = new MBox("temp2", "xcmailr.test", 0L, false, user);
        mailboxRepository.save(box1);
        mailboxRepository.save(box2);

        assertTrue(mailboxRepository.existsByAddressIgnoreCaseAndDomainIgnoreCase("TEMP1", "XCMAILR.TEST"));

        final Page<MBox> searchResult = mailboxRepository.findByUsrAndSearch(user, "temp1", PageRequest.of(0, 10));
        assertEquals(1, searchResult.getTotalElements());
        assertEquals("temp1", searchResult.getContent().get(0).getAddress());
    }

    @Test
    @DisplayName("Verify MailRepository CRUD and deletion")
    public void testMailRepository()
    {
        final User user = new User("Bob", "Smith", "bob@example.com", "secret", "en");
        userRepository.save(user);

        final MBox box = new MBox("bobbox", "xcmailr.test", 0L, false, user);
        mailboxRepository.save(box);

        final Mail mail = new Mail();
        mail.setMailbox(box);
        mail.setSender("sender@test.com");
        mail.setSubject("Test Subject");
        mail.setMessage("Body text".getBytes());
        mail.setReceiveTime(System.currentTimeMillis());
        mailRepository.save(mail);

        assertNotNull(mail.getUuid());
        final List<Mail> mails = mailRepository.findByMailboxOrderByReceiveTimeAsc(box.getId());
        assertEquals(1, mails.size());

        final Optional<Mail> byUuid = mailRepository.findByUuid(mail.getUuid());
        assertTrue(byUuid.isPresent());
    }
}
