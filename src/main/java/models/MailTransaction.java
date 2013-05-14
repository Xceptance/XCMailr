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
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 * 
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
     * the Default-Constructor which initializes all Fields with Default-values
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
     * Creates an MailTransaction-Object, with Parameters<br/>
     * <b>Statuscodes:</b> <br/>
     *   0 - Mail has a wrong Pattern<br/>
     * 100 - Mail does not exist<br/>
     * 200 - Mail exists but is inactive <br/>
     * 300 - Mail has been forwarded successfully <br/>
     * 400 - the Mail can't be forwarded (target not reachable)<br/>
     * 
     * @param stat
     *            - Statuscode of the Transaction
     * @param target
     *            - Original Recipients-Address of the Mail
     * @param source 
     *            - the Sender's - Address
     */
    public MailTransaction(int stat, String target, String source)
    {
        ts = DateTime.now().getMillis();
        this.status = stat;
        this.targetaddr = target;
        this.sourceaddr = source;
    }

    /**
     * @return the ID of this Transaction 
     */
    public Long getId()
    {
        return id;
    }

    /**
     * @param id
     *            - the ID of this Transaction to set
     */
    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * @return the Timestamp of this Transaction
     */
    public Long getTs()
    {
        return ts;
    }

    /**
     * @return the Timestamp as String in the Format "dd.mm.yyyy hh:mm"
     */
    public String getTsAsString()
    {
        DateTime dt = new DateTime(this.ts);
        String day = "";
        String mon = "";
        String hou = "";
        String min = "";
        
        //add a leading "0" if the value is under ten
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
     *            - sets the Timestamp in Milliseconds
     */
    public void setTs(Long ts)
    {
        this.ts = ts;
    }

    /**
     * <b>Statuscodes:</b> <br/>
     *   0 - Mail has a wrong Pattern <br/>
     * 100 - Mail does not exist<br/>
     * 200 - Mail exists but is inactive <br/>
     * 300 - Mail has been forwarded successfully <br/>
     * 400 - the Mail can't be forwarded (target not reachable)<br/>
     * 
     * @return a Statuscode
     */
    public int getStatus()
    {
        return status;
    }

    /**
     * <b>Statuscodes:</b> <br/>
     *   0 - Mail has a wrong Pattern<br/>
     * 100 - Mail does not exist<br/>
     * 200 - Mail exists but is inactive <br/>
     * 300 - Mail has been forwarded successfully <br/>
     * 400 - the Mail can't be forwarded (target not reachable)<br/>
     * 
     * @param status
     *            - the Status to set
     */
    public void setStatus(int status)
    {
        this.status = status;
    }

    /**
     * @return the Target-Address of this Transaction
     */
    public String getTargetaddr()
    {
        return targetaddr;
    }

    /**
     * @param targetaddr
     *            - the Target-Address to set
     */
    public void setTargetaddr(String targetaddr)
    {
        this.targetaddr = targetaddr;
    }

    /**
     * @return the Source-Address of this transaction
     */
    public String getSourceaddr()
    {
        return sourceaddr;
    }

    /**
     * @param sourceaddr
     *            - the Source-Address to set
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
     * @return all Transactions which were stored in the Database
     */
    public static List<MailTransaction> all()
    {
        return Ebean.find(MailTransaction.class).findList();
    }

    /**
     * Gets all Mail-Transactions in the last "Period"
     * 
     * @param period
     *            - Joda-Time Period
     * @return a List of Mail-Transactions
     */
    public static List<MailTransaction> allInPeriod(Period period)
    {
        return Ebean.find(MailTransaction.class).where().gt("ts", DateTime.now().minus(period).getMillis()).findList();
    }

    /**
     * Generates a List of Status-Numbers and the Number of their occurrences
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
