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
package models;

import java.util.List;
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
 * Object for a virtual Mailbox (a Mail-Forward)
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
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
     * Default-Constructor
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
     * Constructor
     * 
     * @param local
     *            Local-Part of the Forward-Address
     * @param domain
     *            Domain-Part of the Forward-Address
     * @param ts
     *            Timestamp for expiration
     * @param expired
     *            indicates the Status of the Mail-Forward
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
    // Getters and Setters
    // -------------------------------------
    /**
     * @return the ID of this MBox (primary key in the DB)
     */
    public long getId()
    {
        return id;
    }

    /**
     * @param id
     *            the ID of this MBox to set
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
     *            the Local-Part of the Address to set
     */
    public void setAddress(String address)
    {
        this.address = address;
    }

    /**
     * Indicates whether the Box is expired <br/>
     * This means, that the Box is inactive (This flag is some kind of Independent from the Timestamp)
     * 
     * @return true if the Box is expired/inactive
     */
    public boolean isExpired()
    {
        return expired;
    }

    /**
     * indicates whether the Box is active (uses the expired-flag)
     * 
     * @return true if the Box is active
     */
    public boolean isActive()
    {
        return !expired;
    }

    /**
     * if true, the Box will be expired/inactive
     * 
     * @param expired
     *            the Expiration-Status to set
     */
    public void setExpired(boolean expired)
    {
        this.expired = expired;
    }

    /**
     * @return the {@link User} which owns this Box/Forward
     */
    public User getUsr()
    {
        return usr;
    }

    /**
     * @param usr
     *            the {@link User} which owns this Box/Forward
     */
    public void setUsr(User usr)
    {
        this.usr = usr;
    }

    /**
     * @return the Domain-Part of this Mail-Forward
     */
    public String getDomain()
    {
        return domain;
    }

    /**
     * @param domain
     *            the Domain-Part of this Mail-Forward to set
     */
    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    /**
     * @param uid
     *            the {@link User}-ID
     * @return true, if the {@link User} with the given ID owns this Mailbox
     */
    public boolean belongsTo(Long uid)
    {
        return (this.usr.getId() == uid);
    }

    /**
     * @return the Number of successful forwards on this Address
     */
    public int getForwards()
    {
        return forwards;
    }

    /**
     * @param forwards
     *            sets the Number of forwards on this Address
     */
    public void setForwards(int forwards)
    {
        this.forwards = forwards;
    }

    /**
     * Increases the Number of Forwards by one
     */
    public void increaseForwards()
    {
        this.setForwards(this.getForwards() + 1);
    }

    /**
     * Sets the Number of Forwards to 0
     */
    public void resetForwards()
    {
        this.setForwards(0);
    }

    /**
     * @return the Number of suppressed Mails on this Address <br/>
     *         (Mails sent while the Address was inactive)
     */
    public int getSuppressions()
    {
        return suppressions;
    }

    /**
     * @param suppressions
     *            sets the Number of suppressed Mails on this Address<br/>
     */
    public void setSuppressions(int suppressions)
    {
        this.suppressions = suppressions;
    }

    /**
     * Increases the Number of suppressions by one
     */
    public void increaseSuppressions()
    {
        this.setSuppressions(this.getSuppressions() + 1);
    }

    /**
     * Sets the Number of suppressions on this Address to 0
     */
    public void resetSuppressions()
    {
        this.setSuppressions(0);
    }

    /**
     * @return the Timestamp as long as this Address will be active
     */
    public long getTs_Active()
    {
        return ts_Active;
    }

    /**
     * @param ts_Active
     *            sets the Time as long as this Address will be active
     */
    public void setTs_Active(long ts_Active)
    {
        this.ts_Active = ts_Active;
    }

    /**
     * @return the Version of the Box (just for optimistic lock-things)
     */
    public Long getVersion()
    {
        return version;
    }

    /**
     * @param version
     *            the Version to set (just a field for optimistic lock support)
     */
    public void setVersion(Long version)
    {
        this.version = version;
    }

    /**
     * @return the full address of this virtual email
     */

    public String getFullAddress()
    {
        return this.address + "@" + this.domain;
    }

    // ---------------------------------------------
    // EBean Functions
    // ---------------------------------------------

    /**
     * Stores the Mailbox in the Database
     */

    public void save()
    {
        Ebean.save(this);
    }

    /**
     * Updates the Mailbox in the DB
     */
    public void update()
    {
        Ebean.update(this);
    }

    /**
     * Removes a Box from the DB
     * 
     * @param id
     *            the ID of the Box to delete
     */
    public static void delete(Long id)
    {
        Ebean.delete(MBox.class, id);
    }

    /**
     * Resolves the Mailbox by their given ID
     * 
     * @param id
     *            ID of the Box
     * @return the Box
     */
    public static MBox getById(Long id)
    {
        return Ebean.find(MBox.class, id);
    }

    /**
     * returns the Box by the given Name
     * 
     * @param mail
     *            the Local-Part of the virtual Mailbox
     * @param domain
     *            the Domain-Part of the virtual Mailbox
     * @return the MBox-Object that belongs to this Address
     */
    public static MBox getByName(String mail, String domain)
    {
        return Ebean.find(MBox.class).where().eq("address", mail.toLowerCase()).eq("domain", domain).findUnique();
    }

    /**
     * Checks the relation between a {@link User} and a Box
     * 
     * @param bId
     *            the Box-ID
     * @param uId
     *            the {@link User}-ID
     * @return true if the given Box-ID belongs to the given {@link User}-Id
     */
    public static boolean boxToUser(long bId, long uId)
    {
        MBox mb = Ebean.find(MBox.class, bId);

        if (mb != null)
        { // the box exists, return true if the id belongs to the user
            return (mb.usr.getId() == uId);
        }
        else
        { // there's no box with that ID
            return false;
        }
    }

    /**
     * Gets the "real" Mail-Address that belongs to the given virtual Mailbox
     * 
     * @param mail
     *            Local-Part of the virtual Mailbox
     * @param domain
     *            Domain-Part of the virtual Mailbox
     * @return the "real" Mail-Address of the {@link User} that owns this virtual Mailbox
     */
    public static String getFwdByName(String mail, String domain)
    {
        MBox mb = Ebean.find(MBox.class).where().eq("address", mail.toLowerCase()).eq("domain", domain).findUnique();
        if (mb != null)
        { // the requested mailaddress exists
            return mb.getUsr().getMail();
        }
        else
        {// there's no mailaddress
            return "";
        }
    }

    /**
     * @return a List of all Boxes in this System
     */
    public static List<MBox> all()
    {
        return Ebean.find(MBox.class).findList();
    }

    /**
     * Finds all Addresses which belong to a specific {@link User} given by the User-ID
     * 
     * @param id
     *            ID of a {@link User}
     * @return returns all Boxes of a specific {@link User}
     */
    public static List<MBox> allUser(Long id)
    {
        return Ebean.find(MBox.class).where().eq("usr_id", id.toString()).findList();
    }

    /**
     * Checks if a given Mailbox-Address exists
     * 
     * @param mail
     *            Local-Part of the Address to check
     * @param domain
     *            Domain-Part of the Address to check
     * @return true, if the Mailbox-Address exists
     */
    public static boolean mailExists(String mail, String domain)
    {
        if (!Ebean.find(MBox.class).where().eq("address", mail.toLowerCase()).eq("domain", domain).findList().isEmpty())
        { // theres no such mailaddress registered now
            return true;
        }
        else
        { // the mailaddress exists
            return false;
        }
    }

    /**
     * @param local
     *            the new Local-Part of the Box
     * @param domain
     *            the new Domain-Part of the Box
     * @param boxId
     *            the ID of the Box to check
     * @return true, if the Address has changed and is not used
     */

    public static boolean mailChanged(String local, String domain, Long boxId)
    {
        MBox mb = MBox.getById(boxId);
        if (mb == null)
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

    public String getTSAsStringWithNull()
    {
        String tsString = getTSAsString();
        if (tsString.equals("unlimited"))
        {
            tsString = "0";
        }
        return tsString;
    }

    /**
     * @return the Timestamp as String in the Format "dd.mm.yyyy hh:mm"
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
     * Generates a List of the Boxes which will expire in the next minute(s)
     * 
     * @param minutes
     *            the minute(s) to check for
     * @return List of MBoxes
     */
    public static List<MBox> getNextBoxes(int minutes)
    {
        DateTime dt = new DateTime();
        dt = dt.plusHours(minutes);

        return Ebean.find(MBox.class).where().eq("expired", false).lt("ts_Active", dt.getMillis()).ne("ts_Active", 0)
                    .findList();
    }

    /**
     * Sets the valid Box as invalid and vice versa
     * 
     * @return Value of true means that the Box is now enabled (== not expired)
     */

    public boolean enable()
    {
        this.setExpired(!expired);
        Ebean.update(this);
        return !this.isExpired();
    }

    public static List<MBox> findBoxLike(String input, long userId)
    {
        ExpressionList<MBox> exList1 = Ebean.find(MBox.class).where().eq("usr_id", userId);
        if (input.contains("@"))
        {
            String[] split = input.split("@");

            switch (split.length)
            {
                case (1): // the entry may be something like "@domain" or "address@"
                    return exList1.or(Expr.like("address",  split[0]),
                                      Expr.like("domain", "%" + split[0] + "%")).findList();

                case (2): // the entry was something like "address@domain"
                    return exList1.like("address", "%" + split[0] + "%").like("domain", "%" + split[1] + "%").findList();
                default: // the entry was something else
                    return exList1.or(Expr.like("address", "%" + input + "%"), Expr.like("domain", "%" + input + "%"))
                                  .findList();
            }

        }

        return exList1.or(Expr.like("address", "%" + input + "%"), Expr.like("domain", "%" + input + "%")).findList();
    }
}
