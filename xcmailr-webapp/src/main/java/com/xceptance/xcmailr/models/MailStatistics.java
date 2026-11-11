/*
 * Copyright (c) 2013-2026 Xceptance Software Technologies GmbH
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
package com.xceptance.xcmailr.models;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * JPA entity representing aggregated email traffic statistics bucketed by date,
 * 15-minute quarter-hour interval, sender domain, and target domain. Tracks dropped,
 * forwarded, and locally stored email counts.
 *
 * @author Patrick Thum, Xceptance Software Technologies GmbH
 */
@Entity
@Table(name = "MAIL_STATISTICS")
public class MailStatistics implements Serializable
{
    private static final long serialVersionUID = 6489599509883330752L;

    @EmbeddedId
    private MailStatisticsKey key;

    @Column(name = "DROP_COUNT")
    private int dropCount;

    @Column(name = "FORWARD_COUNT")
    private int forwardCount;

    @Column(name = "STORE_COUNT")
    private int storeCount;

    /**
     * Default no-arg constructor required by JPA.
     */
    public MailStatistics()
    {
    }

    /**
     * Parameterized constructor initializing the statistics key and metric counters.
     *
     * @param key the composite statistics key (date, quarter-hour, fromDomain, targetDomain)
     * @param dropCount count of dropped messages
     * @param forwardCount count of forwarded messages
     * @param storeCount count of locally stored messages
     */
    public MailStatistics(final MailStatisticsKey key, final int dropCount, final int forwardCount, final int storeCount)
    {
        this.key = key;
        this.dropCount = dropCount;
        this.forwardCount = forwardCount;
        this.storeCount = storeCount;
    }

    /**
     * Returns the composite primary key.
     *
     * @return the statistics key
     */
    public MailStatisticsKey getKey()
    {
        return key;
    }

    /**
     * Sets the composite primary key.
     *
     * @param key the statistics key to set
     */
    public void setKey(final MailStatisticsKey key)
    {
        this.key = key;
    }

    /**
     * Returns the number of dropped emails in this time interval and domain bucket.
     *
     * @return drop count
     */
    public int getDropCount()
    {
        return dropCount;
    }

    /**
     * Sets the number of dropped emails.
     *
     * @param dropCount drop count to set
     */
    public void setDropCount(final int dropCount)
    {
        this.dropCount = dropCount;
    }

    /**
     * Returns the number of forwarded emails in this time interval and domain bucket.
     *
     * @return forward count
     */
    public int getForwardCount()
    {
        return forwardCount;
    }

    /**
     * Sets the number of forwarded emails.
     *
     * @param forwardCount forward count to set
     */
    public void setForwardCount(final int forwardCount)
    {
        this.forwardCount = forwardCount;
    }

    /**
     * Returns the number of locally stored emails in this time interval and domain bucket.
     *
     * @return store count
     */
    public int getStoreCount()
    {
        return storeCount;
    }

    /**
     * Sets the number of locally stored emails.
     *
     * @param storeCount store count to set
     */
    public void setStoreCount(final int storeCount)
    {
        this.storeCount = storeCount;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + dropCount;
        result = prime * result + forwardCount;
        result = prime * result + storeCount;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (!(obj instanceof MailStatistics))
        {
            return false;
        }
        final MailStatistics other = (MailStatistics) obj;
        if (dropCount != other.dropCount)
        {
            return false;
        }
        if (forwardCount != other.forwardCount)
        {
            return false;
        }
        if (storeCount != other.storeCount)
        {
            return false;
        }
        if (key == null)
        {
            if (other.key != null)
            {
                return false;
            }
        }
        else if (!key.equals(other.key))
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        final String dateStr = (key != null && key.getDate() != null) ? key.getDate().toString() : "null";
        final String qHourStr = (key != null) ? String.valueOf(key.getQuarterHour()) : "null";
        final String fromStr = (key != null) ? key.getFromDomain() : "null";
        final String targetStr = (key != null) ? key.getTargetDomain() : "null";

        return String.format("MailStatistics [date=%s, quarterHour=%s, fromDomain=%s, targetDomain=%s, dropCount=%d, forwardCount=%d, storeCount=%d]",
                             dateStr, qHourStr, fromStr, targetStr, dropCount, forwardCount, storeCount);
    }

    /**
     * Formats an integer value with space grouping separators.
     *
     * @param value the numeric value to format
     * @return formatted string representation
     */
    public static String format(final int value)
    {
        final DecimalFormat formatter = new DecimalFormat("");
        final DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        decimalFormatSymbols.setGroupingSeparator(' ');
        formatter.setDecimalFormatSymbols(decimalFormatSymbols);
        formatter.setGroupingUsed(true);

        return formatter.format(value);
    }
}
