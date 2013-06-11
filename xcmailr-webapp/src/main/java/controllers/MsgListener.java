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
public class MsgListener implements SimpleMessageListener
{
    @Inject
    XCMailrConf xcmConf;

    @Inject
    NinjaProperties ninjaProp;

    @Inject
    MailrMessageSenderFactory mmhf;

    @Inject
    Logger log;

    @Override
    public boolean accept(String from, String recipient)
    {
        String[] splitaddress = recipient.split("@");

        List<String> domainlist = Arrays.asList(xcmConf.DM_LIST);
        if ((splitaddress.length != 2) || (!domainlist.contains(splitaddress[1])))
        {
            MailTransaction mtx = new MailTransaction(500, from, null, recipient);
            mtx.saveTx();
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
        Session session = mmhf.getSession();
        session.setDebug(true);
        MimeMessage mail;
        try
        {
            mail = new MimeMessage(session, data);

            MailTransaction mtx;
            String[] splitAddress;
            Address fwdAddress;
            String fwdTarget;
            MBox mb;

            splitAddress = recipient.split("@");

            if (!(splitAddress.length == 2))
            { // the mailaddress does not have the expected pattern -> do nothing, just log it
                mtx = new MailTransaction(0, from, null, recipient);
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
                        mail.setRecipient(Message.RecipientType.TO, fwdAddress);
                        mail.removeHeader("Cc");
                        mail.removeHeader("BCC");
                        // send the mail in a separate thread
                        mmhf.new ThreadedMailSend(mail, mb);
                    }
                    catch (AddressException e)
                    {
                        log.error(e.getMessage());
                        // the message can't be forwarded (has not the correct format)
                        // this SHOULD never be the case...
                        mtx = new MailTransaction(400, from, recipient, fwdTarget);
                        mtx.saveTx();
                    }
                }
                else
                { // there's a mailaddress, but the forward is inactive
                    mtx = new MailTransaction(200, from, recipient, null);
                    mtx.saveTx();
                    mb.increaseSuppressions();
                    mb.update();
                }
            }
            else
            { // mailaddress/forward does not exist
                mtx = new MailTransaction(100, from, recipient, null);
                mtx.saveTx();
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
