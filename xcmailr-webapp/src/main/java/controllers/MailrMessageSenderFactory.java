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

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

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

/**
 * Handles all Actions for outgoing Mails
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Singleton
public class MailrMessageSenderFactory
{
    @Inject
    Messages messages;

    @Inject
    XCMailrConf xcmConfiguration;

    @Inject
    Logger log;

    @Inject
    NinjaProperties ninjaProperties;

    @Inject
    JobController jobController;

    private Session session;

    /**
     * Reads the Configuration-File and creates the Session for the Mail-Transport
     * 
     * @return the Session-Object
     */
    public Session getSession()
    {
        if (session != null)
            return session;

        // set the data from application.conf
        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", xcmConfiguration.OUT_SMTP_HOST);
        properties.put("mail.smtp.port", xcmConfiguration.OUT_SMTP_PORT);
        properties.put("mail.smtp.debug", xcmConfiguration.OUT_SMTP_DEBUG);
        properties.put("mail.smtp.auth", xcmConfiguration.OUT_SMTP_AUTH);
        properties.put("mail.smtp.starttls.enable", xcmConfiguration.OUT_SMTP_TLS);
        
        // intention: Set the smtp.from to <> to request the Return-Path header be set to <> 
        // which would sigal that no automatic responses should be sent
        // Unfortunately it doesn't work because the SMTP server refuses to send these mails 
        // properties.put("mail.smtp.from", "<>"); 
        
        session = Session.getInstance(properties, new javax.mail.Authenticator()
        {
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return xcmConfiguration.OUT_SMTP_AUTH ? new PasswordAuthentication(xcmConfiguration.OUT_SMTP_USER, xcmConfiguration.OUT_SMTP_PASS) : null;
            }
        });
        return session;
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
     * @return true, if the mail had been successfully pushed to the thread
     */
    public boolean sendMail(String from, String to, String content, String subject)
    {
        Session session = getSession();

        // set the debug-mode as specified in the application.conf
        session.setDebug(xcmConfiguration.OUT_SMTP_DEBUG);

        MimeMessage message = new MimeMessage(session);

        try
        {
            message.setFrom(new InternetAddress(from));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(content);
            message.saveChanges();
            
            // send the mail in an own thread
            ThreadedMailSend tms = new ThreadedMailSend(message);
            tms.start();
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
     *            first-name of the Recipient
     * @param id
     *            {@link User}-ID of the Recipient
     * @param token
     *            the generated Confirmation-Token of the {@link User}
     * @param language
     *            The Language for the Mail
     */
    public void sendConfirmAddressMail(String to, String forename, String id, String token, Optional<String> language)
    {
        String from = xcmConfiguration.ADMIN_ADDRESS;

        // build the Verification Link
        StringBuilder strb = new StringBuilder();
        strb.append(xcmConfiguration.APP_HOME);
        if (!xcmConfiguration.APP_BASEPATH.isEmpty())
        {
            strb.append("/" + xcmConfiguration.APP_BASEPATH);
        }
        strb.append("/verify/" + id + "/" + token);

        // generate the message-body
        String body = messages.get("user_Verify_Message", language, forename, strb.toString(),
                                   xcmConfiguration.CONFIRMATION_PERIOD).get();
        // generate the message-subject
        String subject = messages.get("user_Verify_Subject", language, (Object) null).get();

        // send the Mail
        sendMail(from, to, body, subject);

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
     * @param language
     *            The Language for the Mail
     */
    public void sendPwForgotAddressMail(String to, String forename, String id, String token, Optional<String> language)
    {

        String from = xcmConfiguration.ADMIN_ADDRESS;

        // build the PW-Reset Link
        StringBuilder strb = new StringBuilder();
        strb.append(xcmConfiguration.APP_HOME);
        if (!xcmConfiguration.APP_BASEPATH.isEmpty())
        {
            strb.append("/" + xcmConfiguration.APP_BASEPATH);
        }
        strb.append("/lostpw/" + id + "/" + token);

        // generate the Message-Body
        String body = messages.get("user_PwResend_Message", language, forename, strb.toString(),
                                   xcmConfiguration.CONFIRMATION_PERIOD).get();

        // generate the Message-Subject
        String subject = messages.get("user_PwResend_Subject", language, (Object) null).get();

        // send the Mail
        sendMail(from, to, body, subject);
    }

    public void addMtxToJCList(MailTransaction mtx)
    {
        jobController.mtxQueue.add(mtx);
    }

    public class ThreadedMailSend extends Thread
    {

        private MimeMessage mail;

        private MBox mailBox;

        private MailTransaction mtx;

        public ThreadedMailSend(MimeMessage mail)
        {
            this(mail, null);
        }

        public ThreadedMailSend(MimeMessage mail, MBox mailBox)
        {
            this.mailBox = mailBox;
            this.mail = mail;
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
                // check whether the sender and recipient had been set
                if (mail.getFrom().length > 0 && mail.getRecipients(Message.RecipientType.TO).length > 0)
                {
                    // extract the senders and recipients-address to log the transaction
                    recipient = mail.getRecipients(Message.RecipientType.TO)[0].toString();
                    from = mail.getFrom()[0].toString();
                }
                if (!ninjaProperties.isTest()) // no messages will be sent when running in test-mode
                {
                    Transport.send(mail);
                    if (xcmConfiguration.MTX_MAX_AGE != 0)
                    {// if mailtransaction.maxage is set to 0 -> log nothing
                     // log the transaction
                        mtx = new MailTransaction(300, from, mailBox == null ? null : mailBox.getFullAddress(),
                                                  recipient);

                        addMtxToJCList(mtx);
                    }
                    log.info("Message sent, From: " + from + " To:" + recipient);

                    if (mailBox != null)
                    { // the message belongs to one of our mailboxes
                        mailBox.increaseFwd();
                    }
                }
            }
            catch (MessagingException e)
            {
                // the message sending-process failed
                // log it
                if (xcmConfiguration.MTX_MAX_AGE != 0 && mailBox != null)
                { // if mailtransaction.maxage is set to 0 -> log nothing
                    mtx = new MailTransaction(400, from, mailBox.getFullAddress(), recipient);
                    addMtxToJCList(mtx);
                }
                log.error(e.getMessage());
            }
        }
    }
}
