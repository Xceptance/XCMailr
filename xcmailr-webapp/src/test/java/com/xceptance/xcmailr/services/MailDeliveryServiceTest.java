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
package com.xceptance.xcmailr.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import org.junit.jupiter.api.BeforeAll;
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
import com.xceptance.xcmailr.util.JakartaMimeMessageParser;

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

    @BeforeAll
    public static void beforeAll()
    {
        com.xceptance.xcmailr.XcmailrApplication.configureMailSystemProperties();
    }

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

        // Verify suppressions incremented atomically via repository
        verify(mailboxRepository).incrementSuppressions(inactiveBox.getId());
        verify(mailboxRepository, never()).save(inactiveBox);

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

        // Verify suppressions incremented atomically via repository
        verify(mailboxRepository).incrementSuppressions(activeBox.getId());
        verify(mailboxRepository, never()).save(activeBox);

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

        // Verify forward count incremented atomically via repository
        verify(mailboxRepository).incrementForwards(mbox.getId());
        verify(mailboxRepository, never()).save(mbox);

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
    // Group 2.4: deliver(...) UTF-8 and internationalized header handling
    // =========================================================================

    @Test
    @DisplayName("deliver raw 8-bit UTF-8 email persists unmangled Unicode subject and sender in mail entity")
    public void testDeliverRawUtf8SubjectAndSender() throws Exception
    {
        final User user = new User("Unicode", "User", "unicode@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("unicodetest", "xcmailr.test", 0L, false, user);
        mbox.setActive(true);
        mbox.setForwardEmails(false);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("unicodetest", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));

        final String rawEmail = "From: \"Jürgen Schmidt\" <sender@external.com>\r\n"
            + "To: unicodetest@xcmailr.test\r\n"
            + "Subject: Docker Smoke Test ü\r\n"
            + "Content-Type: text/plain; charset=UTF-8\r\n\r\n"
            + "Testing containerized email reception.";

        final byte[] rawBytes = rawEmail.getBytes(StandardCharsets.UTF_8);
        deliveryService.deliver("sender@external.com", "unicodetest@xcmailr.test", new ByteArrayInputStream(rawBytes));

        final ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);
        verify(mailRepository).save(mailCaptor.capture());
        final Mail savedMail = mailCaptor.getValue();

        assertEquals("Docker Smoke Test ü", savedMail.getSubject());
        assertEquals("Jürgen Schmidt <sender@external.com>", savedMail.getSender());
        assertArrayEquals(rawBytes, savedMail.getMessage());
    }

    @Test
    @DisplayName("deliver raw 8-bit UTF-8 email forwards to outbound recipients with intact UTF-8 headers")
    public void testDeliverRawUtf8WithForwardingPreservesUtf8Headers() throws Exception
    {
        final User user = new User("Forward", "User", "forward_target@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("utf8forward", "xcmailr.test", 0L, false, user);
        mbox.setActive(true);
        mbox.setForwardEmails(true);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("utf8forward", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));
        when(outboundMailService.createSession()).thenReturn(Session.getInstance(JakartaMimeMessageParser.getDefaultSessionProperties()));

        final String rawEmail = "From: \"Élodie & 太郎\" <sender@external.com>\r\n"
            + "To: utf8forward@xcmailr.test\r\n"
            + "Subject: Internationalized 🚀 Forwarding\r\n"
            + "Content-Type: text/plain; charset=UTF-8\r\n\r\n"
            + "Body with emojis and accents";

        final byte[] rawBytes = rawEmail.getBytes(StandardCharsets.UTF_8);
        deliveryService.deliver("sender@external.com", "utf8forward@xcmailr.test", new ByteArrayInputStream(rawBytes));

        // Verify mail persisted locally with proper subject
        final ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);
        verify(mailRepository).save(mailCaptor.capture());
        assertEquals("Internationalized 🚀 Forwarding", mailCaptor.getValue().getSubject());

        // Verify outbound forwarded message has preserved UTF-8 subject and recipient
        final ArgumentCaptor<MimeMessage> msgCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(outboundMailService).send(msgCaptor.capture());
        final MimeMessage forwardedMsg = msgCaptor.getValue();
        assertEquals("Internationalized 🚀 Forwarding", forwardedMsg.getSubject());
        assertEquals("forward_target@realdomain.org", forwardedMsg.getRecipients(Message.RecipientType.TO)[0].toString());
        verify(mailboxRepository).incrementForwards(mbox.getId());
    }

    @Test
    @DisplayName("deliver RFC 2047 encoded subject and sender display name decodes properly into entity")
    public void testDeliverRfc2047EncodedSubjectAndSender() throws Exception
    {
        final User user = new User("Rfc", "User", "rfc@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("rfcbox", "xcmailr.test", 0L, false, user);
        mbox.setActive(true);
        mbox.setForwardEmails(false);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("rfcbox", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));

        final String rawEmail = "From: =?UTF-8?B?SsO8cmdlbiBTY2htaWR0?= <sender@external.com>\r\n"
            + "To: rfcbox@xcmailr.test\r\n"
            + "Subject: =?UTF-8?Q?Docker_Smoke_Test_=C3=BC?=\r\n"
            + "Content-Type: text/plain; charset=UTF-8\r\n\r\n"
            + "RFC 2047 test content";

        deliveryService.deliver("sender@external.com", "rfcbox@xcmailr.test",
                               new ByteArrayInputStream(rawEmail.getBytes(StandardCharsets.UTF_8)));

        final ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);
        verify(mailRepository).save(mailCaptor.capture());
        final Mail savedMail = mailCaptor.getValue();

        assertEquals("Docker Smoke Test ü", savedMail.getSubject());
        assertTrue(savedMail.getSender().contains("Jürgen Schmidt"));
    }

    @Test
    @DisplayName("deliver email with malformed UTF-8 header octets does not crash delivery and preserves raw payload")
    public void testDeliverMalformedUtf8OctetsDoesNotCrash() throws Exception
    {
        final User user = new User("Malformed", "User", "malformed@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("malformedbox", "xcmailr.test", 0L, false, user);
        mbox.setActive(true);
        mbox.setForwardEmails(false);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("malformedbox", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));

        // Construct raw bytes with invalid UTF-8 sequence (0xC3 followed by 0x28 instead of continuation byte)
        final byte[] headerPrefix = "From: sender@external.com\r\nTo: malformedbox@xcmailr.test\r\nSubject: Broken ".getBytes(StandardCharsets.US_ASCII);
        final byte[] invalidBytes = new byte[] { (byte) 0xC3, 0x28 };
        final byte[] body = "\r\n\r\nPayload body".getBytes(StandardCharsets.US_ASCII);

        final byte[] rawBytes = new byte[headerPrefix.length + invalidBytes.length + body.length];
        System.arraycopy(headerPrefix, 0, rawBytes, 0, headerPrefix.length);
        System.arraycopy(invalidBytes, 0, rawBytes, headerPrefix.length, invalidBytes.length);
        System.arraycopy(body, 0, rawBytes, headerPrefix.length + invalidBytes.length, body.length);

        deliveryService.deliver("sender@external.com", "malformedbox@xcmailr.test", new ByteArrayInputStream(rawBytes));

        // Delivery should still succeed and persist entity
        final ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);
        verify(mailRepository).save(mailCaptor.capture());
        final Mail savedMail = mailCaptor.getValue();

        assertNotNull(savedMail.getSubject());
        assertArrayEquals(rawBytes, savedMail.getMessage());
    }

    @Test
    @DisplayName("deliver email missing Subject and From headers uses safe defaults")
    public void testDeliverMissingSubjectAndFromHeaders() throws Exception
    {
        final User user = new User("Minimal", "User", "minimal@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("minimalbox", "xcmailr.test", 0L, false, user);
        mbox.setActive(true);
        mbox.setForwardEmails(false);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("minimalbox", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));

        final String rawEmail = "To: minimalbox@xcmailr.test\r\n\r\nBody without headers";
        deliveryService.deliver("envelope-sender@external.com", "minimalbox@xcmailr.test",
                               new ByteArrayInputStream(rawEmail.getBytes(StandardCharsets.UTF_8)));

        final ArgumentCaptor<Mail> mailCaptor = ArgumentCaptor.forClass(Mail.class);
        verify(mailRepository).save(mailCaptor.capture());
        final Mail savedMail = mailCaptor.getValue();

        assertEquals("", savedMail.getSubject());
        assertEquals("envelope-sender@external.com", savedMail.getSender());
    }

    // =========================================================================
    // Group 2.5: Forwarding MIME header normalization tests (Test Pyramid)
    // =========================================================================

    // --- Happy Path Tests ---

    @Test
    @DisplayName("forward untyped single-part UTF-8 email normalizes MIME-Version and Content-Type to text/plain; charset=UTF-8")
    public void testForwardUntypedSinglePartUtf8EmailNormalizesMimeHeaders() throws Exception
    {
        final User user = new User("Happy", "User", "forward_happy@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("untypedbox", "xcmailr.test", 0L, false, user);
        mbox.setActive(true);
        mbox.setForwardEmails(true);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("untypedbox", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));
        when(outboundMailService.createSession())
            .thenReturn(Session.getInstance(JakartaMimeMessageParser.getDefaultSessionProperties()));

        final String rawEmail = "From: sender@external.com\r\n"
            + "To: untypedbox@xcmailr.test\r\n"
            + "Subject: Docker Smoke Test ü\r\n\r\n"
            + "üüü Testing containerized email reception.";

        final byte[] rawBytes = rawEmail.getBytes(StandardCharsets.UTF_8);
        deliveryService.deliver("sender@external.com", "untypedbox@xcmailr.test", new ByteArrayInputStream(rawBytes));

        final ArgumentCaptor<MimeMessage> msgCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(outboundMailService).send(msgCaptor.capture());
        final MimeMessage forwardedMsg = msgCaptor.getValue();

        assertEquals("1.0", forwardedMsg.getHeader("MIME-Version", null), "Forwarded message must declare MIME-Version: 1.0");
        assertEquals("text/plain; charset=UTF-8", forwardedMsg.getHeader("Content-Type", null),
                     "Untyped message must declare text/plain; charset=UTF-8");
        assertEquals("Docker Smoke Test ü", forwardedMsg.getSubject(), "Subject must preserve UTF-8 umlaut");
        assertEquals("forward_happy@realdomain.org", forwardedMsg.getRecipients(Message.RecipientType.TO)[0].toString());
        verify(mailboxRepository).incrementForwards(mbox.getId());
    }

    @Test
    @DisplayName("forward single-part text/html email lacking charset parameter appends charset=UTF-8 and ensures MIME-Version")
    public void testForwardSinglePartTextHtmlWithoutCharsetNormalizesContentType() throws Exception
    {
        final User user = new User("Html", "User", "forward_html@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("htmlbox", "xcmailr.test", 0L, false, user);
        mbox.setActive(true);
        mbox.setForwardEmails(true);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("htmlbox", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));
        when(outboundMailService.createSession())
            .thenReturn(Session.getInstance(JakartaMimeMessageParser.getDefaultSessionProperties()));

        final String rawEmail = "From: sender@external.com\r\n"
            + "To: htmlbox@xcmailr.test\r\n"
            + "Subject: HTML Status Report\r\n"
            + "Content-Type: text/html\r\n\r\n"
            + "<html><body><p>Status: OK üüü</p></body></html>";

        final byte[] rawBytes = rawEmail.getBytes(StandardCharsets.UTF_8);
        deliveryService.deliver("sender@external.com", "htmlbox@xcmailr.test", new ByteArrayInputStream(rawBytes));

        final ArgumentCaptor<MimeMessage> msgCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(outboundMailService).send(msgCaptor.capture());
        final MimeMessage forwardedMsg = msgCaptor.getValue();

        assertEquals("1.0", forwardedMsg.getHeader("MIME-Version", null), "Forwarded message must declare MIME-Version: 1.0");
        assertEquals("text/html; charset=UTF-8", forwardedMsg.getHeader("Content-Type", null),
                     "Single-part text/html lacking charset must append charset=UTF-8");
        verify(mailboxRepository).incrementForwards(mbox.getId());
    }

    // --- Error and Edge Case Tests ---

    @Test
    @DisplayName("forwarding is suppressed and no outbound email sent when recipient mailbox is inactive")
    public void testForwardingSuppressedWhenMailboxInactive() throws Exception
    {
        final User user = new User("Inactive", "User", "forward_target@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("inactiveforward", "xcmailr.test", 0L, false, user);
        mbox.setActive(false);
        mbox.setForwardEmails(true);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("inactiveforward", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));

        final String rawEmail = "From: sender@external.com\r\nTo: inactiveforward@xcmailr.test\r\nSubject: Test\r\n\r\nBody";
        deliveryService.deliver("sender@external.com", "inactiveforward@xcmailr.test",
                               new ByteArrayInputStream(rawEmail.getBytes(StandardCharsets.UTF_8)));

        // Verify outbound send was never called
        verify(outboundMailService, never()).send(any(MimeMessage.class));
        verify(mailboxRepository, never()).incrementForwards(anyLong());

        final ArgumentCaptor<MailTransaction> captor = ArgumentCaptor.forClass(MailTransaction.class);
        verify(mailTransactionRepository).save(captor.capture());
        assertEquals(200, captor.getValue().getStatus(), "Transaction status 200 (inactive mailbox) must be recorded");
    }

    @Test
    @DisplayName("forwarding is suppressed and no outbound email sent when recipient mailbox does not exist")
    public void testForwardingSuppressedWhenMailboxDoesNotExist() throws Exception
    {
        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("missingforward", "xcmailr.test"))
            .thenReturn(Optional.empty());

        final String rawEmail = "From: sender@external.com\r\nTo: missingforward@xcmailr.test\r\nSubject: Test\r\n\r\nBody";
        deliveryService.deliver("sender@external.com", "missingforward@xcmailr.test",
                               new ByteArrayInputStream(rawEmail.getBytes(StandardCharsets.UTF_8)));

        // Verify outbound send was never called
        verify(outboundMailService, never()).send(any(MimeMessage.class));
        verify(mailboxRepository, never()).incrementForwards(anyLong());

        final ArgumentCaptor<MailTransaction> captor = ArgumentCaptor.forClass(MailTransaction.class);
        verify(mailTransactionRepository).save(captor.capture());
        assertEquals(100, captor.getValue().getStatus(), "Transaction status 100 (mailbox not found) must be recorded");
    }

    @Test
    @DisplayName("forwarding handles minimal headers and empty body without errors")
    public void testForwardMinimalHeadersAndEmptyBody() throws Exception
    {
        final User user = new User("Minimal", "Forward", "forward_minimal@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("minimalforward", "xcmailr.test", 0L, false, user);
        mbox.setActive(true);
        mbox.setForwardEmails(true);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("minimalforward", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));
        when(outboundMailService.createSession())
            .thenReturn(Session.getInstance(JakartaMimeMessageParser.getDefaultSessionProperties()));

        final String rawEmail = "To: minimalforward@xcmailr.test\r\n\r\n";
        deliveryService.deliver("sender@external.com", "minimalforward@xcmailr.test",
                               new ByteArrayInputStream(rawEmail.getBytes(StandardCharsets.UTF_8)));

        final ArgumentCaptor<MimeMessage> msgCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(outboundMailService).send(msgCaptor.capture());
        final MimeMessage forwardedMsg = msgCaptor.getValue();

        assertEquals("1.0", forwardedMsg.getHeader("MIME-Version", null));
        assertEquals("text/plain; charset=UTF-8", forwardedMsg.getHeader("Content-Type", null));
        assertNull(forwardedMsg.getSubject());
        verify(mailboxRepository).incrementForwards(mbox.getId());
    }

    // --- Special Case Tests ---

    @Test
    @DisplayName("forwarding email with explicit ISO-8859-1 charset preserves Content-Type header untouched")
    public void testForwardExplicitIso88591CharsetPreservedUnmodified() throws Exception
    {
        final User user = new User("Iso", "User", "forward_iso@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("isobox", "xcmailr.test", 0L, false, user);
        mbox.setActive(true);
        mbox.setForwardEmails(true);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("isobox", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));
        when(outboundMailService.createSession())
            .thenReturn(Session.getInstance(JakartaMimeMessageParser.getDefaultSessionProperties()));

        final String originalContentType = "text/plain; charset=ISO-8859-1";
        final String rawEmail = "From: sender@external.com\r\n"
            + "To: isobox@xcmailr.test\r\n"
            + "Subject: ISO Message\r\n"
            + "Content-Type: " + originalContentType + "\r\n\r\n"
            + "Legacy body content";

        deliveryService.deliver("sender@external.com", "isobox@xcmailr.test",
                               new ByteArrayInputStream(rawEmail.getBytes(StandardCharsets.ISO_8859_1)));

        final ArgumentCaptor<MimeMessage> msgCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(outboundMailService).send(msgCaptor.capture());
        final MimeMessage forwardedMsg = msgCaptor.getValue();

        assertEquals("1.0", forwardedMsg.getHeader("MIME-Version", null));
        assertEquals(originalContentType, forwardedMsg.getHeader("Content-Type", null),
                     "Explicit ISO-8859-1 charset must be preserved without alteration");
        verify(mailboxRepository).incrementForwards(mbox.getId());
    }

    @Test
    @DisplayName("forwarding email with explicit UTF-8 charset preserves Content-Type header untouched")
    public void testForwardExplicitUtf8CharsetPreservedUnmodified() throws Exception
    {
        final User user = new User("Utf8", "User", "forward_utf8@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("utf8explicitbox", "xcmailr.test", 0L, false, user);
        mbox.setActive(true);
        mbox.setForwardEmails(true);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("utf8explicitbox", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));
        when(outboundMailService.createSession())
            .thenReturn(Session.getInstance(JakartaMimeMessageParser.getDefaultSessionProperties()));

        final String originalContentType = "text/plain; charset=UTF-8; format=flowed";
        final String rawEmail = "From: sender@external.com\r\n"
            + "To: utf8explicitbox@xcmailr.test\r\n"
            + "Subject: Explicit UTF-8 Message\r\n"
            + "Content-Type: " + originalContentType + "\r\n\r\n"
            + "UTF-8 body content üüü";

        deliveryService.deliver("sender@external.com", "utf8explicitbox@xcmailr.test",
                               new ByteArrayInputStream(rawEmail.getBytes(StandardCharsets.UTF_8)));

        final ArgumentCaptor<MimeMessage> msgCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(outboundMailService).send(msgCaptor.capture());
        final MimeMessage forwardedMsg = msgCaptor.getValue();

        assertEquals("1.0", forwardedMsg.getHeader("MIME-Version", null));
        assertEquals(originalContentType, forwardedMsg.getHeader("Content-Type", null),
                     "Explicit UTF-8 charset with parameters must be preserved untouched");
        verify(mailboxRepository).incrementForwards(mbox.getId());
    }

    @Test
    @DisplayName("forwarding multipart email preserves top-level multipart Content-Type header and boundaries unmodified")
    public void testForwardMultipartEmailPreservedUnmodified() throws Exception
    {
        final User user = new User("Multi", "User", "forward_multi@realdomain.org", "hash", "en");
        user.setActive(true);

        final MBox mbox = new MBox("multibox", "xcmailr.test", 0L, false, user);
        mbox.setActive(true);
        mbox.setForwardEmails(true);

        when(mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("multibox", "xcmailr.test"))
            .thenReturn(Optional.of(mbox));
        when(outboundMailService.createSession())
            .thenReturn(Session.getInstance(JakartaMimeMessageParser.getDefaultSessionProperties()));

        final String boundary = "----=_Part_123_456";
        final String originalContentType = "multipart/alternative; boundary=\"" + boundary + "\"";
        final String rawEmail = "From: sender@external.com\r\n"
            + "To: multibox@xcmailr.test\r\n"
            + "Subject: Multipart Message\r\n"
            + "MIME-Version: 1.0\r\n"
            + "Content-Type: " + originalContentType + "\r\n\r\n"
            + "--" + boundary + "\r\n"
            + "Content-Type: text/plain; charset=UTF-8\r\n\r\n"
            + "Plain text part üüü\r\n"
            + "--" + boundary + "\r\n"
            + "Content-Type: text/html; charset=UTF-8\r\n\r\n"
            + "<b>HTML part üüü</b>\r\n"
            + "--" + boundary + "--\r\n";

        deliveryService.deliver("sender@external.com", "multibox@xcmailr.test",
                               new ByteArrayInputStream(rawEmail.getBytes(StandardCharsets.UTF_8)));

        final ArgumentCaptor<MimeMessage> msgCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(outboundMailService).send(msgCaptor.capture());
        final MimeMessage forwardedMsg = msgCaptor.getValue();

        assertEquals("1.0", forwardedMsg.getHeader("MIME-Version", null));
        final String forwardedContentType = forwardedMsg.getHeader("Content-Type", null);
        assertNotNull(forwardedContentType);
        assertTrue(forwardedContentType.contains("multipart/alternative"), "Multipart subtype must be preserved");
        assertTrue(forwardedContentType.contains(boundary), "Boundary parameter must be preserved");
        assertFalse(forwardedContentType.toLowerCase().contains("charset="),
                    "Top-level multipart Content-Type must not have charset appended");
        verify(mailboxRepository).incrementForwards(mbox.getId());
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
