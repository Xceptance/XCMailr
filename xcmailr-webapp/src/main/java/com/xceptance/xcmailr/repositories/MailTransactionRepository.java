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

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import models.MailTransaction;
import models.Status;

/**
 * Spring Data JPA repository for inbound and relay transaction audit logs.
 *
 * @author Xceptance Software Technologies GmbH
 */
@Repository
public interface MailTransactionRepository extends JpaRepository<MailTransaction, Long>
{
    /**
     * Finds transactions logged before the specified millisecond timestamp.
     *
     * @param ts timestamp cutoff
     * @return list of matching transactions
     */
    List<MailTransaction> findByTsBefore(final Long ts);

    /**
     * Purges transaction records logged before the given timestamp.
     *
     * @param ts timestamp cutoff
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM MailTransaction mt WHERE mt.ts < :ts")
    void deleteByTsBefore(@Param("ts") final Long ts);

    /**
     * Retrieves transactions ordered by timestamp descending for paged audit viewing.
     *
     * @param pageable pagination parameters
     * @return page of transactions
     */
    Page<MailTransaction> findAllByOrderByTsDesc(final Pageable pageable);

    /**
     * Aggregates total transaction counts grouped by status code.
     *
     * @return list of Status objects representing grouped counts
     */
    @Query("SELECT new models.Status(mt.status, COUNT(mt)) FROM MailTransaction mt GROUP BY mt.status")
    List<Status> getStatusCounts();
}
