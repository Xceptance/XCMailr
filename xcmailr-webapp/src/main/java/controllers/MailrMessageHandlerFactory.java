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
import conf.XCMailrConf;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Handles all Actions for Mails and especially processes all incoming Mails
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Singleton
public class MailrMessageHandlerFactory implements MessageHandlerFactory
{
    @Inject
    Messages msg;

    @Inject
    XCMailrConf xcmConf;

    @Inject
    Logger log;

    @Inject
    NinjaProperties ninjaProp;

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

        InputStream data;

        MimeMessage mail;

        ArrayList<String> rcpts;

        List<String> domainlist;

        String rcpAddr;

        boolean accepted;

        public Handler(MessageContext ctx)
        {

            this.ctx = ctx;
            rcpts = new ArrayList<String>();
            rcpAddr = "";
            domainlist = Arrays.asList(xcmConf.DM_LIST);
            accepted = true;

        }

        public void from(String from) throws RejectException
        {
            sender = from;
        }

        public void recipient(String recipient) throws RejectException
        {

            String[] splitaddress = recipient.split("@");

            if ((splitaddress.length != 2) || (!domainlist.contains(splitaddress[1])))
            {
                accepted = false;
                MailTransaction mtx = new MailTransaction(300, recipient, sender);
                mtx.saveTx();
                throw new RejectException("Relay access denied");
            }
            else
            {
                rcpts.add(recipient);
            }
        }

