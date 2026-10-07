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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.subethamail.smtp.TooMuchDataException;

import com.xceptance.xcmailr.config.XcmailrProperties;
import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;

import models.MBox;
import models.Mail;
import models.MailTransaction;
import models.User;

/**
 * Isolated unit tests for {@link MailDeliveryService} covering SMTP RCPT acceptance (anti-relay),
 * mailbox disposition routing, transaction audit logging, and forwarding lifecycle.
 *
 * @author Xceptance Software Technologies GmbH
 */
@ExtendWith(MockitoExtension.class)
public class MailDeliveryServiceTest
{
    @Mock
    private DomainRepository domainRepository;

    @Mock
    private MailboxRepository mailboxRepository;

    @Mock
    private MailRepository mailRepository;

    @Mock
    private MailTransactionRepository mailTransactionRepository;

    @Mock
    private OutboundMailService outboundMailService;

    private XcmailrProperties properties;

    private MailDeliveryService deliveryService;

    /**
     * Initializes test dependencies and configures baseline application properties.
     */
    @BeforeEach
    public void setUp()
    {
        properties = new XcmailrProperties();
        properties.getMbox().setDomainList(List.of("xcmailr.test", "configured.domain"));
        properties.getMbox().setMaxSize(10 * 1024 * 1024); // 10MB
        properties.getMailTransaction().setMaxAgeHours(24);

        deliveryService = new MailDeliveryService(
            properties,
            domainRepository,
            mailboxRepository,
            mailRepository,
            mailTransactionRepository,
            outboundMailService
        );
    }

    // =========================================================================
    // Group 2.1: accept(...) validation tests
    // =========================================================================

    @Test
    @DisplayName("accept returns true when recipient domain is in configured properties domain list")
    public void testAcceptWithConfiguredDomainWhitelist()
    {
        final boolean accepted = deliveryService.accept("sender@external.org", "target@xcmailr.test");

        assertTrue(accepted, "Configured whitelisted domain must be accepted");
        verify(mailTransactionRepository, never()).save(any(MailTransaction.class));
    }

    @Test
    @DisplayName("accept returns true when recipient domain is registered in domain repository")
    public void testAcceptWithDatabaseDomainWhitelist()
    {
        when(domainRepository.existsByDomainnameIgnoreCase("custom-database-domain.org")).thenReturn(true);

        final boolean accepted = deliveryService.accept("sender@external.org", "target@custom-database-domain.org");

        assertTrue(accepted, "Database whitelisted domain must be accepted");
        verify(mailTransactionRepository, never()).save(any(MailTransaction.class));
    }

    @Test
    @DisplayName("accept returns false and logs transaction status 500 when recipient domain is unmanaged")
    public void testAcceptWithUnmanagedExternalDomainRelayRejection()
    {
        when(domainRepository.existsByDomainnameIgnoreCase("unmanaged-relay.com")).thenReturn(false);

        final boolean accepted = deliveryService.accept("attacker@external.org", "victim@unmanaged-relay.com");

        assertFalse(accepted, "Unmanaged external domain must be rejected to prevent open relay");

        final ArgumentCaptor<MailTransaction> captor = ArgumentCaptor.forClass(MailTransaction.class);
        verify(mailTransactionRepository).save(captor.capture());
        final MailTransaction mtx = captor.getValue();
        assertEquals(500, mtx.getStatus(), "Transaction status 500 (relay denied) must be logged");
        assertEquals("victim@unmanaged-relay.com", mtx.getRelayaddr());
    }

    @Test
    @DisplayName("accept returns false and logs transaction status 0 when recipient address is malformed")
    public void testAcceptWithMalformedRecipientAddress()
    {
        final boolean accepted = deliveryService.accept("sender@external.org", "invalid-address-without-at");

        assertFalse(accepted, "Malformed recipient address must be rejected");

        final ArgumentCaptor<MailTransaction> captor = ArgumentCaptor.forClass(MailTransaction.class);
        verify(mailTransactionRepository).save(captor.capture());
        final MailTransaction mtx = captor.getValue();
        assertEquals(0, mtx.getStatus(), "Transaction status 0 (malformed address) must be logged");
    }

    // =========================================================================
    // Group 2.2: deliver(...) drop logic and size limit tests
    // =========================================================================

    @Test
    @DisplayName("deliver drops email and logs status 100 when recipient mailbox does not exist")
    public void testDeliverNonExistentMailboxDrop() throws Exception
    {
        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("unknown", "xcmailr.test"))
            .thenReturn(Optional.empty());

        final InputStream mailStream = createMimeMessageStream("sender@example.com", "unknown@xcmailr.test", "Hello", "Content", null);

