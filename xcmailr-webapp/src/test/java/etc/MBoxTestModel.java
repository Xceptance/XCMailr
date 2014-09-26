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
package etc;

import models.User;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Object for a virtual Mailbox (a Mail-Forward)
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@JsonSerialize(using = MBoxTestSerializer.class)
@JsonDeserialize(using = MBoxTestDeserializer.class)
public class MBoxTestModel 
{
    private long id;
    /** Mailaddress of the Box */
    private String address;

    /** Timestamp for the end of the validity period */
    private long ts_Active;

    /** Flag for the validity */
    private boolean expired;

    /** the domain-part of an address */
    private String domain;

    /** the number of forwards for this box */
    private int forwards;

    /** the number of suppressions for this box */
    private int suppressions;

    private String datetime;
    

    /** the owner of the address/box */
    private User usr;

    /**
     * Default-Constructor
     */
    public MBoxTestModel()
    {
        this.address = "";
        this.ts_Active = 0L;
        this.expired = false;
        this.domain = "";
        this.datetime="0";
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

    public MBoxTestModel(String local, String domain, long ts, boolean expired, User usr, String datetime)
    {
        this.address = local;
        this.ts_Active = ts;
        this.expired = expired;
        this.domain = domain;
        this.forwards = 0;
        this.suppressions = 0;
        this.datetime = datetime;
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
     * @return true, if the mail is inactive and the TS has a value in the past<br/>
     *         false, else
     */

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
     * increases the forward-count directly in the database
     */
    public void increaseFwd()
    {
        setForwards(getForwards() + 1);
    }

    /**
     * increases the suppression-count directly in the database
     */
    public void increaseSup()
    {
        setSuppressions(getSuppressions() + 1);
    }


    /**
     * @return the timestamp as string in the format "yyyy-MM-dd hh:mm" <br/>
     *         if its 0, then also 0 is returned
     */
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
     * @return the timestamp as string in the format "yyyy-MM-dd hh:mm"<br/>
     *         if its 0, then "unlimited" is returned
     */

    public String getDatetime()
    {
        return datetime;
    }

    public void resetIdAndCounterFields()
    {
        this.setId(0);
        resetForwards();
        resetSuppressions();
    }



    public void setDatetime(String datetime)
    {
        this.datetime = datetime;
    }

    /**
     * Sets the valid Box as invalid and vice versa (and updates the database!)
     * 
     * @return Value of true means that the Box is now enabled (== not expired)
     */

    public boolean enable()
    {
        this.setExpired(!expired);
        return !this.isExpired();
    }

    public String toString(){
        return getFullAddress()+" "+getTSAsStringWithNull()+" expired:"+isExpired();
    }
}
