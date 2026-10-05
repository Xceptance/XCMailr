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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import models.Mail;

/**
 * Spring Data JPA repository for persisted email messages.
 *
 * @author Xceptance Software Technologies GmbH
 */
@Repository
public interface MailRepository extends JpaRepository<Mail, Long>
{
    /**
     * Retrieves all mails delivered to the specified mailbox ID, ordered by receive timestamp ascending.
     *
     * @param mailboxId ID of the mailbox
     * @return list of matching mails
     */
    List<Mail> findByMailboxOrderByReceiveTimeAsc(final long mailboxId);

    /**
     * Retrieves all mails delivered to any of the specified mailbox IDs, ordered by receive timestamp descending.
     *
     * @param mailboxIds collection of mailbox IDs
     * @return list of matching mails ordered newest first
     */
    List<Mail> findByMailboxInOrderByReceiveTimeDesc(final List<Long> mailboxIds);

    /**
     * Finds a mail by its unique UUID identifier.
     *
     * @param uuid message UUID
     * @return an {@link Optional} containing the mail if found
     */
    Optional<Mail> findByUuid(final String uuid);

    /**
     * Counts the total number of mails in a specific mailbox.
     *
     * @param mailboxId ID of the mailbox
     * @return mail count
     */
    long countByMailbox(final long mailboxId);

    /**
     * Deletes all mails belonging to a given mailbox ID.
     *
     * @param mailboxId target mailbox ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Mail m WHERE m.mailbox = :mailboxId")
    void deleteByMailbox(@Param("mailboxId") final long mailboxId);

    /**
     * Purges mails received before the given epoch millisecond timestamp.
     *
     * @param timestamp cutoff timestamp
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Mail m WHERE m.receiveTime < :timestamp")
    void deleteByReceiveTimeLessThan(@Param("timestamp") final long timestamp);
}
