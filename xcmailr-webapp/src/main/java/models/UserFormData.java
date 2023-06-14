/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package models;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Holds all Data for the Edit Form (and is also used in the Registration-Process)
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */

public class UserFormData
{
    @NotEmpty
    @Length(min = 1, max = 255)
    public String firstName;

    @NotEmpty
    @Length(min = 1, max = 255)
    private String surName;

    @Email
    @NotEmpty
    @Length(min = 1, max = 255)
    private String mail;

    @NotEmpty
    @Length(min = 1, max = 255)
    private String password;

    @Length(min = 0, max = 255)
    private String passwordNew1;

    @Length(min = 0, max = 255)
    private String passwordNew2;

    @Length(min = 0, max = 255)
    private String language;

    /**
     * @return the first name
     */
    public String getFirstName()
    {
        return firstName;
    }

    /**
     * @param firstName
     *            the Forename to set
     */
    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
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
     *            the Surname to set
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
     *            the Mail-Address to set
     */
    public void setMail(String mail)
    {
        this.mail = mail;
    }

    /**
     * @return the Password
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * @param password
     *            the Password to set
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * @return the new Password
     */
    public String getPasswordNew1()
    {
        return passwordNew1;
    }

    /**
     * @param passwordNew1
     *            the new Password to set
     */
    public void setPasswordNew1(String passwordNew1)
    {
        this.passwordNew1 = passwordNew1;
    }

    /**
     * @return the Repetition of the new Password
     */
    public String getPasswordNew2()
    {
        return passwordNew2;
    }

    /**
     * @param passwordNew2
     *            the Repetition of the new Password to set
     */
    public void setPasswordNew2(String passwordNew2)
    {
        this.passwordNew2 = passwordNew2;
    }

    /**
     * @return the language of the user
     */
    public String getLanguage()
    {
        return language;
    }

    /**
     * @param language
     *            the language of the user
     */
    public void setLanguage(String language)
    {
        this.language = language;
    }

    /**
     * This Method will create a {@link User}-Object by the given Data. Be careful: The pwn1-field will be used as
     * Password for the {@link User}-Object.
     * 
     * @return a {@link User}-Object instantiated with the given Data
     * @see User
     */
    public User getAsUser()
    {
        return new User(firstName, surName, mail, passwordNew1, language);
    }

    /**
     * This Method takes an {@link User}-Object and converts it into a returned EditUsr-Object.
     * 
     * @param user
     *            the {@link User}-Object to convert
     * @return an EditUsr-Object filled with the User-Data
     */
    public static UserFormData prepopulate(User user)
    {
        UserFormData userFormData = new UserFormData();
        userFormData.setFirstName(user.getForename());
        userFormData.setMail(user.getMail());
        userFormData.setSurName(user.getSurname());
        userFormData.setLanguage(user.getLanguage());
        return userFormData;
    }

    public void clearPasswordFields()
    {
        this.setPassword("");
        this.setPasswordNew1("");
        this.setPasswordNew2("");
    }

}
