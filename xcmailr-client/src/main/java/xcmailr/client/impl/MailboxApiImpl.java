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
package xcmailr.client.impl;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;

import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import xcmailr.client.Mailbox;
import xcmailr.client.MailboxApi;

/**
 * Implements the {@link MailboxApi}.
 */
public class MailboxApiImpl extends AbstractApiImpl implements MailboxApi
{
    private static final String RELATIVE_PATH = "mailboxes";

    /**
     * The type of <code>List&lt;Mailbox&gt;</code>.
     */
    private static final Type mailboxListType = new TypeToken<List<Mailbox>>()
    {
    }.getType();

    /**
     * Creates a new {@link MailboxApiImpl} instance.
     * 
     * @param client
     *            the REST API client to use
     * @param gson
     *            the GSON parser to use
     */
    public MailboxApiImpl(final RestApiClient client, final Gson gson)
    {
        super(client, gson);
    }

    /**
     * {@inheritDoc}
     */
    public List<Mailbox> listMailboxes() throws Exception
    {
        final HttpResponse<String> response = client.executeRequest(HttpMethod.GET, RELATIVE_PATH);
        checkStatusCode(response, 200);

        return gson.fromJson(response.body(), mailboxListType);
    }

    /**
     * {@inheritDoc}
     */
    public Mailbox createMailbox(final String address, final int minutesActive, final boolean forwardEnabled) throws Exception
    {
        Utils.notBlank(address, "address");

        final long deactivationTime = now().plus(minutesActive, MINUTES).toEpochMilli();

        return createMailbox(new Mailbox(address, deactivationTime, forwardEnabled));
    }

    /**
     * {@inheritDoc}
     */
    public Mailbox createMailbox(final Mailbox mailbox) throws Exception
    {
        Utils.notNull(mailbox, "mailbox");
        Utils.notBlank(mailbox.address, "mailbox.address");

        final String json = gson.toJson(mailbox);

        final HttpResponse<String> response = client.executeRequest(HttpMethod.POST, RELATIVE_PATH, json);
        checkStatusCode(response, 200, 201);

        return gson.fromJson(response.body(), Mailbox.class);
    }

    /**
     * {@inheritDoc}
     */
    public Mailbox getMailbox(final String address) throws Exception
    {
        Utils.notBlank(address, "address");

        final HttpResponse<String> response = client.executeRequest(HttpMethod.GET, RELATIVE_PATH + "/" + Utils.encodePathSegment(address));
        checkStatusCode(response, 200);

        return gson.fromJson(response.body(), Mailbox.class);
    }

    /**
     * {@inheritDoc}
     */
    public Mailbox updateMailbox(final String address, final String newAddress, final int minutesActive, final boolean forwardEnabled)
        throws Exception
    {
        final long deactivationTime = now().plus(minutesActive, MINUTES).toEpochMilli();
        final Mailbox mailbox = new Mailbox(newAddress, deactivationTime, forwardEnabled);

        return updateMailbox(address, mailbox);
    }

    /**
     * {@inheritDoc}
     */
    public Mailbox updateMailbox(final String address, final Mailbox mailbox) throws Exception
    {
        Utils.notBlank(address, "address");
        Utils.notNull(mailbox, "mailbox");
        Utils.notBlank(mailbox.address, "mailbox.address");

        final String json = gson.toJson(mailbox);

        final HttpResponse<String> response = client.executeRequest(HttpMethod.PUT,
                                                                    RELATIVE_PATH + "/" + Utils.encodePathSegment(address),
                                                                    json);
        checkStatusCode(response, 200);

        return gson.fromJson(response.body(), Mailbox.class);
    }

    /**
     * {@inheritDoc}
     */
    public void deleteMailbox(final String address) throws Exception
    {
        Utils.notBlank(address, "address");

        final HttpResponse<String> response = client.executeRequest(HttpMethod.DELETE,
                                                                    RELATIVE_PATH + "/" + Utils.encodePathSegment(address));
        checkStatusCode(response, 204);
    }
}
