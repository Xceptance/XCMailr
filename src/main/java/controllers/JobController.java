package controllers;

import org.slf4j.Logger;
import org.subethamail.smtp.server.SMTPServer;

import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;

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
	
	@Inject 
	Logger logger;
	
	@Start(order = 90)
	public void startActions(){
		logger.info("Start the Messaging-Service");
		//TODO start the MsgHandlerFactory
		MailrMessageHandlerFactory mailrFactory = new MailrMessageHandlerFactory() ;
        SMTPServer smtpServer = new SMTPServer(mailrFactory);
        smtpServer.setPort(25000);
        smtpServer.start();
		
		//TODO start the job to expire the mailboxes
		
	}
	
	
	@Dispose(order = 90)
	public void stopActions(){
		//TODO stop the MsgHandlerFactory
		//TODO stop the job to expire the mailboxes
		
	}
	
	
}
