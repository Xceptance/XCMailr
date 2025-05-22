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

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.mindrot.jbcrypt.BCrypt;

import io.ebean.DB;
import io.ebean.ExpressionList;
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
    void setPasswd(String passwd)
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

    // ---------------------------- EBean-Functions----------------------
    /**
     * @return the List of all Users in the Database
     */
    public static List<User> all()
    {
        return DB.find(User.class).findList();

    }

    /**
     * Returns whether the Database contains an user with the given mail address (case-insensitive lookup).
     * 
     * @param mail
     *            the given mail address
     * @return true if there is an user with the given address
     */
    public static boolean mailExists(String mail)
    {
        return queryByMail(mail).exists();
    }

    /**
     * @return true, if the user is the last admin-account in this app
     */
    public boolean isLastAdmin()
    {
        // this user is admin and there's only one admin in the database, so he's the last one
        return isAdmin() && (DB.find(User.class).where().eq("admin", true).findCount() == 1);
    }

    /**
     * returns the User-Object that belongs to the given Mail-Address
     * 
     * @param mail
     *            Address of a User
     * @return the User
     */
    public static User getUsrByMail(String mail)
    {
        return queryByMail(mail).findOne();
    }

    /**
     * Creates and returns a query for an user with the given mail address.
     * 
     * @param mail
     *            the user's mail address
     * @return query for an user with the given mail address
     */
    private static ExpressionList<User> queryByMail(String mail)
    {
        return DB.find(User.class).where().ieq("mail", mail);
    }

    /**
     * returns the User-Object if the given Mail and Password combination matches
     * 
     * @param mail
     *            the User's Mail-Address
     * @param pw
     *            the Password the User entered
     * @return the User Object if the given Mail and Password belong together, null - else
     */

    public static User auth(String mail, String pw)
    {
        // get the user by the mailadress
        final User usr = getUsrByMail(mail);
        return (usr != null && BCrypt.checkpw(pw, usr.getPasswd())) ? usr : null;
    }

    /**
     * This Method returns the User-Object if the given User-ID and Password belong together, otherwise it will return
     * null
     * 
     * @param id
     *            the ID of a User
     * @param pw
     *            the Password of a User
     * @return the User-Object if the given User-ID and Password belong together
     */
    public static User authById(Long id, String pw)
    {
        final User usr = getById(id);
        return (usr != null && BCrypt.checkpw(pw, usr.getPasswd())) ? usr : null;
    }

    /**
     * Returns the User-Object which belongs to the given User-ID
     * 
     * @param id
     *            an Users-ID
     * @return the User-Object
     */
    public static User getById(Long id)
    {
        return DB.find(User.class, id);
    }

    /**
     * Deletes the User with the given ID
     * 
     * @param id
     *            the ID of the User that has to be deleted
     */
    public static void delete(Long id)
    {
        DB.delete(User.class, id);
    }

    /**
     * Promotes or Demotes the User and Updates the DB. The Method checks the actual state and sets the Opposite
     * 
     * @param id
     *            ID of the User
     */
    public static void promote(Long id)
    {
        final User usr = getById(id);
        usr.setAdmin(!usr.admin);
        DB.update(usr);
    }

    /**
     * Activates or Deactivates the User and Updates the DB. The Method checks the actual state and sets the opposite
     * 
     * @param id
     *            ID of the User
     */
    public static boolean activate(Long id)
    {
        final User usr = getById(id);
        usr.setActive(!usr.isActive());
        DB.update(usr);
        return usr.isActive();

    }

    /**
     * Returns a List of all users which have email-addresses that belong to the given domain
     * 
     * @param domainName
     *            the domain-name
     * @return a List of Users
     */
    public static List<User> getUsersOfDomain(String domainName)
    {
        return DB.find(User.class).where().ilike("mail", "%@" + domainName).findList();
    }

    /**
     * Deletes all users with an email-addresses of the given domain
     * 
     * @param domainName
     *            the domain-name
     */
    public static void deleteUsersOfDomain(String domainName)
    {
        DB.delete(getUsersOfDomain(domainName));
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

    /**
     * Finds all users with an email that is like the Input-String
     * 
     * @param input
     *            the full email or just a part of it so search for
     * @return a list of users
     */
    public static List<User> findUserLike(String input)
    {
        if (input.equals(""))
        {
            return all();
        }
        return DB.find(User.class).where().ilike("mail", "%" + input + "%").findList();
    }

    /**
     * Searches the user the given API token. If it doesn't exist or if token's value isn't unique then null will be
     * returned.
     * 
     * @param apiToken
     *            the apiToken
     * @return active user owning the given token or {@code null} otherwise
     */
    public static User findUserByToken(String apiToken)
    {
        try
        {
            return DB.find(User.class).where().eq("APITOKEN", apiToken).eq("active", true).findOne();
        }
        catch (PersistenceException e)
        {
            // in case there is more than one user with the exact same token
            // this should never ever happen except someone is extreme lucky
            return null;
        }
    }
}
