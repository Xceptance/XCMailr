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
        final StringBuilder url = new StringBuilder("mails?");
        appendQueryParameter(url, "mailboxAddress", mailboxAddress);

        if (options != null)
        {
            appendQueryParameter(url, "from", options.fromPattern);
            appendQueryParameter(url, "subject", options.subjectPattern);
            appendQueryParameter(url, "mailHeader", options.mailHeaderPattern);
            appendQueryParameter(url, "htmlContent", options.htmlContentPattern);
            appendQueryParameter(url, "textContent", options.textContentPattern);
            appendQueryParameter(url, "lastMatch", String.valueOf(options.lastMatchOnly));
        }

        final HttpResponse<String> response = client.executeRequest("GET", url.toString(), null);
        checkResponse(response, 200);

        return gson.fromJson(response.body(), mailListType);
    }

    /**
     * {@inheritDoc}
     */
    public Mail getMail(final String mailId) throws Exception
    {
        final HttpResponse<String> response = client.executeRequest("GET", "mails/" + mailId, null);
        checkResponse(response, 200);

        return gson.fromJson(response.body(), Mail.class);
    }

    /**
     * {@inheritDoc}
     */
    public InputStream downloadAttachment(final String mailId, final String attachmentName) throws Exception
    {
        final HttpResponse<InputStream> response = client.executeRequest2("GET",
                                                                          "mails/" + mailId + "/attachments/" + attachmentName,
                                                                          null);
        checkResponse(response, 200);

        return response.body();
    }

    /**
     * {@inheritDoc}
     */
    public void deleteMail(final String mailId) throws Exception
    {
        final HttpResponse<String> response = client.executeRequest("DELETE", "mails/" + mailId, null);
        checkResponse(response, 204);
    }
}
