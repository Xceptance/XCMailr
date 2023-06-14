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

import java.nio.charset.StandardCharsets;

/**
 * A collection of utility methods.
 */
public class Utils
{
    private static final String HEX = "0123456789ABCDEF";

    private static final String ALPHA_NUMERIC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final String UNRESERVED = ALPHA_NUMERIC + "-._~";

    private static final String ALLOWED_CHARACTERS_IN_PATH_SEGMENT = UNRESERVED + ":@" + "!$'()*+,;" + "&=";

    private static final String ALLOWED_CHARACTERS_IN_QUERY = UNRESERVED + ":@" + "!$'()*+,;" + "/?";

    /**
     * Encodes a string such that it can safely be used as a path segment in an URI path.
     * 
     * @param pathSegment
     *            the path segment to encode
     * @return the encoded path segment
     */
    public static String encodePathSegment(final String pathSegment)
    {
        return encode(pathSegment, ALLOWED_CHARACTERS_IN_PATH_SEGMENT, "%20");
    }

    /**
     * Encodes a string such that it can safely be used as name or value in the query string of an URI.
     * 
     * @param queryParameter
     *            the query parameter name or value to encode
     * @return the encoded query parameter name or value
     */
    public static String encodeQueryParameter(final String queryParameter)
    {
        return encode(queryParameter, ALLOWED_CHARACTERS_IN_QUERY, "+");
    }

    /**
     * Encodes a string such that all characters not contained in the allowed characters are percent-encoded.
     * 
     * @param s
     *            the string to encode
     * @param allowedCharacters
     *            a string listing all those characters that don't have to be encoded
     * @param spaceEncoding
     *            the special encoding to use for the space character
     * @return the encoded string
     */
    private static String encode(final String s, final String allowedCharacters, final String spaceEncoding)
    {
        final StringBuilder sb = new StringBuilder();
        final char[] chars = s.toCharArray();

        for (int i = 0; i < chars.length; i++)
        {
            final char c = chars[i];

            if (allowedCharacters.indexOf(c) >= 0)
            {
                sb.append(c);
            }
            else if (c == ' ')
            {
                sb.append(spaceEncoding);
            }
            else
            {
                final byte[] bytes = new String(chars, i, 1).getBytes(StandardCharsets.UTF_8);

                for (int j = 0; j < bytes.length; j++)
                {
                    final byte b = bytes[j];

                    final int high = (b & 0xF0) >> 4;
                    final int low = (b & 0xF);

                    sb.append('%').append(HEX.charAt(high)).append(HEX.charAt(low));
                }
            }
        }

        return sb.toString();
    }

    /**
     * Checks that the value of the passed method parameter is not <code>null</code>.
     * 
     * @param parameterValue
     *            the parameter value
     * @param parameterName
     *            the parameter name
     * @throws IllegalArgumentException
     *             if the value is <code>null</code>
     */
    public static void notNull(final Object parameterValue, final String parameterName) throws IllegalArgumentException
    {
        if (parameterValue == null)
        {
            throw new IllegalArgumentException("Parameter '" + parameterName + "' must not be null");
        }
    }

    /**
     * Checks that the value of the passed method parameter is neither <code>null</code> nor blank.
     * 
     * @param parameterValue
     *            the parameter value
     * @param parameterName
     *            the parameter name
     * @throws IllegalArgumentException
     *             if the value is <code>null</code> or blank
     */
    public static void notBlank(final String parameterValue, final String parameterName) throws IllegalArgumentException
    {
        if (parameterValue == null || parameterValue.isBlank())
        {
            throw new IllegalArgumentException("Parameter '" + parameterName + "' must neither be null nor empty nor blank");
        }
    }
}
