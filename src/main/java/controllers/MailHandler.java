package controllers;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ninja.i18n.Messages;
import ninja.utils.NinjaProperties;

import org.slf4j.Logger;
import models.MBox;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import etc.HelperUtils;

@Singleton
public class MailHandler
{

    @Inject
    private Logger log;

    @Inject
    Messages msg;

    @Inject
    NinjaProperties ninjaProp;

    private JobController jc;

    @Inject
    public MailHandler(JobController jc)
    {
        this.jc = jc;
    }

    /**
     * Takes the unchanged incoming mail and forwards it
     * 
     * @param from
     *            - the unchanged From-Address
     * @param to
     *            - the trash-mail target (the forward address will be automatically fetched from DB)
     * @param content
     *            - the message body
     * @return
     */
    public boolean forwardMail(String from, String to, String content, String lang)
    {
        String[] splitaddress = to.split("@");
        String fwdtarget = MBox.getFwdByName(splitaddress[0], splitaddress[1]);
        String s = msg.get("i18nmsg_fwdsubj", lang, (Object) null);
        return sendMail(from, fwdtarget, content, s);

    }

    /**
     * Takes the mail specified by the parameters and sends it to the given target
     * 
     * @param from
     *            - the mail-author
     * @param to
     *            - the recipients-address
     * @param content
     *            - the message body
     * @param subject
     *            - the message subject
     * @return true if the mail-transmission was successful
     */

    public boolean sendMail(String from, String to, String content, String subject)
    {
        try
        {
            // TODO retry until the message could be sent(?)
            Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", HelperUtils.getMailTarget(to));
            Session session = Session.getDefaultInstance(properties);

            // create the message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(content);
            jc.mailqueue.add(message);

            return true;

        }
        catch (AddressException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        catch (MessagingException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }

    }

    public void sendConfirmAddressMail(String to, String forename, String id, String token, String lang)
    {
        String from = ninjaProp.get("mbox.adminaddr");
        String url = "http://"+ninjaProp.get("mbox.host")+"/verify/"+id+"/"+token;
        Object[] object = new Object[] {
                                        forename, url
                                      
        };
        String body = msg.get("i18nuser_verify_message", lang, object);
        
        String subj = msg.get("i18nuser_verify_subject", lang, (Object)null);
        sendMail(from, to, body, subj);

    }

}
