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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import conf.XCMailrConf;
import models.MailTransaction;
import models.User;
import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;
import ninja.utils.NinjaProperties;

/**
 * Service responsible for handling of e-mails (sending, forwarding, expiring).
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Singleton
public class MailService
{
    private final ScheduledExecutorService expirationService = Executors.newSingleThreadScheduledExecutor();

    private SMTPServer smtpServer;

    private SMTPServer smtpServer2;

    @Inject
    Logger log;

    @Inject
    NinjaProperties ninjaProperties;

    @Inject
    XCMailrConf xcmConfiguration;

    @Inject
    MessageListener messageListener;

    private boolean deleteTransactions;

    ConcurrentLinkedQueue<MailTransaction> mtxQueue = new ConcurrentLinkedQueue<MailTransaction>();

    /**
     * Starts the mail-server, creates the Admin-Account specified in application.conf and threads to expire the
     * mail-addresses
     */
    @Start(order = 90)
    public void startActions()
    {
        // only delete transactions if mailtransaction.maxage is not -1 or 0
        deleteTransactions = (xcmConfiguration.MTX_MAX_AGE != 0 && xcmConfiguration.MTX_MAX_AGE != -1);

        if (xcmConfiguration.ADMIN_PASSWORD != null && !User.mailExists(xcmConfiguration.ADMIN_ADDRESS))
        { // if a password is set in application.conf ...and the admin-account doesn't exist
          // create the admin-account
            User user = new User("Site", "Admin", xcmConfiguration.ADMIN_ADDRESS, xcmConfiguration.ADMIN_PASSWORD,
                                 "en");

            // set the status and admin flags
            user.setAdmin(true);
            user.setActive(true);
            user.save();

        }
        // create and start the main SMTP server for incoming mails
        // use a dynamic port in test-mode or the port specified in application.conf in all other modes
        int port = ninjaProperties.isTest() ? findAvailablePort(49152, 65535) : xcmConfiguration.MB_PORT;
        smtpServer = createSmtpServer(port);
        smtpServer.start();

        // create and start the alternative SMTP server for incoming mails
        if (xcmConfiguration.MB_PORT2 != null)
        {
            // use a dynamic port in test-mode or the port specified in application.conf in all other modes
            int alternativePort = ninjaProperties.isTest() ? findAvailablePort(49152, 65535)
                                                           : xcmConfiguration.MB_PORT2;

            smtpServer2 = createSmtpServer(alternativePort);
            smtpServer2.start();
        }

        // create the executor-service to check the mail-addresses which were expired since the last run and disable
        // them and also all new MailTransactions will be stored here and old entries will be removed
        expirationService.scheduleAtFixedRate(new ExpirationService(mtxQueue, deleteTransactions, xcmConfiguration), 0L,
                                              xcmConfiguration.MB_INTERVAL.longValue(), TimeUnit.MINUTES);
    }

    /**
     * Stops the Threads and the SMTP-Server
     */
    @Dispose(order = 90)
    public void stopActions()
    {
        // stop the main SMTP server if present
        if (smtpServer != null)
        {
            smtpServer.stop();
            smtpServer = null;
        }

        // stop the alternative SMTP server if present
        if (smtpServer2 != null)
        {
            smtpServer2.stop();
            smtpServer2 = null;
        }

        // stop the job to expire the mailboxes
        expirationService.shutdown();
    }

    /**
     * Creates the {@link SMTPServer} instance responsible for incoming mails and sets it up.
     * 
     * @return the configured SMTP server
     */
    private SMTPServer createSmtpServer(int port)
    {
        SMTPServer smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(messageListener));

        smtpServer.setPort(port);

        // configure TLS support
        smtpServer.setEnableTLS(xcmConfiguration.MB_ENABLE_TLS);
        smtpServer.setRequireTLS(xcmConfiguration.MB_REQUIRE_TLS);

        return smtpServer;
    }

    /**
     * Finds an available Port in the Range of "min" and "max". Code taken from NinjaTestServer.
     * 
     * @param min
     *            lower bound of ports to search
     * @param max
     *            upper bound of ports to search
     * @return an available port
     */
    // from NinjaTestServer-source
    private static int findAvailablePort(int min, int max)
    {
        for (int port = min; port < max; port++)
        {
            try
            { // try to create a now port at "port"
              // if there's no exception, return it
                new ServerSocket(port).close();
                return port;
            }
            catch (IOException e)
            {
                // Must already be taken
            }
        }
        throw new IllegalStateException("Could not find available port in range " + min + " to " + max);
    }
}
