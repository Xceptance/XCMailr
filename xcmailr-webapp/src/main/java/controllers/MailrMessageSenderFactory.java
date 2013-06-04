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
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import conf.XCMailrConf;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Handles all Actions for outgoing Mails
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Singleton
public class MailrMessageSenderFactory
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
     * Reads the Configuration-File and creates the Session for the Mail-Transport
     * 
     * @return the Session-Object
     */
    public Session getSession()
    {
        if (sess == null)
        {
            // set the data from application.conf
            Properties prop = System.getProperties();
            prop.put("mail.smtp.host", xcmConf.OUT_SMTP_HOST);
            prop.put("mail.smtp.port", xcmConf.OUT_SMTP_PORT);
            prop.put("mail.smtp.debug", xcmConf.OUT_SMTP_DEBUG);
            prop.put("mail.smtp.auth", xcmConf.OUT_SMTP_AUTH);
            prop.put("mail.smtp.starttls.enable", xcmConf.OUT_SMTP_TLS);
            sess = Session.getInstance(prop, new javax.mail.Authenticator()
            {
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    return new PasswordAuthentication(xcmConf.OUT_SMTP_USER, xcmConf.OUT_SMTP_PASS);
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
        session.setDebug(xcmConf.OUT_SMTP_DEBUG);
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
            log.error(e.getMessage());
            return false;
        }
        catch (MessagingException e)
        {
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
                forename, strb.toString(), xcmConf.CONF_PERIOD
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

    public class ThreadedMailSend extends Thread
    {

        private MimeMessage mail;

        private MBox mb;

        private MailTransaction mtx;

        public ThreadedMailSend(MimeMessage mail)
        {
            this(mail, null);
        }

        public ThreadedMailSend(MimeMessage mail, MBox mb)
        {
            this.mb = mb;
            this.mail = mail;
            this.run();
        }

        /**
         * Sends the mail in a new thread
         */
        @Override
        public void run()
        {
            String recipient = "";
            String from = "";
            try
            {
                if (mail.getFrom().length > 0 && mail.getRecipients(Message.RecipientType.TO).length > 0)
                {
                    recipient = mail.getRecipients(Message.RecipientType.TO)[0].toString();
                    from = mail.getFrom()[0].toString();
                }
                if (!ninjaProp.isTest()) // no messages will be sent when running in test-mode
                {
                    Transport.send(mail);

                    mtx = new MailTransaction(300, recipient, from);
                    mtx.saveTx();
                    log.info("Message sent, From: " + from + " To:" + recipient);
                    if (mb != null)
                    {
                        mb.increaseForwards();
                        mb.update();
                    }
                    log.info("Message sent, From:" + from + " To:" + recipient);
                }

            }
            catch (MessagingException e)
            {
                // the message sending-process failed
                // log it
                mtx = new MailTransaction(0, recipient, from);
                mtx.saveTx();

                log.error(e.getMessage());
            }
        }

    }

}
