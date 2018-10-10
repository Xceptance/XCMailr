package models;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

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
}
