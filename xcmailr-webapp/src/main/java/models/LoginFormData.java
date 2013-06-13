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

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Holds the Data for the Login Form
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class LoginFormData
{
    @NotEmpty
    String mail;

    String password;

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

}
