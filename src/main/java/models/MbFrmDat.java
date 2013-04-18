package models;

import javax.validation.constraints.Pattern;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Holds the Data for the Mailbox Forms
 * 
 * @author Patrick Thum 2012 released under Apache 2.0 License
 */
public class MbFrmDat
{

    private Long boxId;

    @NotEmpty
    private String address;

    @NotEmpty
    private String duration;

    @NotEmpty
    @Pattern(regexp="[A-Za-z-]+(\\.[\\w-]+)+")
    private String domain;

    public Long getBoxId()
    {
        return boxId;
    }

    public void setBoxId(Long boxId)
    {
        this.boxId = boxId;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getDuration()
    {
        return duration;
    }

    public void setDuration(String duration)
    {
        this.duration = duration;
    }

    public String getDomain()
    {
        return domain;
    }

    public void setDomain(String domain)
    {
        this.domain = domain;
    }

}
