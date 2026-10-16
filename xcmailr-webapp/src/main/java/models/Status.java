/*
 * Copyright (c) 2013-2026 Xceptance Software Technologies GmbH
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

/**
 * Aggregate projection data transfer object representing mail transaction status distribution.
 *
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class Status implements Serializable
{
    private static final long serialVersionUID = 1L;

    public MailTransaction mtx;

    public int statuscode;

    public long count;

    /**
     * Default constructor for serialization and reflection.
     */
    public Status()
    {
    }

    /**
     * Parameterized constructor for JPQL projection queries.
     *
     * @param statuscode the HTTP / SMTP transaction status code
     * @param count the aggregate count of transactions for this status code
     */
    public Status(final int statuscode, final long count)
    {
        this.statuscode = statuscode;
        this.count = count;
    }

    /**
     * @return the status code of this status object
     */
    public int getStatuscode()
    {
        return statuscode;
    }

    /**
     * @param statuscode the status code to set
     */
    public void setStatuscode(final int statuscode)
    {
        this.statuscode = statuscode;
    }

    /**
     * @return the number of occurrences of this status
     */
    public long getCount()
    {
        return count;
    }

    /**
     * @param count the number of occurrences to set
     */
    public void setCount(final long count)
    {
        this.count = count;
    }

    /**
     * @return the associated MailTransaction or null
     */
    public MailTransaction getMtx()
    {
        return mtx;
    }

    /**
     * @param mtx the associated MailTransaction to set
     */
    public void setMtx(final MailTransaction mtx)
    {
        this.mtx = mtx;
    }
}
