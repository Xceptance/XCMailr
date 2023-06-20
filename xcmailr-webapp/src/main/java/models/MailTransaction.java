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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.joda.time.DateTime;
import org.joda.time.Period;

import io.ebean.DB;
import io.ebean.Query;
import io.ebean.RawSql;
import io.ebean.RawSqlBuilder;
import io.ebean.SqlUpdate;

/**
 * This Class is used to save all Actions on the Mailserver
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Entity
@Table(name = "mailtransactions")
public class MailTransaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private Long ts;

    private int status;

    private String sourceaddr;

    private String relayaddr;

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
     * 0 - Mail has a wrong Pattern<br/>
     * 100 - Mail does not exist<br/>
     * 200 - Mail exists but is inactive <br/>
     * 300 - Mail has been forwarded successfully <br/>
     * 400 - the Mail can't be forwarded (target not reachable)<br/>
     * 500 - Relay denied (recipient's address does not belong to this server)<br/>
     * 600 - User is inactive</br>
     * 
     * @param stat
     *            Statuscode of the Transaction
     * @param source
     *            the Sender's - Address
     * @param relay
     *            Relay-Address of the Mail (the mail which is virtually created on this app)
     * @param target
     *            Original Recipients-Address of the Mail
     */
    public MailTransaction(int stat, String source, String relay, String target)
    {
        ts = DateTime.now().getMillis();
        this.status = stat;
        this.targetaddr = target;
        this.sourceaddr = source;
        this.relayaddr = relay;
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
     *            the ID of this Transaction to set
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
     * @return the Timestamp as String in the Format "dd.MM.yyyy hh:mm"
     */
    public String getTsAsString()
    {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(this.ts)).toString();
    }

    /**
     * @param ts
     *            sets the Timestamp in Milliseconds
     */
    public void setTs(Long ts)
    {
        this.ts = ts;
    }

    /**
     * <b>Statuscodes:</b> <br/>
     * 0 - Mail has a wrong Pattern <br/>
     * 100 - Mail does not exist<br/>
     * 200 - Mail exists but is inactive <br/>
     * 300 - Mail has been forwarded successfully <br/>
     * 400 - the Mail can't be forwarded (target not reachable)<br/>
     * 500 - Relay denied (recipient's address does not belong to this server)<br/>
     * 600 - User is inactive</br>
     * 
     * @return a Statuscode
     */
    public int getStatus()
    {
        return status;
    }

    /**
     * <b>Statuscodes:</b> <br/>
     * 0 - Mail has a wrong Pattern<br/>
     * 100 - Mail does not exist<br/>
     * 200 - Mail exists but is inactive <br/>
     * 300 - Mail has been forwarded successfully <br/>
     * 400 - the Mail can't be forwarded (target not reachable)<br/>
     * 500 - Relay denied (recipient's address does not belong to this server)<br/>
     * 600 - User is inactive</br>
     * 
     * @param status
     *            the Status to set
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
     *            the Target-Address to set
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
     *            the Source-Address to set
     */
    public void setSourceaddr(String sourceaddr)
    {
        this.sourceaddr = sourceaddr;
    }

    /**
     * @return the Relay-Address of this transaction (if existent)
     */
    public String getRelayaddr()
    {
        return relayaddr;
    }

    /**
     * @param relayaddr
     *            the Relay-Address of this transaction (if existent)
     */
    public void setRelayaddr(String relayaddr)
    {
        this.relayaddr = relayaddr;
    }

    // -------------------------------------------------------
    // E-Bean Functions
    // -------------------------------------------------------

    /**
     * @return all Transactions which were stored in the Database
     */
    public static List<MailTransaction> all()
    {
        return DB.find(MailTransaction.class).findList();
    }

    /**
     * @param sortage
     *            a String which indicates the sortage of the returned list, the string should be in the form "fieldname
     *            asc" or "fieldname desc"
     * @return a sorted list of all MailTransactions
     */
    public static List<MailTransaction> all(String sortage)
    {
        List<MailTransaction> list = DB.find(MailTransaction.class).where().orderBy(sortage).findList();

        return list;
    }

    /**
     * Gets all Mail-Transactions in the last "Period"
     * 
     * @param period
     *            Joda-Time Period
     * @return a List of Mail-Transactions
     */
    public static List<MailTransaction> getAllInPeriod(Period period)
    {
        return DB.find(MailTransaction.class).where().gt("ts", DateTime.now().minus(period).getMillis()).findList();
    }

    /**
     * returns a list of MailTransactions sorted descending and limited by the given number
     * 
     * @param limit
     *            the maximal row number
     * @return a sorted list of all MailTransactions
     */
    public static List<MailTransaction> getSortedAndLimitedList(int limit)
    {
        List<MailTransaction> list = DB.find(MailTransaction.class).where().orderBy("ts desc").setMaxRows(limit)
                                          .findList();
        return list;
    }

    /**
     * returns a list of MailTransactions with the given target address
     * 
     * @param targetAddr
     *            the target address
     * @return sorted list of MailTransactions with given target address
     */
    public static List<MailTransaction> getForTarget(final String targetAddr)
    {
        List<MailTransaction> list = DB.find(MailTransaction.class).where().eq("targetaddr", targetAddr)
                                          .orderBy("ts desc").findList();
        return list;
    }

    /**
     * returns a list of MailTransactions with the given relay address
     * 
     * @param relayAddr
     *            the relay address
     * @return sorted list of MailTransactions with given target address
     */
    public static List<MailTransaction> getForRelay(final String relayAddr)
    {
        List<MailTransaction> list = DB.find(MailTransaction.class).where().eq("relayaddr", relayAddr)
                                          .orderBy("ts desc").findList();
        return list;
    }

    /**
     * returns a list of MailTransactions with the given source address
     * 
     * @param sourceAddr
     *            the source address
     * @return sorted list of MailTransactions with given target address
     */
    public static List<MailTransaction> getForSource(final String sourceAddr)
    {
        List<MailTransaction> list = DB.find(MailTransaction.class).where().eq("sourceaddr", sourceAddr)
                                          .orderBy("ts desc").findList();
        return list;
    }

    /**
     * Deletes all Transactions that have been stored before the given Timestamp
     * 
     * @param ts
     *            the Timestamp in milliseconds
     */
    public static void deleteTxInPeriod(Long ts)
    {
        String sql = "DELETE FROM MAILTRANSACTIONS";
        if (ts != null)
        { // there's a timestamp, add
            sql += " WHERE ts < " + ts;
        }
        SqlUpdate down = DB.sqlUpdate(sql);
        down.execute();
    }

    /**
     * returns a specific MailTransaction that belongs to the ID
     * 
     * @param id
     *            the ID of an MailTransaction
     * @return a MailTransaction
     */
    public static MailTransaction getById(long id)
    {
        return DB.find(MailTransaction.class, id);
    }

    /**
     * Generates a List of Status-Numbers and the Number of their occurrences
     * 
     * @return a List of Status-Elements (as an aggregate of Transactions)
     * @see Status
     */
    public static List<Status> getStatusList()
    {
        // create a sql-query that contains the statuscode and their number of occurences
        String sql = "SELECT mtx.status, COUNT(mtx.status) AS count  FROM mailtransactions mtx GROUP BY mtx.status";
        RawSql rawSql = RawSqlBuilder.parse(sql).columnMapping("mtx.status", "statuscode").create();
        Query<Status> query = DB.find(Status.class);
        query.setRawSql(rawSql);
        List<Status> list = query.findList();

        return list;
    }

    /**
     * Saves the Transaction in the Database
     */
    public void save()
    {
        DB.save(this);
    }

    /**
     * saves multiple elements
     * 
     * @param mtxList
     */
    public static void saveMultipleTx(List<MailTransaction> mtxList)
    {
        DB.saveAll(mtxList);
    }
}
