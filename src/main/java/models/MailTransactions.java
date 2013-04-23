package models;

import javax.persistence.Entity;
import javax.persistence.Id;


/**
 * This Class is used to save all Actions on the Mailserver
 * @author Patrick Thum
 *
 */
@Entity
public class MailTransactions
{
    @Id
    private Long id;
    
    private Long ts;
    
    private int status;
    
    private String targetaddr;

    
    
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getTs()
    {
        return ts;
    }

    public void setTs(Long ts)
    {
        this.ts = ts;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public String getTargetaddr()
    {
        return targetaddr;
    }

    public void setTargetaddr(String targetaddr)
    {
        this.targetaddr = targetaddr;
    }
    
    
    

}
