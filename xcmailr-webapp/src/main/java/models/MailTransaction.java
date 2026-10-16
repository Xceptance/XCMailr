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

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.joda.time.DateTime;
import org.joda.time.Period;

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
}
