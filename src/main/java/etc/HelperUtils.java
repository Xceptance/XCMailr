package etc;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.joda.time.DateTime;

import ninja.utils.NinjaProperties;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HelperUtils
{

    public static Map<String, String[]> getDomainsFromConfig(NinjaProperties ninjaProp)
    {

        String dlist = ninjaProp.get("mbox.dlist");

        if (!dlist.equals(null))
        {
            String[] list = dlist.split(";");
            if (!list.equals(null))
            {
                Map<String, String[]> map = new HashMap<String, String[]>();

                map.put("domain", list);

                return map;
            }
        }
        return null;
    }

    /**
     * Generates a random name, e.g. for the mailbox
     * 
     * @return a random name
     */
    private static String getRndName()
    {
        // TODO modify this function, also use at least digits and uppercase-letters and a variable length
        Random rand = new Random();
        StringBuffer strBuf = new StringBuffer();
        for (int i = 0; i < 7; i++)
        {
            // generates a random char between a and z (an ascii a is 97, z is 122 in dec)
            strBuf.append((char) ((Math.abs(rand.nextInt()) % 26) + 97));
        }
        return strBuf.toString();
    }

    /**
     * checks if a given string is in the right format <br/>
     * there should be 1 or 2 values (d,h or h,d or h or d or 0)
     * 
     * @param s the String  to parse
     * @return the Duration which was given by the String
     */
    public static long parseDuration(String s)
    {
        int d = 0;
        int h = 0;
        if (parseHelp2(s) >= 0)
        {
            //if possible: split the given String
            String[] str = s.split(",");
            
            String helper = "";
            
            //go through the array
            for (int i = 0; i < str.length; i++)
            {
                str[i] = str[i].toLowerCase();

                if (str[i].contains("d"))
                {   //our string has a value for the day
                    //try to get the number 
                    helper = str[i].substring(0, str[i].indexOf('d'));
                    //try to parse the number
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
            s = s.trim();
            if (s.equals("0"))
            {
                return 0;
            }
        }
        else
        {
            //
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
     * helper function for parseDuration() checks if a string consists only of digits
     * 
     * @param helper
     *            string to check
     * @return the integer value of the string or -1 if the string does not match
     */
    private static int digitsOnly(String helper)
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
     * @param helper
     * @return
     */
    private static int parseHelp2(String helper)
    {
        helper = helper.trim();
        if (helper.matches("[\\d+][d|h][,][\\d][d|h]") || helper.matches("[\\d+][d|h]") || helper.matches("[0]"))
        {
            return 0;

        }
        else
        {
            return -1;
        }
    }

    public static String parseTime(Long milis)
    {
        DateTime dt = new DateTime();
        float times = (milis - dt.getMillis()) / 3600000.0f; // in hours
        if (milis == 0)
        {
            // the box is "unlimited"
            return "0";
        }
        if (times < 0)
        {
            // the box is expired
            return "1h,1d";
        }

        int hours = Math.round(times);
        int days = hours / 24;
        hours = hours % 24;

        return hours + "h," + days + "d";
    }
}
