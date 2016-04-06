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

import java.io.Serializable;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.Query;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.SqlUpdate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import etc.HelperUtils;

/**
 * Object for a virtual Mailbox (a Mail-Forward)
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Entity
@Table(name = "mailboxes")
public class MBox extends AbstractEntity implements Serializable
{
    /** UID to serialize this object */
    private static final long serialVersionUID = 6111058118487675662L;

    /** Mailaddress of the Box */
    @NotEmpty
    @Pattern(regexp = "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*")
    private String address;

    /** Timestamp for the end of the validity period */
    @JsonIgnore
    private long ts_Active;

    /** Flag for the validity */
    @JsonIgnore
    private boolean expired;

    /** the domain-part of an address */
    @NotEmpty
    @Pattern(regexp = "[A-Za-z-]+(\\.[\\w-]+)+")
    @Length(min = 1, max = 255)
    private String domain;

    /** the number of forwards for this box */
    @JsonIgnore
    private int forwards;

    /** the number of suppressions for this box */
    @JsonIgnore
    private int suppressions;

    /** the version of this box (used for optimisticLock) */

    @Version
    @JsonIgnore
    private Long version;

    /** the owner of the address/box */
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
     * Indicates whether the Box is expired. This means, that the Box is inactive (This flag is some kind of Independent
     * from the Timestamp)
     * 
     * @return true if the Box is expired/inactive
     */
    @JsonProperty("expired")
    public boolean isExpired()
    {
        return expired;
    }

    /**
     * indicates whether the Box is active (uses the expired-flag)
     * 
     * @return true if the Box is active
     */
    @JsonIgnore
    public boolean isActive()
    {
        return !expired;
    }

    /**
     * @return <code>true</code> if the mail is inactive and the TS has a value in the past, <code>false</code>
     *         otherwise
     */
    @JsonIgnore
    public boolean isExpiredByTimestamp()
    {
        return (expired && (ts_Active != 0) && (DateTime.now().isAfter(ts_Active)));
    }

    /**
     * if true, the Box will be expired/inactive
     * 
     * @param expired
     *            the Expiration-Status to set
     */
    @JsonIgnore
    public void setExpired(boolean expired)
    {
        this.expired = expired;
    }

    /**
     * @return the {@link User} which owns this Box/Forward
     */
    @JsonIgnore
    public User getUsr()
    {
        return usr;
    }

    /**
     * @param usr
     *            the {@link User} which owns this Box/Forward
     */
    @JsonIgnore
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
    @JsonProperty("forwards")
    public int getForwards()
    {
        return forwards;
    }

    /**
     * @param forwards
     *            sets the Number of forwards on this Address
     */
    @JsonIgnore
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
     * @return the Number of suppressed Mails on this Address (Mails sent while the Address was inactive)
     */
    @JsonProperty("suppressions")
    public int getSuppressions()
    {
        return suppressions;
    }

    /**
     * @param suppressions
     *            sets the Number of suppressed Mails on this Address
     */
    @JsonIgnore
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
    @JsonProperty("ts_Active")
    public long getTs_Active()
    {
        return ts_Active;
    }

    /**
     * @param ts_Active
     *            sets the Time as long as this Address will be active
     */
    @JsonIgnore
    public void setTs_Active(long ts_Active)
    {
        this.ts_Active = ts_Active;
    }

    /**
     * sets the ts_Active by the given datetime String by using {@link HelperUtils#parseTimeString(String)}
     * 
     * @param dateTime
     */
    @JsonProperty("datetime")
    public void setDateTime(String dateTime)
    {
        this.setTs_Active(HelperUtils.parseTimeString(dateTime));
    }

    /**
     * @return the Version of the Box (just for optimistic lock-things)
     */
    @JsonIgnore
    public Long getVersion()
    {
        return version;
    }

    /**
     * @param version
     *            the Version to set (just a field for optimistic lock support)
     */
    @JsonIgnore
    public void setVersion(Long version)
    {
        this.version = version;
    }

    /**
     * @return the full address of this virtual email
     */
    @JsonProperty("fullAddress")
    public String getFullAddress()
    {
        return this.address + "@" + this.domain;
    }

    /**
     * dummy method to set the full address. <b>it does nothing!</b> and is only for Jackson-Parser
     * 
     * @param dummy
     */
    @JsonIgnore
    public void setFullAddress(String dummy)
    {

    }

    // ---------------------------------------------
    // EBean Functions
    // ---------------------------------------------
    /**
     * increases the forward-count directly in the database
     */
    public void increaseFwd()
    {
        StringBuilder sqlSb = new StringBuilder();
        sqlSb.append("UPDATE MAILBOXES SET FORWARDS = FORWARDS + 1 WHERE ID=");
        sqlSb.append(getId());
        sqlSb.append(";");
        SqlUpdate down = Ebean.createSqlUpdate(sqlSb.toString());
        down.execute();
        setForwards(getForwards() + 1);
    }

    /**
     * increases the suppression-count directly in the database
     */
    public void increaseSup()
    {
        StringBuilder sqlSb = new StringBuilder();
        sqlSb.append("UPDATE MAILBOXES SET SUPPRESSIONS = SUPPRESSIONS + 1 WHERE ID=");
        sqlSb.append(getId());
        sqlSb.append(";");
        SqlUpdate down = Ebean.createSqlUpdate(sqlSb.toString());
        down.execute();
        setSuppressions(getSuppressions() + 1);
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
        return (mb != null && (mb.getUsr() != null) && (mb.getUsr().getId() == uId));
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
        return (mb != null && mb.getUsr() != null) ? mb.getUsr().getMail() : "";
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
        return (!Ebean.find(MBox.class).where().eq("address", mail.toLowerCase()).eq("domain", domain).findList()
                      .isEmpty());
    }

    /**
     * @return the timestamp as string in the format "yyyy-MM-dd hh:mm"; if it is 0, then also 0 is returned
     */
    @JsonIgnore
    public String getTSAsStringWithNull()
    {
        if (this.ts_Active == 0)
        {
            return "0";
        }
        else if (this.ts_Active == -1)
        {
            return "-1";
        }
        else
        {
            DateTime dt = new DateTime(this.ts_Active);
            StringBuilder timeString = new StringBuilder();
            // add a leading "0" if the value is under ten
            timeString.append(dt.getYear()).append("-");
            timeString.append(HelperUtils.addZero(dt.getMonthOfYear()));
            timeString.append("-");
            timeString.append(HelperUtils.addZero(dt.getDayOfMonth()));
            timeString.append(" ");
            timeString.append(HelperUtils.addZero(dt.getHourOfDay()));
            timeString.append(":");
            timeString.append(HelperUtils.addZero(dt.getMinuteOfHour()));
            return timeString.toString();
        }

    }

    /**
     * @return the timestamp as string in the format "yyyy-MM-dd hh:mm"; if it is 0, then "unlimited" is returned
     */
    public String getDatetime()
    {
        String tsString = getTSAsStringWithNull();
        if (tsString.equals("0"))
        {
            tsString = "unlimited";
        }
        else if (tsString.equals("-1"))
        {
            return "wrong timestamp";
        }
        return tsString;
    }

    public void resetIdAndCounterFields()
    {
        this.setId(0);
        resetForwards();
        resetSuppressions();
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
        // get the time to check for
        DateTime dt = new DateTime().plusHours(minutes);

        // get a list of boxes, that are active, have a timestamp that is lower than the time to check for and not
        // unlimited
        return Ebean.find(MBox.class).where().eq("expired", false).lt("ts_Active", dt.getMillis()).ne("ts_Active", 0)
                    .findList();
    }

    /**
     * Sets the valid Box as invalid and vice versa (and updates the database!)
     * 
     * @return Value of true means that the Box is now enabled (== not expired)
     */

    public boolean enable()
    {
        this.setExpired(!expired);
        Ebean.update(this);
        return !this.isExpired();
    }

    /**
     * Searches for a given input-string over all boxes that belong to this userID
     * 
     * @param input
     *            the search-string
     * @param userId
     *            the user ID
     * @return a List of all Emails of this user that match the input-string
     */
    public static List<MBox> findBoxLike(String input, long userId)
    {
        if (input.equals(""))
        {
            return allUser(userId);
        }
        ExpressionList<MBox> exList1 = Ebean.find(MBox.class).where().eq("usr_id", userId);
        if (input.contains("@"))
        { // check for a correct format of a box
            String[] split = input.split("@");

            switch (split.length)
            {
                case (1): // the entry may be something like "@domain" or "address@"
                    return exList1.or(Expr.like("address", "%" + split[0] + "%"),
                                      Expr.like("domain", "%" + split[0] + "%")).findList();

                case (2): // the entry was something like "address@domain"
                    return exList1.eq("address", split[0]).like("domain", "%" + split[1] + "%").findList();
                default: // the entry was something else
                    return exList1.or(Expr.like("address", "%" + input + "%"), Expr.like("domain", "%" + input + "%"))
                                  .findList();
            }

        }
        // the entry may be something like "addr" or "doma" (just a part of the address)
        return exList1.or(Expr.like("address", "%" + input + "%"), Expr.like("domain", "%" + input + "%")).findList();
    }

    /**
     * Returns a list of all email-addresses of the given user
     * 
     * @param userId
     *            the user-ID
     * @return a list of email-addresses
     */

    public static String getMailsForTxt(long userId)
    {
        List<MBox> allBoxes = MBox.allUser(userId);
        return getBoxListToText(allBoxes);
    }

    /**
     * Returns a list of all ACTIVE e-mails of the given user
     * 
     * @param userId
     *            the user-id
     * @return a list of e-mails
     */
    public static String getActiveMailsForTxt(Long userId)
    {
        List<MBox> allActiveBoxes = Ebean.find(MBox.class).where().eq("usr_id", userId.toString()).eq("expired", false)
                                         .findList();
        return getBoxListToText(allActiveBoxes);
    }

    /**
     * Returns a list of all selected e-mails of the given user
     * 
     * @param userId
     *            the user-id
     * @return a list of e-mails
     */
    public static String getSelectedMailsForTxt(Long userId, List<Long> boxIds)
    {

        StringBuilder query = new StringBuilder();
        if (boxIds.size() <= 0)
            return "";

        query.append("SELECT m.id, m.address, m.domain FROM MAILBOXES m WHERE ");
        query.append("m.usr_id = ").append(userId);
        query.append(" AND (");
        for (Long bId : boxIds)
        {
            query.append(" id = ").append(bId);
            query.append(" OR");
        }
        query.delete(query.length() - 2, query.length());
        query.append(");");

        RawSql rawSql = RawSqlBuilder.parse(query.toString()).columnMapping("m.id", "id")
                                     .columnMapping("m.address", "address").columnMapping("m.domain", "domain")
                                     .create();
        Query<MBox> quer = Ebean.find(MBox.class).setRawSql(rawSql);
        List<MBox> selectedBoxes = quer.findList();
        return getBoxListToText(selectedBoxes);

    }

    /**
     * 
     * @param boxes
     *            the list of mail boxed
     * @return the List of Mails as Text
     */
    private static String getBoxListToText(List<MBox> boxes)
    {
        StringBuilder csvMail = new StringBuilder();
        for (MBox mailBox : boxes)
        {
            csvMail.append(mailBox.getFullAddress()).append("\n");
        }
        return csvMail.toString();
    }

    /**
     * Takes the user-ID and Box-IDs, builds an SQL-Statement and deletes all given Boxes.
     * <strong> WARNING:</strong> The Box-ID-String won't be checked again! The calling method has to self-check the
     * correctness of the string (esp. thinking on SQL-Injections, etc.)
     * 
     * @param userId
     *            the userID to whom the boxes should belong (should prevent deletion of 'foreign' boxes)
     * @param boxIds
     *            a List of BoxIDs
     * @return the number of boxes removed (or -1 on error)
     */
    public static int removeListOfBoxes(long userId, List<Long> boxIds)
    {
        StringBuilder sqlSb = new StringBuilder();
        sqlSb.append("DELETE FROM MAILBOXES WHERE USR_ID=").append(userId).append(" AND (");
        return appendIdsAndExecuteSql(sqlSb, boxIds);
    }

    /**
     * Takes the user-ID and Box-IDs, builds an SQL-Statement and resets the suppression and forward-counters of the
     * given Boxes.
     * <strong> WARNING:</strong> The Box-ID-String won't be checked again! The calling method has to self-check the
     * correctness of the string (esp. thinking on SQL-Injections, etc.)
     * 
     * @param userId
     *            the userID to whom the boxes should belong (should prevent reset of 'foreign' boxes)
     * @param boxIds
     *            a List of BoxIDs to process
     * @return the number of boxes reseted (or -1 on error)
     */
    public static int resetListOfBoxes(long userId, List<Long> boxIds)
    {
        StringBuilder sqlSb = new StringBuilder();
        sqlSb.append("UPDATE MAILBOXES SET SUPPRESSIONS = 0, FORWARDS = 0 WHERE USR_ID=").append(userId)
             .append(" AND (");
        return appendIdsAndExecuteSql(sqlSb, boxIds);
    }

    /**
     * Takes the user-ID and Box-IDs, builds an SQL-Statement and disables the given Boxes.
     * <strong> WARNING:</strong> The Box-ID-String won't be checked again! The calling method has to self-check the
     * correctness of the String (esp. thinking on SQL-Injections, etc.)
     * 
     * @param userId
     *            the userID to whom the boxes should belong (should prevent disabling of 'foreign' boxes)
     * @param boxIds
     *            a String of BoxIDs concatenated by a comma
     * @return the number of boxes disabled (or -1 on error)
     */
    public static int disableListOfBoxes(long userId, List<Long> boxIds)
    {
        StringBuilder sqlSb = new StringBuilder();
        sqlSb.append("UPDATE MAILBOXES SET EXPIRED = TRUE WHERE USR_ID=").append(userId).append(" AND (");
        return appendIdsAndExecuteSql(sqlSb, boxIds);
    }

    /**
     * Takes the user-ID and Box-IDs, builds an SQL-Statement and enables the given Boxes (if not already enabled).
     * <strong> WARNING:</strong> The Box-ID-String won't be checked again! The calling method has to self-check the
     * correctness of the String (esp. thinking on SQL-Injections, etc.)
     * 
     * @param userId
     *            the userID to whom the boxes should belong (should prevent enabling of 'foreign' boxes)
     * @param boxIds
     *            a String of BoxIDs concatenated by a comma
     * @return the number of boxes enabled (or -1 on error)
     */
    public static int enableListOfBoxesIfPossible(long userId, List<Long> boxIds)
    {
        StringBuilder sqlSb = new StringBuilder();
        sqlSb.append("UPDATE MAILBOXES SET EXPIRED = FALSE WHERE USR_ID=").append(userId);
        sqlSb.append(" AND (TS_ACTIVE > ").append(DateTime.now().getMillis()).append(" OR TS_ACTIVE = 0) ");
        sqlSb.append(" AND (");
        return appendIdsAndExecuteSql(sqlSb, boxIds);
    }

    /**
     * Takes the user-ID and Box-IDs, builds an SQL-Statement and sets a new timestamp for the given Boxes.
     * <strong> WARNING:</strong> The Box-ID-String won't be checked again! The calling method has to self-check the
     * correctness of the String (esp. thinking on SQL-Injections, etc.)
     * 
     * @param userId
     *            the userID to whom the boxes should belong (should prevent new timestamp for 'foreign' boxes)
     * @param boxIds
     *            a String of BoxIDs concatenated by a comma
     * @param ts_Active
     *            the timestamp to set
     * @return the number of boxes where a new timestamp was set (or -1 on error)
     */
    public static int setNewDateForListOfBoxes(long userId, List<Long> boxIds, long ts_Active)
    {
        StringBuilder sqlSb = new StringBuilder();
        sqlSb.append("UPDATE MAILBOXES SET EXPIRED = FALSE, TS_ACTIVE =").append(ts_Active);
        sqlSb.append("WHERE USR_ID=").append(userId);
        sqlSb.append(" AND (");
        return appendIdsAndExecuteSql(sqlSb, boxIds);
    }

    private static int appendIdsAndExecuteSql(StringBuilder sqlSb, List<Long> boxIds)
    {
        if (boxIds.isEmpty())
            return -1;

        for (Long id : boxIds)
        {
            sqlSb.append(" ID=").append(id).append(" OR");
        }
        sqlSb.delete(sqlSb.length() - 2, sqlSb.length());
        sqlSb.append(");");
        SqlUpdate down = Ebean.createSqlUpdate(sqlSb.toString());

        return down.execute();
    }

    public String toString()
    {
        return getFullAddress() + " " + getTSAsStringWithNull() + " expired:" + isExpired();
    }
}
