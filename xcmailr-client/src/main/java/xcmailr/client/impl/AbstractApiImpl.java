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
package xcmailr.client.impl;

import java.net.http.HttpResponse;

import com.google.gson.Gson;

import xcmailr.client.XCMailrApiException;

/**
 * Base class for API implementations.
 */
public abstract class AbstractApiImpl
{
    protected final RestApiClient client;

    protected final Gson gson;

    /**
     * Creates a new {@link AbstractApiImpl} instance.
     * 
     * @param client
     *            the REST API client to use
     * @param gson
     *            the GSON parser to use
     */
    protected AbstractApiImpl(final RestApiClient client, final Gson gson)
    {
        this.client = client;
        this.gson = gson;
    }

    /**
     * Checks that the status code of the response is one of the expected codes. Otherwise an exception is thrown.
     * 
     * @param response
     *            the response to check
     * @param expectedStatusCodes
     *            an array of expected status codes
     * @throws XCMailrApiException
     *             in case of an unexpected status code
     */
    protected void checkStatusCode(final HttpResponse<?> response, final int... expectedStatusCodes) throws XCMailrApiException
    {
        final int actualStatusCode = response.statusCode();

        for (final int expectedStatusCode : expectedStatusCodes)
        {
            if (actualStatusCode == expectedStatusCode)
            {
                return;
            }
        }

        // unexpected status code encountered
        final Object responseBody = response.body();
        final String responseBodyAsText = responseBody instanceof String ? (String) responseBody : null;

        throw new XCMailrApiException("API call failed", actualStatusCode, responseBodyAsText);
    }

    /**
     * Appends a query parameter to the given URL query string, but only if the parameter value is not
     * <code>null</code>. Name and value of the query parameter will be properly encoded.
     * 
     * @param query
     *            the URL query string
     * @param name
     *            the name of the query parameter
     * @param value
     *            the value of the query parameter
     * @return the query string
     */
    protected StringBuilder appendQueryParameter(final StringBuilder query, final String name, final String value)
    {
        if (value != null)
        {
            query.append('&');
            query.append(Utils.encodeQueryParameter(name)).append('=').append(Utils.encodeQueryParameter(value));
        }

        return query;
    }
}
