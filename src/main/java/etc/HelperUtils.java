/**  
 *  Copyright 2013 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package etc;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.joda.time.DateTime;
import ninja.Context;
import ninja.utils.NinjaProperties;
import com.google.inject.Singleton;

/**
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Singleton
public class HelperUtils
{

    /**
     * Generates a random name, generated with {@link java.util.Random} and an Alphabet of 0-9,a-z,A-Z <br/>
     * e.g. for the Mailbox
     * 
     * @param length
     *            Length of the returned String
     * @return a randomly generated String consisting of a-z,A-Z and 0-9
     */

    public static String getRndString(int length)
    {
        char[] values =
            {
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
                'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9'
            };

        Random rand = new Random();
        StringBuffer strBuf = new StringBuffer();
        for (int i = 0; i < length; i++)
        {
            // generates a random number and stores it in the stringbuffer
            strBuf.append(values[Math.abs(rand.nextInt()) % (values.length)]);

        }
        return strBuf.toString();
    }

    /**
     * All in all, the same like the getRndString(), but here's {@link java.security.SecureRandom} used
     * 
     * @param length
     *            Length of the returned String
     * @return a secure-randomly generated String consisting of a-z,A-Z and 0-9
     */
    public static String getRndSecureString(int length)
    {
        SecureRandom srnd = new SecureRandom();
        srnd.setSeed(srnd.generateSeed(23));
        char[] values =
            {
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
                'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9'
            };

        StringBuffer strBuf = new StringBuffer();
        for (int i = 0; i < length; i++)
        {
            // generates a random number and stores it in the stringbuffer
            strBuf.append(values[Math.abs(srnd.nextInt()) % (values.length)]);
        }
        return strBuf.toString();
    }

    /**
     * Checks if a given String is in the correct Format <br/>
     * There should be specified 1 or 2 time-values (e.g. 1d,1h or 1h,1d or 1h or 1d or 0)<br/>
     * Uppercase-Letters will be converted and Whitespaces removed <br/>
     * The highest Time-Interval is set to 30 days, everything above will be set to 0 (unlimited)
     * 
     * @param s
     *            the String to parse
     * @return the Duration which was given by the String
     */
    public static long parseDuration(String s)
    {
        int d = 0;
        int h = 0;

        // we want also cover cases like "2H,2D", so convert the string to lowercase
        s = s.toLowerCase();
        // remove the spaces to cover cases like "2 h, 2 d"
        s = s.replace(" ", "");

        if (s.equals("0"))
        {
            return 0;
        }

        if (hasCorrectFormat(s))
        {
            // if possible: split the given String
            String[] str = s.split(",");
            String helper = "";

            // walk through the array
            for (int i = 0, len = str.length; i < len; i++)
            {
                if (str[i].contains("d"))
                { // our string has a value for the day
                  // try to get the number
                    helper = str[i].substring(0, str[i].indexOf('d'));
                    // try to parse the number
                    d = digitsOnly(helper);
                }
                else if (str[i].contains("h"))
                {
                    helper = str[i].substring(0, str[i].indexOf('h'));
                    h = digitsOnly(helper);
                }
            }
            if ((d == -1) || (h == -1))
            {
                return -1;
            }
        }
        else
        {
            return -1;
        }

        // everything is okay with the String
        // so return the milisecs
        if (h >= 24)
        {
            d += h / 24;
            h = h % 24;
        }
        if ((d > 30) || ((d == 30) && (h >= 0)))
        {
            // max 30days allowed, higher means unlimited
            return 0;
        }

        DateTime dt = new DateTime();
        return dt.plusDays(d).plusHours(h).getMillis();
    }

    /**
     * Helper-Function for parseDuration() <br/>
     * Checks whether a String consists only of digits
     * 
     * @param helper
     *            the String to check
     * @return the Integer-Value of the String or -1 if the String does not match
     */
    public static int digitsOnly(String helper)
    {
        helper = helper.trim();
        if (helper.matches("[0-9]+"))
        {
            return Integer.parseInt(helper);
        }
        else
        {
            return -1;
        }
    }

    /**
     * Checks whether the Input-String is in the Form [digit][d or h][,][digit][d or h] or [digit][d or h] or [0]
     * 
     * @param helper
     *            the Input-String to check
     * @return true for a match, false for a mismatch
     */
    public static boolean hasCorrectFormat(String helper)
    {
        helper = helper.trim();
        if (helper.matches("(\\d+)[d|h][,](\\d+)[d|h]") || helper.matches("(\\d+)[d|h]"))
        {
            return true;

        }
        else
        {
            return false;
        }
    }

    /**
     * Gets a Timestamp in Milliseconds and parses the Time-Interval to this Timestamp in a readable way
     * 
     * @param millis
     *            Timestamp in Milliseconds
     * @return the Duration to this Date (with the Pattern: 1h, 1d) or a Default-Value (if the given Timestamp was in
     *         the past)
     */
    public static String parseTime(Long millis)
    {
        if (millis == 0)
        { // the box is "unlimited"
            return "0";
        }

        // calculate the time from now to the given timestamp in hours
        float times = (millis - DateTime.now().getMillis()) / 3600000.0f; // in hours

        if (times < 0)
        { // the box is expired, return a default value
            return "1h,1d";
        }
        // round the hours to a full hour
        int hours = Math.round(times);

        // get the days
        int days = hours / 24;
        // calculate the hours of a day
        hours = hours % 24;

        return hours + "h," + days + "d";
    }

    /**
     * Helper-Function for Pagination Tries to parse the number (Parameter "no") from the Context and stores this number
     * in the session-cookie.<br/>
     * If the Parameter has been set to "all", the number 0 is set.<br/>
     * If no number (except "all") is given, the number 5 will be set. If null, there will be a separated check, whether
     * a value is already set
     * 
     * @param context
     *            the Context
     */
    public static void parseEntryValue(Context context)
    {

        String no = context.getParameter("no");
        String value;
        if (no == null)
        {// no parameter
            if (!(context.getSessionCookie().get("no") == null))
            {
                return;
            }
            value = "5";
        }
        else
        {
            if (no.equals("all"))
            {
                value = "0";
            }
            else
            {
                try
                {
                    value = no;
                }
                catch (NumberFormatException nfe)
                {
                    value = "5";
                }
            }
        }
        context.getSessionCookie().put("no", value);
    }

}
