package controllers;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
    public MemCachedSessionHandler mcsh;

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
     * @return a boolean whether the addition to the mailqueue was successful
     */
    public boolean forwardMail(String from, String to, String content, String lang)
    {
        String[] splitaddress = to.split("@");
        String fwdtarget = MBox.getFwdByName(splitaddress[0], splitaddress[1]);
        String s = msg.get("i18nmsg_fwdsubj", lang, (Object) null);
        try
        {
            return sendMail(from, fwdtarget, content, s);
        }
        catch (UnknownHostException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
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
     * @return true if the addition to the mailqueue was successful
     * @throws UnknownHostException
     */
    public boolean sendMail(String from, String to, String content, String subject) throws UnknownHostException
    {
        String domainTo = to.split("@")[1];

        Map<String, Integer> domainTargets = (Map<String, Integer>) mcsh.get(domainTo);
        if (domainTargets == null)
        { // there was no entry for this domain until now
            domainTargets = prepareMailTarget(to);
        }
        if (domainTargets == null)
        {
            throw new UnknownHostException();
        }
        Set<Entry<String, Integer>> eSet = domainTargets.entrySet();
        Iterator<Entry<String, Integer>> it = eSet.iterator();
        Entry<String, Integer> entry = it.next();
        while (entry.getValue() == 1 && it.hasNext())
        { // get the next set until the list ended or the name is available or unchecked
            entry = it.next();
        }
        if (entry == null || entry.getValue() == 1)
        { // there were no items or all entrys are unavailable
            throw new UnknownHostException();
        }

        String targ = entry.getKey();
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
     * Takes the mail again and specifies a new host 
     * 
     * @param from
     *            - the mail-author
     * @param to
     *            - the recipients-address
     * @param content
     *            - the message body
     * @param subject
     *            - the message subject
     * @return true if the addition to the mailqueue was successful
     * @throws UnknownHostException
     */
    public boolean sendMailAgain(SimpleEmail mail) throws UnknownHostException
    {
        String mailTo = mail.getToAddresses().get(0).getAddress();
        String domainTo = mailTo.split("@")[1];

        Map<String, Integer> domainTargets = (Map<String, Integer>) mcsh.get(domainTo);
        if (domainTargets == null)
        { // there was no entry for this domain until now
            domainTargets = prepareMailTarget(mailTo);
        }
        if (domainTargets == null)
        {
            throw new UnknownHostException();
        }
        Set<Entry<String, Integer>> eSet = domainTargets.entrySet();
        Iterator<Entry<String, Integer>> it = eSet.iterator();
        Entry<String, Integer> entry = it.next();
        while (entry.getValue() == 1 && it.hasNext())
        { // get the next set until the list ended or the name is available or unchecked
            entry = it.next();
        }
        if (entry == null || entry.getValue() == 1)
        { // there were no items or all entrys are unavailable
            throw new UnknownHostException();
        }
        String targ = entry.getKey();
        mail.setHostName(targ);
        jc.emailqueue.add(mail);

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
        try
        {
            sendMail(from, to, body, subj);
        }
        catch (UnknownHostException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

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
        try
        {
            sendMail(from, to, body, subj);
        }
        catch (UnknownHostException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Searches the MX-Host of a given Mailaddress
     * 
     * @param mailadr
     *            - the mailaddress
     * @return the mx-record for this address as string
     * @deprecated 
     */
    public String getMailTarget(String mailadr)
    {
        try
        {
            String domainpart = mailadr.split("@")[1];
            Record[] records = new Lookup(domainpart, Type.MX).run();

            MXRecord mx = (MXRecord) records[0];
            String targetaddress = mx.getTarget().toString();

            // remove the dot at the end of the entry
            if (targetaddress.endsWith("."))
            {
                targetaddress = targetaddress.substring(0, targetaddress.length() - 1);
            }
            return targetaddress;
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

    /**
     * Tries to find another MX-Record than the given one
     * 
     * @param mailadr
     * @param host
     * @return the mx-host-record as String
     */

    public Map<String, Integer> prepareMailTarget(String mailadr)
    {
        try
        {
            String domainpart = mailadr.split("@")[1];
            Record[] records = new Lookup(domainpart, Type.MX).run();
            Map<String, Integer> recMap = new HashMap<String, Integer>();
            MXRecord mxr;
            String target;

            for (Record r : records)
            {
                mxr = (MXRecord) r;
                target = mxr.getTarget().toString();
                if (target.endsWith("."))
                {
                    target = target.substring(0, target.length() - 1);
                }
                // put every target with 0, this should indicate that this name is unchecked
                recMap.put(target, 0);
            }
            return recMap;
        }
        catch (TextParseException e)
        {
            return null;
        }
    }
}
