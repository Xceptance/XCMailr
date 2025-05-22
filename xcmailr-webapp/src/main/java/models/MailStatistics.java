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
import java.sql.Date;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.ebean.DB;

@Entity
@Table(name = "MAIL_STATISTICS")
public class MailStatistics implements Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = 6489599509883330752L;

    @EmbeddedId
    private MailStatisticsKey key;

    @Column(name = "DROP_COUNT")
    private int dropCount;

    @Column(name = "FORWARD_COUNT")
    private int forwardCount;

    public MailStatisticsKey getKey()
    {
        return key;
    }

    public void setKey(MailStatisticsKey key)
    {
        this.key = key;
    }

    public int getDropCount()
    {
        return dropCount;
    }

    public void setDropCount(int dropCount)
    {
        this.dropCount = dropCount;
    }

    public int getForwardCount()
    {
        return forwardCount;
    }

    public void setForwardCount(int forwardCount)
    {
        this.forwardCount = forwardCount;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + dropCount;
        result = prime * result + forwardCount;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
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
        MailStatistics other = (MailStatistics) obj;
        if (dropCount != other.dropCount)
        {
            return false;
        }
        if (forwardCount != other.forwardCount)
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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return String.format("MailStatistics [date=%s, quarterHour=%s, fromDomain=%s, targetDomain=%s, dropCount=%s, forwardCount=%s]",
                             key.getDate(), key.getQuarterHour(), key.getFromDomain(), key.getTargetDomain(), dropCount,
                             forwardCount);
    }

    public static String format(int value)
    {
        DecimalFormat formatter = new DecimalFormat("");
        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        decimalFormatSymbols.setGroupingSeparator(' ');
        formatter.setDecimalFormatSymbols(decimalFormatSymbols);
        formatter.setGroupingUsed(true);

        return formatter.format(value);
    }

    /**
     * Deletes all {@link MailStatistics} entries with a date value older than the given date.
     * 
     * @param date
     *            the date
     * @return the number of deleted entries
     */
    public static int deleteAllOlderThan(Date date)
    {
        String sql = "DELETE FROM MAIL_STATISTICS WHERE date < '" + date + "';";

        return DB.sqlUpdate(sql).execute();
    }
}
