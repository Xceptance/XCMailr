package models;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

import com.avaje.ebean.annotation.Sql;

@Entity
@Sql
public class Status
{
    @OneToOne
    public MailTransaction mtx;
    
    public int statuscode;

    public int count;

    public int getStatuscode()
    {
        return statuscode;
    }

    public void setStatuscode(int statuscode)
    {
        this.statuscode = statuscode;
    }

    public int getCount()
    {
        return count;
    }

    public void setCount(int count)
    {
        this.count = count;
    }

    public MailTransaction getMtx()
    {
        return mtx;
    }

    public void setMtx(MailTransaction mtx)
    {
        this.mtx = mtx;
    }
    
}
