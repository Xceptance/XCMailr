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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;

import com.google.gson.Gson;

import controllers.restapi.MailboxData;
import controllers.restapi.util.ApiError;
import controllers.restapi.util.ApiErrors;
import models.MBox;

/**
 * Common utilities needed when testing the REST API.
 */
public class RestApiTestUtils
{
    /**
     * Validates a {@link MBox} instance by comparing its properties with those given in the {@link MailboxData}
     * instance.
     */
    public static void validateMailbox(final MBox mailbox, final MailboxData mailboxData)
    {
        Assert.assertEquals(mailboxData.address, mailbox.getFullAddress());
        Assert.assertEquals(mailboxData.deactivationTime, mailbox.getTs_Active());
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
        Assert.assertEquals(email, mailbox.address);
        Assert.assertEquals(epirationDate, mailbox.deactivationTime);
        Assert.assertEquals(forwardEnabled, mailbox.forwardEnabled);
    }

    /**
     * Validates the status code of a response.
     */
    public static void validateStatusCode(final HttpResponse response, final int expectedStatusCode)
    {
        final int statusCode = response.getStatusLine().getStatusCode();
        Assert.assertEquals(expectedStatusCode, statusCode);
    }

    /**
     * Validates the response content to be of expected type and length.
     */
    public static void validateResponseContent(final HttpResponse response, final String expectedContentType,
                                               int expectedContentLength)
        throws Exception
    {
        final HttpEntity entity = response.getEntity();

        // check response headers
        Assert.assertTrue(entity.getContentType().getValue().startsWith(expectedContentType));
        Assert.assertEquals(expectedContentLength, entity.getContentLength());

        // check response content
        int actualContentLength = IOUtils.copy(response.getEntity().getContent(), new ByteArrayOutputStream());
        Assert.assertEquals(expectedContentLength, actualContentLength);
    }

    /**
     * Validates an error response by converting it to an array of {@link ApiError} instances and checking that the
     * field names in the errors match the given field names.
     */
    public static void validateErrors(final HttpResponse response, final String... fieldNames) throws Exception
    {
        // extract errors from JSON response
        final ApiErrors errors = getResponseBodyAs(response, ApiErrors.class);

        // validate the name
        Assert.assertEquals(fieldNames.length, errors.errors.size());
        for (int i = 0; i < errors.errors.size(); i++)
        {
            Assert.assertEquals(fieldNames[i], errors.errors.get(i).parameter);
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
