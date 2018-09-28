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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.persistence.PersistenceException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.avaje.ebean.Ebean;
import com.google.inject.Singleton;

import models.User;
import ninja.Context;
import ninja.Result;
import ninja.i18n.Messages;

/**
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Singleton
public class HelperUtils
{

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
     * Parses the page number given as request parameter "no" as integer and puts it into the session cookie.
     * 
     * @param context
     *            the context
     * @param defaultNo
     *            the default page number
     */
    public static void parseEntryValue(Context context, Integer defaultNo)
    {
        final String no = context.getParameter("no");
        final String defaultNoStr = defaultNo.toString();
        String value;
        if (no == null)
        {// no number-param was delivered

            if (context.getSession().get("no") != null)
            { // return with no action, because there is already a value set
                return;
            }
            else
            { // set the default-value if no param was set and theres no value in the cookie
                value = defaultNoStr;
            }
        }
        else
        { // there's a parameter with the key "no"

            if (no.equals("all"))
            { // all entries should be shown
                value = "0";
            }
            else
            {
                if (no.matches("^0|[1-9]\\d*$"))
                {
                    value = no;
                }
                else
                { // set to default if its not an integer
                    value = defaultNoStr;
                }
            }
        }
        // set the number to the session-cookie
        context.getSession().put("no", value);
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
     * @return a List of String[] with the key "available_langs" and a String[]-object containing the localized long
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

    /**
     * Searchs the user the given API token. If it doesn't exist or if token's value isn't unique then null will be
     * returned+
     * 
     * @param apiToken
     * @return
     */
    public static User checkApiToken(String apiToken)
    {
        try
        {
            return Ebean.find(User.class).where().eq("APITOKEN", apiToken).eq("active", true).findUnique();
        }
        catch (PersistenceException e)
        {
            // in case there is more than one user with the exact same token
            // this should never ever happen except someone is extreme lucky
            return null;
        }
    }

    /**
     * Check the given mail address to match format "localpart@domain". Also checks if domain is configured in XCMailr
     * 
     * @param dOMAIN_LIST
     * @param mailAddress
     * @return false if any of the checks fails
     */
    public static boolean checkEmailAddressValidness(String[] mailAddressParts, String[] domainList)
    {
        if (mailAddressParts.length != 2)
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
     * @return
     */
    public static String[] splitMailAddress(String mailAddress)
    {
        return mailAddress.split("\\@");
    }
}
