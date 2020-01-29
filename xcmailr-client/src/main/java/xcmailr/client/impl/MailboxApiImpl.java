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
        final HttpResponse<String> response = client.executeRequest("GET", "mailboxes", null);
        checkResponse(response, 200);

        return gson.fromJson(response.body(), mailboxListType);
    }

    /**
     * {@inheritDoc}
     */
    public Mailbox createMailbox(final String address, final int minutesActive, final boolean forwardEnabled) throws Exception
    {
        final long deactivationTime = now().plus(minutesActive, MINUTES).toEpochMilli();

        return createMailbox(new Mailbox(address, deactivationTime, forwardEnabled));
    }

    /**
     * {@inheritDoc}
     */
    public Mailbox createMailbox(final Mailbox mailbox) throws Exception
    {
        final String json = gson.toJson(mailbox);

        final HttpResponse<String> response = client.executeRequest("POST", "mailboxes", json);
        checkResponse(response, 200, 201);

        return gson.fromJson(response.body(), Mailbox.class);
    }

    /**
     * {@inheritDoc}
     */
    public Mailbox getMailbox(final String address) throws Exception
    {
        final HttpResponse<String> response = client.executeRequest("GET", "mailboxes/" + address, null);
        checkResponse(response, 200);

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
        final String json = gson.toJson(mailbox);

        final HttpResponse<String> response = client.executeRequest("PUT", "mailboxes/" + address, json);
        checkResponse(response, 200);

        return gson.fromJson(response.body(), Mailbox.class);
    }

    /**
     * {@inheritDoc}
     */
    public void deleteMailbox(final String address) throws Exception
    {
        final HttpResponse<String> response = client.executeRequest("DELETE", "mailboxes/" + address, null);
        checkResponse(response, 204);
    }
}
