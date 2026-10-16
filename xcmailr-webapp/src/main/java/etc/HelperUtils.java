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
package etc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public final class HelperUtils
{
    private HelperUtils() { }

    private static final Pattern PATTERN_DATEFORMAT = Pattern.compile("(\\d+){4}[\\-](\\d+){1,2}[\\-](\\d+){1,2}(\\s)(\\d+){1,2}[\\:](\\d+){1,2}");

    /**
     * the input string has the format: yyyy-MM-dd hh:mm
     * 
     * @param input
     *            the (hopefully correct) formatted input-string
     * @return the timestamp as millisecs, or -1 if the String is malformed
     */
    public static Long parseTimeString(String input)
    {
        if (input.equals("0") || input.equals("unlimited"))
        { // return the "TS" for an unlimited Box
            return 0L;
        }
        if (!hasCorrectFormat(input))
        { // wrong input
            return -1L;
        }
        input = input.trim();
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
        return formatter.parseDateTime(input).getMillis();
    }

    /**
     * Takes the Timestamp in milis and parses it to the form "yyyy-MM-dd HH:mm" or to "unlimited", if zero
     * 
     * @param ts_Active
     *            the timestamp
     * @return the parsed timestamp
     */
    public static String parseStringTs(long ts_Active)
    {
        if (ts_Active == 0)
            return "unlimited";

        DateTime dt = new DateTime(ts_Active);
        StringBuilder timeString = new StringBuilder();
        // add a leading "0" if the value is under ten
        timeString.append(dt.getYear()).append("-");
        timeString.append(addZero(dt.getMonthOfYear()));
        timeString.append("-");
        timeString.append(addZero(dt.getDayOfMonth()));
        timeString.append(" ");
        timeString.append(addZero(dt.getHourOfDay()));
        timeString.append(":");
        timeString.append(addZero(dt.getMinuteOfHour()));
        return timeString.toString();

    }

    /**
     * Returns a String with the given number and an appended "0" if the number is between 0 and 9
     * 
     * @param no
     *            the input number
     * @return the number with a leading zero if between 0 and 9
     */
    public static String addZero(int no)
    {
        return no < 10 && no >= 0 ? "0" + no : "" + no;
    }

    /**
     * Checks whether the Input-String is in the Form "dddd-dd-dd dd:dd" (where "d" stands for digit).
     * 
     * @param input
     *            the Input-String to check
     * @return true for a match, false for a mismatch
     */
    public static boolean hasCorrectFormat(String input)
    {
        input = input.trim();
        return PATTERN_DATEFORMAT.matcher(input).matches();
    }



    /**
     * Check the given mail address to match format "localpart@domain". Also checks if domain is configured in XCMailr
     * 
     * @param mailAddress
     * @param domainList
     * @return false if any of the checks fails
     */
    public static boolean checkEmailAddressValidness(String[] mailAddressParts, String[] domainList)
    {
        if (mailAddressParts == null || domainList == null || mailAddressParts.length != 2)
        {
            return false;
        }

        // check if the domain of that email address is available to XCMailr
        boolean foundDomain = false;
        for (String domain : domainList)
        {
            if (domain.equalsIgnoreCase(mailAddressParts[1]))
            {
                foundDomain = true;
                break;
            }
        }

        return foundDomain;
    }

    /**
     * Splits an email address at the '@' and returns an array containing the local and domain part.
     * 
     * @param mailAddress
     *            the mail address
     * @return two-dimensional array representing the local and domain part of the given mail address in case it is
     *         valid, and <code>null</code> otherwise
     */
    public static String[] splitMailAddress(String mailAddress)
    {
        mailAddress = StringUtils.defaultString(mailAddress).trim();
        if (mailAddress.length() > 0)
        {
            return mailAddress.split("@");
        }
        return null;
    }



    /**
     * Reads up to maxSize bytes from data input stream. If the limit is exceeded an {@link SizeLimitExceededException}
     * is thrown.
     * 
     * @param data
     *            an {@link InputStream}
     * @param maxSize
     *            determines the maximum amount of bytes to be read from data
     * @return the streams' data
     * @throws SizeLimitExceededException
     *             if maxSize read limit is exceeded
     * @throws IOException
     *             if an I/O error occurred
     */
    public static byte[] readLimitedAmount(InputStream data, int maxSize) throws IOException
    {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(data.available());
        final long count = IOUtils.copy(data, bos, 4096);

        if (count > maxSize)
        {
            throw new SizeLimitExceededException("Data stream exceeds size limit of " + maxSize + " bytes");
        }

        return bos.toByteArray();
    }
}
