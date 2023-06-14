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
package xcmailr.client;

/**
 * An exception thrown whenever XCMailr responded with a status code that indicates an error.
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
        final String body = responseBody == null ? "<null>" : responseBody.isEmpty() ? "<empty>" : responseBody;

        final StringBuilder sb = new StringBuilder();

        sb.append(message).append('\n');
        sb.append("  Status Code  : ").append(statusCode).append('\n');
        sb.append("  Response Body: ").append(body);

        return sb.toString();
    }
}
