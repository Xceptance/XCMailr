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

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Holds all Data for the Edit Form (and is also used in the Registration-Process)
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 * 
 */

public class EditUsr
{
    @NotEmpty
    public String forename;

    @NotEmpty
    private String surName;

    @Email
    @NotEmpty
    private String mail;

    @NotEmpty
    private String pw;

    private String pwn1;

    private String pwn2;

    /**
     * @return the Forename
     */
    public String getForename()
    {
        return forename;
    }

    /**
     * @param foreName
     *            - the Forename to set
     */
    public void setForeName(String foreName)
    {
        this.forename = foreName;
    }

    /**
     * @return the Surname
     */
    public String getSurName()
    {
        return surName;
    }

    /**
     * @param surName
     *            - the Surname to set
     */
    public void setSurName(String surName)
    {
        this.surName = surName;
    }

    /**
     * @return the Mail-Address
     */
    public String getMail()
    {
        return mail;
    }

    /**
     * @param mail
     *            - the Mail-Address to set
     */
    public void setMail(String mail)
    {
        this.mail = mail;
    }

    /**
     * @return the Password
     */
    public String getPw()
    {
        return pw;
    }

    /**
     * @param pw
     *            - the Password to set
     */
    public void setPw(String pw)
    {
        this.pw = pw;
    }

    /**
     * @return the new Password
     */
    public String getPwn1()
    {
        return pwn1;
    }

    /**
     * @param pwn1
     *            - the new Password to set
     */
    public void setPwn1(String pwn1)
    {
        this.pwn1 = pwn1;
    }

    /**
     * @return the Repetition of the new Password
     */
    public String getPwn2()
    {
        return pwn2;
    }

    /**
     * @param pwn2
     *            the Repetition of the new Password to set
     */
    public void setPwn2(String pwn2)
    {
        this.pwn2 = pwn2;
    }

    /**
     * This Method will create a User-Object by the given Data<br/>
     * Be careful: The pwn1-field will be used as Password for the -object <br/>
     * 
     * @return a User-Object instantiated with the given data
     * @see User
     */
    public User getAsUser()
    {
        return new User(forename, surName, mail, pwn1);
    }

    /**
     * This Method takes a User-Object and converts it into a returned EditUsr-Object<br/>
     * 
     * @param user - the User-object to convert
     * @return - an EditUsr-Object filled with the User-Data
     */
    public static EditUsr prepopulate(User user)
    {
        EditUsr eDat = new EditUsr();
        eDat.setForeName(user.getForename());
        eDat.setMail(user.getMail());
        eDat.setSurName(user.getSurname());
        return eDat;
    }

}
