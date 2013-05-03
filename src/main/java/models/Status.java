package models;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

import com.avaje.ebean.annotation.Sql;


/**
 * An aggregate Model for the Mailtransactions
 * @author Patrick Thum
 *
 */
@Entity
@Sql
public class Status
{
    @OneToOne
    public MailTransaction mtx;
    
    public int statuscode;

    public int count;
/**
 * 
 * @return the Status-code of this status-object
 */
    public int getStatuscode()
    {
        return statuscode;
    }
    /**
     * 
     * @param statuscode the Status-code of this status-object to set
     */
    public void setStatuscode(int statuscode)
    {
        this.statuscode = statuscode;
    }
    /**
     * 
     * @return the number of occurences of this status
     */
    public int getCount()
    {
        return count;
    }
    /**
     * 
     * @param count - the number of occurences of this status
     */
    public void setCount(int count)
    {
        this.count = count;
    }
    
    //TODO  1to1-relation, orly?
    /**
     * 
     * @return the Mailtransaction to which this belongs 
     */
    public MailTransaction getMtx()
    {
        return mtx;
    }
    /**
     * 
     * @param mtx - the Mailtransaction to which this belongs 
     */
    public void setMtx(MailTransaction mtx)
    {
        this.mtx = mtx;
    }
    
}
