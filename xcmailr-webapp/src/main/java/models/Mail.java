/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

import io.ebean.DB;

@Entity
@Table
public class Mail extends AbstractEntity implements Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = 7644423623786133196L;

    @NotEmpty
    @Length(max = 255)
    private String sender;

    @NotNull
    @Length(max = 255)
    private String subject;

    /**
     * timestamp when the email was received
     */
    @NotNull
    private long receiveTime;

    @Lob
    @NotNull
    private byte[] message;

    @NotNull
    @Column(name = "mailbox_id")
    private long mailbox;

    private String uuid;

    public String getSender()
    {
        return sender;
    }

    public void setSender(String sender)
    {
        this.sender = sender;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public long getReceiveTime()
    {
        return receiveTime;
    }

    public void setReceiveTime(long receiveTime)
    {
        this.receiveTime = receiveTime;
    }

    public byte[] getMessage()
    {
        return message;
    }

    public void setMessage(byte[] message)
    {
        this.message = message;
    }

    public MBox getMailbox()
    {
        return MBox.getById(mailbox);
    }

    public void setMailbox(MBox mailbox)
    {
        if (mailbox != null)
        {
            this.mailbox = mailbox.getId();
        }
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

    /**
     * Finds a mail by ID.
     * 
     * @param id
     *            the ID
     * @return the mail or <code>null</code> if not found
     */
    public static Mail find(long id)
    {
        return DB.find(Mail.class).where().idEq(id).findOne();
    }

    /**
     * Finds all mails in the mailbox with the given ID and returns them sorted by receive time.
     * 
     * @param mailboxId
     *            the ID of the mailbox
     * @return the list of mails found
     */
    public static List<Mail> findAndSort(long mailboxId)
    {
        return DB.find(Mail.class).where().eq("mailbox_id", mailboxId).order("receiveTime").findList();
    }
}
