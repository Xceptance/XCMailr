package controllers;

import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

import org.subethamail.smtp.server.SMTPServer;

import models.MBox;
import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;
import ninja.utils.NinjaProperties;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Handles the Jobs... 
 * @author Patrick Thum
 *
 * Service order:
 * 10 - Services that connect to resources and do not depend on other services, for example, database connections
 * 20-80 - Services that depend on resources, but don’t actually start the app doing its core functions
 * 90 - Services that start the app doing its core functions, for example, listen on queues, listen for HTTP, start scheduled services
 */

@Singleton
public class JobController {
	private final ScheduledExecutorService executorService = Executors
            .newSingleThreadScheduledExecutor();
	private SMTPServer smtpServer;

	@Inject 
	NinjaProperties ninjaProp;
	
	@Start(order = 90)
	public void startActions(){
		
		MailrMessageHandlerFactory mailrFactory = new MailrMessageHandlerFactory() ;
        smtpServer = new SMTPServer(mailrFactory);

        int port = Integer.parseInt(ninjaProp.get("mbox.port"));
        
        smtpServer.setPort(port);
        smtpServer.start();
		
    	int interval = Integer.parseInt(ninjaProp.get("mbox.interval"));
    	
        executorService.schedule(new Runnable() {
		    public void run() {
		    	
		    	int size = Integer.parseInt(ninjaProp.get("mbox.size"));
		    	List<MBox> mbList = MBox.getNextBoxes(size);
		    	ListIterator<MBox> it = mbList.listIterator();
		    	DateTime dt = new DateTime();
		    	while(it.hasNext()){
		    		MBox mb = it.next();
		    		if(dt.isAfter( mb.getTS_Active() ) && !( mb.getTS_Active() == 0 ) ){
		    		  //this element is expired
		    			MBox.enable( mb.getId() );
		    		  
		    		}
		    	}
		    }
        }, new Long(interval), TimeUnit.MINUTES);
		
	}
	
	
	@Dispose(order = 90)
	public void stopActions(){
		//stop the forwarding-service
		smtpServer.stop();
		// stop the job to expire the mailboxes
		executorService.shutdown();
		
	}
	
	
}
