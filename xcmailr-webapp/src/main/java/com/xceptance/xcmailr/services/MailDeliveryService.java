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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;

import com.xceptance.xcmailr.config.XcmailrProperties;
import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.util.JakartaMimeMessageParser;

import etc.HelperUtils;
import etc.SizeLimitExceededException;
import models.MBox;
import models.Mail;
import models.MailTransaction;

/**
 * Message delivery handler implementing SubEthaSMTP's {@link SimpleMessageListener}.
 * <p>
 * Handles inbound SMTP RCPT validation (anti-relay protection), mailbox precondition checks,
 * message persistence into database storage, loop detection, and mail forwarding.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@Service
public class MailDeliveryService implements SimpleMessageListener
{
    private static final Logger LOG = LoggerFactory.getLogger(MailDeliveryService.class);

    private static final String LOOP_HEADER_NAME = "X-Loop";
    private static final String LOOP_HEADER_VALUE_PREFIX = "loopbreaker";

    private final XcmailrProperties properties;
    private final DomainRepository domainRepository;
    private final MailboxRepository mailboxRepository;
    private final MailRepository mailRepository;
    private final MailTransactionRepository mailTransactionRepository;
    private final OutboundMailService outboundMailService;

    /**
     * Constructs the mail delivery service with repositories and mail dispatch service.
     *
     * @param properties application properties
     * @param domainRepository repository for whitelisted domains
     * @param mailboxRepository repository for virtual mailboxes
     * @param mailRepository repository for persisted mail bodies
     * @param mailTransactionRepository repository for SMTP transaction audit logs
     * @param outboundMailService outbound mail forwarding service
     */
    public MailDeliveryService(final XcmailrProperties properties,
                               final DomainRepository domainRepository,
                               final MailboxRepository mailboxRepository,
                               final MailRepository mailRepository,
                               final MailTransactionRepository mailTransactionRepository,
                               final OutboundMailService outboundMailService)
    {
        this.properties = properties;
        this.domainRepository = domainRepository;
        this.mailboxRepository = mailboxRepository;
        this.mailRepository = mailRepository;
        this.mailTransactionRepository = mailTransactionRepository;
        this.outboundMailService = outboundMailService;
    }

    /**
     * Checks if the recipient's domain is managed by this XCMailr instance (anti-open-relay check).
     *
     * @param from envelope sender address
     * @param recipient envelope recipient address
     * @return {@code true} if recipient domain is handled; {@code false} to reject relay (SMTP 553)
     */
    @Override
    public boolean accept(final String from, final String recipient)
    {
        final String[] split = HelperUtils.splitMailAddress(recipient);
        if (split == null || split.length != 2)
        {
            LOG.warn("Relay rejected: malformed recipient address '{}'", recipient);
            recordTransaction(0, from, recipient, null);
            return false;
        }

        final String domain = split[1];
        if (!isDomainHandled(domain))
        {
            LOG.warn("Relay rejected: unmanaged domain '{}' from sender '{}' to recipient '{}'", domain, from, recipient);
            recordTransaction(500, from, recipient, null);
            return false;
        }

        return true;
    }

    /**
     * Processes incoming email data after sender and recipient have been accepted.
     *
     * @param from envelope sender address
     * @param recipient envelope recipient address
     * @param data email message input stream
     * @throws TooMuchDataException if data exceeds configured maximum size
     * @throws IOException on input stream reading errors
     */
    @Override
    @Transactional
    public void deliver(final String from, final String recipient, final InputStream data)
        throws TooMuchDataException, IOException
    {
        final String[] split = HelperUtils.splitMailAddress(recipient);
        if (split == null || split.length != 2)
        {
            recordTransaction(0, from, recipient, null);
            return;
        }

        // 1. Verify mailbox existence
        final Optional<MBox> optionalMbox = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(split[0], split[1]);
        if (optionalMbox.isEmpty())
        {
            LOG.info("Mailbox not found for recipient '{}'", recipient);
            recordTransaction(100, from, recipient, null);
            return;
        }

        final MBox mbox = optionalMbox.get();
        final String forwardTarget = (mbox.getUsr() != null) ? mbox.getUsr().getMail() : "";

        // 2. Check mailbox active status
        if (!mbox.isActive())
        {
            LOG.info("Mailbox '{}' is inactive / expired", recipient);
            recordTransaction(200, from, recipient, forwardTarget);
            mbox.increaseSuppressions();
            mailboxRepository.save(mbox);
            return;
        }

        // 3. Check owning user active status
        if (mbox.getUsr() == null || !mbox.getUsr().isActive())
        {
            LOG.info("Owning user for mailbox '{}' is inactive", recipient);
            recordTransaction(600, from, recipient, forwardTarget);
            mbox.increaseSuppressions();
            mailboxRepository.save(mbox);
            return;
        }

        // 4. Ingest raw message bytes respecting maximum configured size
        final byte[] rawBytes;
        try
        {
            rawBytes = HelperUtils.readLimitedAmount(data, properties.getMbox().getMaxSize());
        }
        catch (final SizeLimitExceededException e)
        {
            LOG.error("Dropped mail '{} => {}': size exceeded configured limit of {} bytes",
                      from, recipient, properties.getMbox().getMaxSize());
            throw new TooMuchDataException("Mail size exceeds configured limit");
        }

        // 5. Parse MIME message structure
        final MimeMessage mimeMessage;
        final String subject;
        final String originator;
        try
        {
            mimeMessage = JakartaMimeMessageParser.createMimeMessage(rawBytes);
            subject = (mimeMessage.getSubject() != null) ? mimeMessage.getSubject() : "";
            originator = (mimeMessage.getFrom() != null && mimeMessage.getFrom().length > 0)
                ? mimeMessage.getFrom()[0].toString()
                : from;
        }
        catch (final Exception e)
        {
            LOG.error("Failed to parse MIME message from " + from + " to " + recipient, e);
            return;
        }

        // 6. Persist email into database
        final Mail mail = new Mail();
        mail.setMailbox(mbox);
        mail.setSender(originator);
        mail.setSubject(subject);
        mail.setMessage(rawBytes);
        mail.setReceiveTime(System.currentTimeMillis());
        mail.setUuid(UUID.randomUUID().toString());
        mailRepository.save(mail);
        LOG.info("Persisted email '{}' for mailbox '{}' with UUID '{}'", subject, recipient, mail.getUuid());

        // 7. Check if forwarding is enabled for this mailbox
        if (!mbox.isForwardEmails())
        {
            LOG.debug("Mailbox '{}' is configured not to forward emails", recipient);
            return;
        }

        // 8. Loop prevention check
        final String loopError = checkForLoop(mimeMessage, recipient);
        if (loopError != null)
        {
            LOG.warn("Broke potential email loop for recipient '{}': {}", recipient, loopError);
            return;
        }

        // 9. Prepare forwarded message
        try
        {
            final MimeMessage forwardMsg = new MimeMessage(outboundMailService.createSession(),
                                                           new ByteArrayInputStream(rawBytes));
            forwardMsg.setRecipient(Message.RecipientType.TO, new InternetAddress(forwardTarget));
            forwardMsg.removeHeader("Cc");
            forwardMsg.removeHeader("Bcc");
            forwardMsg.setSender(new InternetAddress(recipient));
            forwardMsg.setFrom(new InternetAddress(recipient));
            forwardMsg.setReplyTo(InternetAddress.parse(from));
            forwardMsg.addHeader("X-FORWARDED-FROM", from);
            forwardMsg.addHeader(LOOP_HEADER_NAME, LOOP_HEADER_VALUE_PREFIX + recipient);
            forwardMsg.addHeader("Auto-Submitted", "auto-forwarded");

            // Dispatch outbound email
            outboundMailService.send(forwardMsg);

            // Update forward counters
            mbox.increaseForwards();
            mailboxRepository.save(mbox);
            recordTransaction(300, from, recipient, forwardTarget);
            LOG.info("Successfully forwarded email to '{}'", forwardTarget);
        }
        catch (final Exception e)
        {
            LOG.error("Failed to forward email from " + recipient + " to " + forwardTarget, e);
            recordTransaction(400, from, recipient, forwardTarget);
        }
    }

    /**
     * Checks whether a domain name is handled by this server.
     *
     * @param domain domain name to verify
     * @return true if domain is configured or registered in whitelist table
     */
    public boolean isDomainHandled(final String domain)
    {
        if (domain == null || domain.isBlank())
        {
            return false;
        }

        // Check configured static domains
        final List<String> configuredDomains = properties.getMbox().getDomainList();
        if (configuredDomains != null)
        {
            for (final String d : configuredDomains)
            {
                if (domain.equalsIgnoreCase(d))
                {
                    return true;
                }
            }
        }

        // Check database domain whitelist table
        return domainRepository.existsByDomainnameIgnoreCase(domain);
    }

    /**
     * Detects mail routing loops based on custom X-Loop headers and Message-ID references.
     *
     * @param mail MIME message to examine
     * @param recipient recipient address
     * @return error description if loop detected, or null if safe
     * @throws MessagingException if header inspection fails
     */
    private String checkForLoop(final MimeMessage mail, final String recipient)
    {
        try
        {
            // Check custom X-Loop header
            final String loopHeader = mail.getHeader(LOOP_HEADER_NAME, "###");
            if (loopHeader != null)
            {
                final String expectedContent = (LOOP_HEADER_VALUE_PREFIX + recipient).toLowerCase();
                if (loopHeader.toLowerCase().contains(expectedContent))
                {
                    return "X-Loop header with this email address present";
                }
            }

            // Determine domain from message ID
            final String messageId = mail.getMessageID();
            if (messageId != null)
            {
                final String[] splitMsgId = HelperUtils.splitMailAddress(messageId);
                if (splitMsgId != null && splitMsgId.length > 1)
                {
                    final String domain = splitMsgId[1];

                    final String references = mail.getHeader("References", "###");
                    if (references != null && StringUtils.containsIgnoreCase(references, "@" + domain))
                    {
                        return "References header references domain of this address: " + domain;
                    }

                    final String inReplyTo = mail.getHeader("In-Reply-To", "###");
                    if (inReplyTo != null && StringUtils.containsIgnoreCase(inReplyTo, "@" + domain))
                    {
                        return "In-Reply-To header references domain of this address: " + domain;
                    }
                }
            }
        }
        catch (final MessagingException e)
        {
            LOG.warn("Failed to check for mail loop", e);
        }

        return null;
    }

    /**
     * Records an SMTP transaction log entry if MTX logging is active.
     *
     * @param status transaction status code
     * @param source sender address
     * @param relay relay recipient address
     * @param target forwarding target address
     */
    private void recordTransaction(final int status, final String source, final String relay, final String target)
    {
        if (properties.getMailTransaction().getMaxAgeHours() != 0)
        {
            final MailTransaction mtx = new MailTransaction(status, source, relay, target);
            mailTransactionRepository.save(mtx);
        }
    }
}
