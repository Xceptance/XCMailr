package com.xceptance.xcmailr.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLContext;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
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
import org.subethamail.smtp.server.SMTPServer;

import com.xceptance.xcmailr.config.XcmailrProperties;
import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.Domain;
import models.MBox;
import models.Mail;
import models.User;

/**
 * Integration tests verifying inbound SMTP STARTTLS negotiation, opportunistic plaintext
 * backwards compatibility, and mandatory TLS enforcement with SubEthaSMTP.
 * <p>
 * Follows the integration testing tier of the Test Pyramid:
 * <ul>
 *   <li>Happy path: Client issues STARTTLS, completes TLS handshake, and sends encrypted email.</li>
 *   <li>Special case (Opportunistic Plaintext Fallback): Client connects without STARTTLS under default
 *       opportunistic TLS ({@code require-tls=false}) and delivers successfully in plaintext.</li>
 *   <li>Error / Special case (Mandatory TLS): Server configured with {@code require-tls=true} rejects unencrypted
 *       mail commands with SMTP 530, while accepting TLS-upgraded connections.</li>
 * </ul>
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
public class SmtpStartTlsIT
{
    @Autowired
    private SmtpServerService smtpServerService;

    @Autowired
    private MailDeliveryService mailDeliveryService;

    @Autowired
    private SmtpSslContextFactory smtpSslContextFactory;

    @Autowired
    private XcmailrProperties properties;

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
    private MBox testMailbox;

    /**
     * Initializes a clean database state with an active user, whitelisted domain,
     * and active target mailbox prior to each integration test.
     */
    @BeforeEach
    public void setUp()
    {
        mailRepository.deleteAll();
        mailTransactionRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();

        // Ensure whitelisted domain exists
        final Domain domain = new Domain("xcmailr.test");
        domainRepository.save(domain);

        // Create active test user
        testUser = new User("Tls", "Tester", "tlstester@domain.test", "password", "en");
        testUser.setActive(true);
        userRepository.save(testUser);

        // Create active mailbox
        testMailbox = new MBox("tlsbox", "xcmailr.test", 0L, false, testUser);
        testMailbox.setActive(true);
        testMailbox.setForwardEmails(false);
        mailboxRepository.save(testMailbox);
    }

    /**
     * Happy Path: Verifies that an SMTP client can negotiate STARTTLS with the server,
     * complete the TLS handshake using the configured keystore certificate, and deliver
     * an encrypted email message.
     */
    @Test
    @DisplayName("Happy Path: Successfully negotiate STARTTLS and deliver encrypted email")
    public void testSuccessfulStartTlsDelivery() throws Exception
    {
        final int port = smtpServerService.getPort();
        assertTrue(port > 0, "Inbound SMTP server must be running on a bound port");

        final String subject = "STARTTLS Encrypted Message";
        final String body = "This is a secure email sent over STARTTLS.";

        // Send email with STARTTLS explicitly enabled and required
        sendSmtpEmail(port, true, true, "sender@xcmailr.test", "tlsbox@xcmailr.test", subject, body);

        // Verify that the email was successfully received and stored in the database
        final List<Mail> mails = mailRepository.findByMailboxOrderByReceiveTimeAsc(testMailbox.getId());
        assertFalse(mails.isEmpty(), "Email sent via STARTTLS must be persisted");
        assertEquals(1, mails.size(), "Exactly one email should have been delivered");

        final Mail deliveredMail = mails.get(0);
        assertEquals(subject, deliveredMail.getSubject(), "Delivered email subject must match");
        assertEquals("sender@xcmailr.test", deliveredMail.getSender(), "Delivered email sender must match");
    }

    /**
     * Special Case (Opportunistic Plaintext Fallback): Verifies that when {@code require-tls=false}
     * (the default opportunistic TLS configuration), clients without STARTTLS support can still connect
     * and deliver emails directly in plaintext, maintaining 100% backward compatibility for legacy senders.
     */
    @Test
    @DisplayName("Special Case: Deliver plaintext email when require-tls is false (opportunistic fallback)")
    public void testOpportunisticPlaintextDeliveryWhenTlsNotRequired() throws Exception
    {
        final int port = smtpServerService.getPort();
        assertTrue(port > 0, "Inbound SMTP server must be running on a bound port");

        final String subject = "Plaintext Backward Compatibility Message";
        final String body = "This is an unencrypted email sent without STARTTLS.";

        // Send email with STARTTLS disabled (pure plaintext)
        sendSmtpEmail(port, false, false, "sender@xcmailr.test", "tlsbox@xcmailr.test", subject, body);

        // Verify that the plaintext email was accepted and stored
        final List<Mail> mails = mailRepository.findByMailboxOrderByReceiveTimeAsc(testMailbox.getId());
        assertFalse(mails.isEmpty(), "Email sent in plaintext must be persisted under opportunistic TLS");
        assertEquals(1, mails.size(), "Exactly one email should have been delivered");

        final Mail deliveredMail = mails.get(0);
        assertEquals(subject, deliveredMail.getSubject(), "Delivered email subject must match");
    }

    /**
     * Error / Special Case (Mandatory TLS): Verifies that when {@code require-tls=true},
     * unencrypted SMTP transaction commands are rejected with SMTP 530 (Must issue a STARTTLS command first),
     * while clients that negotiate STARTTLS are accepted.
     */
    @Test
    @DisplayName("Error/Special Case: Mandatory TLS rejects plaintext with SMTP 530 and accepts STARTTLS")
    public void testMandatoryTlsRejectionAndAcceptance() throws Exception
    {
        // Obtain initialized SSLContext for the test server
        final SSLContext sslContext = smtpSslContextFactory.createSslContext(properties.getMbox());
        assertNotNull(sslContext, "SSLContext must be created for mandatory TLS test server");

        // Create an ephemeral SMTPServer with requireTLS=true
        final SMTPServer mandatoryTlsServer = SMTPServer.port(0)
                                                        .simpleMessageListener(mailDeliveryService)
                                                        .executorService(Executors.newVirtualThreadPerTaskExecutor())
                                                        .enableTLS(true)
                                                        .requireTLS(true)
                                                        .startTlsSocketFactory(sslContext)
                                                        .softwareName("XCMailr-MandatoryTLS")
                                                        .build();
        mandatoryTlsServer.start();

        try
        {
            final int mandatoryPort = mandatoryTlsServer.getPortAllocated();
            assertTrue(mandatoryPort > 0, "Mandatory TLS server must be listening on an ephemeral port");

            // 1. Unencrypted client attempt without STARTTLS must fail with SMTP 530
            final Exception exception = assertThrows(MessagingException.class, () -> {
                sendSmtpEmail(mandatoryPort, false, false, "sender@xcmailr.test", "tlsbox@xcmailr.test",
                              "Plaintext message", "Should be rejected with 530");
            }, "Expected exception when sending unencrypted email to mandatory TLS server");

            final String errorMessage = exception.getMessage();
            assertTrue(
                errorMessage.contains("530") || errorMessage.toLowerCase().contains("starttls"),
                "Error message should indicate STARTTLS requirement (530), got: " + errorMessage
            );

            // 2. Encrypted client attempt with STARTTLS must succeed on the mandatory TLS server
            sendSmtpEmail(mandatoryPort, true, true, "sender@xcmailr.test", "tlsbox@xcmailr.test",
                          "Encrypted Mandatory Message", "Encrypted message over mandatory TLS");

            final List<Mail> mails = mailRepository.findByMailboxOrderByReceiveTimeAsc(testMailbox.getId());
            assertFalse(mails.isEmpty(), "Email sent via STARTTLS to mandatory TLS server must be persisted");
            assertEquals(1, mails.size(), "Exactly one email should have been delivered");
        }
        finally
        {
            mandatoryTlsServer.stop();
        }
    }

    /**
     * Helper method to send an email via raw SMTP to the specified port,
     * optionally enabling and requiring STARTTLS.
     *
     * @param port the local SMTP port to connect to
     * @param startTlsEnable whether to advertise and attempt STARTTLS
     * @param startTlsRequired whether to fail if STARTTLS is not available
     * @param from sender email address
     * @param to recipient email address
     * @param subject email subject
     * @param body email body text
     * @throws MessagingException if an SMTP error or connection failure occurs
     */
    private void sendSmtpEmail(final int port,
                               final boolean startTlsEnable,
                               final boolean startTlsRequired,
                               final String from,
                               final String to,
                               final String subject,
                               final String body)
        throws MessagingException
    {
        final Properties props = new Properties();
        props.put("mail.smtp.host", "127.0.0.1");
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");

        if (startTlsEnable)
        {
            props.put("mail.smtp.starttls.enable", "true");
            if (startTlsRequired)
            {
                props.put("mail.smtp.starttls.required", "true");
            }
            // Trust self-signed development certificate for testing
            props.put("mail.smtp.ssl.trust", "*");
        }
        else
        {
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.starttls.required", "false");
        }

        final Session session = Session.getInstance(props);
        final MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }
}
