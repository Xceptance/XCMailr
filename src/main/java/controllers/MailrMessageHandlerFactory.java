package controllers;

import models.MBox;
import controllers.JobController;

import org.slf4j.Logger;
import org.subethamail.smtp.*;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import etc.HelperUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@Singleton
public class MailrMessageHandlerFactory implements MessageHandlerFactory
{

    
    @Inject
    Logger log;
    
    MailHandler mailhndl;
    
    @Inject
    public MailrMessageHandlerFactory(MailHandler mh){
        this.mailhndl = mh;
    }
    
    
    
    public MessageHandler create(MessageContext ctx)
    {
        return new Handler(ctx);
    }

    class Handler implements MessageHandler
    {
        MessageContext ctx;
        String sender;
        String empf;
        String content;

        public Handler(MessageContext ctx)
        {
            this.ctx = ctx;
        }

        public void from(String from) throws RejectException
        {// no rejections!?
            sender = from;
        }

        public void recipient(String recipient) throws RejectException
        { // no rejections!?
            empf = recipient;
        }

        public void data(InputStream data) throws IOException
        {
            content = this.convertStreamToString(data);
        }

        /**
         * 
         */

        public void done()
        { // do all the mail-fwd things here
            String[] splitaddress = empf.split("@");
            // TODO check if the address is malicious
            if (MBox.mailExists(splitaddress[0], splitaddress[1]))
            {
                MBox mb = MBox.getByName(splitaddress[0], splitaddress[1]);
                if (mb.isActive())
                {
                    
                    mailhndl.forwardMail(sender, empf, content);
                    mb.increaseForwards();
                    MBox.updateMBox(mb);
                }
                else
                {
                    mb.increaseSuppressions();
                    MBox.updateMBox(mb);
                }

            }
            else
            {
                // TODO just increase the general suppressed-mails counter

            }

        }

        public String convertStreamToString(InputStream is)
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();

            String line = null;
            try
            {
                while ((line = reader.readLine()) != null)
                {
                    sb.append(line + "\n");
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return sb.toString();
        }
    }
}
