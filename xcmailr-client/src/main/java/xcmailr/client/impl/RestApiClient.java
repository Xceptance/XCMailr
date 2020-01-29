package xcmailr.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;

/**
 * Simple HTTP client to communicate with the XCMailr REST API.
 */
public class RestApiClient
{
    private final String baseUri;

    private final String apiToken;

    private final HttpClient httpClient;

    public RestApiClient(final String baseUrl, final String apiToken)
    {
        this.baseUri = baseUrl;
        this.apiToken = apiToken;

        httpClient = HttpClient.newHttpClient();
    }

    public HttpResponse<String> executeRequest(final String method, final String relativeUrl, final String jsonBody)
        throws IOException, InterruptedException, URISyntaxException
    {
        return executeRequest(method, relativeUrl, jsonBody, BodyHandlers.ofString());
    }

    public HttpResponse<InputStream> executeRequest2(final String method, final String relativeUrl, final String jsonBody)
        throws IOException, InterruptedException, URISyntaxException
    {
        return executeRequest(method, relativeUrl, jsonBody, BodyHandlers.ofInputStream());
    }

    private <T> HttpResponse<T> executeRequest(final String method, final String relativeUrl, final String jsonBody,
                                               final BodyHandler<T> bodyHandler)
        throws IOException, InterruptedException, URISyntaxException
    {
        // build the final request URI
        final URI uri = new URI(baseUri + "api/v1/" + relativeUrl);

        System.err.printf("### %s\n", uri);

        // prepare Bearer authentication header value
        final String authHeaderValue = "Bearer " + apiToken;

        // build the request
        final Builder requestBuilder = HttpRequest.newBuilder(uri)
                                                  .header("Authorization", authHeaderValue)
                                                  .header("Accept", "application/json, */*");

        if (jsonBody == null)
        {
            requestBuilder.method(method, BodyPublishers.noBody());
        }
        else
        {
            requestBuilder.header("Content-Type", "application/json");
            requestBuilder.method(method, BodyPublishers.ofString(jsonBody));
        }

        final HttpRequest request = requestBuilder.build();

        // send the request
        return httpClient.send(request, bodyHandler);
    }
}
