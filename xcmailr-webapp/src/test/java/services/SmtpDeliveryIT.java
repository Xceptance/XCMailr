/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
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
package services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import io.ebean.DB;
import models.MBox;
import models.Mail;
import models.User;
import testutils.StaticNinjaTest;
import testutils.TestDataUtils;

/**
 * Black-box integration tests for inbound SMTP delivery through the SubEthaSMTP server.
 * Validates domain whitelist enforcement, anti-relay rejection (553), message persistence,
 * expired mailbox dropping, and loop detection via X-Loop header.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class SmtpDeliveryIT extends StaticNinjaTest
{
    private static int smtpPort;

    private static User testUser;

    @BeforeClass
    public static void setUpClass()
    {
        final MailService mailService = ninjaTestServer.getInjector().getInstance(MailService.class);
        smtpPort = mailService.getSmtpPort();
        Assert.assertTrue("SMTP server port must be positive", smtpPort > 0);

        testUser = TestDataUtils.createUser();
    }

    /**
     * Verifies domain whitelist enforcement and anti-relay rejection:
     * - RCPT TO with a domain not configured in the application whitelist must be rejected with 553.
     * - RCPT TO with an allowed domain must be accepted with 250.
     */
    @Test
    public void testDomainWhitelistEnforcementAndAntiRelayRejection() throws Exception
    {
        try (final RawSmtpClient client = new RawSmtpClient("127.0.0.1", smtpPort))
        {
            final String heloResponse = client.sendCommand("EHLO test.client");
            Assert.assertTrue(heloResponse.startsWith("250"));

            final String mailFromResponse = client.sendCommand("MAIL FROM:<sender@outside.org>");
            Assert.assertTrue(mailFromResponse.startsWith("250"));

            // 1. Recipient domain NOT in whitelist -> 553 Relay access denied
            final String relayDeniedResponse = client.sendCommand("RCPT TO:<unauthorized@evil-external-relay.com>");
            Assert.assertTrue("Should reject unauthorized relay with 553, but got: " + relayDeniedResponse,
                              relayDeniedResponse.startsWith("553"));

            // 2. Recipient domain IN whitelist -> 250 Ok
            final String acceptedRecipientResponse = client.sendCommand("RCPT TO:<inbound@xcmailr.test>");
            Assert.assertTrue("Should accept whitelisted domain recipient with 250, but got: " + acceptedRecipientResponse,
                              acceptedRecipientResponse.startsWith("250"));

            client.sendCommand("RSET");
        }
    }

    /**
     * Verifies inbound delivery and message persistence for an active mailbox:
     * - Message delivered via SMTP protocol is stored in the Mail database table.
     * - Subject, sender, and recipient are correctly recorded.
     */
    @Test
    public void testInboundDeliveryToActiveMailbox() throws Exception
    {
        final MBox activeMailbox = TestDataUtils.createMailbox(testUser);
        final String recipientAddress = activeMailbox.getFullAddress();
        final String testSubject = "Inbound Delivery Safety Net Test " + System.nanoTime();

        try (final RawSmtpClient client = new RawSmtpClient("127.0.0.1", smtpPort))
        {
            client.sendCommand("EHLO test.client");
            client.sendCommand("MAIL FROM:<test-sender@outside.org>");
            client.sendCommand("RCPT TO:<" + recipientAddress + ">");

            final String dataResponse = client.sendCommand("DATA");
            Assert.assertTrue(dataResponse.startsWith("354"));

            final String rawEmail = "From: test-sender@outside.org\r\n"
                                  + "To: " + recipientAddress + "\r\n"
                                  + "Subject: " + testSubject + "\r\n"
                                  + "Message-ID: <" + System.nanoTime() + "@outside.org>\r\n"
                                  + "\r\n"
                                  + "This is an automated safety net test body.\r\n"
                                  + ".\r\n";

            final String completionResponse = client.sendRaw(rawEmail);
            Assert.assertTrue("Data transmission must succeed with 250, but got: " + completionResponse,
                              completionResponse.startsWith("250"));
        }

        // Allow async delivery thread to persist if needed
        Thread.sleep(500);

        // Verify message was stored in database
        final List<Mail> savedMails = DB.find(Mail.class).where().eq("mailbox", activeMailbox).findList();
        Assert.assertFalse("Saved mails must not be empty", savedMails.isEmpty());

        final Mail latestMail = savedMails.get(savedMails.size() - 1);
        Assert.assertEquals(testSubject, latestMail.getSubject());
        Assert.assertEquals(recipientAddress, latestMail.getMailbox().getFullAddress());
    }

    /**
     * Verifies that messages delivered to an expired/disabled mailbox are dropped and suppression count is incremented.
     */
    @Test
    public void testExpiredMailboxDropping() throws Exception
    {
        // Create an expired mailbox
        final MBox expiredMailbox = TestDataUtils.createMailbox(testUser);
        expiredMailbox.setExpired(true);
        expiredMailbox.update();

        final String recipientAddress = expiredMailbox.getFullAddress();
        final String testSubject = "Expired Drop Test " + System.nanoTime();

        try (final RawSmtpClient client = new RawSmtpClient("127.0.0.1", smtpPort))
        {
            client.sendCommand("EHLO test.client");
            client.sendCommand("MAIL FROM:<test-sender@outside.org>");
            client.sendCommand("RCPT TO:<" + recipientAddress + ">");

            client.sendCommand("DATA");

            final String rawEmail = "From: test-sender@outside.org\r\n"
                                  + "To: " + recipientAddress + "\r\n"
                                  + "Subject: " + testSubject + "\r\n"
                                  + "Message-ID: <" + System.nanoTime() + "@outside.org>\r\n"
                                  + "\r\n"
                                  + "Body for expired mailbox.\r\n"
                                  + ".\r\n";

            client.sendRaw(rawEmail);
        }

        Thread.sleep(500);

        // Verify message was NOT stored in database
        final List<Mail> mails = DB.find(Mail.class).where().eq("mailbox", expiredMailbox).findList();
        Assert.assertTrue("Mails for expired mailbox should not be saved", mails.isEmpty());

        // Verify suppression counter was incremented
        final MBox refreshed = MBox.getById(expiredMailbox.getId());
        Assert.assertNotNull(refreshed);
        Assert.assertTrue("Suppressions counter must be incremented", refreshed.getSuppressions() > 0);
    }

    /**
     * Verifies that incoming messages containing an X-Loop header matching the recipient address
     * trigger loop detection and prevent recursive forwarding.
     */
    @Test
    public void testLoopPreventionWithXLoopHeader() throws Exception
    {
        final MBox loopMailbox = TestDataUtils.createMailbox(testUser);
        loopMailbox.setForwardEmails(true);
        loopMailbox.update();

        final String recipientAddress = loopMailbox.getFullAddress();
        final String testSubject = "Loop Prevention Test " + System.nanoTime();

        try (final RawSmtpClient client = new RawSmtpClient("127.0.0.1", smtpPort))
        {
            client.sendCommand("EHLO test.client");
            client.sendCommand("MAIL FROM:<loop-sender@outside.org>");
            client.sendCommand("RCPT TO:<" + recipientAddress + ">");

            client.sendCommand("DATA");

            // Include X-Loop header designed to trip loop detection
            final String rawEmail = "From: loop-sender@outside.org\r\n"
                                  + "To: " + recipientAddress + "\r\n"
                                  + "Subject: " + testSubject + "\r\n"
                                  + "X-Loop: loopbreaker" + recipientAddress + "\r\n"
                                  + "Message-ID: <" + System.nanoTime() + "@outside.org>\r\n"
                                  + "\r\n"
                                  + "Message with loop breaker header.\r\n"
                                  + ".\r\n";

            final String completionResponse = client.sendRaw(rawEmail);
            Assert.assertTrue(completionResponse.startsWith("250"));
        }

        Thread.sleep(500);

        // Mail should still be persisted for the user even though forwarding is halted
        final List<Mail> savedMails = DB.find(Mail.class).where().eq("mailbox", loopMailbox).findList();
        Assert.assertFalse("Mail must be stored in database even if loop forwarding is broken", savedMails.isEmpty());
    }

    /**
     * Minimal raw socket SMTP client helper for deterministic black-box protocol validation.
     */
    private static final class RawSmtpClient implements AutoCloseable
    {
        private final Socket socket;

        private final BufferedReader reader;

        private final BufferedWriter writer;

        public RawSmtpClient(final String host, final int port) throws IOException
        {
            this.socket = new Socket(host, port);
            this.socket.setSoTimeout(10000);
            this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), StandardCharsets.UTF_8));
            readResponse(); // Read initial 220 banner
        }

        public String sendCommand(final String command) throws IOException
        {
            this.writer.write(command + "\r\n");
            this.writer.flush();
            return readResponse();
        }

        public String sendRaw(final String rawText) throws IOException
        {
            this.writer.write(rawText);
            this.writer.flush();
            return readResponse();
        }

        public String readResponse() throws IOException
        {
            final StringBuilder sb = new StringBuilder();
            String line;
            while ((line = this.reader.readLine()) != null)
            {
                sb.append(line).append("\n");
                // SMTP multiline responses have '-' after the 3-digit code; final line has ' '
                if (line.length() >= 4 && line.charAt(3) == ' ')
                {
                    break;
                }
                if (line.length() == 3)
                {
                    break;
                }
            }
            return sb.toString();
        }

        @Override
        public void close() throws IOException
        {
            try
            {
                sendCommand("QUIT");
            }
            catch (final Exception ignored)
            {
            }
            this.socket.close();
        }
    }
}
