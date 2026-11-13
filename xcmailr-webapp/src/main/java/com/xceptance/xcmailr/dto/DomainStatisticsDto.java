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
package com.xceptance.xcmailr.dto;

import java.io.Serializable;
import java.sql.Date;

/**
 * Immutable data transfer record representing aggregated traffic metrics for a specific
 * sender domain, optionally bound to a specific calendar date, including drop count,
 * forward count, and local store count.
 *
 * @param date calendar date of activity (null for cumulative time-window aggregations)
 * @param domain sender domain name (e.g. "example.com")
 * @param dropCount total number of dropped emails from this domain
 * @param forwardCount total number of forwarded emails from this domain
 * @param storeCount total number of locally stored emails from this domain
 * @author Xceptance Software Technologies GmbH
 */
public record DomainStatisticsDto(Date date, String domain, long dropCount, long forwardCount, long storeCount)
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Compact constructor normalizing null domain strings to an empty string.
     *
     * @param date calendar date of activity
     * @param domain sender domain name
     * @param dropCount total dropped email count
     * @param forwardCount total forwarded email count
     * @param storeCount total stored email count
     */
    public DomainStatisticsDto
    {
        domain = (domain != null) ? domain : "";
    }

    /**
     * Overloaded constructor for cumulative domain statistics without a date context.
     *
     * @param domain sender domain name
     * @param dropCount total dropped email count
     * @param forwardCount total forwarded email count
     * @param storeCount total stored email count
     */
    public DomainStatisticsDto(final String domain, final long dropCount, final long forwardCount, final long storeCount)
    {
        this(null, domain, dropCount, forwardCount, storeCount);
    }

    /**
     * Returns the aggregate sum of all traffic (drops + forwards + stores) for this domain.
     *
     * @return total mail count
     */
    public long totalCount()
    {
        return dropCount + forwardCount + storeCount;
    }

    /**
     * JavaBeans property getter alias for {@link #date()}.
     *
     * @return calendar date or null
     */
    public Date getDate()
    {
        return date();
    }

    /**
     * JavaBeans property getter alias for {@link #domain()}.
     *
     * @return domain string
     */
    public String getDomain()
    {
        return domain();
    }

    /**
     * JavaBeans property getter alias for {@link #dropCount()}.
     *
     * @return drop count
     */
    public long getDropCount()
    {
        return dropCount();
    }

    /**
     * JavaBeans property getter alias for {@link #forwardCount()}.
     *
     * @return forward count
     */
    public long getForwardCount()
    {
        return forwardCount();
    }

    /**
     * JavaBeans property getter alias for {@link #storeCount()}.
     *
     * @return store count
     */
    public long getStoreCount()
    {
        return storeCount();
    }

    /**
     * JavaBeans property getter alias for {@link #totalCount()}.
     *
     * @return total mail count
     */
    public long getTotalCount()
    {
        return totalCount();
    }
}
