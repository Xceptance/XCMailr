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
    @Pattern(regexp = "[A-Za-z-]+(\\.[\\w-]+)+")
    private String domain;

    /**
     * @return the id of an MBox
     */
    public Long getBoxId()
    {
        return boxId;
    }

    /**
     * @param boxId - the MBox-Id to set
     */
    public void setBoxId(Long boxId)
    {
        this.boxId = boxId;
    }

    /**
     * @return the local-part of a Mailaddress
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * @param address - the local-part of a mailaddress to set
     */
    public void setAddress(String address)
    {
        this.address = address;
    }

    /**
     * @return the duration until the validity-period of this box ends
     * @see HelperUtils for more info
     */
    public String getDuration()
    {
        return duration;
    }

    /**
     * @param duration - the duration until the validity-period ends
     * @see HelperUtils for more info
     */
    public void setDuration(String duration)
    {
        this.duration = duration;
    }

    /**
     * @return the domain-part of the mailaddress
     */
    public String getDomain()
    {
        return domain;
    }

    /**
     * @param domain - the domain-part to set
     */
    public void setDomain(String domain)
    {
        this.domain = domain;
    }

}
