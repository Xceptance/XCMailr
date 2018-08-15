package models;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "MAIL_STATISTICS")
public class MailStatistics
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private Date date;

    /**
     * The n-th 15-minute intervall in the day indicated by "date". A day usually has 96 15-minute intervalls
     */
    @Column(name = "QUARTER_HOUR", nullable = false)
    private int quarterHour;

    @Column(name = "FROM_DOMAIN", nullable = false)
    private String fromDomain;

    @Column(name = "TARGET_DOMAIN", nullable = false)
    private String targetDomain;

    @Column(name = "DROP_COUNT")
    private int dropCount;

    @Column(name = "FORWARD_COUNT")
    private int forwardCount;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public int getQuarterHour()
    {
        return quarterHour;
    }

    public void setQuarterHour(int quarterHour)
    {
        this.quarterHour = quarterHour;
    }

    public String getFromDomain()
    {
        return fromDomain;
    }

    public void setFromDomain(String fromDomain)
    {
        this.fromDomain = fromDomain;
    }

    public String getTargetDomain()
    {
        return targetDomain;
    }

    public void setTargetDomain(String targetDomain)
    {
        this.targetDomain = targetDomain;
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
        result = prime * result + ((date == null) ? 0 : date.hashCode());
        result = prime * result + dropCount;
        result = prime * result + forwardCount;
        result = prime * result + ((fromDomain == null) ? 0 : fromDomain.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + quarterHour;
        result = prime * result + ((targetDomain == null) ? 0 : targetDomain.hashCode());
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
        if (date == null)
        {
            if (other.date != null)
            {
                return false;
            }
        }
        else if (!date.equals(other.date))
        {
            return false;
        }
        if (dropCount != other.dropCount)
        {
            return false;
        }
        if (forwardCount != other.forwardCount)
        {
            return false;
        }
        if (fromDomain == null)
        {
            if (other.fromDomain != null)
            {
                return false;
            }
        }
        else if (!fromDomain.equals(other.fromDomain))
        {
            return false;
        }
        if (id == null)
        {
            if (other.id != null)
            {
                return false;
            }
        }
        else if (!id.equals(other.id))
        {
            return false;
        }
        if (quarterHour != other.quarterHour)
        {
            return false;
        }
        if (targetDomain == null)
        {
            if (other.targetDomain != null)
            {
                return false;
            }
        }
        else if (!targetDomain.equals(other.targetDomain))
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
        return String.format("MailStatistics [id=%s, date=%s, quarterHour=%s, fromDomain=%s, targetDomain=%s, dropCount=%s, forwardCount=%s]",
                             id, date, quarterHour, fromDomain, targetDomain, dropCount, forwardCount);
    }
}