        public void data(InputStream data) throws IOException
        {
            if (accepted)
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
                    log.error(e.getMessage());
                }
            }
        }

        public void done()
        {
            if (accepted)
            {
                Iterator<String> it = rcpts.iterator();
                MailTransaction mtx;
                String[] splitAddress;
                String recipient;
                ArrayList<Address> mailRecipients = new ArrayList<Address>();
                Address fwdAddress;
                String fwdTarget;
                MBox mb;
                while (it.hasNext())
                {
                    recipient = it.next();
                    splitAddress = recipient.split("@");
                    if (!(splitAddress.length == 2))
                    { // the mailaddress does not have the expected pattern -> do nothing, just log it
                        mtx = new MailTransaction(0, recipient, sender);
                        mtx.saveTx();
                        return;
                    }

                    if (MBox.mailExists(splitAddress[0], splitAddress[1]))
                    { // the given mailaddress exists in the db

                        mb = MBox.getByName(splitAddress[0], splitAddress[1]);
                        if (mb.isActive())
                        { // there's an existing and active mailaddress
                          // add the target-address to the list
                            fwdTarget = MBox.getFwdByName(splitAddress[0], splitAddress[1]);
                            try
                            {
                                fwdAddress = new InternetAddress(fwdTarget);

                                // add the recipient if its not already in the list
                                if (!mailRecipients.contains(fwdAddress))
                                {
                                    mailRecipients.add(fwdAddress);
                                }
                                mtx = new MailTransaction(300, recipient, sender);
                                mtx.saveTx();
                                mb.increaseForwards();
                                mb.update();
                            }
                            catch (AddressException e)
                            {
                                log.error(e.getMessage());
                                // the message can't be forwarded (has not the correct format)
                                // this SHOULD never be the case...
                                mtx = new MailTransaction(400, recipient, sender);
                                mtx.saveTx();
                            }
                        }
                        else
                        { // there's a mailaddress, but the forward is inactive
                            mtx = new MailTransaction(200, recipient, sender);
                            mtx.saveTx();
                            mb.increaseSuppressions();
                            mb.update();
                        }
                    }
                    else
                    { // mailaddress does not exist
                        mtx = new MailTransaction(100, recipient, sender);
                        mtx.saveTx();
                    }
                }

                // object to store the extracted new recipients of the message
                if (!mailRecipients.isEmpty())
                {
                    try
                    {
                        Address[] recipientArray = new Address[mailRecipients.size()];
                        recipientArray = mailRecipients.toArray(recipientArray);
                        mail.setRecipients(Message.RecipientType.TO, recipientArray);
                        mail.removeHeader("Cc");
                        mail.removeHeader("BCC");
                        new ThreadedMailSend(mail);
                    }
                    catch (MessagingException e)
                    {
                        log.error(e.getMessage());
                    }
                }
            }
        }
    }

    class ThreadedMailSend extends Thread
    {
        private MimeMessage mail;

        public ThreadedMailSend(MimeMessage mail)
        {
            this.mail = mail;
            this.run();
        }

        /**
         * Sends the mail in a new thread
         */
        @Override
        public void run()
        {
            try
            {
                if (!ninjaProp.isTest()) // no messages will be sent when running in test-mode
                {
                    Transport.send(mail);
                }
                log.debug("Message sent");
            }
            catch (MessagingException e)
            {
                log.error(e.getMessage());
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
            final String host = xcmConf.OUT_SMTP_HOST;
            final String port = Integer.toString(xcmConf.OUT_SMTP_PORT);
            final String user = xcmConf.OUT_SMTP_USER;
            final String pwd = xcmConf.OUT_SMTP_PASS;
            boolean auth = xcmConf.OUT_SMTP_AUTH;
            boolean tls = xcmConf.OUT_SMTP_TLS;
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
     *            the Mail-Author
     * @param to
     *            the Recipients-Address
     * @param content
     *            the Message-Body
     * @param subject
     *            the Message Subject
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
            new ThreadedMailSend(message);
        }
        catch (AddressException e)
        {
            // e.printStackTrace();
            log.error(e.getMessage());
            return false;
        }
        catch (MessagingException e)
        {
            // e.printStackTrace();
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Generates the Confirmation-Mail after Registration
     * 
     * @param to
     *            Recipients-Address
     * @param forename
     *            Forename of the Recipient
     * @param id
     *            {@link User}-ID of the Recipient
     * @param token
     *            the generated Confirmation-Token of the {@link User}
     * @param lang
     *            The Language for the Mail
     */
    public void sendConfirmAddressMail(String to, String forename, String id, String token, Optional<String> lang)
    {
        String from = xcmConf.ADMIN_ADD;
        StringBuilder strb = new StringBuilder();
        strb.append(xcmConf.APP_HOME);
        if (!xcmConf.APP_BASE.isEmpty())
        {
            strb.append("/" + xcmConf.APP_BASE);
        }
        strb.append("/verify/" + id + "/" + token);

        Object[] object = new Object[]
            {
                forename, strb.toString(),xcmConf.CONF_PERIOD
            };

        String body = msg.get("i18nUser_Verify_Message", lang, object).get();
        String subj = msg.get("i18nUser_Verify_Subject", lang, (Object) null).get();

        sendMail(from, to, body, subj);

    }

    /**
     * Generates the Confirmation-Mail for a forgotten Password
     * 
     * @param to
     *            Recipients-Address
     * @param forename
     *            Forename of the Recipient
     * @param id
     *            {@link User}-ID of the Recipient
     * @param token
     *            The generated Confirmation-Token of the User
     * @param lang
     *            The Language for the Mail
     */
    public void sendPwForgotAddressMail(String to, String forename, String id, String token, Optional<String> lang)
    {
        String from = xcmConf.ADMIN_ADD;
        StringBuilder strb = new StringBuilder();
        strb.append(xcmConf.APP_HOME);
        if (!xcmConf.APP_BASE.isEmpty())
        {
            strb.append("/" + xcmConf.APP_BASE);
        }
        strb.append("/lostpw/" + id + "/" + token);

        Object[] object = new Object[]
            {
                forename, strb.toString(), xcmConf.CONF_PERIOD
            };

        String body = msg.get("i18nUser_PwResend_Message", lang, object).get();
        String subj = msg.get("i18nUser_PwResend_Subject", lang, (Object) null).get();
        sendMail(from, to, body, subj);
    }
}
