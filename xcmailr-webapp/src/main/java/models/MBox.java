/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.Pattern;

import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;

import io.ebean.Ebean;
import io.ebean.Expr;
import io.ebean.ExpressionList;
import io.ebean.Query;
import io.ebean.RawSql;
import io.ebean.RawSqlBuilder;
import io.ebean.SqlUpdate;
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
    @Pattern(regexp = "(?i)^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*")
    private String address;

    /** Timestamp for the end of the validity period */
    @JsonIgnore
    private long ts_Active;

    /** Flag for the validity */
    @JsonIgnore
    private boolean expired;

    /** the domain-part of an address */
    @NotEmpty
    @Pattern(regexp = "(?i)[a-z-]+(\\.[\\w-]+)+")
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

    /** should emails forwarded for this address */
    private boolean forwardEmails;

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
        increaseForwards();
        Ebean.createSqlUpdate("update mailboxes set forwards = ? where id = ?;") //
             .setParameter(1, getForwards()) //
             .setParameter(2, getId()) //
             .execute();
    }

    /**
     * increases the suppression-count directly in the database
     */
    public void increaseSup()
    {
        increaseSuppressions();
        Ebean.createSqlUpdate("update mailboxes set suppressions = ? where id = ?;") //
             .setParameter(1, getSuppressions()) //
             .setParameter(2, getId()) //
             .execute();
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
        return queryByName(mail, domain).findOne();
    }

    /**
     * Creates an EBean expression list for the given local and domain part.
     * 
     * @param localPart
     *            the local part
     * @param domainPart
     *            the domain part
     * @return expression list
     */
    private static ExpressionList<MBox> queryByName(String localPart, String domainPart)
    {
        return Ebean.find(MBox.class).where().ieq("address", localPart).ieq("domain", domainPart);
    }

    /**
     * Returns the mailbox with the given mail address.
     * 
     * @param mailAddress
     *            the complete mail address
     * @return the MBox object that belongs to this address, or <code>null</code> if not found
     */
    public static MBox getByAddress(final String mailAddress)
    {
        final String[] parts = StringUtils.split(mailAddress, "@");
        if (parts == null || parts.length != 2)
        {
            return null;
        }

        return getByName(parts[0], parts[1]);
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
        final MBox mb = getByName(mail, domain);
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
        return queryByName(mail, domain).exists();
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
     * Sets the Box as valid (and updates the database!)
     */
    public void enable()
    {
        this.setExpired(false);
        Ebean.update(this);
    }

    /**
     * Sets the Box as invalid (and updates the database!)
     */
    public void disable()
    {
        this.setExpired(true);
        Ebean.update(this);
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
                    return exList1.or(Expr.ilike("address", "%" + split[0] + "%"),
                                      Expr.ilike("domain", "%" + split[0] + "%"))
                                  .findList();

                case (2): // the entry was something like "address@domain"
                    return exList1.ieq("address", split[0]).ilike("domain", "%" + split[1] + "%").findList();
                default: // the entry was something else
                    return exList1.or(Expr.ilike("address", "%" + input + "%"), Expr.ilike("domain", "%" + input + "%"))
                                  .findList();
            }

        }
        // the entry may be something like "address" or "domain" (just a part of the address)
        return exList1.or(Expr.ilike("address", "%" + input + "%"), Expr.ilike("domain", "%" + input + "%")).findList();
    }

    /**
     * Finds a mailbox with the given address parts (local part and domain). The address part lookup is
     * case-insensitive.
     * 
     * @param localPart
     *            the local part of the address
     * @param domain
     *            the domain of the address
     * @return the mailbox or <code>null</code> if not found
     */
    public static MBox find(String localPart, String domain)
    {
        return Ebean.find(MBox.class).where().ieq("address", localPart).ieq("domain", domain).findOne();
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
        if (boxIds.size() <= 0)
            return "";

        final String baseStmt = "SELECT id, address, domain FROM MAILBOXES WHERE usr_id=" + userId;

        final List<MBox> selectedBoxes = new ArrayList<>();

        // process mailboxes in chunks
        processMailboxesInChunks(baseStmt.toString(), boxIds,
                                 // cannot use a lambda here as the EBean enhancer is unable to handle it :-(
                                 new Consumer<String>()
                                 {
                                     public void accept(String finalStmt)
                                     {
                                         // execute the final statement
                                         final RawSql rawSql = RawSqlBuilder.parse(finalStmt).create();
                                         final Query<MBox> query = Ebean.find(MBox.class).setRawSql(rawSql);
                                         final List<MBox> boxes = query.findList();

                                         // aggregate the results
                                         selectedBoxes.addAll(boxes);
                                     }
                                 });

        return getBoxListToText(selectedBoxes);
    }

    /**
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
     * Takes the user-ID and Box-IDs, builds an SQL-Statement and deletes all given Boxes. <strong> WARNING:</strong>
     * The Box-ID-String won't be checked again! The calling method has to self-check the correctness of the string
     * (esp. thinking on SQL-Injections, etc.)
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
        sqlSb.append("DELETE FROM MAILBOXES WHERE USR_ID=").append(userId);
        return appendIdsAndExecuteSql(sqlSb, boxIds);
    }

    /**
     * Takes the user-ID and Box-IDs, builds an SQL-Statement and resets the suppression and forward-counters of the
     * given Boxes. <strong> WARNING:</strong> The Box-ID-String won't be checked again! The calling method has to
     * self-check the correctness of the string (esp. thinking on SQL-Injections, etc.)
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
        sqlSb.append("UPDATE MAILBOXES SET SUPPRESSIONS = 0, FORWARDS = 0 WHERE USR_ID=").append(userId);
        return appendIdsAndExecuteSql(sqlSb, boxIds);
    }

    /**
     * Takes the user-ID and Box-IDs, builds an SQL-Statement and disables the given Boxes. <strong> WARNING:</strong>
     * The Box-ID-String won't be checked again! The calling method has to self-check the correctness of the String
     * (esp. thinking on SQL-Injections, etc.)
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
        sqlSb.append("UPDATE MAILBOXES SET EXPIRED = TRUE WHERE USR_ID=").append(userId);
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
        return appendIdsAndExecuteSql(sqlSb, boxIds);
    }

    /**
     * Takes the user-ID and Box-IDs, builds an SQL-Statement and sets a new timestamp for the given Boxes. <strong>
     * WARNING:</strong> The Box-ID-String won't be checked again! The calling method has to self-check the correctness
     * of the String (esp. thinking on SQL-Injections, etc.)
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
        sqlSb.append(" WHERE USR_ID=").append(userId);
        return appendIdsAndExecuteSql(sqlSb, boxIds);
    }

    private static int appendIdsAndExecuteSql(StringBuilder baseStmt, List<Long> boxIds)
    {
        if (boxIds.isEmpty())
            return -1;

        final AtomicInteger totalProcessedCount = new AtomicInteger(0);

        // process mailboxes in chunks
        processMailboxesInChunks(baseStmt.toString(), boxIds,
                                 // cannot use a lambda here as the EBean enhancer is unable to handle it :-(
                                 new Consumer<String>()
                                 {
                                     public void accept(String stmt)
                                     {
                                         // execute the SQL statement
                                         final SqlUpdate down = Ebean.createSqlUpdate(stmt.toString());
                                         final int processedCount = down.execute();

                                         // aggregate result
                                         totalProcessedCount.addAndGet(processedCount);
                                     }
                                 });

        // return the aggregated result
        return totalProcessedCount.get();
    }

    /**
     * Processes the mailboxes for the given list of mailbox IDs in chunks of 1000. For each chunk, the base SQL
     * statement will be extended with the IDs in that chunk and the action will be called with that extended statement.
     * 
     * @param baseStmt
     *            the base SQL statement that will be extended by a chunk of mailbox IDs
     * @param boxIds
     *            the total list of mailbox IDs
     * @param action
     *            the action to be performed with each chunk
     */
    private static void processMailboxesInChunks(String baseStmt, List<Long> boxIds, Consumer<String> action)
    {
        int chunkSize = 1000;
        int totalCount = boxIds.size();

        // process the mailboxes in chunks of 1000
        for (int i = 0; i < totalCount; i = i + chunkSize)
        {
            // get the next chunk of mailbox IDs
            List<Long> subList = boxIds.subList(i, Math.min(i + chunkSize, totalCount));

            // create a new SQL statement and complete it with our chunk of mailbox IDs
            StringBuilder stmt = new StringBuilder(baseStmt);
            stmt.append(" AND ID IN (");
            stmt.append(StringUtils.join(subList, ", "));
            stmt.append(");");

            // execute the custom action with the final statement
            action.accept(stmt.toString());
        }
    }

    public String toString()
    {
        return getFullAddress() + " " + getTSAsStringWithNull() + " expired:" + isExpired();
    }

    /**
     * @return boolean indicating whether arriving e-mails should be forwarded to accounts email address or not
     */
    public boolean isForwardEmails()
    {
        return forwardEmails;
    }

    /**
     * @param forwardEmails
     *            set if e-mails should be forwarded to user account's email address or not
     */
    public void setForwardEmails(boolean forwardEmails)
    {
        this.forwardEmails = forwardEmails;
    }
}
