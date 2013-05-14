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
 * The Model for the Password-Reset-Form
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 * 
 */
public class PwData
{
    @NotEmpty
    private String pw;

    @NotEmpty
    private String pw2;

    /**
     * @return the new password
     */
    public String getPw()
    {
        return pw;
    }

    /**
     * @param pw
     *            - the new password to set
     */
    public void setPw(String pw)
    {
        this.pw = pw;
    }

    /**
     * @return the repetition of the new Password
     */
    public String getPw2()
    {
        return pw2;
    }

    /**
     * @param pw2
     *            - the repetition of the new Password
     */
    public void setPw2(String pw2)
    {
        this.pw2 = pw2;
    }

}
