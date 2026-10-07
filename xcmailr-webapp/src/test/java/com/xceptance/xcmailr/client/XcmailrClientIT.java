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
package com.xceptance.xcmailr.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.xceptance.xcmailr.XcmailrApplication;
import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import models.Domain;
import models.MBox;
import models.Mail;
import models.User;
import xcmailr.client.MailApi;
import xcmailr.client.MailboxApi;
import xcmailr.client.XCMailrClient;

/**
 * End-to-end integration test validating the {@link XCMailrClient} client library
 * communication against the modernized Spring Boot REST API.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = XcmailrApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class XcmailrClientIT
{
    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private DomainRepository domainRepository;

    private static final String API_TOKEN = "test-client-token-1234567890";
    private User testUser;
    private XCMailrClient client;

    @BeforeEach
    public void setup()
    {
        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();

        final Domain domain = new Domain("xcmailr.test");
        domainRepository.save(domain);

        testUser = new User("Client", "Tester", "tester@xcmailr.test", "Password123", "en");
        testUser.setActive(true);
        testUser.setApiToken(API_TOKEN);
        testUser.setApiTokenCreationTimestamp(System.currentTimeMillis());
        userRepository.save(testUser);

        final String baseUrl = "http://localhost:" + port;
        client = new XCMailrClient(baseUrl, API_TOKEN);
    }

    @Test
    @DisplayName("Verify XCMailrClient full mailbox and mail operations against Spring Boot server")
    public void testClientEndToEndFlow() throws Exception
    {
        final MailboxApi mailboxApi = client.mailboxes();
        final MailApi mailApi = client.mails();

        // 1. Initial listing is empty
        final List<xcmailr.client.Mailbox> initialList = mailboxApi.listMailboxes();
        assertNotNull(initialList);
        assertTrue(initialList.isEmpty(), "Initial mailbox list should be empty");

        // 2. Create a mailbox via client
        final xcmailr.client.Mailbox created = mailboxApi.createMailbox("mybox@xcmailr.test", 60, true);
        assertNotNull(created);
        assertEquals("mybox@xcmailr.test", created.address);
        assertTrue(created.forwardEnabled);

        // 3. Fetch mailbox details
        final xcmailr.client.Mailbox fetched = mailboxApi.getMailbox("mybox@xcmailr.test");
        assertNotNull(fetched);
        assertEquals("mybox@xcmailr.test", fetched.address);

        // 4. List mailboxes contains newly created box
        final List<xcmailr.client.Mailbox> listAfterCreate = mailboxApi.listMailboxes();
        assertEquals(1, listAfterCreate.size());
        assertEquals("mybox@xcmailr.test", listAfterCreate.get(0).address);

        // 5. Insert an email into this mailbox
        final MBox dbBox = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("mybox", "xcmailr.test").orElseThrow();

        final Session session = Session.getInstance(new java.util.Properties());
        final MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setFrom(new InternetAddress("sender@external.com"));
        mimeMessage.setRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress("mybox@xcmailr.test"));
        mimeMessage.setSubject("Test Email via Client");
        mimeMessage.setText("Hello from client test!");
        mimeMessage.saveChanges();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mimeMessage.writeTo(baos);

        final Mail testMail = new Mail();
        testMail.setSender("sender@external.com");
        testMail.setSubject("Test Email via Client");
        testMail.setReceiveTime(System.currentTimeMillis());
        testMail.setMessage(baos.toByteArray());
        testMail.setMailbox(dbBox);
        mailRepository.save(testMail);

        // 6. List mails via client
        final List<xcmailr.client.Mail> mails = mailApi.listMails("mybox@xcmailr.test", null);
        assertEquals(1, mails.size());
        final xcmailr.client.Mail clientMail = mails.get(0);
        assertEquals("sender@external.com", clientMail.sender);
        assertEquals("Test Email via Client", clientMail.subject);

        // 7. Fetch full mail details
        final xcmailr.client.Mail fullMail = mailApi.getMail(clientMail.id);
        assertNotNull(fullMail);
        assertEquals(clientMail.id, fullMail.id);
        assertNotNull(fullMail.textContent);
        assertTrue(fullMail.textContent.contains("Hello from client test!"));

        // 8. Delete mail
        mailApi.deleteMail(clientMail.id);
        final List<xcmailr.client.Mail> mailsAfterDelete = mailApi.listMails("mybox@xcmailr.test", null);
        assertTrue(mailsAfterDelete.isEmpty(), "Mails list should be empty after deletion");

        // 9. Delete mailbox
        mailboxApi.deleteMailbox("mybox@xcmailr.test");
        final List<xcmailr.client.Mailbox> listAfterDelete = mailboxApi.listMailboxes();
        assertTrue(listAfterDelete.isEmpty(), "Mailbox list should be empty after mailbox deletion");
    }
}
