/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Test;

import com.xceptance.xlt.api.tests.AbstractTestCase;

import xcmailr.client.Mailbox;

/**
 * Simulates the typical usage scenario of XCMailr. Creates a new mailbox at XCMailr, sends a test mail to it, retrieves
 * that mail from the mailbox and checks it, and finally deletes the mailbox again.
 */
public class SendAndCheckMail extends AbstractTestCase
{
    private Mailbox mailbox;

    @Test
    public void test() throws Throwable
    {
        // create mailbox
        final String mailboxAddress = RandomStringUtils.randomAlphabetic(16) + "@xcmailr.test";
        mailbox = Actions.createMailbox(mailboxAddress);

        // send test mail to that mailbox
        final String from = "john.doe@example.com";
        final String to = mailboxAddress;
        final String subject = "Test Mail - " + RandomStringUtils.randomAlphanumeric(16);
        final String htmlContent = RandomStringUtils.randomAlphanumeric(4096);
        final String textContent = RandomStringUtils.randomAlphanumeric(4096);

        Actions.sendMail(from, to, subject, htmlContent, textContent);

        // check test mail has arrived in that mailbox
        Actions.checkMail(from, to, subject, htmlContent, textContent);
    }

    @After
    public void cleanUp() throws Throwable
    {
        // delete mailbox again
        if (mailbox != null)
        {
            Actions.deleteMailbox(mailbox.address);
        }
    }
}
