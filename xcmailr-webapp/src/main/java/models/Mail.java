package models;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
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
    private static final long serialVersionUID = -381571832126165482L;

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
    private long recieveTime;

    @Lob
    private String message;

    @ManyToOne
    @JoinColumn(name = "mailbox_id", nullable = false)
    private MBox mailbox;

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
        return recieveTime;
    }

    public void setReceiveTime(long recieveTime)
    {
        this.recieveTime = recieveTime;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public MBox getMailbox()
    {
        return mailbox;
    }

    public void setMailbox(MBox mailbox)
    {
        this.mailbox = mailbox;
    }
}
