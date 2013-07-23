/**  
 *  Copyright 2013 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package controllers;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;
import models.MBox;
import models.MailTransaction;
import models.User;
import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;
import ninja.utils.NinjaProperties;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import conf.XCMailrConf;

/**
 * Handles the Jobs which will be executed on Start and Stop of the Application
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Singleton
public class JobController
{
    private final ScheduledExecutorService expirationService = Executors.newSingleThreadScheduledExecutor();

    private SMTPServer smtpServer;

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
        log.debug("prod:" + ninjaProperties.isProd() + " dev: " + ninjaProperties.isDev() + " test: "
                  + ninjaProperties.isTest());

        // only delete transactions if mailtransaction.maxage is not -1 or 0
        deleteTransactions = (xcmConfiguration.MTX_MAX_AGE != 0 && xcmConfiguration.MTX_MAX_AGE != -1);

        if (xcmConfiguration.ADMIN_PASSWORD != null)
        { // if a password is set in application.conf..

            if (!User.mailExists(xcmConfiguration.ADMIN_ADDRESS))
            {// ...and the admin-account doesn't exist
             // create the admin-account
                User user = new User("Site", "Admin", xcmConfiguration.ADMIN_ADDRESS, xcmConfiguration.ADMIN_PASSWORD,
                                     "en");

                // set the status and admin flags
                user.setAdmin(true);
                user.setActive(true);
                user.save();
            }
        }
        // create the server for incoming mails
        smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(messageListener));

        // use a dynamic port for the smtp in test-mode or the port specified in application.conf in all other modes
        int port = ninjaProperties.isTest() ? findAvailablePort(49152, 65535) : xcmConfiguration.MB_PORT;

        // set the port and start the SMTP-Server
        smtpServer.setPort(port);
        smtpServer.start();

        // create the executor-service to check the mail-addresses which were expired since the last run and disable
        // them
        // and also all new MailTransactions will be stored here and old entries will be removed
        expirationService.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                log.debug("Emailaddress Expiration Task run");

                // get the number of MBox-Elements that will expire in the next "MB_INT"-minutes
                List<MBox> expiringMailBoxesList = MBox.getNextBoxes(xcmConfiguration.MB_INTERVAL);
                ListIterator<MBox> mailBoxIterator = expiringMailBoxesList.listIterator();

                DateTime dt = new DateTime();
                MBox mailBox;
                // disable expired mail-addresses
                while (mailBoxIterator.hasNext())
                {
                    mailBox = mailBoxIterator.next();
                    if (dt.isAfter(mailBox.getTs_Active()) && (mailBox.getTs_Active() != 0))
                    { // this element is now expired
                        mailBox.enable();
                        log.debug("now expired: " + mailBox.getFullAddress());
                    }
                }

                // add the new Mailtransactions
                List<MailTransaction> mtxToSave = new LinkedList<MailTransaction>();

                // add all transactions from the queue to a list
                while (!mtxQueue.isEmpty())
                {
                    mtxToSave.add(mtxQueue.poll());
                }
                // and save all entries of this list in one transaction to the list
                MailTransaction.saveMultipleTx(mtxToSave);
                log.info("stored " + mtxToSave.size() + " entries in the database");

                // remove old MailTransactions
                if (deleteTransactions)
                { // execute only if a value has been set
                    log.debug("Cleanup Mailtransaction-list");
                    long removalTS = dt.minusHours(xcmConfiguration.MTX_MAX_AGE).getMillis();

                    MailTransaction.deleteTxInPeriod(removalTS);
                    log.debug("finished Mailtransaction cleanup");
                }
            }
        }, new Long(0), new Long(xcmConfiguration.MB_INTERVAL), TimeUnit.MINUTES);

    }

    /**
     * Stops the Threads and the SMTP-Server
     */
    @Dispose(order = 90)
    public void stopActions()
    {
        // stop the forwarding-service
        smtpServer.stop();

        // stop the job to expire the mailboxes
        expirationService.shutdown();
    }

    /**
     * finds an available Port in the Range of "min" and "max" <br/>
     * Copyright information: this Method comes from NinjaTestServer
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
