package models;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

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

}
