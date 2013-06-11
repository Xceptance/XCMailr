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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
     * the input string has the format: dd.MM.yyyy hh:mm
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
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm");
        return fmt.parseDateTime(input).getMillis();
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
            String day = "";
            String mon = "";
            String hou = "";
            String min = "";
            // add a leading "0" if the value is under ten
            if (dt.getDayOfMonth() < 10)
            {
                day += "0";
            }
            day += String.valueOf(dt.getDayOfMonth());

            if (dt.getMonthOfYear() < 10)
            {
                mon += "0";
            }
            mon += String.valueOf(dt.getMonthOfYear());

            if (dt.getHourOfDay() < 10)
            {
                hou += "0";
            }
            hou += String.valueOf(dt.getHourOfDay());

            if (dt.getMinuteOfHour() < 10)
            {
                min += "0";
            }
            min += String.valueOf(dt.getMinuteOfHour());

            return day + "." + mon + "." + dt.getYear() + " " + hou + ":" + min;
        }

    }

    /**
     * Checks whether the Input-String is in the Form "dd.dd.dddd dd:dd"
     * 
     * @param helper
     *            the Input-String to check
     * @return true for a match, false for a mismatch
     * @TODO rewrite this for the new format :)
     */
    public static boolean hasCorrectFormat(String helper)
    {
        helper = helper.trim();
        if (helper.matches("(\\d+){1,2}[\\.](\\d+){1,2}[\\.](\\d+){4}(\\s)(\\d+){1,2}[\\:](\\d+){1,2}"))
        {
            return true;
        }
        return false;
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
        {// no parameter
            if (!(context.getSessionCookie().get("no") == null))
            {
                return;
            }
            value = defaultNo.toString();
        }
        else
        {
            if (no.equals("all"))
            {
                value = defaultNo.toString();
            }
            else
            {
                try
                {
                    value = no;
                }
                catch (NumberFormatException nfe)
                {
                    value = defaultNo.toString();
                }
            }
        }
        context.getSessionCookie().put("no", value);
    }

    /**
     * Creates a Map which contains the key "available_langs" with a String[] containing the long form of all languages
     * translated to the language which is given by the context
     * 
     * @param avLangs
     *            the short form of all languages (e.g. "en", "de")
     * @param context
     *            the current user-context
     * @param msg
     *            the Messages-object
     * @return a map with the key "available_langs" and a String[]-object containing the localised long form of all
     *         languages
     */
    public static Map<String, Object> geti18nPrefixedLangMap(String[] avLangs, Context context, Messages msg)
    {
        String lng;
        Optional<Result> opt = Optional.absent();
        Map<String, Object> map = new HashMap<String, Object>();
        List<String> avlngs = new ArrayList<String>();
        for (String s : avLangs)
        {
            lng = msg.get("i18nLang_" + s, context, opt, (Object) null).get();
            avlngs.add(lng);
        }
        map.put("available_langs", avlngs.toArray());
        return map;

    }

}
