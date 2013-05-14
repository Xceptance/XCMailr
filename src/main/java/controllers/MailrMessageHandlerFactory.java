/**  
 *  Copyright 2013 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package controllers;

import models.MBox;
import models.MailTransaction;
import models.User;
import ninja.i18n.Messages;
import ninja.utils.NinjaProperties;
import org.slf4j.Logger;
import org.subethamail.smtp.*;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Handles all actions for Mails, especially processes all incoming Mails
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Singleton
public class MailrMessageHandlerFactory implements MessageHandlerFactory
{
    @Inject
    Logger log;

    @Inject
    Messages msg;

    @Inject
    NinjaProperties ninjaProp;

    @Inject
    JobController jc;

    private Session sess;
    
    /**
     * This Method will be called by the SubethaSMTP-Server for every incoming Mail
     */
    public MessageHandler create(MessageContext ctx)
    {

        return new Handler(ctx);
    }
    
    /**
     * Handles incoming Mails
     */
    class Handler implements MessageHandler
    {
        MessageContext ctx;

        String sender;

        String rcpt;

        InputStream data;

        MimeMessage mail;

        public Handler(MessageContext ctx)
        {

            this.ctx = ctx;

        }

        public void from(String from) throws RejectException
        {
            sender = from;
        }

        public void recipient(String recipient) throws RejectException
        { 
            rcpt = recipient;
        }

        public void data(InputStream data) throws IOException
        {
            // create the session, read the entrys from the config file
            Session session = MailrMessageHandlerFactory.this.getSession();
            session.setDebug(true);
            try
            {
                mail = new MimeMessage(session, data);
            }
            catch (MessagingException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        /**
         * this will handle the message
         */

        public void done()
        {
            String[] splitaddress = rcpt.split("@");
            MailTransaction mtx;

            if (!(splitaddress.length == 2))
            { // the mailaddress does not have the expected pattern -> do nothing, just log it
                mtx = new MailTransaction(0, rcpt, sender);
                mtx.saveTx();
                return;
            }

            if (MBox.mailExists(splitaddress[0], splitaddress[1]))
            { // the given mailaddress exists in the db

                MBox mb = MBox.getByName(splitaddress[0], splitaddress[1]);
                if (mb.isActive())
                { // there's an existing and active mailaddress
                  // TODO the language here
                  // prepare the message
                    String fwdtarget = MBox.getFwdByName(splitaddress[0], splitaddress[1]);
                    try
                    {
                        mail.setFrom(new InternetAddress(sender));
                        mail.addRecipient(Message.RecipientType.TO, new InternetAddress(fwdtarget));
                        jc.addMessage(mail);
                        mtx = new MailTransaction(300, rcpt, sender);
                        mtx.saveTx();
                        mb.increaseForwards();
                        mb.update();
                    }
                    catch (MessagingException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        // the message can't be forwarded
                        mtx = new MailTransaction(400, rcpt, sender);
                        mtx.saveTx();
                    }
                }
                else
                { // there's a mailaddress, but its inactive
                    mb.increaseSuppressions();
                    mtx = new MailTransaction(200, rcpt, sender);
                    mtx.saveTx();
                    mb.update();
                }
            }
            else
            { // mailaddress does not exist
                mtx = new MailTransaction(100, rcpt, sender);
                mtx.saveTx();
            }

        }
    }

    /**
     * Reads the Configuration-File and creates the Session for the Mail-Transport
     * 
     * @return the Session-Object
     */
    public Session getSession()
    {
        if (sess == null)
        {
            // get the data from application.conf
            final String host = ninjaProp.get("mail.smtp.host");
            final String port = ninjaProp.get("mail.smtp.port");
            final String user = ninjaProp.get("mail.smtp.user");
            final String pwd = ninjaProp.get("mail.smtp.pass");
            boolean auth = ninjaProp.getBoolean("mail.smtp.auth");
            boolean tls = ninjaProp.getBoolean("mail.smtp.tls");
            // set the data
            Properties prop = System.getProperties();
            prop.put("mail.smtp.host", host);
            prop.put("mail.smtp.port", port);
            prop.put("mail.smtp.debug", true);
            prop.put("mail.smtp.auth", auth);
            prop.put("mail.smtp.starttls.enable", tls);
            sess = Session.getInstance(prop, new javax.mail.Authenticator()
            {
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    return new PasswordAuthentication(user, pwd);
                }
            });
        }
        return sess;
    }

    /**
     * Takes the Mail specified by the Parameters and sends it to the given Target
     * 
     * @param from
     *            - the Mail-Author
     * @param to
     *            - the Recipients-Address
     * @param content
     *            - the Message-Body
     * @param subject
     *            - the Message Subject
     * @return true, if the Addition to the Mail-Queue was successful
     */
    public boolean sendMail(String from, String to, String content, String subject)
    {
        Session session = getSession();
        session.setDebug(true);
        MimeMessage message = new MimeMessage(session);
        try
        {
            message.setFrom(new InternetAddress(from));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(content);
            message.saveChanges();
            jc.addMessage(message);
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
     *            - {@link User}-ID of the Recipient
     * @param token
     *            - the generated Confirmation-Token of the {@link User}
     * @param lang
     *            - The Language for the Mail
     */
    public void sendConfirmAddressMail(String to, String forename, String id, String token, Optional<String> lang)
    {
        String from = ninjaProp.get("mbox.adminaddr");
        String url = "http://" + ninjaProp.get("mbox.host") + "/verify/" + id + "/" + token;
        Object[] object = new Object[]
            {
                forename, url

            };
        String body = msg.get("i18nUser_Verify_Message", lang, object).get();
        String subj = msg.get("i18nUser_Verify_Subject", lang, (Object) null).get();

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
     *            - {@link User}-ID of the Recipient
     * @param token
     *            - the generated Confirmation-Token of the User
     * @param lang
     *            - The Language for the Mail
     */
    public void sendPwForgotAddressMail(String to, String forename, String id, String token, Optional<String> lang)
    {
        String from = ninjaProp.get("mbox.adminaddr");
        String url = "http://" + ninjaProp.get("mbox.host") + "/lostpw/" + id + "/" + token;
        Object[] object = new Object[]
            {
                forename, url
            };
        String body = msg.get("i18nUser_PwResend_Message", lang, object).get();
        String subj = msg.get("i18nUser_PwResend_Subject", lang, (Object) null).get();
        sendMail(from, to, body, subj);
    }
}
