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

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import com.xceptance.xcmailr.models.MBox;

/**
 * Data transfer object representing the configuration and state of a mailbox
 * exchanged via the REST API endpoints.
 */
public class MailboxData
{
    /**
     * The full email address of the mailbox (e.g. {@code user@domain.org}).
     */
    @NotEmpty
    @Email
    public String address;

    /**
     * The expiration timestamp in epoch milliseconds, or 0 if permanent.
     */
    public long deactivationTime;

    /**
     * Whether incoming emails should be forwarded to the owner's primary address.
     */
    public boolean forwardEnabled;

    /**
     * Optional comment or note documenting the purpose of this mailbox (maximum 255 characters).
     */
    @Size(max = 255, message = "{validation.comment.size}")
    public String comment;

    /**
     * Default no-argument constructor required for JSON deserialization by Jackson.
     */
    public MailboxData()
    {
    }

    /**
     * Constructs a {@code MailboxData} DTO populated from a domain {@link MBox} entity.
     *
     * @param mailbox the persisted mailbox domain entity, must not be {@code null}
     */
    public MailboxData(final MBox mailbox)
    {
        address = mailbox.getFullAddress();
        deactivationTime = mailbox.getTs_Active();
        forwardEnabled = mailbox.isForwardEmails();
        comment = mailbox.getComment();
    }
}
