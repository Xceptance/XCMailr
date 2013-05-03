package etc;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.joda.time.DateTime;
import ninja.utils.NinjaProperties;

import com.google.inject.Singleton;

@Singleton
public class HelperUtils
{

    /**
     * Extracts the List of Domains from the ninjaProperties and returns it as a nicely renderable Map
     * 
     * @param ninjaProp
     *            - the properties of this application
     * @return a Map with a "domain"-key which contains the list of domains
     */
    public static Map<String, Object> getDomainsFromConfig(NinjaProperties ninjaProp)
    {
        Map<String, Object> map = new HashMap<String, Object>();
        String dlist = ninjaProp.get("mbox.dlist");

        if (!dlist.equals(null))
        {
            String[] list = dlist.split(";");
            if (!list.equals(null))
            {
                map.put("domain", list);
                return map;
            }
        }

        // the property does not exist or has no value
        // prevent a NullPointerException by returning an empty Stringarray
        map.put("domain", new String[] {});
        return map;
    }

    /**
     * Generates a random name, generated with java.util.Random and an alphabet of 0-9,a-z,A-Z <br/>
     * e.g. for the mailbox
     * 
     * @param length
     *            - length of the returned string
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
     * All in all, the same like the getRndString(), but here's SecureRandom used
     * 
     * @param length
     *            - length of the returned string
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
     * Checks if a given string is in the right format <br/>
     * there should be 1 or 2 time-values (d,h or h,d or h or d or 0)<br/>
     * uppercase-letters will be converted and spaces removed the highest time-interval is set to 30 days, everything
     * above will be set to 0 (unlimited)
     * 
     * @param s
     *            - the String to parse
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

        // TODO this is just for testing-purposes, remove it
        if (s.equals("1"))
        {
            DateTime dt1 = new DateTime();
            return dt1.plusMinutes(1).getMillis();
        }

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
     * Helper function for parseDuration() checks if a string consists only of digits
     * 
     * @param helper
     *            - String to check
     * @return the integer value of the string or -1 if the string does not match
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
     * Checks whether the Inputstring is in the Form [digit][d or h][,][digit][d or h] or [digit][d or h] or [0]
     * 
     * @param helper
     *            the Inputstring to check
     * @return 0 for a match, -1 for a mismatch
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
     * Gets a Timestamp in milliseconds and parses the time-interval to that in a readable way
     * 
     * @param millis - Timestamp in milliseconds
     * @return the duration to this date (in the pattern 1h, 1d) or a default value (given timestamp was in the past)
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

}
