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

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * The Model for the Password-Reset-Form
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class PasswordFormData
{
    /**
     * the new password
     */
    @NotEmpty
    @Length(min = 1, max = 255)
    private String password;

    /**
     * the repetition of the new password
     */
    @NotEmpty
    @Length(min = 1, max = 255)
    private String password2;

    /**
     * @return the new password
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * @param password
     *            the new password to set
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * @return the repetition of the new Password
     */
    public String getPassword2()
    {
        return password2;
    }

    /**
     * @param password2
     *            the repetition of the new Password
     */
    public void setPassword2(String password2)
    {
        this.password2 = password2;
    }

}
