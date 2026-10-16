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
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entity representing an email received and stored for a temporary mailbox.
 * Maps to the {@code mail} table.
 *
 * @author Xceptance Software Technologies GmbH
 */
@Entity
@Table(name = "mail")
public class Mail extends AbstractEntity implements Serializable
{
    private static final long serialVersionUID = 7644423623786133196L;

    /**
     * Sender email address.
     */
    @NotEmpty
    @Size(max = 255)
    @Column(name = "sender", nullable = false, length = 255)
    private String sender;

    /**
     * Subject line of the email.
     */
    @NotNull
    @Size(max = 255)
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    /**
     * Epoch millisecond timestamp when the email was received by the SMTP listener.
     */
    @NotNull
    @Column(name = "receive_time", nullable = false)
    private long receiveTime;

    /**
     * Raw MIME email payload stored as a BLOB.
     */
    @Lob
    @NotNull
    @Column(name = "message", nullable = false)
    private byte[] message;

    /**
     * Foreign mailbox identifier.
     */
    @NotNull
    @Column(name = "mailbox_id", nullable = false)
    private long mailbox;

    /**
     * Unique identifier for email retrieval and referencing.
     */
    @Column(name = "uuid", length = 255)
    private String uuid;

    /**
     * Default constructor for JPA.
     */
    public Mail()
    {
    }

    /**
     * @return sender email address
     */
    public String getSender()
    {
        return sender;
    }

    /**
     * @param sender sender email address to set
     */
    public void setSender(final String sender)
    {
        this.sender = sender;
    }

    /**
     * @return email subject
     */
    public String getSubject()
    {
        return subject;
    }

    /**
     * @param subject email subject to set
     */
    public void setSubject(final String subject)
    {
        this.subject = subject;
    }

    /**
     * @return receive time in milliseconds since epoch
     */
    public long getReceiveTime()
    {
        return receiveTime;
    }

    /**
     * @param receiveTime receive time in milliseconds since epoch to set
     */
    public void setReceiveTime(final long receiveTime)
    {
        this.receiveTime = receiveTime;
    }

    /**
     * @return raw MIME message bytes
     */
    public byte[] getMessage()
    {
        return message;
    }

    /**
     * @param message raw MIME message bytes to set
     */
    public void setMessage(final byte[] message)
    {
        this.message = message;
    }



    /**
     * @return foreign mailbox identifier
     */
    public long getMailboxId()
    {
        return mailbox;
    }

    /**
     * Sets the foreign mailbox reference.
     *
     * @param mailbox mailbox entity whose ID will be assigned
     */
    public void setMailbox(final MBox mailbox)
    {
        if (mailbox != null)
        {
            this.mailbox = mailbox.getId();
        }
    }

    /**
     * Sets the foreign mailbox identifier directly.
     *
     * @param mailboxId the mailbox ID
     */
    public void setMailboxId(final long mailboxId)
    {
        this.mailbox = mailboxId;
    }

    /**
     * @return UUID string
     */
    public String getUuid()
    {
        return uuid;
    }

    /**
     * @param uuid UUID string to set
     */
    public void setUuid(final String uuid)
    {
        this.uuid = uuid;
    }

    @jakarta.persistence.PrePersist
    public void prePersist()
    {
        if (this.uuid == null || this.uuid.isBlank())
        {
            this.uuid = java.util.UUID.randomUUID().toString();
        }
    }
}
