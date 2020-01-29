package xcmailr.client.impl;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;

import xcmailr.client.XCMailrApiException;

/**
 * Base class for API implementations.
 */
public abstract class AbstractApiImpl
{
    protected final RestApiClient client;

    protected final Gson gson;

    public AbstractApiImpl(final RestApiClient client, final Gson gson)
    {
        this.client = client;
        this.gson = gson;
    }

    protected void checkResponse(final HttpResponse<?> response, final int... expectedStatusCodes) throws Exception
    {
        final int actualStatusCode = response.statusCode();

        for (final int expectedStatusCode : expectedStatusCodes)
        {
            if (actualStatusCode == expectedStatusCode)
            {
                return;
            }
        }

        final Object responseBody = response.body();
        final String responseBodyAsText = responseBody instanceof String ? (String) responseBody : null;

        throw new XCMailrApiException("API call failed", actualStatusCode, responseBodyAsText);
    }

    protected void appendQueryParameter(final StringBuilder query, final String name, final String value)
    {
        if (value != null)
        {
            query.append('&');
            query.append(urlEncode(name)).append('=').append(urlEncode(value));
        }
    }

    /**
     * Encodes the value of a form/URL parameter.
     * 
     * @param value
     *            the value
     * @return the encoded value
     */
    protected static String urlEncode(final String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