        deliveryService.deliver("sender@example.com", "unknown@xcmailr.test", mailStream);

        // Verify mail is NOT persisted
        verify(mailRepository, never()).save(any(Mail.class));

        // Verify transaction status 100 (mailbox not found) is recorded
        final ArgumentCaptor<MailTransaction> captor = ArgumentCaptor.forClass(MailTransaction.class);
        verify(mailTransactionRepository).save(captor.capture());
        final MailTransaction mtx = captor.getValue();
        assertEquals(100, mtx.getStatus());
        assertEquals("unknown@xcmailr.test", mtx.getRelayaddr());
    }

    @Test
    @DisplayName("deliver drops email, increments suppressions, and logs status 200 when mailbox is inactive")
    public void testDeliverInactiveMailboxDrop() throws Exception
    {
        final User user = new User("Owner", "User", "owner@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox inactiveBox = new MBox("inactive", "xcmailr.test", 0L, true, user);
        inactiveBox.setActive(false);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("inactive", "xcmailr.test"))
            .thenReturn(Optional.of(inactiveBox));

        final InputStream mailStream = createMimeMessageStream("sender@example.com", "inactive@xcmailr.test", "Subject", "Body", null);

        deliveryService.deliver("sender@example.com", "inactive@xcmailr.test", mailStream);

        // Verify mail is NOT persisted
        verify(mailRepository, never()).save(any(Mail.class));

        // Verify suppressions incremented and mailbox updated
        assertEquals(1, inactiveBox.getSuppressions(), "Suppressions counter must be incremented");
        verify(mailboxRepository).save(inactiveBox);

        // Verify transaction status 200 (mailbox inactive) recorded
        final ArgumentCaptor<MailTransaction> captor = ArgumentCaptor.forClass(MailTransaction.class);
        verify(mailTransactionRepository).save(captor.capture());
        final MailTransaction mtx = captor.getValue();
        assertEquals(200, mtx.getStatus());
        assertEquals("owner@realdomain.org", mtx.getTargetaddr());
    }

    @Test
    @DisplayName("deliver drops email, increments suppressions, and logs status 600 when owning user is inactive")
    public void testDeliverInactiveUserDrop() throws Exception
    {
        final User inactiveUser = new User("Disabled", "User", "disabled@realdomain.org", "hash", "en");
        inactiveUser.setActive(false);

        final MBox activeBox = new MBox("userdisabled", "xcmailr.test", 0L, false, inactiveUser);
        activeBox.setActive(true);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("userdisabled", "xcmailr.test"))
            .thenReturn(Optional.of(activeBox));

        final InputStream mailStream = createMimeMessageStream("sender@example.com", "userdisabled@xcmailr.test", "Subject", "Body", null);

        deliveryService.deliver("sender@example.com", "userdisabled@xcmailr.test", mailStream);

        // Verify mail is NOT persisted
        verify(mailRepository, never()).save(any(Mail.class));

        // Verify suppressions incremented and mailbox updated
        assertEquals(1, activeBox.getSuppressions(), "Suppressions counter must be incremented");
        verify(mailboxRepository).save(activeBox);

        // Verify transaction status 600 (user inactive) recorded
        final ArgumentCaptor<MailTransaction> captor = ArgumentCaptor.forClass(MailTransaction.class);
        verify(mailTransactionRepository).save(captor.capture());
        final MailTransaction mtx = captor.getValue();
        assertEquals(600, mtx.getStatus());
        assertEquals("disabled@realdomain.org", mtx.getTargetaddr());
    }

    @Test
    @DisplayName("deliver throws TooMuchDataException when email stream exceeds configured max size")
    public void testDeliverOversizedEmailThrowsTooMuchDataException()
    {
        // Configure low max-size limit for test
        properties.getMbox().setMaxSize(50);

        final User user = new User("Size", "Tester", "size@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("sizelimit", "xcmailr.test", 0L, false, user);
        mbox.setActive(true);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("sizelimit", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));

        // Create stream with 100 bytes exceeding 50 byte limit
        final byte[] oversizedData = new byte[100];
        final InputStream stream = new ByteArrayInputStream(oversizedData);

        assertThrows(TooMuchDataException.class, () -> {
            deliveryService.deliver("sender@example.com", "sizelimit@xcmailr.test", stream);
        });

        // Verify no mail persisted
        verify(mailRepository, never()).save(any(Mail.class));
    }

    // =========================================================================
    // Group 2.3: deliver(...) persistence, forwarding, and loop prevention
    // =========================================================================

    @Test
    @DisplayName("deliver persists mail entity and does not forward when forwardEmails is false")
    public void testDeliverActiveMailboxWithoutForwarding() throws Exception
    {
        final User user = new User("Store", "Only", "store@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("storebox", "xcmailr.test", 0L, false, user);
        mbox.setActive(true);
        mbox.setForwardEmails(false);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("storebox", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));

        final InputStream mailStream = createMimeMessageStream("sender@example.com", "storebox@xcmailr.test",
                                                               "Storage Test", "Plain text message content", null);

        deliveryService.deliver("sender@example.com", "storebox@xcmailr.test", mailStream);

        // Verify mail entity persisted
        final ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);
        verify(mailRepository).save(mailCaptor.capture());
        final Mail savedMail = mailCaptor.getValue();
        assertEquals("Storage Test", savedMail.getSubject());
        assertEquals("sender@example.com", savedMail.getSender());
        assertNotNull(savedMail.getUuid());

        // Verify outbound mail was NOT dispatched
        verify(outboundMailService, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("deliver persists mail entity and forwards when forwardEmails is true")
    public void testDeliverActiveMailboxWithForwardingSuccess() throws Exception
    {
        final User user = new User("Forward", "User", "forward_destination@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("forwardbox", "xcmailr.test", 0L, false, user);
        mbox.setActive(true);
        mbox.setForwardEmails(true);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("forwardbox", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));
        when(outboundMailService.createSession()).thenReturn(Session.getInstance(new Properties()));

        final InputStream mailStream = createMimeMessageStream("sender@example.com", "forwardbox@xcmailr.test",
                                                               "Forward Subject", "Body content to forward", null);

        deliveryService.deliver("sender@example.com", "forwardbox@xcmailr.test", mailStream);

        // Verify mail persisted
        verify(mailRepository).save(any(Mail.class));

        // Verify outbound mail sent
        final ArgumentCaptor<MimeMessage> msgCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(outboundMailService).send(msgCaptor.capture());
        final MimeMessage forwardedMsg = msgCaptor.getValue();
        assertEquals("forward_destination@realdomain.org", forwardedMsg.getRecipients(Message.RecipientType.TO)[0].toString());

        // Verify forward count incremented
        assertEquals(1, mbox.getForwards());
        verify(mailboxRepository).save(mbox);

        // Verify transaction status 300 (forwarded) recorded
        final ArgumentCaptor<MailTransaction> captor = ArgumentCaptor.forClass(MailTransaction.class);
        verify(mailTransactionRepository).save(captor.capture());
        final MailTransaction mtx = captor.getValue();
        assertEquals(300, mtx.getStatus());
        assertEquals("forward_destination@realdomain.org", mtx.getTargetaddr());
    }

    @Test
    @DisplayName("deliver persists mail entity but prevents forwarding when loop is detected via X-Loop header")
    public void testDeliverLoopPreventionWithXLoopHeader() throws Exception
    {
        final User user = new User("Loop", "User", "loop_destination@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("loopdetect", "xcmailr.test", 0L, false, user);
        mbox.setActive(true);
        mbox.setForwardEmails(true);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("loopdetect", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));

        // Create email containing matching X-Loop header
        final String xLoopHeader = "loopbreakerloopdetect@xcmailr.test";
        final InputStream mailStream = createMimeMessageStream("sender@example.com", "loopdetect@xcmailr.test",
                                                               "Loop Subject", "Body", xLoopHeader);

        deliveryService.deliver("sender@example.com", "loopdetect@xcmailr.test", mailStream);

        // Verify mail persisted for local web viewing
        verify(mailRepository).save(any(Mail.class));

        // Verify outbound mail was NOT dispatched because loop was broken
        verify(outboundMailService, never()).send(any(MimeMessage.class));

        // Verify forward counter was NOT incremented
        assertEquals(0, mbox.getForwards());
    }

    // =========================================================================
    // Test Helper Methods
    // =========================================================================

    /**
     * Helper to construct a raw MIME message byte input stream for testing.
     *
     * @param from envelope sender address
     * @param to envelope recipient address
     * @param subject email subject line
     * @param body plain text body
     * @param xLoopHeader optional X-Loop header value
     * @return input stream containing serialized MIME message
     * @throws Exception if MIME message composition fails
     */
    private InputStream createMimeMessageStream(final String from,
                                                final String to,
                                                final String subject,
                                                final String body,
                                                final String xLoopHeader) throws Exception
    {
        final Session session = Session.getInstance(new Properties());
        final MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setText(body);

        if (xLoopHeader != null)
        {
            message.addHeader("X-Loop", xLoopHeader);
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.writeTo(out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}
