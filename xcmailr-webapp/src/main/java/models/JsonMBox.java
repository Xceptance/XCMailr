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


import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;


/**
 * Object for a virtual Mailbox (a Mail-Forward)
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class JsonMBox
{
    /**
     * Mailbox ID
     */
    private long id;

    /**
     * Mailaddress of the Box
     */
    @NotEmpty
    @Pattern(regexp = "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*")
    private String address;

    /**
     * Timestamp for the end of the validity period
     */
    private long ts_Active;

    /**
     * Flag for the validity
     */
    private boolean expired;

    /**
     * the domain-part of an address
     */
    @NotEmpty
    @Pattern(regexp = "[A-Za-z-]+(\\.[\\w-]+)+")
    @Length(min = 1, max = 255)
    private String domain;

    /**
     * the number of forwards for this box
     */
    private int forwards;

    /**
     * the number of suppressions for this box
     */
    private int suppressions;

    @NotEmpty
    @Length(min = 1, max = 255)
    private String datetime;

    private String fullAddress;

    /**
     * Default-Constructor
     */
    public JsonMBox()
    {
        this.address = "";
        this.ts_Active = 0L;
        this.expired = false;
        this.domain = "";
        this.forwards = 0;
        this.suppressions = 0;
        this.fullAddress = "";
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

    public JsonMBox(String local, String domain, long ts, boolean expired)
    {
        this.address = local;
        this.ts_Active = ts;
        this.expired = expired;
        this.domain = domain;
        this.forwards = 0;
        this.suppressions = 0;
        this.fullAddress = local + '@' + domain;

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
        if (expired && (ts_Active != 0) && (DateTime.now().isAfter(ts_Active)))
        {
            return true;
        }
        else
        {
            return false;
        }
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

    /**
     * @return the date-time in an easy human-readable way
     */
    public String getDatetime()
    {
        return datetime;
    }

    /**
     * @param dateTime
     *            the date-time in an easy human-readable way
     */
    public void setDatetime(String dateTime)
    {
        this.datetime = dateTime;
    }

    public void setFullAddress(String fullAddress)
    {
        this.fullAddress = fullAddress;
    }

    /**
     * pre-populates the fields of the current JsonMBox-Object with the given MBox-Object
     * 
     * @param mailbox
     *            the MBox-Object
     */
    public void prepopulateJS(MBox mailbox)
    {
        this.address = mailbox.getAddress();
        this.datetime = mailbox.getTSAsString();
        this.domain = mailbox.getDomain();
        this.expired = mailbox.isExpired();
        this.forwards = mailbox.getForwards();
        this.id = mailbox.getId();
        this.suppressions = mailbox.getSuppressions();
        this.ts_Active = mailbox.getTs_Active();
        this.fullAddress = mailbox.getFullAddress();
    }

    /**
     * pre-populates a JsonMBox-Object in a static way, by the given MBox-Object-values and returns it
     * 
     * @param mailbox
     *            the MBox-Object to parse
     * @return a JsonMBox-Object
     */
    public static JsonMBox prepopulate(MBox mailbox)
    {
        JsonMBox jsonMailbox = new JsonMBox();
        jsonMailbox.setAddress(mailbox.getAddress());
        jsonMailbox.setDatetime(mailbox.getTSAsString());
        jsonMailbox.setDomain(mailbox.getDomain());
        jsonMailbox.setExpired(mailbox.isExpired());
        jsonMailbox.setForwards(mailbox.getForwards());
        jsonMailbox.setSuppressions(mailbox.getSuppressions());
        jsonMailbox.setId(mailbox.getId());
        jsonMailbox.setTs_Active(mailbox.getTs_Active());
        jsonMailbox.setFullAddress(mailbox.getFullAddress());
        return jsonMailbox;

    }
}
