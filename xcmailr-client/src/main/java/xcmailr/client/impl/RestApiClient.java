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
    /**
     * The URL of the XCMailr application, including any context part.
     */
    private final String baseUrl;

    /**
     * The API token that allows access to XCMailr.
     */
    private final String apiToken;

    /**
     * The preconfigured {@link HttpClient} instance to use.
     */
    private final HttpClient httpClient;

    /**
     * Creates a new {@link RestApiClient} instance.
     * 
     * @param baseUrl
     *            the URL of the XCMailr application, including any context part
     * @param apiToken
     *            the API token that allows access to XCMailr
     * @param httpClient
     *            a preconfigured {@link HttpClient} instance
     */
    public RestApiClient(final String baseUrl, final String apiToken, final HttpClient httpClient)
    {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.apiToken = apiToken;
        this.httpClient = httpClient;
    }

    /**
     * Executes an HTTP request with no request body and returns an HTTP response with the response body converted to a
     * string.
     * 
     * @param method
     *            the request method
     * @param relativeUrl
     *            the relative URL (to be appended to the base URL)
     * @return the HTTP response
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    public HttpResponse<String> executeRequest(final HttpMethod method, final String relativeUrl)
        throws IOException, InterruptedException, URISyntaxException
    {
        return executeRequest(method, relativeUrl, null);
    }

    /**
     * Executes an HTTP request with a JSON body and returns an HTTP response with the response body converted to a
     * string.
     * 
     * @param method
     *            the request method
     * @param relativeUrl
     *            the relative URL (to be appended to the base URL)
     * @param jsonBody
     *            the request body in JSON format (may be <code>null</code>)
     * @return the HTTP response
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    public HttpResponse<String> executeRequest(final HttpMethod method, final String relativeUrl, final String jsonBody)
        throws IOException, InterruptedException, URISyntaxException
    {
        return executeRequest(method, relativeUrl, jsonBody, BodyHandlers.ofString());
    }

    /**
     * Executes an HTTP request with no request body and returns a HTTP response with the response body ready to be
     * consumed as stream.
     * 
     * @param method
     *            the request method
     * @param relativeUrl
     *            the relative URL (to be appended to the base URL)
     * @return the HTTP response
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    public HttpResponse<InputStream> executeRequest2(final HttpMethod method, final String relativeUrl)
        throws IOException, InterruptedException, URISyntaxException
    {
        return executeRequest(method, relativeUrl, null, BodyHandlers.ofInputStream());
    }

    /**
     * Executes an HTTP request and returns the HTTP response.
     * 
     * @param method
     *            the request method
     * @param relativeUrl
     *            the relative URL (to be appended to the base URL)
     * @param jsonBody
     *            the request body in JSON format (may be <code>null</code>)
     * @param responseBodyHandler
     *            the handler to use for the response body
     * @return the HTTP response
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    private <T> HttpResponse<T> executeRequest(final HttpMethod method, final String relativeUrl, final String jsonBody,
                                               final BodyHandler<T> responseBodyHandler)
        throws IOException, InterruptedException, URISyntaxException
    {
        // build the final request URI
        final URI uri = new URI(baseUrl + "api/v1/" + relativeUrl);

        // prepare Bearer authentication header value
        final String authHeaderValue = "Bearer " + apiToken;

        // build the request
        final Builder requestBuilder = HttpRequest.newBuilder(uri)
                                                  .header("Authorization", authHeaderValue)
                                                  .header("Accept", "application/json, */*");

        if (jsonBody == null)
        {
            requestBuilder.method(method.toString(), BodyPublishers.noBody());
        }
        else
        {
            requestBuilder.header("Content-Type", "application/json");
            requestBuilder.method(method.toString(), BodyPublishers.ofString(jsonBody));
        }

        final HttpRequest request = requestBuilder.build();

        // execute the request
        return httpClient.send(request, responseBodyHandler);
    }
}
