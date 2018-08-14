package etc;

import java.sql.Date;

public class MailStatisticsKey
{
    private Date date;

    /**
     * The n-th 15-minute intervall in the day indicated by "date". A day usually has 96 15-minute intervalls
     */
    private int quarterHour;

    private String fromDomain;

    private String targetDomain;

    public MailStatisticsKey(Date date, int quarterHour, String fromDomain, String targetDomain)
    {
        this.date = date;
        this.quarterHour = quarterHour;
        this.fromDomain = fromDomain;
        this.targetDomain = targetDomain;
    }

    public Date getDate()
    {
        return date;
    }

    public int getQuarterHour()
    {
        return quarterHour;
    }

    public String getFromDomain()
    {
        return fromDomain;
    }

    public String getTargetDomain()
    {
        return targetDomain;
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
        result = prime * result + ((fromDomain == null) ? 0 : fromDomain.hashCode());
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
        if (!(obj instanceof MailStatisticsKey))
        {
            return false;
        }
        MailStatisticsKey other = (MailStatisticsKey) obj;
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
}
