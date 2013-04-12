package controllers;
//-------------------------------------------------------------------
//MyMessageHandlerFactory.java
//-------------------------------------------------------------------
import models.MBox;

import org.subethamail.smtp.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailrMessageHandlerFactory implements MessageHandlerFactory {

    public MessageHandler create(MessageContext ctx) {
        return new Handler(ctx);
    }

    class Handler implements MessageHandler {
        MessageContext ctx;
        String sender;
        String empf;
        String content;

        public Handler(MessageContext ctx) {
                this.ctx = ctx;
        }
        
        public void from(String from) throws RejectException {//no rejections!?
                sender = from;
        }

        public void recipient(String recipient) throws RejectException { //no rejections!?
                empf = recipient;
        }

        public void data(InputStream data) throws IOException {
                content = this.convertStreamToString(data);
                }

        
        /**
         * test
         */
        
        public void done() {
        	//do all the mail-fwd things here

                String[] splitaddress = empf.split("@"); 
                //TODO check if the address is malicious
                if(MBox.mailExists(splitaddress[0], splitaddress[1])){
                	//TODO implement check for the validity of a mbox
		          	  String host = "gmail-smtp-in.l.google.com";
		    	      String fwdtarget = MBox.getFwdByName(splitaddress[0],splitaddress[1]); 
		    	      Properties properties = System.getProperties();
		    	      //TODO create an option in the application.conf
		    	      properties.setProperty("mail.smtp.host", host);
		    	      Session session = Session.getDefaultInstance(properties);
		    	      	
		    	      try{	
		    	    	  //add the header-informations
		    		         MimeMessage message = new MimeMessage(session);
		    		         message.setFrom(new InternetAddress(sender));
		    		         message.addRecipient(Message.RecipientType.TO,
		    		                                  new InternetAddress(fwdtarget));
		    		         //TODO implement an i18n Subject-text
		    		         message.setSubject("Weitergeleitete Nachricht");
		    		         
		    		      //add the message body
		    		         message.setText(content);
		    		         Transport.send(message);
		    		         
		    		         //TODO increase the mbox-fwd-counter
		    	                System.out.println("Finished");
		    	                System.out.println("The Message was:\n");
		    	                System.out.println("From:" + sender);
		    	                System.out.println("To:" + empf);
		    	                System.out.println("Content:" + content);
		    		         
		    		         
		    	      }catch (Exception e) {
		    	         e.printStackTrace();
		    	         
		    	      }
                }else{
                	//just increase the suppressed-mails counter
                }
                
        }

        public String convertStreamToString(InputStream is) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                
                String line = null;
                try {
                        while ((line = reader.readLine()) != null) {
                                sb.append(line + "\n");
                        }
                } catch (IOException e) {
                        e.printStackTrace();
                }
                return sb.toString();
        }

    }
}