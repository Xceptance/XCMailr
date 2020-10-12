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
package xcmailr.client;

import java.net.http.HttpClient;

import com.google.gson.Gson;

import xcmailr.client.impl.MailApiImpl;
import xcmailr.client.impl.MailboxApiImpl;
import xcmailr.client.impl.RestApiClient;
import xcmailr.client.impl.Utils;

/**
 * The entry point of the XCMailr REST API client.
 * <p>
 * In order to get access to the REST API, you'll need an API token. To get such a token, log in to XCMailr and create
 * an API token for your account.
 */
public class XCMailrClient
{
    /**
     * The API endpoint responsible for the management of mails in a mailbox.
     */
    private final MailApi mailApi;

    /**
     * The API endpoint responsible for the management of mailboxes.
     */
    private final MailboxApi mailboxApi;

    /**
     * Creates a new {@link XCMailrClient} instance which will use a default {@link HttpClient} instance.
     * 
     * @param baseUrl
     *            the URL of the XCMailr application, including any context part
     * @param apiToken
     *            the API token that allows access to XCMailr
     */
    public XCMailrClient(final String baseUrl, final String apiToken)
    {
        this(baseUrl, apiToken, HttpClient.newHttpClient());
    }

    /**
     * Creates a new {@link XCMailrClient} instance.
     * 
     * @param baseUrl
     *            the URL of the XCMailr application, including any context part
     * @param apiToken
     *            the API token that allows access to XCMailr
     * @param httpClient
     *            a preconfigured {@link HttpClient} instance
     */
    public XCMailrClient(final String baseUrl, final String apiToken, final HttpClient httpClient)
    {
        Utils.notBlank(baseUrl, "baseUrl");
        Utils.notBlank(apiToken, "apiToken");
        Utils.notNull(httpClient, "httpClient");

        final RestApiClient client = new RestApiClient(baseUrl, apiToken, httpClient);
        final Gson gson = new Gson();

        mailApi = new MailApiImpl(client, gson);
        mailboxApi = new MailboxApiImpl(client, gson);
    }

    /**
     * Returns the API endpoint responsible for the management of mailboxes.
     * 
     * @return the mailbox API
     */
    public MailboxApi mailboxes()
    {
        return mailboxApi;
    }

    /**
     * Returns the API endpoint responsible for the management of mails in a mailbox.
     * 
     * @return the mail API
     */
    public MailApi mails()
    {
        return mailApi;
    }
}
