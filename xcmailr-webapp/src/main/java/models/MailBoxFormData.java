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

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Holds the Data for the Mailbox Forms
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class MailBoxFormData
{

    private Long boxId;

    @NotEmpty
    @Length(min = 1, max = 64)
    @Pattern(regexp = "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*")
    private String address;

    @NotEmpty
    @Pattern(regexp = "[A-Za-z-]+(\\.[\\w-]+)+")
    @Length(min = 1, max = 255)
    private String domain;

    @NotEmpty
    @Length(min = 1, max = 255)
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
     *            the {@link MBox}-ID to set
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
     *            the Local-Part of a Mail-Address to set
     */
    public void setAddress(String address)
    {
        this.address = address;
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
     *            the Domain-Part to set
     */
    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    /**
     * The Date and Time which was set by the DateTimePicker The format used here is: "dd.MM.yyyy hh:mm"
     * 
     * @return
     */
    public String getDatetime()
    {
        return datetime;
    }

    /**
     * The Date and Time which was set by the DateTimePicker The format used here is: "dd.MM.yyyy hh:mm"
     * 
     * @param datetime
     *            the datetime-string to set
     */
    public void setDatetime(String datetime)
    {
        this.datetime = datetime;
    }

    public static MailBoxFormData prepopulate(MBox mb)
    {
        MailBoxFormData mbDat = new MailBoxFormData();
        mbDat.setBoxId(mb.getId());
        mbDat.setAddress(mb.getAddress());
        mbDat.setDomain(mb.getDomain());
        mbDat.setDatetime(mb.getTSAsStringWithNull());
        return mbDat;

    }

}
