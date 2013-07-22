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

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import models.MBox;
import models.MailTransaction;
import ninja.utils.NinjaProperties;
import org.slf4j.Logger;
import org.subethamail.smtp.helper.SimpleMessageListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import conf.XCMailrConf;

/**
 * Handles all Actions for incoming Mails
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Singleton
public class MessageListener implements SimpleMessageListener
{
    @Inject
    XCMailrConf xcmConfiguration;

    @Inject
    NinjaProperties ninjaProperties;

    @Inject
    MailrMessageSenderFactory mailrSenderFactory;

    @Inject
    Logger log;

    @Override
    public boolean accept(String from, String recipient)
    {
        // accept the address if the domain is contained in the application.conf
        String[] splitaddress = recipient.split("@");

        List<String> domainlist = Arrays.asList(xcmConfiguration.DOMAIN_LIST);
        if ((splitaddress.length != 2) || (!domainlist.contains(splitaddress[1])))
        {
            // the mailaddress has a strange form or has an recipient with a domain-part that does not belong to our
            // domains
            // log status 500 (relay denied)
            if (xcmConfiguration.MTX_MAX_AGE != 0)
            { // if mailtransaction.maxage is set to 0 -> log nothing
                MailTransaction mtx = new MailTransaction(500, from, null, recipient);
                mailrSenderFactory.addMtxToJCList(mtx);

            }
            return false;
        }
        else
        {
            return true;
        }
    }

    @Override
    public void deliver(String from, String recipient, InputStream data)
    {
        Session session = mailrSenderFactory.getSession();
        session.setDebug(true);
        MimeMessage mail;
        try
        {
            mail = new MimeMessage(session, data);

            MailTransaction mtx;
            String[] splitAddress;
            Address forwardAddress;
            String forwardTarget;
            MBox mailBox;

            splitAddress = recipient.split("@");

            if (splitAddress.length != 2)
            { // the mailaddress does not have the expected pattern -> do nothing, just log it
                if (xcmConfiguration.MTX_MAX_AGE != 0)
                {// if mailtransaction.maxage is set to 0 -> log nothing
                    mtx = new MailTransaction(0, from, null, recipient);
                    mtx.save();
                }
                return;
            }

            if (MBox.mailExists(splitAddress[0], splitAddress[1]))
            { // the given mailaddress exists in the db
                mailBox = MBox.getByName(splitAddress[0], splitAddress[1]);
                forwardTarget = MBox.getFwdByName(splitAddress[0], splitAddress[1]);

                if (mailBox.isActive())
                { // there's an existing and active mailaddress
                  // add the target-address to the list

                    try
                    {
                        forwardAddress = new InternetAddress(forwardTarget);
                        mail.setRecipient(Message.RecipientType.TO, forwardAddress);
                        mail.removeHeader("Cc");
                        mail.removeHeader("BCC");
                        // send the mail in a separate thread
                        MailrMessageSenderFactory.ThreadedMailSend tms = mailrSenderFactory.new ThreadedMailSend(mail,
                                                                                                                 mailBox);
                        tms.start();
                    }
                    catch (AddressException e)
                    {
                        log.error(e.getMessage());
                        // the message can't be forwarded (has not the correct format)
                        // this SHOULD never be the case...
                        if (xcmConfiguration.MTX_MAX_AGE != 0)
                        {// if mailtransaction.maxage is set to 0 -> log nothing
                            mtx = new MailTransaction(400, from, recipient, forwardTarget);
                            //mtx.saveTx();
                            mailrSenderFactory.addMtxToJCList(mtx);
                        }
                    }
                }
                else
                { // there's a mailaddress, but the forward is inactive
                    if (xcmConfiguration.MTX_MAX_AGE != 0)
                    { // if mailtransaction.maxage is set to 0 -> log nothing
                        mtx = new MailTransaction(200, from, recipient, forwardTarget);
//                        mtx.saveTx();
                        mailrSenderFactory.addMtxToJCList(mtx);
                    }
                    mailBox.increaseSuppressions();
                    mailBox.update();
                }
            }
            else
            { // mailaddress/forward does not exist
                if (xcmConfiguration.MTX_MAX_AGE != 0)
                { // if mailtransaction.maxage is set to 0 -> log nothing
                    mtx = new MailTransaction(100, from, recipient, null);
//                    mtx.saveTx();
                    mailrSenderFactory.addMtxToJCList(mtx);
                }
            }
        }
        catch (MessagingException e)
        {
            // the message-creation-process failed
            // either the session can't be created or the input-stream was wrong
            log.error(e.getMessage());
        }
    }
}
