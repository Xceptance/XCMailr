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

import javax.persistence.Entity;
import javax.persistence.OneToOne;

import io.ebean.annotation.Sql;

/**
 * An aggregate Model for the Mailtransactions
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Entity
@Sql
public class Status
{
    @OneToOne
    public MailTransaction mtx;

    public int statuscode;

    public int count;

    /**
     * @return the Status-code of this status-object
     */
    public int getStatuscode()
    {
        return statuscode;
    }

    /**
     * @param statuscode
     *            the Status-code of this status-object to set
     */
    public void setStatuscode(int statuscode)
    {
        this.statuscode = statuscode;
    }

    /**
     * @return the number of occurences of this status
     */
    public int getCount()
    {
        return count;
    }

    /**
     * @param count
     *            the number of occurences of this status
     */
    public void setCount(int count)
    {
        this.count = count;
    }

    /**
     * @return the Mailtransaction to which this belongs
     */
    public MailTransaction getMtx()
    {
        return mtx;
    }

    /**
     * @param mtx
     *            the Mailtransaction to which this belongs
     */
    public void setMtx(MailTransaction mtx)
    {
        this.mtx = mtx;
    }

}
