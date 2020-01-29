package xcmailr.client;

/**
 * An exception thrown whenever XCMailr responded with an error.
 */
public class XCMailrApiException extends Exception
{
    /**
     * The HTTP status code returned.
     */
    public final int statusCode;

    /**
     * The body of the response, typically text in JSON format.
     */
    public final String responseBody;

    /**
     * Creates a new exception.
     * 
     * @param message
     *            the exception message (may be <code>null</code>)
     * @param statusCode
     *            the HTTP status code received as part of the response
     * @param responseBody
     *            the text body of the response (may be <code>null</code>)
     */
    public XCMailrApiException(final String message, final int statusCode, final String responseBody)
    {
        super(buildMessage(message, statusCode, responseBody));

        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * Builds a formatted message text from the passed data. This text will become the message of this exception.
     */
    private static String buildMessage(final String message, final int statusCode, final String responseBody)
    {
        final StringBuilder sb = new StringBuilder();

        sb.append(message).append('\n');
        sb.append("- Status Code: ").append(statusCode).append('\n');
        sb.append("- Response Body: ").append(responseBody);

        return sb.toString();
    }
}
