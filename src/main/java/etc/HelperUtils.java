package etc;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.joda.time.DateTime;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import models.MBox;
import ninja.utils.NinjaProperties;

import com.google.inject.Singleton;

@Singleton
public class HelperUtils
{

    public static Map<String, Object> getDomainsFromConfig(NinjaProperties ninjaProp)
    {

        String dlist = ninjaProp.get("mbox.dlist");

        if (!dlist.equals(null))
        {
            String[] list = dlist.split(";");
            if (!list.equals(null))
            {
                Map<String, Object> map = new HashMap<String, Object>();

                map.put("domain", list);

                return map;
            }
        }
        return null;
        // TODO what will happen if this returned?
    }

    /**
     * Generates a random name, e.g. for the mailbox
     * 
     * @return a random name
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
     * checks if a given string is in the right format <br/>
     * there should be 1 or 2 time-values (d,h or h,d or h or d or 0)<br/>
     * uppercase-letters will be converted and spaces removed the highest time-interval is set to 30 days, everything
     * above will be set to 0 (unlimited)
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
        if(s.equals("1")){
            DateTime dt1 = new DateTime();
            return dt1.plusMinutes(1).getMillis();
        }
        if (hasCorrectFormat(s))
        {

            if (s.equals("0"))
            {
                return 0;
            }

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
     * Checks whether the Inputstring is in the Form [digit][d or h][,][digit][d or h] or [digit][d or h] or [0]
     * 
     * @param helper
     *            the Inputstring to check
     * @return 0 for a match, -1 for a mismatch
     */
    private static boolean hasCorrectFormat(String helper)
    {
        helper = helper.trim();
        if (helper.matches("[\\d+][d|h][,][\\d][d|h]") || helper.matches("[\\d+][d|h]") || helper.matches("[0]"))
        {
            return true;

        }
        else
        {
            return false;
        }
    }

    /**
     * @param milis
     * @return
     */
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

    /**
     * searches the mx-host of a given mailaddress
     * 
     * @param mailadr
     *            the mailaddress
     * @return the mx-record for this address as string
     */

    public static String getMailTarget(String mailadr)
    {

        try
        {
            Record[] records = new Lookup(mailadr.split("@")[1], Type.MX).run();

            MXRecord mx = (MXRecord) records[0];
            return mx.getTarget().toString();

        }
        catch (TextParseException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (NullPointerException e)
        {
            return null;
        }
        return null;

    }

    /**
     * Takes the unchanged incoming mail and forwards it
     * 
     * @param from
     *            - the unchanged From-Address
     * @param to
     *            - the trash-mail target (the forward address will be automatically fetched from DB)
     * @param content
     *            - the message body
     * @return
     */
    public static boolean forwardMail(String from, String to, String content)
    {
        String[] splitaddress = to.split("@");
        String fwdtarget = MBox.getFwdByName(splitaddress[0], splitaddress[1]);
        // TODO implement an i18n Subject-text
        return sendMail(from, fwdtarget, content, "Weitergeleitete Nachricht");

    }

    /**
     * Takes the mail specified by the parameters and sends it to the given target
     * 
     * @param from
     *            - the mail-author
     * @param to
     *            - the recipients-address
     * @param content
     *            - the message body
     * @param subject
     *            - the message subject
     * @return true if the mail-transmission was successful
     */

    public static boolean sendMail(String from, String to, String content, String subject)
    {
        try
        {
            // TODO retry until the message could be sent(?)
            Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", HelperUtils.getMailTarget(to));
            Session session = Session.getDefaultInstance(properties);

            // create the message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(content);
            Transport.send(message);
            return true;

        }
        catch (AddressException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        catch (MessagingException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }

    }

}
