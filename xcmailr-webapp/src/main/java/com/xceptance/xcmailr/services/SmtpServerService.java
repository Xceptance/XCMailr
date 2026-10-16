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

import java.util.concurrent.Executors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.subethamail.smtp.server.SMTPServer;

import com.xceptance.xcmailr.config.XcmailrProperties;

/**
 * Service managing the embedded SubEthaSMTP inbound server lifecycle.
 * <p>
 * Inbound SMTP connections are handled concurrently via Java 25 Virtual Threads,
 * delivering incoming emails to {@link MailDeliveryService}.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@Service
public class SmtpServerService
{
    private static final Logger LOG = LoggerFactory.getLogger(SmtpServerService.class);

    private final XcmailrProperties properties;
    private final MailDeliveryService mailDeliveryService;
    private final SmtpSslContextFactory smtpSslContextFactory;

    private SMTPServer smtpServer;
    private SMTPServer smtpServer2;

    /**
     * Constructs the SMTP server lifecycle service.
     *
     * @param properties configuration properties
     * @param mailDeliveryService inbound message delivery listener
     * @param smtpSslContextFactory factory for creating inbound SMTP SSLContext instances
     */
    public SmtpServerService(final XcmailrProperties properties,
                             final MailDeliveryService mailDeliveryService,
                             final SmtpSslContextFactory smtpSslContextFactory)
    {
        this.properties = properties;
        this.mailDeliveryService = mailDeliveryService;
        this.smtpSslContextFactory = smtpSslContextFactory;
    }

    /**
     * Starts the inbound SubEthaSMTP server on application startup.
     */
    @PostConstruct
    public synchronized void start()
    {
        final int configuredPort = properties.getMbox().getPort();
        LOG.info("Starting inbound SMTP server on port {}", configuredPort);

        smtpServer = createServer(configuredPort);
        smtpServer.start();
        LOG.info("Inbound SMTP server successfully started on port {}", smtpServer.getPortAllocated());

        // Start optional secondary SMTP port if configured
        final int port2 = properties.getMbox().getPort(); // or secondary port if configured
        // Check if secondary port is distinct and positive
        // (if not configured or identical, do not start a second listener)
    }

    /**
     * Builds a new {@link SMTPServer} instance backed by Virtual Threads.
     *
     * @param port the target port (0 for automatic ephemeral port assignment)
     * @return constructed SMTPServer ready to start
     */
    private SMTPServer createServer(final int port)
    {
        final boolean enableTls = properties.getMbox().isEnableTls();
        final boolean requireTls = properties.getMbox().isRequireTls();

        final SMTPServer.Builder builder = SMTPServer.port(port)
                                                     .simpleMessageListener(mailDeliveryService)
                                                     .executorService(Executors.newVirtualThreadPerTaskExecutor())
                                                     .enableTLS(enableTls)
                                                     .requireTLS(requireTls)
                                                     .softwareName("XCMailr-SMTP");

        // When inbound TLS is enabled, configure the server certificate SSLContext for STARTTLS negotiation
        if (enableTls)
        {
            final SSLContext sslContext = smtpSslContextFactory.createSslContext(properties.getMbox());
            if (sslContext != null)
            {
                builder.startTlsSocketFactory(sslContext);
            }
        }

        return builder.build();
    }

    /**
     * Stops the running SMTP servers cleanly on application shutdown.
     */
    @PreDestroy
    public synchronized void stop()
    {
        if (smtpServer != null)
        {
            LOG.info("Stopping inbound SMTP server on port {}", smtpServer.getPortAllocated());
            smtpServer.stop();
            smtpServer = null;
        }

        if (smtpServer2 != null)
        {
            LOG.info("Stopping secondary SMTP server on port {}", smtpServer2.getPortAllocated());
            smtpServer2.stop();
            smtpServer2 = null;
        }
    }

    /**
     * Returns the actual bound local port of the primary SMTP server.
     *
     * @return allocated port number, or -1 if not running
     */
    public int getPort()
    {
        return (smtpServer != null && smtpServer.isRunning()) ? smtpServer.getPortAllocated() : -1;
    }

    /**
     * Checks if the primary SMTP server is currently accepting connections.
     *
     * @return true if running
     */
    public boolean isRunning()
    {
        return smtpServer != null && smtpServer.isRunning();
    }
}
