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

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import xcmailr.client.Mail;
import xcmailr.client.MailApi;
import xcmailr.client.MailFilterOptions;

/**
 * Implements the {@link MailApi}.
 */
public class MailApiImpl extends AbstractApiImpl implements MailApi
{
    /**
     * The type of <code>List&lt;Mail&gt;</code>.
     */
    private static final Type mailListType = new TypeToken<List<Mail>>()
    {
    }.getType();

    /**
     * Creates a new {@link MailApiImpl} instance.
     * 
     * @param client
     *            the REST API client to use
     * @param gson
     *            the GSON parser to use
     */
    public MailApiImpl(final RestApiClient client, final Gson gson)
    {
        super(client, gson);
    }

    /**
     * {@inheritDoc}
     */
    public List<Mail> listMails(final String mailboxAddress, final MailFilterOptions options) throws Exception
    {
        Utils.notBlank(mailboxAddress, "mailboxAddress");

        final StringBuilder url = new StringBuilder("mails?");
        appendQueryParameter(url, "mailboxAddress", mailboxAddress);

        if (options != null)
        {
            appendQueryParameter(url, "from", options.senderPattern);
            appendQueryParameter(url, "subject", options.subjectPattern);
            appendQueryParameter(url, "mailHeader", options.headersPattern);
            appendQueryParameter(url, "htmlContent", options.htmlContentPattern);
            appendQueryParameter(url, "textContent", options.textContentPattern);
            appendQueryParameter(url, "lastMatch", String.valueOf(options.lastMatchOnly));
        }

        final HttpResponse<String> response = client.executeRequest(HttpMethod.GET, url.toString());
        checkStatusCode(response, 200);

        return gson.fromJson(response.body(), mailListType);
    }

    /**
     * {@inheritDoc}
     */
    public Mail getMail(final long mailId) throws Exception
    {
        final HttpResponse<String> response = client.executeRequest(HttpMethod.GET, "mails/" + mailId);
        checkStatusCode(response, 200);

        return gson.fromJson(response.body(), Mail.class);
    }

    /**
     * {@inheritDoc}
     */
    public InputStream openAttachment(final long mailId, final String attachmentName) throws Exception
    {
        Utils.notBlank(attachmentName, "attachmentName");

        final HttpResponse<InputStream> response = client.executeRequest2(HttpMethod.GET,
                                                                          "mails/" + mailId + "/attachments/"
                                                                                          + Utils.encodePathSegment(attachmentName));
        checkStatusCode(response, 200);

        return response.body();
    }

    /**
     * {@inheritDoc}
     */
    public void deleteMail(final long mailId) throws Exception
    {
        final HttpResponse<String> response = client.executeRequest(HttpMethod.DELETE, "mails/" + mailId);
        checkStatusCode(response, 204);
    }
}
