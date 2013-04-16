package models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import ninja.validation.Required;

import org.joda.time.DateTime;
import com.avaje.ebean.*;

/**
 * Object for a Mailbox
 * 
 * @author Patrick Thum 2012 released under Apache 2.0 License
 */
@Entity
@Table(name = "mailboxes")
public class MBox
{
    // Mailbox ID
    @Id
    private long id;

    // Mailaddress of the Box
    private String address;

    // Timestamp for the end of the validity period

    // TODO @version annotation ?

    private long ts_Active;

    // Flag for the validity
    private boolean expired;

    private String domain;

    private int forwards;

    private int suppressions;

    // Owner of the Box
    @ManyToOne
    @JoinColumn(name = "usr_id", nullable = false)
    private User usr;

    // Finder
    // public static Finder<Long,MBox> find = new Finder(Long.class, MBox.class);

    // Getter und Setter
    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public long getTS_Active()
    {
        return ts_Active;
    }

    public void setTS_Active(long tS_Active)
    {
        ts_Active = tS_Active;
    }

    public boolean isExpired()
    {
        return expired;
    }

    public boolean isActive()
    {
        return !expired;
    }

    public void setExpired(boolean expired)
    {
        this.expired = expired;
    }

    public User getUsr()
    {
        return usr;
    }

    public void setUsr(User usr)
    {
        this.usr = usr;
    }

    public String getDomain()
    {
        return domain;
    }

    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    public boolean belongsTo(Long uid)
    {
        return (this.usr.getId() == uid);
    }

    public int getForwards()
    {
        return forwards;
    }

    public void setForwards(int forwards)
    {
        this.forwards = forwards;
    }

    // EBean Functions

    public int getSuppressions()
    {
        return suppressions;
    }

    public void setSuppressions(int suppressions)
    {
        this.suppressions = suppressions;
    }

    public long getTs_Active()
    {
        return ts_Active;
    }

    public void setTs_Active(long ts_Active)
    {
        this.ts_Active = ts_Active;
    }

    /**
     * deletes a box
     * 
     * @param id
     */
    public static void delete(Long id)
    {
        Ebean.delete(MBox.class, id);
    }

    /**
     * @param id
     *            ID of the Box
     * @return the box
     */
    public static MBox getById(Long id)
    {
        return Ebean.find(MBox.class, id);
    }

    /**
     * @param bId
     * @param uId
     * @return
     */
    public static boolean boxToUser(long bId, long uId)
    {
        return (Ebean.find(MBox.class, bId).usr.getId() == uId);
    }

    /**
     * returns the Local Part of a Box
     * 
     * @param id
     * @return the local part
     */
    public static String getNameById(Long id)
    {
        return Ebean.find(MBox.class, id).getAddress();
    }

    /**
     * @param mbId
     * @return the mailaddress of the owner of this box
     */
    public static String getFWD(Long mbId)
    {
        return Ebean.find(MBox.class, mbId).getUsr().getMail();
    }

    /**
     * Gives the real address to a fake-address
     * 
     * @param mail
     *            - local part of the fake-address
     * @param domain
     *            - domain part of the fake-address
     * @return the real mailaddress of the user that owns this fake-address
     */
    public static String getFwdByName(String mail, String domain)
    {
        return Ebean.find(MBox.class).where().eq("address", mail.toLowerCase()).eq("domain", domain).findUnique()
                    .getUsr().getMail();
    }

    /**
     * @return all available boxes
     */
    public static List<MBox> all()
    {
        return Ebean.find(MBox.class).findList();
    }

    /**
     * @param id
     *            ID of a User
     * @return returns all Boxes of a specific user
     */
    public static List<MBox> allUser(Long id)
    {
        return Ebean.find(MBox.class).where().eq("usr_id", id.toString()).findList();
    }

    public static Map<String, List<MBox>> allUserMap(Long id)
    {
        List<MBox> list = Ebean.find(MBox.class).where().eq("usr_id", id.toString()).findList();
        Map<String, List<MBox>> map = new HashMap<String, List<MBox>>();
        map.put("mboxes", list);

        return map;
    }

    /**
     * stores the Mailbox in the database
     * 
     * @param mb
     */
    public static void createMBox(MBox mb)
    {
        Ebean.save(mb);

    }

    /**
     * checks if a given address exists
     * 
     * @param mail
     * @return
     */
    public static boolean mailExists(String mail, String domain)
    {
        if (!Ebean.find(MBox.class).where().eq("address", mail.toLowerCase()).eq("domain", domain).findList().isEmpty())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * @return the timestamp in a Date format
     */
    public String getTSAsString()
    {

        DateTime dt = new DateTime(this.ts_Active);

        if (this.ts_Active == 0)
        {
            return "unlimited";
        }
        else
        {

            String min = "";
            if (dt.getMinuteOfHour() < 10)
            {
                min = "0" + String.valueOf(dt.getMinuteOfHour());
            }
            else
            {
                min = String.valueOf(dt.getMinuteOfHour());
            }

            return dt.getDayOfMonth() + "." + dt.getMonthOfYear() + "." + dt.getYear() + " " + dt.getHourOfDay() + ":"
                   + min;
        }
    }
    
    //TODO check this method
    // rewrote mailExists() for Editing MBoxes
    public static boolean mailExists(String mail, String domain, Long mbId)
    {

        List<MBox> ml = Ebean.find(MBox.class).where().eq("address", mail.toLowerCase()).eq("domain", domain)
                             .findList();

        if (!ml.isEmpty())
        { // there's another address..
            if ((ml.size() == 1) && (ml.get(0).getId() == mbId))
            {
                // Mailbox has the same Id
                return false;
            }
            else
            {
                // more than 1 result or another Id
                return true;
            }
        }
        else
        { // there's no other address
            return false;
        }

    }

    /**
     * generates a list of the boxes who will expire in the next hour(s)
     * 
     * @param hours
     *            the hour(s)
     * @return List of MBoxes
     */
    public static List<MBox> getNextBoxes(int hours)
    {
        DateTime dt = new DateTime();
        dt = dt.plusHours(hours);

        return Ebean.find(MBox.class).where().eq("expired", false).lt("ts_Active", dt.getMillis()).ne("ts_Active", 0)
                    .findList();
    }

    public static void updateMBox(MBox mb)
    {
        Ebean.update(mb);
    }

    /**
     * sets the valid box as invalid and vice versa
     * 
     * @param mId
     *            Id of the box
     * @return value of true means that its now enabled (== not expired)
     */
    public static boolean enable(Long mId)
    {
        MBox mb = Ebean.find(MBox.class, mId);
        mb.setExpired(!mb.isExpired());
        Ebean.update(mb);
        return (!mb.isExpired());
    }

}
