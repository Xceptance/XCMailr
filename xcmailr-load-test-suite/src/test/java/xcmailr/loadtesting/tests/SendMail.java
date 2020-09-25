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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import com.xceptance.xlt.api.tests.AbstractTestCase;

/**
 * Sends a mail to the configured mailbox at XCMailr. Use this scenario to test if XCMailr is able to receive (and
 * optionally store) lots of mails without problems.
 */
public class SendMail extends AbstractTestCase
{
    @Test
    public void test() throws Throwable
    {
        final String from = "john.doe@example.com";
        final String to = "jw@xcmailr.test";
        final String subject = "Test Mail - " + RandomStringUtils.randomAlphanumeric(16);
        final String htmlContent = RandomStringUtils.randomAlphanumeric(4096);
        final String textContent = RandomStringUtils.randomAlphanumeric(4096);

        Actions.sendMail(from, to, subject, htmlContent, textContent);
    }
}
