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
    public static String getRndString()
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
    
    public static String getMailTarget(String mailadr){
        
        
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

        return null;
        
    }
    /**
     *  Takes the unchanged incoming mail and forwards it
     * @param from - the unchanged From-Address
     * @param to - the trash-mail target (the forward address will be automatically fetched from DB)
     * @param content - the message body
     * @return
     */
    public static boolean forwardMail(String from, String to, String content){
        String[] splitaddress = to.split("@");
        String fwdtarget = MBox.getFwdByName(splitaddress[0], splitaddress[1]);
        // TODO implement an i18n Subject-text
        return sendMail(from, fwdtarget, content, "Weitergeleitete Nachricht");
        
    }
    
    /**
     *  Takes the mail specified by the parameters and sends it to the given target
     * @param from
     * @param to
     * @param content
     * @param subject
     * @return
     */
    
    public static boolean sendMail(String from, String to, String content, String subject){
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
            //TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        
    }
    
}
