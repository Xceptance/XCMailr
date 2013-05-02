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

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import models.MBox;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MailHandler
{
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

        // TODO retry until the message could be sent(?)
        String targ = getMailTarget(to);
        if (targ == null)
        {
            // if there's no mx-record, return false
            return false;
        }
        SimpleEmail email = new SimpleEmail();
        try
        {
            email.setHostName(targ);
            email.setFrom(from);
            email.addTo(to);
            email.setSubject(subject);
            email.setMsg(content);
            jc.emailqueue.add(email);
        }
        catch (EmailException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }

        return true;

    }

    /**
     * Generates the Confirmation-Mail after Registration
     * 
     * @param to
     *            - Recipients-Address
     * @param forename
     *            - Forename of the Recipient
     * @param id
     *            - UserID of the Recipient
     * @param token
     *            - the generated Confirmation-Token of the User
     * @param lang
     *            - The Language for the Mail
     */
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

    /**
     * Generates the Confirmation-Mail for a forgotten Password
     * 
     * @param to
     *            - Recipients-Address
     * @param forename
     *            - Forename of the Recipient
     * @param id
     *            - UserID of the Recipient
     * @param token
     *            - the generated Confirmation-Token of the User
     * @param lang
     *            - The Language for the Mail
     */
    public void sendPwForgotAddressMail(String to, String forename, String id, String token, String lang)
    {
        String from = ninjaProp.get("mbox.adminaddr");
        String url = "http://" + ninjaProp.get("mbox.host") + "/lostpw/" + id + "/" + token;
        Object[] object = new Object[]
            {
                forename, url
            };
        String body = msg.get("i18nuser_pwresend_message", lang, object);
        String subj = msg.get("i18nuser_pwresend_subject", lang, (Object) null);
        sendMail(from, to, body, subj);
    }

    /**
     * Searches the MX-Host of a given Mailaddress
     * 
     * @param mailadr
     *            - the mailaddress
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
            return null;
        }
        catch (NullPointerException e)
        {
            return null;
        }
    }
}
