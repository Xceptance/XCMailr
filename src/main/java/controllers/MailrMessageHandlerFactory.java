package controllers;

import models.MBox;
import models.MailTransaction;
import org.slf4j.Logger;
import org.subethamail.smtp.*;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Singleton
public class MailrMessageHandlerFactory implements MessageHandlerFactory
{

    @Inject
    Logger log;

    MailHandler mailhndl;

    @Inject
    public MailrMessageHandlerFactory(MailHandler mh)
    {
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

        String rcpt;

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
            rcpt = recipient;
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
            String[] splitaddress = rcpt.split("@");
            MailTransaction mtx;
            // TODO check if the address is malicious
            if (MBox.mailExists(splitaddress[0], splitaddress[1]))
            {
                MBox mb = MBox.getByName(splitaddress[0], splitaddress[1]);
                if (mb.isActive())
                { // there's an existing and active mailaddress
                  // TODO the language here
                    if (!mailhndl.forwardMail(sender, rcpt, content, ""))
                    { //the message can't be forwarded
                        mtx = new MailTransaction(400,rcpt,sender);
                        mtx.saveTx();
                    }
                    else
                    { //message forward was successcul
                        mtx = new MailTransaction(300, rcpt, sender);
                        mtx.saveTx();
                        mb.increaseForwards();
                        MBox.updateMBox(mb);
                    }
                }
                else
                { // there's a mailaddress, but its inactive

                    mb.increaseSuppressions();
                    mtx = new MailTransaction(200, rcpt, sender);
                    mtx.saveTx();
                    MBox.updateMBox(mb);
                }

            }
            else
            {
                // mailaddress does not exist
                mtx = new MailTransaction(100, rcpt, sender);
                mtx.saveTx();

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
