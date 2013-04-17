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
        
//        log.debug("EBEAN DATASOURCE:" + ninjaProp.get("ebean.datasource.databaseUrl"));
//        log.debug("Test:" + ninjaProp.isTest());
//        log.debug("Dev:" + ninjaProp.isDev());
//        log.debug("Prod:" + ninjaProp.isProd());
        MailrMessageHandlerFactory mailrFactory = new MailrMessageHandlerFactory();
        smtpServer = new SMTPServer(mailrFactory);
        //dynamic ports: 49152–65535
        int port ;
        //TODO think about another solution..
        if(ninjaProp.getBoolean("test.serv")==true){
            port = findAvailablePort(49152, 65535);
            log.debug("port gewaehlt: "+port);
        }else{
            port = Integer.parseInt(ninjaProp.get("mbox.port"));
        }
        
 //       log.debug("----------------------------------------------STarte!!----------------------------");
        smtpServer.setPort(port);
   

                smtpServer.start();


        int interval = Integer.parseInt(ninjaProp.get("mbox.interval"));

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
 //       log.debug("----------------------------------------------SToppe!!----------------------------");
        // stop the forwarding-service
        smtpServer.stop();
        // stop the job to expire the mailboxes
        executorService.shutdown();
    }
    
    //stolen from ninjatestserver-code
    private static int findAvailablePort(int min, int max) {
        for (int port = min; port < max; port++) {
            try {
                new ServerSocket(port).close();
                return port;
            } catch (IOException e) {
                // Must already be taken
            }
        }
        throw new IllegalStateException(
                "Could not find available port in range " + min + " to " + max);
    }
}
