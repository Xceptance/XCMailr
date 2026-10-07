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

import java.util.List;
import java.util.Properties;

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
import com.xceptance.xcmailr.services.SmtpServerService;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import models.Domain;
import models.User;
import xcmailr.client.MailApi;
import xcmailr.client.Mailbox;
import xcmailr.client.MailboxApi;
import xcmailr.client.XCMailrClient;

/**
 * Automated end-to-end integration test verifying the complete email roundtrip lifecycle
 * across protocol boundaries:
 * <ol>
 *   <li>Provision a mailbox through the REST API using {@link XCMailrClient}.</li>
 *   <li>Dispatch a multipart (text/plain and text/html) email over the live embedded SMTP server socket.</li>
 *   <li>Poll and retrieve the delivered message through {@link XCMailrClient#mails()}.</li>
 *   <li>Validate email attributes (sender, subject, text, and html bodies).</li>
 *   <li>Clean up the message and mailbox using {@link XCMailrClient}.</li>
 * </ol>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = XcmailrApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ClientSmtpRoundtripIT
{
    /** The dynamic HTTP port assigned by Spring Boot for the embedded web container. */
    @LocalServerPort
    private int httpPort;

    /** The embedded SubEtha SMTP server service. */
    @Autowired
    private SmtpServerService smtpServerService;

    /** Repository for managing user entities and API tokens. */
    @Autowired
    private UserRepository userRepository;

    /** Repository for managing mailbox entities. */
    @Autowired
    private MailboxRepository mailboxRepository;

    /** Repository for managing persisted mail entities. */
    @Autowired
    private MailRepository mailRepository;

    /** Repository for managing domain whitelist entries. */
    @Autowired
    private DomainRepository domainRepository;

    /** Constant test API token value. */
    private static final String API_TOKEN = "roundtrip-test-token-abcdef123456";

    /** Configured test domain. */
    private static final String DOMAIN = "xcmailr.test";

    /** The REST client instance under test. */
    private XCMailrClient client;

    /**
     * Pre-test setup clearing all database state and creating the required test domain,
     * test user, and initializing the {@link XCMailrClient}.
     */
    @BeforeEach
    public void setup()
    {
        // Purge any stale database entries
        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();

        // Whitelist the domain for SMTP reception and mailbox creation
        final Domain domain = new Domain(DOMAIN);
        domainRepository.save(domain);

        // Provision the test user equipped with the API token
        final User testUser = new User("Roundtrip", "Tester", "tester@" + DOMAIN, "Password123", "en");
        testUser.setActive(true);
        testUser.setApiToken(API_TOKEN);
        testUser.setApiTokenCreationTimestamp(System.currentTimeMillis());
        userRepository.save(testUser);

        // Instantiate the XCMailrClient pointing to the live embedded HTTP port
        final String baseUrl = "http://localhost:" + httpPort;
        this.client = new XCMailrClient(baseUrl, API_TOKEN);
    }

    /**
     * Executes the full roundtrip test scenario:
     * 1. Creates a mailbox via REST.
     * 2. Transmits a multipart email via the live SMTP socket.
     * 3. Polls the REST API until the email is ingested.
     * 4. Asserts email properties and payload content.
     * 5. Deletes the mail and mailbox via REST.
     *
     * @throws Exception if any unexpected communication or protocol error occurs
     */
    @Test
    @DisplayName("Verify complete SMTP delivery and REST retrieval roundtrip using XCMailrClient")
    public void testFullSmtpAndClientRoundtrip() throws Exception
    {
        final MailboxApi mailboxApi = this.client.mailboxes();
        final MailApi mailApi = this.client.mails();

        final String mailboxAddress = "roundtrip-user@" + DOMAIN;
        final String senderAddress = "notifications@external-service.org";
        final String subject = "Welcome to Roundtrip Verification!";
        final String plainTextBody = "Hello from the automated roundtrip test plain text payload.";
        final String htmlBody = "<html><body><h1>Hello!</h1><p>Roundtrip HTML payload.</p></body></html>";

        // Step 1: Provision the test mailbox via REST API
        final Mailbox createdBox = mailboxApi.createMailbox(mailboxAddress, 60, false);
        assertNotNull(createdBox, "Created mailbox response must not be null");
        assertEquals(mailboxAddress, createdBox.address, "Mailbox address must match created address");
        assertFalse(createdBox.forwardEnabled, "Forwarding should be disabled");

        // Step 2: Determine dynamic SMTP port and transmit email over SMTP socket
        final int smtpPort = this.smtpServerService.getPort();
        assertTrue(smtpPort > 0, "SMTP server port must be bound and greater than zero");

        sendMultipartSmtpEmail(smtpPort, senderAddress, mailboxAddress, subject, plainTextBody, htmlBody);

        // Step 3: Poll the REST API via XCMailrClient until the message is ingested
        final long timeoutMillis = 10_000L;
        final long pollIntervalMillis = 250L;
        final long startTime = System.currentTimeMillis();

        List<xcmailr.client.Mail> receivedMails = List.of();
        while (System.currentTimeMillis() - startTime < timeoutMillis)
        {
            receivedMails = mailApi.listMails(mailboxAddress, null);
            if (!receivedMails.isEmpty())
            {
                break;
            }
            Thread.sleep(pollIntervalMillis);
        }

        // Step 4: Validate that the email arrived and verify its headers and content
        assertFalse(receivedMails.isEmpty(), "Email should be retrieved via REST client within timeout");
        assertEquals(1, receivedMails.size(), "Exactly one email should be present in the mailbox");

        final xcmailr.client.Mail mailSummary = receivedMails.get(0);
        assertEquals(senderAddress, mailSummary.sender, "Sender address must match");
        assertEquals(subject, mailSummary.subject, "Subject line must match");

        // Fetch full mail detail including body contents
        final xcmailr.client.Mail fullMail = mailApi.getMail(mailSummary.id);
        assertNotNull(fullMail, "Full mail detail must be returned");
        assertEquals(mailSummary.id, fullMail.id, "Mail ID must match");
        assertNotNull(fullMail.textContent, "Plain text content must be present");
        assertTrue(fullMail.textContent.contains(plainTextBody), "Plain text content must contain expected text");
        assertNotNull(fullMail.htmlContent, "HTML content must be present");
        assertTrue(fullMail.htmlContent.contains(htmlBody), "HTML content must contain expected HTML markup");

        // Step 5: Clean up the mail via REST API
        mailApi.deleteMail(fullMail.id);
        final List<xcmailr.client.Mail> mailsAfterDelete = mailApi.listMails(mailboxAddress, null);
        assertTrue(mailsAfterDelete.isEmpty(), "Mailbox should contain 0 mails after mail deletion");

        // Step 6: Clean up the mailbox via REST API
        mailboxApi.deleteMailbox(mailboxAddress);
        final List<Mailbox> mailboxesAfterDelete = mailboxApi.listMailboxes();
        assertTrue(mailboxesAfterDelete.isEmpty(), "Mailbox list should be empty after mailbox deletion");
    }

    /**
     * Helper method to send a multipart (plain text + HTML) email via JavaMail SMTP to the target port.
     *
     * @param port the SMTP port number to connect to
     * @param from the envelope sender address
     * @param to the envelope recipient address
     * @param subject the email subject line
     * @param textBody the plain text message body
     * @param htmlBody the HTML message body
     * @throws MessagingException if an error occurs during MIME message creation or SMTP transport
     */
    private void sendMultipartSmtpEmail(final int port,
                                        final String from,
                                        final String to,
                                        final String subject,
                                        final String textBody,
                                        final String htmlBody)
        throws MessagingException
    {
        final Properties props = new Properties();
        props.put("mail.smtp.host", "127.0.0.1");
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");

        final Session session = Session.getInstance(props);
        final MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);

        // Construct multipart alternative containing both text/plain and text/html
        final MimeMultipart multipart = new MimeMultipart("alternative");

        final BodyPart textPart = new MimeBodyPart();
        textPart.setText(textBody);
        multipart.addBodyPart(textPart);

        final BodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=utf-8");
        multipart.addBodyPart(htmlPart);

        message.setContent(multipart);
        message.saveChanges();

        Transport.send(message);
    }
}
