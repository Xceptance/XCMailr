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

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.joda.time.DateTime;

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
    @Size(min = 1, max = 255)
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
     * Sets whether the mailbox is active.
     *
     * @param active true if active, false if expired/inactive
     */
    @JsonIgnore
    public void setActive(boolean active)
    {
        this.expired = !active;
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
