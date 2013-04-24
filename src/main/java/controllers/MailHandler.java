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
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

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
     * @return true whether the addition to the mailqueue was successful
     */

    public boolean sendMail(String from, String to, String content, String subject)
    {
        try
        {
            // TODO retry until the message could be sent(?)
            Properties properties = System.getProperties();
            String targ = getMailTarget(to);
            if (targ == null)
            {
                //if there's no mx-record, return false
                return false;
            }
            properties.setProperty("mail.smtp.host", targ);
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
        String url = "http://" + ninjaProp.get("mbox.host") + "/verify/" + id + "/" + token;
        Object[] object = new Object[]
            {
                forename, url

            };
        String body = msg.get("i18nuser_verify_message", lang, object);

        String subj = msg.get("i18nuser_verify_subject", lang, (Object) null);
        sendMail(from, to, body, subj);

    }

    public void sendPwForgotAddressMail(String to, String forename, String id, String token, String lang)
    {
        String from = ninjaProp.get("mbox.adminaddr");
        String url = "http://" + ninjaProp.get("mbox.host") + "/lostpw/" + id + "/" + token;
        Object[] object = new Object[]
            {
                forename, url

            };
        // TODO change this message
        String body = msg.get("i18nuser_verify_message", lang, object);

        String subj = msg.get("i18nuser_verify_subject", lang, (Object) null);
        sendMail(from, to, body, subj);

    }

    /**
     * searches the mx-host of a given mailaddress
     * 
     * @param mailadr
     *            the mailaddress
     * @return the mx-record for this address as string
     */

    public static String getMailTarget(String mailadr)
    {

        try
        {
            Record[] records = new Lookup(mailadr.split("@")[1], Type.MX).run();

            MXRecord mx = (MXRecord) records[0];
            return mx.getTarget().toString();

        }
        catch (TextParseException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (NullPointerException e)
        {
            return null;
        }
        return null;
        // TODO nullpointerex
    }
}
