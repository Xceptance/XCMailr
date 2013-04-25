package controllers;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.joda.time.DateTime;

import org.slf4j.Logger;
import org.subethamail.smtp.server.SMTPServer;

import models.MBox;
import models.User;
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

    private final ScheduledExecutorService executorService2 = Executors.newSingleThreadScheduledExecutor();

    public SMTPServer smtpServer;

    public Queue<MimeMessage> mailqueue = new LinkedList<MimeMessage>();

    @Inject
    Logger log;

    @Inject
    NinjaProperties ninjaProp;

    @Inject
    MailHandler mailhndl;


    @Start(order = 90)
    public void startActions()
    {
        log.info("prod:" + ninjaProp.isProd() + " dev: " + ninjaProp.isDev() + " test: " + ninjaProp.isTest());

        // TODO check whether the domains contained in application.conf are correct (spelling)

        String pwd = ninjaProp.get("admin.pass");
        if (!(pwd == null))
        { // if a pw is set in application.conf..
            log.info("the passwd is set in the db");
            String mail = ninjaProp.getOrDie("mbox.adminaddr");
            if (!User.mailExists(mail))
            {// ...and the admin-acc doesn't exist
             // create the adminaccount
                log.info("Adminaccount is: " + mail + ":" + pwd);
                User usr = new User("Site", "Admin", mail, pwd);
                // set the status flag
                usr.setAdmin(true);
                usr.setActive(true);

                User.createUser(usr);
            }
        }
        MailrMessageHandlerFactory mailrFactory = new MailrMessageHandlerFactory(mailhndl);
        smtpServer = new SMTPServer(mailrFactory);
        // dynamic ports: 49152–65535
        int port;

        /*
         * check if the test.serv option in application.conf was set to true
         * 
         * TODO maybe use the mode (e.g. check for ninjaProp.isDev() or ninjaProp.isTest() ) or alternatively check if
         * the port which was set in application.conf at mbox.port is used
         */
        if (ninjaProp.getBoolean("test.serv") == true)
        {
            port = findAvailablePort(49152, 65535);

        }
        else
        {
            port = Integer.parseInt(ninjaProp.get("mbox.port"));
        }

        // set the port and start it
        smtpServer.setPort(port);
        smtpServer.start();

        int interval = Integer.parseInt(ninjaProp.get("mbox.interval"));

        // create the sheduler-service to check the mailboxes which were expired since the last run and disable them
        // TODO maybe use the Shedule-Annotation instead, see:
        // http://www.ninjaframework.org/documentation/scheduler.html
        executorService.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                log.info("mbox-scheduler run");
                int size = Integer.parseInt(ninjaProp.get("mbox.size"));
                List<MBox> mbList = MBox.getNextBoxes(size);
                ListIterator<MBox> it = mbList.listIterator();
                DateTime dt = new DateTime();
                while (it.hasNext())
                {
                    MBox mb = it.next();
                    if (dt.isAfter(mb.getTs_Active()) && !(mb.getTs_Active() == 0))
                    {
                        // this element is expired
                        MBox.enable(mb.getId());

                    }
                }
            }
        }, new Long(1), new Long(interval), TimeUnit.MINUTES);

        executorService2.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run() // Mailjob
            {
                log.info("mailjob run " + mailqueue.size());
                MimeMessage message = mailqueue.poll();

                while (!(message == null))
                {
                    log.info("Mailjob: Message found");
                    try
                    {
                        Transport.send(message);
                        message = mailqueue.poll();
                    }
                    catch (MessagingException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }, new Long(2), new Long(30), TimeUnit.SECONDS);

        // TODO also create a scheduler to send mails in the queue to prevent a blocked application
    }

    // TODO add tests for the activations..

    public void addMessage(MimeMessage msg)
    {
        mailqueue.add(msg);
    }

    @Dispose(order = 90)
    public void stopActions()
    {

        // stop the forwarding-service
        smtpServer.stop();
        // stop the job to expire the mailboxes
        executorService.shutdown();
        executorService2.shutdown();
    }

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
