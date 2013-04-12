package controllers;

import models.MBox;

import org.subethamail.smtp.*;
import org.xbill.DNS.*;

import com.google.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


@Singleton
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
        	//TODO what will happen if there are more than one recipient?
                String[] splitaddress = empf.split("@"); 
                //TODO check if the address is malicious
                if(MBox.mailExists(splitaddress[0], splitaddress[1])){
                	//TODO implement check for the validity of a mbox
                	//TODO change the host..
                	
                	
		          	  String host;
		    	      String fwdtarget = MBox.getFwdByName(splitaddress[0],splitaddress[1]);
		    	      
		    	      
					try {
						Record[] records = new Lookup(fwdtarget.split("@")[1], Type.MX).run();
						//TODO try until the message could be sent
	                	MXRecord mx = (MXRecord) records[0];
	                	host = mx.getTarget().toString();
			    	    Properties properties = System.getProperties();
			    	      System.out.println(host);
			    	    properties.setProperty("mail.smtp.host", host);
			    	    Session session = Session.getDefaultInstance(properties);
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
	    		         
			    	      	
					} catch (TextParseException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (AddressException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (MessagingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

		    	      try{	

		    		         
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