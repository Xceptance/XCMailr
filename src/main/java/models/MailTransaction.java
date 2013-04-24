package models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.joda.time.DateTime;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.annotation.Sql;

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
     * 100 - targetmail does not exist<br/>
     * 200 - targetmail exists but is inactive <br/>
     * 300 - mail has been successfully
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

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getTs()
    {
        return ts;
    }

    public String getTsAsString()
    {
        DateTime dt = new DateTime(this.ts);

        String min = "";
        if (dt.getMinuteOfHour() < 10)
        {
            min = "0" + String.valueOf(dt.getMinuteOfHour());
        }
        else
        {
            min = String.valueOf(dt.getMinuteOfHour());
        }

        return dt.getDayOfMonth() + "." + dt.getMonthOfYear() + "." + dt.getYear() + " " + dt.getHourOfDay() + ":"
               + min;

    }

    public void setTs(Long ts)
    {
        this.ts = ts;
    }

    /**
     * <b>Statuscodes:</b> <br/>
     * 100 - targetmail does not exist 200 - targetmail exists but is inactive 300 - mail has been successfully
     * forwarded
     * 
     * @return a statuscode
     */
    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public String getTargetaddr()
    {
        return targetaddr;
    }

    public void setTargetaddr(String targetaddr)
    {
        this.targetaddr = targetaddr;
    }

    public String getSourceaddr()
    {
        return sourceaddr;
    }

    public void setSourceaddr(String sourceaddr)
    {
        this.sourceaddr = sourceaddr;
    }

    // -------------------------------------------------------
    // E-Bean Functions
    // -------------------------------------------------------

    public void saveTx()
    {
        Ebean.save(this);
    }

    public static List<MailTransaction> all()
    {
        return Ebean.find(MailTransaction.class).findList();
    }

    public static List<Status> getStatusMap()
    {

        String sql = "SELECT mtx.status, COUNT(mtx.status) AS count  FROM mailtransactions mtx GROUP BY mtx.status";
        RawSql rawSql = RawSqlBuilder.parse(sql).columnMapping("mtx.status", "statuscode").create();
        Query<Status> query = Ebean.find(Status.class);
        query.setRawSql(rawSql);
        List<Status> list = query.findList();

        return list;
    }


}
