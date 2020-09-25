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
 * Creates a new mailbox at XCMailr and deletes it again. Use this scenario to test the mailbox management functionality
 * at XCMailr under load.
 */
public class CreateAndDeleteMailbox extends AbstractTestCase
{
    @Test
    public void test() throws Throwable
    {
        final String mailboxAddress = RandomStringUtils.randomAlphabetic(16) + "@xcmailr.test";

        Actions.createMailbox(mailboxAddress);
        Actions.deleteMailbox(mailboxAddress);
    }
}
