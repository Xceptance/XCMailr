/*
 *  Copyright 2020 by the original author or authors.
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
 */
package testutils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;

import ninja.utils.NinjaTestBrowser;
import ninja.utils.NinjaTestServer;

/**
 * Simple HTTP client for REST API testing.
 */
public class RestApiClient
{
    private final NinjaTestServer ninjaTestServer;

    private final NinjaTestBrowser ninjaTestBrowser;

    private String apiToken;

    public RestApiClient(final NinjaTestServer ninjaTestServer, final NinjaTestBrowser ninjaTestBrowser,
        final String apiToken)
    {
        this.ninjaTestServer = ninjaTestServer;
        this.ninjaTestBrowser = ninjaTestBrowser;
        this.apiToken = apiToken;
    }

    public String getApiToken()
    {
        return apiToken;
    }

    public void setApiToken(final String apiToken)
    {
        this.apiToken = apiToken;
    }

    // --- generic ---------------------------------------------

    public HttpResponse executeRequest(final String method, final String relativeUrl, final String jsonBody)
        throws Exception
    {
        final String url = ninjaTestServer.getBaseUrl() + "/api/v1/" + relativeUrl;

        HttpRequestBase request = null;

        switch (method)
        {
            case HttpGet.METHOD_NAME:
                request = new HttpGet(url);
                break;
            case HttpPost.METHOD_NAME:
                request = new HttpPost(url);
                break;
            case HttpPut.METHOD_NAME:
                request = new HttpPut(url);
                break;
            case HttpDelete.METHOD_NAME:
                request = new HttpDelete(url);
                break;
            default:
                Assert.fail("Unknown method: " + method);
        }

        request.addHeader("Accept", "application/json, */*");

        if (apiToken != null)
        {
            request.addHeader("Authorization", "Bearer " + apiToken);
        }

        if (jsonBody != null)
        {
            ((HttpEntityEnclosingRequest) request).setEntity(new StringEntity(jsonBody,
                                                                              ContentType.create("application/json",
                                                                                                 "UTF-8")));
        }

        try
        {
            final HttpResponse response = ninjaTestBrowser.getHttpClient().execute(request);

            // buffer the response entity in memory so we can release the connection further down
            final HttpEntity entity = response.getEntity();
            if (entity != null)
            {
                response.setEntity(new BufferedHttpEntity(response.getEntity()));
            }

            return response;
        }
        finally
        {
            request.releaseConnection();
        }
    }

    // --- mailboxes ---------------------------------------------

    private static final String jsonBodyTemplate = "{ 'address': '%s', 'deactivationTime': %d, 'forwardEnabled': %b }".replace('\'',
                                                                                                                               '"');

    public HttpResponse listMailboxes() throws Exception
    {
        return executeRequest(HttpGet.METHOD_NAME, "mailboxes", null);
    }

    public HttpResponse createMailbox(final String address, final long expirationDate, final boolean forwardEnabled)
        throws Exception
    {
        final String body = String.format(jsonBodyTemplate, address, expirationDate, forwardEnabled);

        return executeRequest(HttpPost.METHOD_NAME, "mailboxes", body);
    }

    public HttpResponse getMailbox(final String address) throws Exception
    {
        return executeRequest(HttpGet.METHOD_NAME, "mailboxes/" + address, null);
    }

    public HttpResponse updateMailbox(final String address, final String newAddress, final long deactivationTime,
                                      final boolean forwardEnabled)
        throws Exception
    {
        final String body = String.format(jsonBodyTemplate, newAddress, deactivationTime, forwardEnabled);

        return executeRequest(HttpPut.METHOD_NAME, "mailboxes/" + address, body);
    }

    public HttpResponse deleteMailbox(final String address) throws Exception
    {
        return executeRequest(HttpDelete.METHOD_NAME, "mailboxes/" + address, null);
    }

    // --- mails ---------------------------------------------

    public HttpResponse listMails(final String mailboxAddress) throws Exception
    {
        return listMails(mailboxAddress, false, "");
    }

    public HttpResponse listMails(final String mailboxAddress, final String subjectPattern) throws Exception
    {
        return listMails(mailboxAddress, false, subjectPattern);
    }

    public HttpResponse listMails(final String mailboxAddress, final boolean lastMatchOnly) throws Exception
    {
        return listMails(mailboxAddress, lastMatchOnly, "");
    }

    public HttpResponse listMails(final String mailboxAddress, final boolean lastMatchOnly, final String subjectPattern)
        throws Exception
    {
        return executeRequest(HttpGet.METHOD_NAME, "mails?mailboxAddress=" + mailboxAddress + "&lastMatch="
                                                   + lastMatchOnly + "&subject=" + subjectPattern,
                              null);
    }

    public HttpResponse getMail(final String mailId) throws Exception
    {
        return executeRequest(HttpGet.METHOD_NAME, "mails/" + mailId, null);
    }

    public HttpResponse getMailAttachment(final String mailId, final String attachmentName) throws Exception
    {
        return executeRequest(HttpGet.METHOD_NAME, "mails/" + mailId + "/attachments/" + attachmentName, null);
    }

    public HttpResponse deleteMail(final String mailId) throws Exception
    {
        return executeRequest(HttpDelete.METHOD_NAME, "mails/" + mailId, null);
    }
}
