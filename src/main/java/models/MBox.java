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

import org.joda.time.DateTime;
import com.avaje.ebean.*;
import com.avaje.ebean.validation.NotEmpty;

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

    @NotEmpty
    // Mailaddress of the Box
    private String address;

    // Timestamp for the end of the validity period
    private long ts_Active;

    // Flag for the validity
    private boolean expired;

    @NotEmpty
    private String domain;

    private int forwards;

    private int suppressions;

    @Version
    private Long version;

    // Owner of the Box
    @ManyToOne
    @JoinColumn(name = "usr_id", nullable = false)
    private User usr;

    /**
     * default constructor
     */
    public MBox()
    {
        this.address = "";
        this.ts_Active = 0L;
        this.expired = false;
        this.domain = "";
        this.forwards = 0;
        this.suppressions = 0;
    }

    /**
     * overloaded Constructor
     * 
     * @param local
     *            - local part
     * @param domain
     *            - domain part
     * @param ts
     *            - timestamp for expiration
     * @param expired
     *            - indicates the status of the mail
     */

    public MBox(String local, String domain, long ts, boolean expired, User usr)
    {
        this.address = local;
        this.ts_Active = ts;
        this.expired = expired;
        this.domain = domain;
        this.forwards = 0;
        this.suppressions = 0;
        this.usr = usr;
    }

    // -------------------------------------
    // Getter und Setter
    // -------------------------------------
    /**
     * @return the Id of this MBox (primary key in the DB)
     */
    public long getId()
    {
        return id;
    }

    /**
     * @param id
     *            - the Id of this MBox to set
     */
    public void setId(long id)
    {
        this.id = id;
    }

    /**
     * @return the Local-Part of this MBox
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * @param address
     *            - the Local-Part of the Address to set
     */
    public void setAddress(String address)
    {
        this.address = address;
    }

    /**
     * indicates whether the box is expired <br/>
     * this means, that the box is inactive (this flag is some kind of independent from the timestamp)
     * 
     * @return true if the box is expired/inactive
     */
    public boolean isExpired()
    {
        return expired;
    }

    /**
     * indicates whether the box is active (uses the expired-flag)
     * 
     * @return true if the box is active
     */
    public boolean isActive()
    {
        return !expired;
    }

    /**
     * if true, the box will be expired/inactive
     * 
     * @param expired
     *            - the expiration-status to set
     */
    public void setExpired(boolean expired)
    {
        this.expired = expired;
    }

    /**
     * @return the user which owns the box
     */
    public User getUsr()
    {
        return usr;
    }

    /**
     * @param usr
     *            - the user which owns the box
     */
    public void setUsr(User usr)
    {
        this.usr = usr;
    }

    /**
     * @return the domain-part of this mailaddress
     */
    public String getDomain()
    {
        return domain;
    }

    /**
     * @param domain
     *            - the domain-part of this mailaddress to set
     */
    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    /**
     * @param uid
     *            - the Users ID
     * @return true if the User with that id owns this mailbox
     */
    public boolean belongsTo(Long uid)
    {
        return (this.usr.getId() == uid);
    }

    /**
     * @return the number of successful forwards on this address
     */
    public int getForwards()
    {
        return forwards;
    }

    /**
     * @param forwards
     *            sets the number of forwards on this address
     */
    public void setForwards(int forwards)
    {
        this.forwards = forwards;
    }

    /**
     * increases the number of forwards by one
     */
    public void increaseForwards()
    {
        this.setForwards(this.getForwards() + 1);
    }

    /**
     * sets the number of forwards to 0
     */
    public void resetForwards()
    {
        this.forwards = 0;
    }

    /**
     * @return the number of suppressed mails on this address <br/>
     *         (while the address was inactive)
     */
    public int getSuppressions()
    {
        return suppressions;
    }

    /**
     * @param suppressions
     *            - sets the number of suppressed mails on this address<br/>
     *            (while the address was inactive)
     */
    public void setSuppressions(int suppressions)
    {
        this.suppressions = suppressions;
    }

    /**
     * increases the number of suppressions by one
     */
    public void increaseSuppressions()
    {
        this.setSuppressions(this.getSuppressions() + 1);
    }

    /**
     * sets the number of suppressions on this address to 0
     */
    public void resetSuppressions()
    {
        this.suppressions = 0;
    }

    /**
     * @return the Timestamp to which time this address will be active
     */
    public long getTs_Active()
    {
        return ts_Active;
    }

    /**
     * @param ts_Active
     *            - sets the time until that the address will be active
     */
    public void setTs_Active(long ts_Active)
    {
        this.ts_Active = ts_Active;
    }

    /**
     * @return the version of the box (just for optimistic lock-things)
     */
    public Long getVersion()
    {
        return version;
    }

    /**
     * @param version
     *            the version to set (just a field for optimistic lock support)
     */
    public void setVersion(Long version)
    {
        this.version = version;
    }

    // ---------------------------------------------
    // EBean Functions
    // ---------------------------------------------

    /**
     * Stores the Mailbox in the database
     */

    public void save()
    {
        Ebean.save(this);
    }

    /**
     * Updates the MailFWD in the DB
     */
    public void update()
    {
        Ebean.update(this);
    }

    /**
     * Removes a box from the DB
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
     * returns the Box by the given name
     * 
     * @param mail
     * @param domain
     * @return the MBox-Object to this address
     */
    public static MBox getByName(String mail, String domain)
    {
        return Ebean.find(MBox.class).where().eq("address", mail.toLowerCase()).eq("domain", domain).findUnique();
    }

    /**
     * Checks the relation between a user and a box
     * 
     * @param bId
     *            - the Boxid
     * @param uId
     *            - the UserId
     * @return true if the given BoxId belongs to the given UserId
     */
    public static boolean boxToUser(long bId, long uId)
    {
        return (Ebean.find(MBox.class, bId).usr.getId() == uId);
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
     * Finds all Addresses which belong to a specific user given by the userid
     * 
     * @param id
     *            - ID of a User
     * @return returns all Boxes of a specific user
     */
    public static List<MBox> allUser(Long id)
    {
        return Ebean.find(MBox.class).where().eq("usr_id", id.toString()).findList();
    }

    /**
     * @param id
     * @return
     */
    public static Map<String, List<MBox>> allUserMap(Long id)
    {
        List<MBox> list = Ebean.find(MBox.class).where().eq("usr_id", id.toString()).findList();
        Map<String, List<MBox>> map = new HashMap<String, List<MBox>>();
        map.put("mboxes", list);

        return map;
    }

    /**
     * checks if a given address exists
     * 
     * @param mail
     * @return true if the mail exists
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
     * @param local
     *            - the new local-part of the box
     * @param domain
     *            - the new domainname of the box
     * @param boxId
     *            - the Id of the box to check
     * @return true if the address is not used
     */

    public static boolean mailChanged(String local, String domain, Long boxId)
    {
        MBox mb = MBox.getById(boxId);
        if (mb.equals(null))
        { // there's no box with that id
            return false;
        }
        else
        { // there is a mbox with that id
            if (mb.address.equals(local) && mb.address.equals(domain))
            { // mbox-mailaddr is equal to the given address -> nothing changed
                return false;
            }
            else
            { // the addresses differ
                if (MBox.mailExists(local, domain))
                { // the given address already exists for another mbox
                    return false;
                }
                else
                { // the given address is not used
                    return true;
                }
            }
        }
    }

    /**
     * @return the timestamp in a Date format
     */
    public String getTSAsString()
    {
        if (this.ts_Active == 0)
        {
            return "unlimited";
        }
        else
        {
            DateTime dt = new DateTime(this.ts_Active);
            String day = "";
            String mon = "";
            String hou = "";
            String min = "";

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

    /**
     * sets the valid box as invalid and vice versa
     * 
     * @return value of true means that its now enabled (== not expired)
     */

    public boolean enable()
    {
        this.setExpired(!expired);
        Ebean.update(this);
        return !this.isExpired();
    }

}
