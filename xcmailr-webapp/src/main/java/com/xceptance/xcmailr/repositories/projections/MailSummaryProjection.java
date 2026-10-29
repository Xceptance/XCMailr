/*
 * Copyright (c) Xceptance Software Technologies GmbH
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
package com.xceptance.xcmailr.repositories.projections;

/**
 * Lightweight Spring Data JPA interface projection for email list views.
 * <p>
 * Selects only the essential metadata fields required for summary tables and
 * HTMX list fragments (ID, mailbox ID, sender, subject, and receive timestamp),
 * completely excluding the raw {@code byte[] message} BLOB column. This avoids
 * allocating large byte arrays in the JVM heap and prevents Hibernate from creating
 * deep-copy snapshots during persistence context dirty checking.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
public interface MailSummaryProjection
{
    /**
     * Returns the primary key identifier of the email.
     *
     * @return the unique email ID
     */
    long getId();

    /**
     * Returns the identifier of the destination mailbox.
     *
     * @return the mailbox ID
     */
    long getMailbox();

    /**
     * Returns the identifier of the destination mailbox (alias for template compatibility).
     *
     * @return the mailbox ID
     */
    default long getMailboxId()
    {
        return getMailbox();
    }

    /**
     * Returns the sender's email address.
     *
     * @return the sender address string
     */
    String getSender();

    /**
     * Returns the subject line of the email.
     *
     * @return the email subject
     */
    String getSubject();

    /**
     * Returns the epoch millisecond timestamp when the email was received.
     *
     * @return the receive time in milliseconds since unix epoch
     */
    long getReceiveTime();
}
