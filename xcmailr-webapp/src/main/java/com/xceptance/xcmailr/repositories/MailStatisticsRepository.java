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
package com.xceptance.xcmailr.repositories;

import java.sql.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.xceptance.xcmailr.dto.DomainStatisticsDto;
import com.xceptance.xcmailr.models.MailStatistics;
import com.xceptance.xcmailr.models.MailStatisticsKey;

/**
 * Spring Data JPA repository for aggregated 15-minute mail statistics.
 *
 * @author Xceptance Software Technologies GmbH
 */
@Repository
public interface MailStatisticsRepository extends JpaRepository<MailStatistics, MailStatisticsKey>
{
    /**
     * Finds statistics entries recorded prior to a given SQL date.
     *
     * @param date cutoff date
     * @return list of matching statistics entries
     */
    List<MailStatistics> findByKeyDateBefore(final Date date);

    /**
     * Purges statistics records older than a specified date.
     *
     * @param date cutoff date
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM MailStatistics ms WHERE ms.key.date < :date")
    void deleteByKeyDateBefore(@Param("date") final Date date);

    /**
     * Aggregates email delivery statistics grouped by sender domain since a given calendar date.
     * Orders the results by total traffic volume (drops + forwards + stores) descending.
     *
     * @param startDate earliest inclusive SQL date for statistics aggregation
     * @return list of domain statistics records ordered by total traffic volume descending
     */
    @Query("SELECT new com.xceptance.xcmailr.dto.DomainStatisticsDto(" +
           "ms.key.fromDomain, " +
           "COALESCE(SUM(ms.dropCount), 0L), " +
           "COALESCE(SUM(ms.forwardCount), 0L), " +
           "COALESCE(SUM(ms.storeCount), 0L)) " +
           "FROM MailStatistics ms " +
           "WHERE ms.key.date >= :startDate " +
           "GROUP BY ms.key.fromDomain " +
           "ORDER BY (COALESCE(SUM(ms.dropCount), 0L) + COALESCE(SUM(ms.forwardCount), 0L) + COALESCE(SUM(ms.storeCount), 0L)) DESC")
    List<DomainStatisticsDto> findDomainStatisticsSince(@Param("startDate") final Date startDate);
}
