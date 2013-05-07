package models;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.joda.time.DateTime;
import org.joda.time.Period;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;

/**
 * This Class is used to save all Actions on the Mailserver
 * 
 * @author Patrick Thum
 */
@Entity
@Table(name = "mailtransactions")
public class MailTransaction
{
    @Id
    private Long id;

    private Long ts;

    private int status;

    private String sourceaddr;

    private String targetaddr;

    /**
     * the Default-Constructor which initialises all fields with Default-values
     */
    public MailTransaction()
    {
        id = 0L;
        ts = DateTime.now().getMillis();
        status = 0;
        targetaddr = "";
        sourceaddr = "";
    }

    /**
     * Creates an MailTransaction-Object, with parameters<br/>
     * <b>Statuscodes:</b> <br/>
     *   0 - Mail has a wrong pattern
     * 100 - Mail does not exist<br/>
     * 200 - Mail exists but is inactive <br/>
     * 300 - Mail has been successfully <br/>
     * 400 - the Mail can't be forwarded (target not reachable)<br/>
     * 
     * @param stat
     *            -statuscode of the transaction
     * @param target
     *            -recipients-address of the mail
     */
    public MailTransaction(int stat, String target, String source)
    {
        ts = DateTime.now().getMillis();
        this.status = stat;
        this.targetaddr = target;
        this.sourceaddr = source;
    }

    /**
     * @return the id of this transaction (primary key in the DB)
     */
    public Long getId()
    {
        return id;
    }

    /**
     * @param id
     *            - the id to set
     */
    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * @return the timestamp of this transaction
     */
    public Long getTs()
    {
        return ts;
    }

    /**
     * @return the Timestamp in a displayable form as String
     */
    public String getTsAsString()
    {
        DateTime dt = new DateTime(this.ts);
        String day = "";
        String mon = "";
        String hou = "";
        String min = "";

        if (dt.getDayOfMonth() < 10)
        {
            day += "0";
        }
        day += String.valueOf(dt.getDayOfMonth());

        if (dt.getMonthOfYear() < 10)
        {
            mon += "0";
        }
        mon += String.valueOf(dt.getMonthOfYear());

        if (dt.getHourOfDay() < 10)
        {
            hou += "0";
        }
        hou += String.valueOf(dt.getHourOfDay());
        
        if (dt.getMinuteOfHour() < 10)
        {
            min += "0";
        }
        min += String.valueOf(dt.getMinuteOfHour());

        return day + "." + mon + "." + dt.getYear() + " " + hou + ":" + min;

    }

    /**
     * @param ts
     *            sets the timestamp in milliseconds
     */
    public void setTs(Long ts)
    {
        this.ts = ts;
    }

    /**
     * <b>Statuscodes:</b> <br/>
     * 100 - targetmail does not exist <br/>
     * 200 - targetmail exists but is inactive <br/>
     * 300 - mail has been successfully forwarded <br/>
     * 
     * @return a statuscode
     */
    public int getStatus()
    {
        return status;
    }

    /**
     * <b>Statuscodes:</b> <br/>
     * 100 - targetmail does not exist <br/>
     * 200 - targetmail exists but is inactive <br/>
     * 300 - mail has been successfully forwarded <br/>
     * 
     * @param status
     *            - the status to set
     */
    public void setStatus(int status)
    {
        this.status = status;
    }

    /**
     * @return the targetaddress of this transaction
     */
    public String getTargetaddr()
    {
        return targetaddr;
    }

    /**
     * @param targetaddr
     *            - the targetaddress to set
     */
    public void setTargetaddr(String targetaddr)
    {
        this.targetaddr = targetaddr;
    }

    /**
     * @return the sourceaddress of this transaction
     */
    public String getSourceaddr()
    {
        return sourceaddr;
    }

    /**
     * @param sourceaddr
     *            - the sourceaddress to set
     */
    public void setSourceaddr(String sourceaddr)
    {
        this.sourceaddr = sourceaddr;
    }

    // -------------------------------------------------------
    // E-Bean Functions
    // -------------------------------------------------------
    /**
     * Saves the Transaction in the Database
     */
    public void saveTx()
    {
        Ebean.save(this);
    }

    /**
     * @return all Transactions which were stored in the database
     */
    public static List<MailTransaction> all()
    {
        return Ebean.find(MailTransaction.class).findList();
    }

    /**
     * Gets all mailtransactions in the last "Period"
     * 
     * @param period
     *            - Joda-Time Period
     * @return a List of Mailtransactions
     */
    public static List<MailTransaction> allInPeriod(Period period)
    {
        return Ebean.find(MailTransaction.class).where().gt("ts", DateTime.now().minus(period).getMillis()).findList();
    }

    /**
     * Generates a List of Statusnumbers and the number of ther occurences
     * 
     * @return a List of Status-Elements (as an aggregate of Transactions)
     * @see Status
     */
    public static List<Status> getStatusList()
    {

        String sql = "SELECT mtx.status, COUNT(mtx.status) AS count  FROM mailtransactions mtx GROUP BY mtx.status";
        RawSql rawSql = RawSqlBuilder.parse(sql).columnMapping("mtx.status", "statuscode").create();
        Query<Status> query = Ebean.find(Status.class);
        query.setRawSql(rawSql);
        List<Status> list = query.findList();

        return list;
    }

}
