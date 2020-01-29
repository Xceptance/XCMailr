package xcmailr.client;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import com.google.gson.Gson;

import xcmailr.client.impl.MailApiImpl;
import xcmailr.client.impl.MailboxApiImpl;
import xcmailr.client.impl.RestApiClient;

/**
 * Entry point of the XCMailr REST API client.
 */
public class XCMailrApi
{
    /**
     * The {@link Gson} instance that is responsible to parse and generate JSON responses.
     */
    private final Gson gson;

    /**
     * An HTTP client to access the REST API of XCMailr.
     */
    private final RestApiClient client;

    /**
     * Creates a new {@link XCMailrApi} instance.
     * 
     * @param baseUrl
     *            the URL of the XCMailr application, including any context part
     * @param apiToken
     *            the API token that allows access to XCMailr
     * @throws URISyntaxException
     * @throws MalformedURLException
     */
    public XCMailrApi(final String baseUrl, final String apiToken) throws MalformedURLException, URISyntaxException
    {
        client = new RestApiClient(baseUrl, apiToken);
        gson = new Gson();
    }

    /**
     * Returns the {@link Mailbox}-part of the API.
     * 
     * @return the mailbox API
     */
    public MailboxApi mailboxes()
    {
        return new MailboxApiImpl(client, gson);
    }

    /**
     * Returns the {@link Mail}-part of the API.
     * 
     * @return the mail API
     */
    public MailApi mails()
    {
        return new MailApiImpl(client, gson);
    }
}
