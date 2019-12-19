/**
 * Copyright (C) 2012-2019 the original author or authors.
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

package testutils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;

import com.google.gson.Gson;

import controllers.restapi.MailboxData;
import controllers.restapi.util.ApiError;
import models.MBox;

/**
 * Common utilities needed when testing the REST API.
 */
public class RestApiTestUtils
{
    /**
     * Validates the status code of a response.
     */
    public static void validateStatusCode(final HttpResponse response, final int expectedStatusCode)
    {
        final int statusCode = response.getStatusLine().getStatusCode();
        Assert.assertEquals(expectedStatusCode, statusCode);
    }

    /**
     * Validates a {@link MBox} instance by comparing its properties with those given in the {@link MailboxData}
     * instance.
     */
    public static void validateMailbox(final MBox mailbox, final MailboxData mailboxData)
    {
        Assert.assertEquals(mailboxData.email, mailbox.getFullAddress());
        Assert.assertEquals(mailboxData.expirationDate, mailbox.getTs_Active());
        Assert.assertEquals(mailboxData.forwardEnabled, mailbox.isForwardEmails());
    }

    /**
     * Validates a {@link MailboxData} instance by comparing its properties with those given in the {@link MBox}
     * instance.
     */
    public static void validateMailboxData(final MailboxData mailboxData, final MBox mailbox)
    {
        validateMailboxData(mailboxData, mailbox.getFullAddress(), mailbox.getTs_Active(), mailbox.isForwardEmails());
    }

    /**
     * Validates a {@link MailboxData} instance by comparing its properties with the given values.
     */
    public static void validateMailboxData(final MailboxData mailbox, final String email, final long epirationDate,
                                           final boolean forwardEnabled)
    {
        Assert.assertEquals(email, mailbox.email);
        Assert.assertEquals(epirationDate, mailbox.expirationDate);
        Assert.assertEquals(forwardEnabled, mailbox.forwardEnabled);
    }

    /**
     * Validates an error response by converting it to an array of {@link ApiError} instances and checking that the
     * field names in the errors match the given field names.
     */
    public static void validateErrors(final HttpResponse response, final String... fieldNames) throws Exception
    {
        // extract errors from JSON response
        final HttpEntity entity = response.getEntity();
        final String text = EntityUtils.toString(entity);
        final ApiError[] errors = new Gson().fromJson(text, ApiError[].class);

        // validate
        Assert.assertEquals(fieldNames.length, errors.length);
        for (int i = 0; i < errors.length; i++)
        {
            Assert.assertEquals(fieldNames[i], errors[i].field);
        }
    }

    /**
     * Parses the body of the response as a JSON object and converts it to an instance of the given POJO class.
     */
    public static <T> T getResponseBodyAs(final HttpResponse response, final Class<T> c) throws Exception
    {
        final HttpEntity entity = response.getEntity();
        final String text = EntityUtils.toString(entity);

        return new Gson().fromJson(text, c);
    }
}
