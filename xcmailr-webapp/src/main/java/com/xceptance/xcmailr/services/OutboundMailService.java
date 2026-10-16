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

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.xceptance.xcmailr.config.XcmailrProperties;

/**
 * Service responsible for outbound email delivery (account confirmation, password reset,
 * and forwarded mailbox emails) via Jakarta Mail and virtual threads.
 *
 * @author Xceptance Software Technologies GmbH
 */
@Service
public class OutboundMailService
{
    private static final Logger LOG = LoggerFactory.getLogger(OutboundMailService.class);

    private final XcmailrProperties properties;

    /**
     * Virtual thread executor for asynchronous outbound mail dispatching.
     */
    private final ExecutorService outboundExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Constructs the outbound mail service with strong configuration properties.
     *
     * @param properties application configuration
     */
    public OutboundMailService(final XcmailrProperties properties)
    {
        this.properties = properties;
    }

    /**
     * Creates a new Jakarta Mail {@link Session} configured with outbound SMTP settings.
     *
     * @return configured mail session
     */
    public Session createSession()
    {
        final Properties props = new Properties();
        final XcmailrProperties.OutboundSmtpProperties smtp = properties.getOutboundSmtp();

        final String host = (smtp != null && smtp.getHost() != null && !smtp.getHost().isBlank())
            ? smtp.getHost()
            : "localhost";
        final int port = (smtp != null && smtp.getPort() > 0) ? smtp.getPort() : 25;

        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", String.valueOf(smtp != null && smtp.getUser() != null && !smtp.getUser().isBlank()));
        props.put("mail.smtp.starttls.enable", String.valueOf(smtp != null && smtp.isTls()));
        props.put("mail.smtp.ssl.enable", String.valueOf(smtp != null && smtp.isSsl()));

        final Authenticator authenticator;
        if (smtp.getUser() != null && !smtp.getUser().isBlank())
        {
            authenticator = new Authenticator()
            {
                @Override
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    return new PasswordAuthentication(smtp.getUser(), smtp.getPassword());
                }
            };
        }
        else
        {
            authenticator = null;
        }

        return Session.getInstance(props, authenticator);
    }

    /**
     * Sends an outbound email synchronously.
     * If outbound delivery is disabled (e.g. during test executions without a live outbound SMTP host),
     * sending is safely logged and skipped.
     *
     * @param message the prepared MIME message to send
     * @throws MessagingException if outbound transport fails
     */
    public void send(final MimeMessage message) throws MessagingException
    {
        final XcmailrProperties.OutboundSmtpProperties smtp = properties.getOutboundSmtp();
        // In local test runs without a configured live relay, skip actual socket connection
        if (smtp == null || !smtp.isEnabled())
        {
            LOG.debug("Outbound SMTP delivery is disabled in current environment; skipping Transport.send");
            return;
        }

        LOG.info("Sending outbound email to: {}", (Object) message.getAllRecipients());
        Transport.send(message);
    }

    /**
     * Sends an outbound email asynchronously using a Java 25 virtual thread.
     *
     * @param message the prepared MIME message to send
     * @param onSuccess callback runnable invoked on successful dispatch
     * @param onError callback consumer invoked if an exception occurs
     */
    public void sendAsync(final MimeMessage message, final Runnable onSuccess,
                          final java.util.function.Consumer<Exception> onError)
    {
        outboundExecutor.submit(() -> {
            try
            {
                send(message);
                if (onSuccess != null)
                {
                    onSuccess.run();
                }
            }
            catch (final Exception e)
            {
                LOG.error("Failed to asynchronously deliver outbound email", e);
                if (onError != null)
                {
                    onError.accept(e);
                }
            }
        });
    }

    /**
     * Sends a simple text email asynchronously using virtual threads.
     *
     * @param to recipient email address
     * @param subject email subject
     * @param body email body text
     */
    public void sendAsync(final String to, final String subject, final String body)
    {
        outboundExecutor.submit(() -> {
            try
            {
                final Session session = createSession();
                final MimeMessage msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(properties.getAdmin().getAddress()));
                msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
                msg.setSubject(subject, "UTF-8");
                msg.setText(body, "UTF-8");
                msg.setSentDate(new java.util.Date());
                send(msg);
            }
            catch (final Exception e)
            {
                LOG.error("Failed to send outbound email to {}: {}", to, e.getMessage(), e);
            }
        });
    }

    /**
     * Shuts down the asynchronous virtual thread executor on context tear down.
     */
    @PreDestroy
    public void shutdown()
    {
        outboundExecutor.shutdown();
    }
}
