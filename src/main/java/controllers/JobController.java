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
import org.subethamail.smtp.server.SMTPServer;

import models.MBox;
import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;
import ninja.utils.NinjaProperties;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Handles the Jobs...
 * 
 * @author Patrick Thum
 */

@Singleton
public class JobController
{
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public SMTPServer smtpServer;

    @Inject
    Logger log;

    @Inject
    NinjaProperties ninjaProp;

    /*
     * Service order: 10 - Services that connect to resources and do not depend on other services, for example, database
     * connections 20-80 - Services that depend on resources, but don’t actually start the app doing its core functions
     * 90 - Services that start the app doing its core functions, for example, listen on queues, listen for HTTP, start
     * scheduled services
     */
    @Start(order = 90)
    public void startActions()
    {
        log.info("prod:"+ninjaProp.isProd()+" dev: "+ninjaProp.isDev()+" test: "+ninjaProp.isTest());
        
        //TODO check whether the domains contained in application.conf are correct (spelling)
        
        MailrMessageHandlerFactory mailrFactory = new MailrMessageHandlerFactory();
        smtpServer = new SMTPServer(mailrFactory);
        // dynamic ports: 49152–65535
        int port;
        // TODO think about another solution..

        /*
         * check if the test.serv option in application.conf was set to true
         * 
         * TODO maybe use the mode (e.g. check for ninjaProp.isDev() or ninjaProp.isTest() )
         * or alternatively check if the port which was set in application.conf at mbox.port is used
         */  
        if (ninjaProp.getBoolean("test.serv") == true)
        {
            port = findAvailablePort(49152, 65535);
            
        }
        else
        {
            port = Integer.parseInt(ninjaProp.get("mbox.port"));
        }
        
        //set the port and start it
        smtpServer.setPort(port);
        smtpServer.start();

        int interval = Integer.parseInt(ninjaProp.get("mbox.interval"));
        
        //create the sheduler-service to check the mailboxes which were expired since the last run and disable them
        //TODO maybe use the Shedule-Annotation instead, see: http://www.ninjaframework.org/documentation/scheduler.html
        executorService.schedule(new Runnable()
        {
            public void run()
            {

                int size = Integer.parseInt(ninjaProp.get("mbox.size"));
                List<MBox> mbList = MBox.getNextBoxes(size);
                ListIterator<MBox> it = mbList.listIterator();
                DateTime dt = new DateTime();
                while (it.hasNext())
                {
                    MBox mb = it.next();
                    if (dt.isAfter(mb.getTS_Active()) && !(mb.getTS_Active() == 0))
                    {
                        // this element is expired
                        MBox.enable(mb.getId());

                    }
                }
            }
        }, new Long(interval), TimeUnit.MINUTES);
    }

    @Dispose(order = 90)
    public void stopActions()
    {

        // stop the forwarding-service
        smtpServer.stop();
        // stop the job to expire the mailboxes
        executorService.shutdown();
    }

    // stolen from NinjaTestServer-source
    private static int findAvailablePort(int min, int max)
    {
        for (int port = min; port < max; port++)
        {
            try
            {   //try to create a now port at "port" 
                //if there's no exception, return it
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
