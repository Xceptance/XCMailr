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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.Domain;
import models.MBox;
import models.Mail;
import models.MailTransaction;
import models.User;

/**
 * Full characterization integration test verifying inbound SubEthaSMTP delivery lifecycle,
 * anti-open-relay enforcement, mailbox precondition checks, persistence, and loop prevention.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
public class SmtpDeliveryIT
{
    @Autowired
    private SmtpServerService smtpServerService;

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

    private User testUser;

    @BeforeEach
    public void setup()
    {
        mailRepository.deleteAll();
        mailTransactionRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();

        // Ensure whitelisted domain
        final Domain domain = new Domain("xcmailr.test");
        domainRepository.save(domain);

        testUser = new User("Smtp", "Tester", "smtp_forward_target@realdomain.test", "password", "en");
        testUser.setActive(true);
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should reject relaying to unmanaged external domains with SMTP 553 and log status 500")
    public void testDomainWhitelistEnforcementAndAntiRelayRejection()
    {
        final int port = smtpServerService.getPort();
        assertTrue(port > 0, "SMTP server port should be bound");

        // Attempt sending to external unmanaged domain
        final MessagingException exception = assertThrows(MessagingException.class, () -> {
            sendSmtpEmail(port, "attacker@badactor.org", "victim@unmanaged-external-domain.org",
                          "Spam relay attempt", "Relay body", null);
        });

        assertNotNull(exception.getMessage(), "Relay attempt should fail");

        // Verify MTX status 500 (relay denied)
        final List<MailTransaction> transactions = mailTransactionRepository.findAll();
        assertFalse(transactions.isEmpty(), "Relay rejection should be recorded in MailTransaction");
        final MailTransaction mtx = transactions.stream()
                                                .filter(t -> t.getStatus() == 500)
                                                .findFirst()
                                                .orElse(null);
        assertNotNull(mtx, "Should have logged status 500 transaction");
        assertEquals("victim@unmanaged-external-domain.org", mtx.getRelayaddr());
    }

    @Test
    @DisplayName("Should accept and deliver email to active mailbox, persist mail body, and record transaction")
    public void testInboundDeliveryToActiveMailbox() throws Exception
    {
        final int port = smtpServerService.getPort();
        assertTrue(port > 0, "SMTP server port should be bound");

        final MBox activeBox = new MBox("activebox", "xcmailr.test", 0L, false, testUser);
        activeBox.setActive(true);
        activeBox.setForwardEmails(true);
        mailboxRepository.save(activeBox);

        sendSmtpEmail(port, "sender@example.com", "activebox@xcmailr.test",
                      "Test Inbound Subject", "Hello from SMTP integration test!", null);

        // Verify Mail entity saved
        final List<Mail> mails = mailRepository.findByMailboxOrderByReceiveTimeAsc(activeBox.getId());
        assertEquals(1, mails.size(), "Email should be stored in database");
        final Mail savedMail = mails.get(0);
        assertEquals("Test Inbound Subject", savedMail.getSubject());
        assertEquals("sender@example.com", savedMail.getSender());
        assertTrue(new String(savedMail.getMessage(), StandardCharsets.UTF_8).contains("Hello from SMTP integration test!"));

        // Verify transaction logged
        final List<MailTransaction> transactions = mailTransactionRepository.findAll();
        final MailTransaction forwardTx = transactions.stream()
                                                      .filter(t -> t.getStatus() == 300)
                                                      .findFirst()
                                                      .orElse(null);
        assertNotNull(forwardTx, "Should have logged status 300 for forward");
        assertEquals("activebox@xcmailr.test", forwardTx.getRelayaddr());
    }

    @Test
    @DisplayName("Should drop email sent to expired/inactive mailbox, increment suppressions, and log status 200")
    public void testExpiredMailboxDropping() throws Exception
    {
        final int port = smtpServerService.getPort();
        assertTrue(port > 0, "SMTP server port should be bound");

        final MBox inactiveBox = new MBox("inactivebox", "xcmailr.test", 0L, true, testUser);
        inactiveBox.setActive(false);
        mailboxRepository.save(inactiveBox);

        sendSmtpEmail(port, "sender@example.com", "inactivebox@xcmailr.test",
                      "Dropped Mail", "This should be dropped", null);

        // Verify no mail was persisted
        final List<Mail> mails = mailRepository.findByMailboxOrderByReceiveTimeAsc(inactiveBox.getId());
        assertEquals(0, mails.size(), "No mail should be stored for inactive mailbox");

        // Reload mailbox and check suppression counter
        final MBox reloaded = mailboxRepository.findById(inactiveBox.getId()).orElseThrow();
        assertEquals(1, reloaded.getSuppressions(), "Suppressions counter should be incremented");

        // Verify transaction status 200
        final List<MailTransaction> transactions = mailTransactionRepository.findAll();
        final MailTransaction dropTx = transactions.stream()
                                                   .filter(t -> t.getStatus() == 200)
                                                   .findFirst()
                                                   .orElse(null);
        assertNotNull(dropTx, "Should have logged status 200 for inactive mailbox drop");
    }

    @Test
    @DisplayName("Should detect X-Loop header and prevent forwarding to avoid mail loops")
    public void testLoopPreventionWithXLoopHeader() throws Exception
    {
        final int port = smtpServerService.getPort();
        assertTrue(port > 0, "SMTP server port should be bound");

        final MBox loopBox = new MBox("loopbox", "xcmailr.test", 0L, false, testUser);
        loopBox.setActive(true);
        loopBox.setForwardEmails(true);
        mailboxRepository.save(loopBox);

        // Send with X-Loop header matching the recipient address
        final String xLoopValue = "loopbreakerloopbox@xcmailr.test";
        sendSmtpEmail(port, "sender@example.com", "loopbox@xcmailr.test",
                      "Loop Test", "Loop content", xLoopValue);

        // Email should still be stored in repository for web viewing
        final List<Mail> mails = mailRepository.findByMailboxOrderByReceiveTimeAsc(loopBox.getId());
        assertEquals(1, mails.size(), "Email should be saved even if forwarding is prevented");

        // Reload mailbox - forwards count should NOT be incremented
        final MBox reloaded = mailboxRepository.findById(loopBox.getId()).orElseThrow();
        assertEquals(0, reloaded.getForwards(), "Forward counter should NOT be incremented when loop is broken");

        // No status 300 forward transaction should exist
        final List<MailTransaction> transactions = mailTransactionRepository.findAll();
        final boolean forwardTxExists = transactions.stream().anyMatch(t -> t.getStatus() == 300);
        assertFalse(forwardTxExists, "Forward transaction should not be created for looped message");
    }

    /**
     * Helper to send an email via raw SMTP to the specified port.
     */
    private void sendSmtpEmail(final int port, final String from, final String to,
                               final String subject, final String body, final String xLoopHeader)
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
        message.setText(body);

        if (xLoopHeader != null)
        {
            message.addHeader("X-Loop", xLoopHeader);
        }

        Transport.send(message);
    }
}
