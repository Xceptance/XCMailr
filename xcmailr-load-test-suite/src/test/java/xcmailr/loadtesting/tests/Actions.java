/*
 * Copyright (c) 2020 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xcmailr.loadtesting.tests;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.mail.HtmlEmail;
import org.junit.Assert;

import com.xceptance.xlt.api.tests.AbstractTestCase;

import xcmailr.client.Mail;
import xcmailr.client.MailFilterOptions;
import xcmailr.client.Mailbox;
import xcmailr.client.XCMailrClient;
import xcmailr.loadtesting.util.Utils;

/**
 * The basic actions that are needed to create XCMailr test scenarios.
 */
public class Actions extends AbstractTestCase
{
    /**
     * Sends a mail to XCMailr.
     */
    public static void sendMail(final String from, final String to, final String subject, final String htmlContent,
                                final String textContent)
        throws Throwable
    {
        Utils.executeAction("SendMail", () -> {

            // compose the mail
            final HtmlEmail email = new HtmlEmail();

            email.setMailSession(Utils.getMailSession());

            email.setCharset(StandardCharsets.UTF_8.name());

            email.setSubject(subject);
            email.setFrom(from);
            email.addTo(to);

            email.setHtmlMsg(htmlContent);
            email.setTextMsg(textContent);

            // finally send the mail
            email.send();

            return null;
        });
    }

    /**
     * Checks that a mail with the given subject can be found at XCMailr. The method will poll the server until the mail
     * was found or 10 seconds have passed.
     */
    public static void checkMail(final String from, final String to, final String subject, final String htmlContent,
                                 final String textContent)
        throws Throwable
    {
        Utils.executeAction("CheckMail", () -> {

            int attempts = 10;
            while (true)
            {
                attempts--;

                try
                {
                    final XCMailrClient xcMailrClient = Utils.getXCMailrClient();

                    // fetch the matching mails
                    final List<Mail> mails = xcMailrClient.mails().listMails(to, new MailFilterOptions().subjectPattern(subject));
                    Assert.assertEquals(1, mails.size());

                    // validate the mail
                    final Mail mail = mails.get(0);
                    Assert.assertEquals(from, mail.sender);
                    Assert.assertEquals(to, mail.recipient);
                    Assert.assertEquals(subject, mail.subject);
                    Assert.assertEquals(htmlContent, mail.htmlContent);
                    Assert.assertEquals(textContent, mail.textContent);
                    Assert.assertEquals(1, mails.size());

                    return null;
                }
                catch (final Throwable t)
                {
                    if (attempts == 0)
                    {
                        throw t;
                    }
                }

                Thread.sleep(1000);
            }
        });
    }

    /**
     * Creates a mailbox with the given address at the XCMailr instance. The mailbox will be active for 10 minutes and
     * mails received for this mailbox will not be forwarded.
     */
    public static Mailbox createMailbox(final String mailboxAddress) throws Throwable
    {
        return Utils.executeAction("CreateMailbox", () -> {

            final XCMailrClient xcMailrClient = Utils.getXCMailrClient();
            final Mailbox mailbox = xcMailrClient.mailboxes().createMailbox(mailboxAddress, 10, false);

            Assert.assertEquals(mailboxAddress, mailbox.address);
            Assert.assertEquals(false, mailbox.forwardEnabled);

            return mailbox;
        });
    }

    /**
     * Deletes the mailbox with the given address at the XCMailr instance.
     */
    public static void deleteMailbox(final String mailboxAddress) throws Throwable
    {
        Utils.executeAction("DeleteMailbox", () -> {

            final XCMailrClient xcMailrClient = Utils.getXCMailrClient();
            xcMailrClient.mailboxes().deleteMailbox(mailboxAddress);

            return null;
        });
    }
}
