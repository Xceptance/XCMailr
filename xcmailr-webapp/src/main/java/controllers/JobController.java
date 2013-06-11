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
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;
import models.MBox;
import models.User;
import ninja.i18n.Lang;
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

    private final ScheduledExecutorService mailTransportService = Executors.newSingleThreadScheduledExecutor();

    private SMTPServer smtpServer;

    @Inject
    Logger log;

    @Inject
    NinjaProperties ninjaProp;

    @Inject
    XCMailrConf xcmConf;

    @Inject
    MsgListener msgListener;

    @Inject
    Lang lang;

    /**
     * Starts the Mailserver, creates the Admin-Account specified in application.conf and Threads to expire the
     * Mailaddresses and the Mails which have to be sent
     */
    @Start(order = 90)
    public void startActions()
    {
        log.debug("prod:" + ninjaProp.isProd() + " dev: " + ninjaProp.isDev() + " test: " + ninjaProp.isTest());

        if (!(xcmConf.ADMIN_PASS == null))
        { // if a pw is set in application.conf..

            if (!User.mailExists(xcmConf.ADMIN_ADD))
            {// ...and the admin-acc doesn't exist

                // create the adminaccount
                User usr = new User("Site", "Admin", xcmConf.ADMIN_ADD, xcmConf.ADMIN_PASS, "en");
                
                // set the status and admin flags
                usr.setAdmin(true);
                usr.setActive(true);
                usr.save();
            }
        }
        // create the server for incoming mails
        smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(msgListener));

        // use a dynamic port in test-mode
        int port;
        if (ninjaProp.isTest())
        {
            port = findAvailablePort(49152, 65535);
        }
        else
        {
            port = xcmConf.MB_PORT;
        }
        
        // set the port and start the SMTP-Server
        smtpServer.setPort(port);
        smtpServer.start();
        
        
        if (!ninjaProp.isTest())
        {
            // create the executor-service to check the mailboxes which were expired since the last run and disable them
            expirationService.scheduleAtFixedRate(new Runnable()
            {
                @Override
                public void run()
                {
                    log.debug("mbox-scheduler run");
                    
                    //get the number of MBox-Elements that will expire in the next "MB_INT"-minutes
                    List<MBox> mbList = MBox.getNextBoxes(xcmConf.MB_INT);
                    ListIterator<MBox> it = mbList.listIterator();
                    
                    DateTime dt = new DateTime();
                    
                    while (it.hasNext())
                    {
                        MBox mb = it.next();
                        if (dt.isAfter(mb.getTs_Active()) && !(mb.getTs_Active() == 0))
                        { // this element is now expired
                            mb.enable();
                        }
                    }
                }
            }, new Long(1), new Long(xcmConf.MB_INT), TimeUnit.MINUTES);
        }
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
        mailTransportService.shutdown();
    }

    /**
     * finds an available Port in the Range of "min" and "max" Copyright information: this Method comes from
     * NinjaTestServer
     * 
     * @param min
     *            lower bound of ports to search
     * @param max
     *            upper bound of ports to search
     * @return an available port
     */
    // stolen from NinjaTestServer-source
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
