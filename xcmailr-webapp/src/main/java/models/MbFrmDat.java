/**  
 *  Copyright 2013 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package models;

import javax.validation.constraints.Pattern;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Holds the Data for the Mailbox Forms
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class MbFrmDat
{

    private Long boxId;

    @NotEmpty
    private String address;


    private String duration;

    @NotEmpty
    @Pattern(regexp = "[A-Za-z-]+(\\.[\\w-]+)+")
    private String domain;
    
    private String datetime;

    /**
     * @return the ID of an {@link MBox}
     */
    public Long getBoxId()
    {
        return boxId;
    }

    /**
     * @param boxId
     *             the {@link MBox}-ID to set
     */
    public void setBoxId(Long boxId)
    {
        this.boxId = boxId;
    }

    /**
     * @return the Local-Part of a (virtual) Mail-Address
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * @param address
     *             the Local-Part of a Mail-Address to set
     */
    public void setAddress(String address)
    {
        this.address = address;
    }

    /**
     * @return the Duration until the Validity-Period of this Box ends
     * @see etc.HelperUtils#parseDuration(String) Informations about the Format of the String
     */
    public String getDuration()
    {
        return duration;
    }

    /**
     * @param duration
     *             the Duration until the Validity-Period ends
     * @see etc.HelperUtils#parseDuration(String) Informations about the Format of the String
     */
    public void setDuration(String duration)
    {
        this.duration = duration;
    }

    /**
     * @return the Domain-Part of the Mail-Address
     */
    public String getDomain()
    {
        return domain;
    }

    /**
     * @param domain
     *             the Domain-Part to set
     */
    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    public String getDatetime()
    {
        return datetime;
    }

    public void setDatetime(String datetime)
    {
        this.datetime = datetime;
    }

}
