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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ninja.Context;
import ninja.Result;
import ninja.i18n.Messages;
import com.google.common.base.Optional;
import com.google.inject.Singleton;

/**
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Singleton
public class HelperUtils
{

    private static final Pattern PATTERN_DATEFORMAT = Pattern.compile("(\\d+){4}[\\-](\\d+){1,2}[\\-](\\d+){1,2}(\\s)(\\d+){1,2}[\\:](\\d+){1,2}");
    /**
     * Generates a random name, generated with {@link java.util.Random} and an Alphabet of 0-9,a-z,A-Z <br/>
     * e.g. for the Mailbox
     * 
     * @param length
     *            Length of the returned String
     * @return a randomly generated String consisting of a-z,A-Z and 0-9
     */

    public static String getRandomString(int length)
    {
        char[] values =
            {
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
                'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9'
            };

        Random random = new Random();
        StringBuffer stringBuf = new StringBuffer();
        for (int i = 0; i < length; i++)
        {
            // generates a random number and stores it in the stringbuffer
            stringBuf.append(values[Math.abs(random.nextInt()) % (values.length)]);

        }
        return stringBuf.toString();
    }

    /**
     * All in all, the same like the getRndString(), but here's {@link java.security.SecureRandom} used
     * 
     * @param length
     *            Length of the returned String
     * @return a secure-randomly generated String consisting of a-z,A-Z and 0-9
     */
    public static String getRandomSecureString(int length)
    {
        SecureRandom random = new SecureRandom();
        random.setSeed(random.generateSeed(23));
        char[] values =
            {
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
                'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9'
            };

        StringBuffer stringBuf = new StringBuffer();
        for (int actualLength = 0; actualLength < length; actualLength++)
        {
            // generates a random number and stores it in the stringbuffer
            stringBuf.append(values[Math.abs(random.nextInt()) % (values.length)]);
        }
        return stringBuf.toString();
    }

    /**
     * the input string has the format: yyyy-MM-dd hh:mm
     * 
     * @param input
     *            the (hopefully correct) formatted input-string
     * @return the timestamp as millisecs, or -1 if the String is malformed
     */
    public static Long parseTimeString(String input)
    {
        if (input.equals("0"))
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

    public static String parseStringTs(long ts_Active)
    {
        if (ts_Active == 0)
        {
            return "unlimited";
        }
        else
        {
            DateTime dt = new DateTime(ts_Active);
            StringBuilder timeString = new StringBuilder();
            // add a leading "0" if the value is under ten
            timeString.append(dt.getYear()).append("-");

            if (dt.getMonthOfYear() < 10)
            {
                timeString.append("0");
            }
            timeString.append(dt.getMonthOfYear()).append("-");

            if (dt.getDayOfMonth() < 10)
            {
                timeString.append("0");
            }
            timeString.append(dt.getDayOfMonth()).append(" ");

            if (dt.getHourOfDay() < 10)
            {
                timeString.append("0");
            }
            timeString.append(dt.getHourOfDay()).append(":");

            if (dt.getMinuteOfHour() < 10)
            {
                timeString.append("0");
            }
            timeString.append(dt.getMinuteOfHour());

            return timeString.toString();
        }
    }

    /**
     * Checks whether the Input-String is in the Form "dddd-dd-dd dd:dd"<br/>
     * (where "d" stands for digit)
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
     * Helper-Function for Pagination Tries to parse the number (Parameter "no") from the Context and stores this number
     * in the session-cookie.<br/>
     * If the Parameter has been set to "all", the number 0 is set.<br/>
     * If no number (except "all") is given, the number 5 will be set. If null, there will be a separated check, whether
     * a value is already set
     * 
     * @param context
     *            the Context
     */
    public static void parseEntryValue(Context context, Integer defaultNo)
    {
        String no = context.getParameter("no");
        String value;
        if (no == null)
        {// no number-param was delivered

            if (context.getSessionCookie().get("no") != null)
            { // return with no action, because there is already a value set
                return;
            }
            else
            { // set the default-value if no param was set and theres no value in the cookie
                value = defaultNo.toString();
            }
        }
        else
        { // there's a parameter with the key "no"

            if (no.equals("all"))
            { // all entries should be shown
                value = "0";
            }
            else
            { // otherwise set the number
                try
                { // parse the parameter as integer to ensure that it is a number
                    value = String.valueOf(Integer.parseInt(no));
                }
                catch (NumberFormatException nfe)
                { // set to default if its not an integer
                    value = defaultNo.toString();
                }
            }
        }
        // set the number to the session-cookie
        context.getSessionCookie().put("no", value);
    }

    /**
     * Creates a List which contains a string-array with the abbreviated language first and the long form as second item
     * translated to the language which is given primary by the result (or the context, if e.g. the result is null)
     * 
     * @param availableLanguages
     *            the short form of all languages (e.g. "en", "de")
     * @param context
     *            the current user-context
     * @param result
     *            the result-page
     * @param msg
     *            the Messages-object
     * @return a List of String[] with the key "available_langs" and a String[]-object containing the localised long
     *         form of all languages
     */
    public static List<String[]> getLanguageList(String[] availableLanguages, Context context, Result result,
                                                 Messages msg)
    {
        String languageTranslation;
        Optional<Result> optionalResult = Optional.of(result);
        List<String[]> availableLanguageList = new ArrayList<String[]>();
        for (String abbreviatedLanguageCode : availableLanguages)
        {
            languageTranslation = msg.get("lang_" + abbreviatedLanguageCode, context, optionalResult).get();
            availableLanguageList.add(new String[]
                {
                    abbreviatedLanguageCode, languageTranslation
                });
        }
        return availableLanguageList;
    }
}
