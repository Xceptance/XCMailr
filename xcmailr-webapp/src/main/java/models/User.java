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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import org.mindrot.jbcrypt.BCrypt;

import com.fasterxml.jackson.annotation.JsonManagedReference;

/**
 * User Object
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Entity
@Table(name = "users")
public class User extends AbstractEntity implements Serializable
{
    /**
     * UID to serialize this object
     */
    private static final long serialVersionUID = 1830731471817237181L;

    /**
     * first name of the User
     */
    @NotEmpty
    private String forename;

    /**
     * Surname of the User
     */
    @NotEmpty
    private String surname;

    /**
     * Email-address
     */
    @Email
    private String mail;

    /**
     * Password
     */
    @NotEmpty
    private String passwd;

    /**
     * indicates a user account
     */
    private boolean admin;

    /**
     * indicates whether the account is active or not
     */
    private boolean active;

    /**
     * random string which is used for account activation and password-resend
     */
    private String confirmation;

    /**
     * timestamp for the validity-period of the confirmation-token
     */
    private Long ts_confirm;

    /**
     * number of wrong logins (will be set to 0 on a successful login)
     */
    private int badPwCount;

    /**
     * the language of a user
     */
    private String language;

    /**
     * relation to the mailboxes
     */
    @OneToMany(mappedBy = "usr", cascade = CascadeType.ALL)
    @JsonManagedReference
    public List<MBox> boxes;

    @Column(name = "apitoken")
    private String apiToken;

    /**
     * the timestamp when the token was created
     */
    private long apiTokenCreationTimestamp;

    // ----------------------------- Getter and Setter ------------------------
    /**
     * Default-Constructor, just initializes the Variables
     */
    public User()
    {
        forename = "";
        surname = "";
        mail = "";
        passwd = "";
        language = "";
        boxes = new ArrayList<MBox>();
    }

    /**
     * Constructor for the User-Object
     * 
     * @param fName
     *            Forename of the User
     * @param sName
     *            Surname of the User
     * @param eMail
     *            Mail of the User
     * @param pw
     *            Plaintext-Password of the User
     */
    public User(String fName, String sName, String eMail, String pw, String language)
    {
        setForename(fName);
        setSurname(sName);
        setMail(eMail);
        hashPasswd(pw);
        setLanguage(language);
        boxes = new ArrayList<MBox>();
    }

    /**
     * @return all {@link MBox}-Objects in a List that belong to this User
     */
    public List<MBox> getBoxes()
    {
        return boxes;
    }

    /**
     * @param boxes
     *            all {@link MBox} -Objects in a List that belong to this User
     */
    public void setBoxes(List<MBox> boxes)
    {
        this.boxes = boxes;
    }

    /**
     * @return the Forename of a User
     */
    public String getForename()
    {
        return forename;
    }

    /**
     * Sets the Forename of a User
     * 
     * @param forename
     *            the Forename to set
     */
    public void setForename(String forename)
    {
        this.forename = forename;
    }

    /**
     * @return the Surname of a User
     */
    public String getSurname()
    {
        return surname;
    }

    /**
     * Sets the Surname of a User
     * 
     * @param surname
     */
    public void setSurname(String surname)
    {
        this.surname = surname;
    }

    /**
     * @return the "real" Mail-Address of a User
     */
    public String getMail()
    {
        return mail;
    }

    /**
     * Sets the "real" Mail-Address of a User
     * 
     * @param mail
     *            the Mail-Address to set
     */
    public void setMail(String mail)
    {
        this.mail = mail;
    }

    /**
     * @return the hashed Value of the BCrypted Password
     */
    public String getPasswd()
    {

        return passwd;
    }

    /**
     * Hashes the Password and sets it as the current Password
     * 
     * @param passwd
     *            the Plaintext-Password to set
     */
    public void hashPasswd(String passwd)
    {
        setPasswd(BCrypt.hashpw(passwd, BCrypt.gensalt()));
    }

    /**
     * @param pwd
     *            the Plaintext-Password to check
     * @return true if the Passwords are matching
     */
    public boolean checkPasswd(String pwd)
    {
        return BCrypt.checkpw(pwd, this.passwd);
    }

    /**
     * sets the given Password, WARNING: don't use this Method to set a new Password! always use
     * {@link #hashPasswd(String)} for that!
     * 
     * @param passwd
     *            the Password to set (after hashing with BCrypt)
     */
    public void setPasswd(String passwd)
    {
        this.passwd = passwd;
    }

    /**
     * @return true, if a User is Admin
     */
    public boolean isAdmin()
    {
        return admin;
    }

    /**
     * @return true, if the Account is active
     */
    public boolean isActive()
    {
        return active;
    }

    /**
     * Sets the Account as active or inactive
     * 
     * @param active
     *            boolean which indicates the new state of the User-account
     */
    public void setActive(boolean active)
    {
        this.active = active;
    }

    /**
     * Sets the account as active or inactive
     * 
     * @param admin
     *            the Boolean if the User should be Admin or not
     */
    public void setAdmin(boolean admin)
    {
        this.admin = admin;
    }

    /**
     * Gives the Confirmation-Token.
     * 
     * @return the randomly-generated confirm-token
     */
    public String getConfirmation()
    {
        return confirmation;
    }

    /**
     * Sets the Random-Confirmation-Token.
     * 
     * @param confirmation
     */
    public void setConfirmation(String confirmation)
    {
        this.confirmation = confirmation;
    }

    /**
     * @return the Validity-Period of the String
     */
    public Long getTs_confirm()
    {
        return ts_confirm;
    }

    /**
     * @param ts_confirm
     *            the Timestamp for the End of the Confirmation-Period
     */
    public void setTs_confirm(Long ts_confirm)
    {
        this.ts_confirm = ts_confirm;
    }

    /**
     * @return the Number of wrong entered Passwords
     */
    public int getBadPwCount()
    {
        return badPwCount;
    }

    /**
     * @param badPwCount
     *            the Number of wrong entered Passwords
     */
    public void setBadPwCount(int badPwCount)
    {
        this.badPwCount = badPwCount;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public String getApiToken()
    {
        return apiToken;
    }

    public void setApiToken(String apiToken)
    {
        this.apiToken = apiToken;
    }

    public long getApiTokenCreationTimestamp()
    {
        return apiTokenCreationTimestamp;
    }

    public void setApiTokenCreationTimestamp(long timestamp)
    {
        this.apiTokenCreationTimestamp = timestamp;
    }

    /**
     * Converts the User-Object to a String which contains in one line: UserID, First name, Last name, Mail and
     * Password. The fields are separated by a Whitespace
     */
    @Override
    public String toString()
    {
        return getId() + " " + forename + " " + " " + surname + " " + mail + " ";
    }
}
