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
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import models.MBox;
import models.User;

/**
 * Spring Data JPA repository for mailboxes.
 *
 * @author Xceptance Software Technologies GmbH
 */
@Repository
public interface MailboxRepository extends JpaRepository<MBox, Long>
{
    /**
     * Finds a mailbox by its local address part and domain name, case-insensitively.
     *
     * @param address local address part
     * @param domain domain name
     * @return an {@link Optional} containing the mailbox if found
     */
    Optional<MBox> findByAddressIgnoreCaseAndDomainIgnoreCase(final String address, final String domain);

    /**
     * Checks if a mailbox exists with the given address and domain name, case-insensitively.
     *
     * @param address local address part
     * @param domain domain name
     * @return true if exists
     */
    boolean existsByAddressIgnoreCaseAndDomainIgnoreCase(final String address, final String domain);

    /**
     * Retrieves all mailboxes owned by the specified user.
     *
     * @param usr owning user
     * @return list of user mailboxes
     */
    List<MBox> findByUsr(final User usr);

    /**
     * Retrieves a paginated list of mailboxes belonging to a specific user.
     *
     * @param usr owning user
     * @param pageable pagination parameters
     * @return page of mailboxes
     */
    Page<MBox> findByUsr(final User usr, final Pageable pageable);

    /**
     * Searches a user's mailboxes matching an address or domain search substring.
     *
     * @param usr owning user
     * @param search search pattern
     * @param pageable pagination parameters
     * @return page of matching mailboxes
     */
    @Query("SELECT b FROM MBox b WHERE b.usr = :usr AND (LOWER(b.address) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(b.domain) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<MBox> findByUsrAndSearch(@Param("usr") final User usr, @Param("search") final String search, final Pageable pageable);

    /**
     * Retrieves mailboxes filtered by user and expiration status.
     *
     * @param usr owning user
     * @param expired expiration status
     * @param pageable pagination parameters
     * @return page of matching mailboxes
     */
    Page<MBox> findByUsrAndExpired(final User usr, final boolean expired, final Pageable pageable);

    /**
     * Searches a user's mailboxes matching an address or domain search substring with expiration filter.
     *
     * @param usr owning user
     * @param expired expiration status
     * @param search search pattern
     * @param pageable pagination parameters
     * @return page of matching mailboxes
     */
    @Query("SELECT b FROM MBox b WHERE b.usr = :usr AND b.expired = :expired AND (LOWER(b.address) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(b.domain) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<MBox> findByUsrAndExpiredAndSearch(@Param("usr") final User usr, @Param("expired") final boolean expired, @Param("search") final String search, final Pageable pageable);

    /**
     * Finds mailboxes that are currently marked active (expired = false) but whose expiration
     * timestamp is in the past and not zero (unlimited).
     *
     * @param currentTime current epoch millisecond timestamp
     * @return list of active mailboxes that have passed their expiration deadline
     */
    @Query("SELECT b FROM MBox b WHERE b.expired = false AND b.ts_Active > 0 AND b.ts_Active < :currentTime")
    List<MBox> findActiveExpiredBoxes(@Param("currentTime") final long currentTime);

    /**
     * Finds mailboxes that have been expired for longer than the retention limit.
     *
     * @param cutoffTimestamp epoch millisecond cutoff timestamp
     * @return list of expired mailboxes eligible for deletion
     */
    @Query("SELECT b FROM MBox b WHERE b.expired = true AND b.ts_Active > 0 AND b.ts_Active < :cutoffTimestamp")
    List<MBox> findExpiredBoxesBefore(@Param("cutoffTimestamp") final long cutoffTimestamp);

    /**
     * Increments the forward count on a mailbox.
     *
     * @param id mailbox ID
     */
    @Modifying
    @Transactional
    @Query("UPDATE MBox b SET b.forwards = b.forwards + 1 WHERE b.id = :id")
    void incrementForwards(@Param("id") final long id);

    /**
     * Increments the suppression count on a mailbox.
     *
     * @param id mailbox ID
     */
    @Modifying
    @Transactional
    @Query("UPDATE MBox b SET b.suppressions = b.suppressions + 1 WHERE b.id = :id")
    void incrementSuppressions(@Param("id") final long id);
}
