package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import org.mindrot.jbcrypt.BCrypt;
import com.avaje.ebean.*;
import com.avaje.ebean.validation.Email;
import com.avaje.ebean.validation.NotEmpty;

/**
 * User Object
 * 
 * @author Patrick Thum 2012 released under Apache 2.0 License
 */
@Entity
@Table(name = "users")
public class User implements Serializable
{
    // UserId
    @Id
    private long id;

    @NotEmpty
    // Forename of the User
    private String forename;

    @NotEmpty
    // Surname of the User
    private String surname;

    // Mailadress

    @Email
    private String mail;

    @NotEmpty
    // Password
    private String passwd;

    private boolean admin;

    private boolean active;

    // Random String which is used for Account activation and pwresend
    private String confirmation;

    // timestamp for the validity-period of the confirmation-token
    private Long ts_confirm;

    private int badPwCount;

    // Relation to the Mailboxes
    @OneToMany(mappedBy = "usr", cascade = CascadeType.ALL)
    public List<MBox> boxes;

    // ----------------------------- Getter and Setter ------------------------
    /**
     * Default constructor, just initialize the variables
     */
    public User()
    {
        id = 0;
        forename = "";
        surname = "";
        mail = "";
        passwd = "";
        boxes = new ArrayList<MBox>();
    }

    /**
     * constructor for the userobject
     * 
     * @param fName
     *            forename
     * @param sName
     *            surname
     * @param eMail
     *            mail
     * @param pw
     *            password
     */
    public User(String fName, String sName, String eMail, String pw)
    {
        id = 0;
        setForename(fName);
        setSurname(sName);
        setMail(eMail);
        hashPasswd(pw);
        boxes = new ArrayList<MBox>();
    }

    /**
     * @return all boxes that belong to this user
     */
    public List<MBox> getBoxes()
    {
        return boxes;
    }

    /**
     * @param boxes
     *            - all boxes that belong to this user
     */
    public void setBoxes(List<MBox> boxes)
    {
        this.boxes = boxes;
    }

    /**
     * persists a user in the DB
     */
    public void save()
    {
        Ebean.save(this);
    }

    /**
     * @return the Id of a user
     */
    public long getId()
    {
        return id;
    }

    /**
     * Sets the id of a Userobject used to identify it in the db
     * 
     * @param id
     */
    public void setId(long id)
    {
        this.id = id;

    }

    /**
     * @return the forename of a user
     */
    public String getForename()
    {
        return forename;
    }

    /**
     * Sets the forename of a user
     * 
     * @param forename
     */
    public void setForename(String forename)
    {
        this.forename = forename;
    }

    /**
     * @return the surname of a user
     */
    public String getSurname()
    {
        return surname;
    }

    /**
     * Sets the surname of a user
     * 
     * @param surname
     */
    public void setSurname(String surname)
    {
        this.surname = surname;
    }

    /**
     * @return the real Mailadress of a user
     */
    public String getMail()
    {
        return mail;
    }

    /**
     * Sets the real Mailadress of a user
     * 
     * @param mail
     */
    public void setMail(String mail)
    {
        this.mail = mail.toLowerCase();
    }

    /**
     * @return the hashed value of the bcrypted password
     */
    public String getPasswd()
    {

        return passwd;
    }

    /**
     * Hashes the password and sets it as the current pwd
     * 
     * @param passwd
     */
    public void hashPasswd(String passwd)
    {
        setPasswd(BCrypt.hashpw(passwd, BCrypt.gensalt()));
    }

    /**
     * @param pwd
     *            - the plaintext password to check
     * @return true if the passwords are matching
     */
    public boolean checkPasswd(String pwd)
    {
        return BCrypt.checkpw(pwd, this.passwd);
    }

    /**
     * sets the given password WARNING: don't use this method to set a new password! always use hashPasswd() for that!
     * 
     * @param passwd
     */
    public void setPasswd(String passwd)
    {
        this.passwd = passwd;
    }

    /**
     * @return true if a user is admin
     */
    public boolean isAdmin()
    {
        return admin;
    }

    /**
     * @return true if the account is active
     */
    public boolean isActive()
    {
        return active;
    }

    /**
     * Sets the account as active or inactive
     * 
     * @param active
     *            - boolean which indicates the new state of the account
     */
    public void setActive(boolean active)
    {
        this.active = active;
    }

    /**
     * Sets the account as active or inactive
     * 
     * @param admin
     */
    public void setAdmin(boolean admin)
    {
        this.admin = admin;
    }

    /**
     * @return the randomly-generated confirm-token
     */
    public String getConfirmation()
    {
        return confirmation;
    }

    /**
     * sets the random-confirm-token
     * 
     * @param confirmation
     */
    public void setConfirmation(String confirmation)
    {
        this.confirmation = confirmation;
    }

    /**
     * @return the validity-period of the string
     */
    public Long getTs_confirm()
    {
        return ts_confirm;
    }

    /**
     * @param ts_confirm
     *            - the timestamp for the end of the confirmation-period
     */
    public void setTs_confirm(Long ts_confirm)
    {
        this.ts_confirm = ts_confirm;
    }

    /**
     * @return the number of a wrong entered password
     */
    public int getBadPwCount()
    {
        return badPwCount;
    }

    /**
     * @param badPwCount
     *            - the number of wrong entered passwords
     */
    public void setBadPwCount(int badPwCount)
    {
        this.badPwCount = badPwCount;
    }

    // ---------------------------- EBean-Functions----------------------

    /**
     * @return the list of all users in the database
     */
    public static List<User> all()
    {
        return Ebean.find(User.class).findList();

    }

    /**
     * Updates the Data of a User
     */
    public void update()
    {
        Ebean.update(this);
    }

    /**
     * Checks if a Mailaddress exists in the Database
     * 
     * @param mail
     *            - Mailaddress of a User
     * @return true if the given Address exists
     */
    public static boolean mailExists(String mail)
    {
        if (!Ebean.find(User.class).where().eq("mail", mail.toLowerCase()).findList().isEmpty())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Checks whether the mail of the user (identified by its id) has changed and if the new address can be used
     * 
     * @param mail - the new Mailaddress of the User
     * @param uId - the User-ID
     * @return true - Mail has changed and the new address does not exist <br/>
     *         false - Mail has not changed, User is unknown or Address already exists
     */

    public static boolean mailChanged(String mail, Long uId)
    {
        User usr = User.getById(uId);
        if (usr.equals(null))
        { // theres no user with that id
            return false;
        }
        else
        { // theres a user with that id
            if (usr.mail.equals(mail.toLowerCase()))
            { // the users mail is equal to the given address -> mail not changed
                return false;
            }
            else
            { // the addresses differ
                if (User.mailExists(mail))
                { // the given address already exists for another user
                    return false;
                }
                else
                { // the given address is not used
                    return true;
                }

            }
        }

    }

    /**
     * @return the number of active admin-accounts
     */
    public static int getActiveAdminCount()
    {
        return Ebean.find(User.class).where().eq("admin", true).eq("active", true).findRowCount();
    }

    /**
     * @param id
     *            - userid
     * @return true if the user is an admin
     */
    public static boolean isUserAdmin(long id)
    {
        return Ebean.find(User.class, id).isAdmin();
    }

    /**
     * returns the user-object that belongs to the given mailadress
     * 
     * @param mail
     *            : adress of a user
     * @return the user
     */
    public static User getUsrByMail(String mail)
    {
        return Ebean.find(User.class).where().eq("mail", mail.toLowerCase()).findUnique();
    }

    /**
     * returns the user-object if the given mail and password combination matches
     * 
     * @param mail
     * @param pw
     * @return the user object if the given mail and password belong together <br/>
     *         null - else
     */

    public static User auth(String mail, String pw)
    {
        // get the user by the mailadress
        User usr = Ebean.find(User.class).where().eq("mail", mail.toLowerCase()).findUnique();

        if (!(usr == null))
        {
            // there's a user with that address

            // next code is redundant, same like authById()
            if (BCrypt.checkpw(pw, usr.getPasswd()))
            {

                // check if the given password is correct
                return usr;
            }
            else
            {
                // the password is wrong
                return null;
            }
        }
        else
        {
            // there's no user with that address
            return null;
        }
    }

    /**
     * This Method returns the user object if the given userId and password belong together otherwise it will return
     * null
     * 
     * @param id
     *            - the id of a user
     * @param pw
     *            - the pw of a user
     * @return the user object if the given userId and password belong together
     */

    public static User authById(Long id, String pw)
    {
        User usr = Ebean.find(User.class, id);
        if (BCrypt.checkpw(pw, usr.getPasswd()))
        {
            return usr;
        }
        else
        {

            return null;
        }
    }

    /**
     * Returns the User-Object which belongs to the given User-ID
     * 
     * @param id
     *            : a users id
     * @return the user-object
     */
    public static User getById(Long id)
    {
        return Ebean.find(User.class, id);
    }

    /**
     * Deletes the User with the given ID
     * 
     * @param id
     *            : the id of the user that has to be deleted
     */

    public static void delete(Long id)
    {
        Ebean.delete(User.class, id);
    }

    /**
     * Promotes or Demotes the User and Updates the DB the Method checks the actual state and sets the opposite
     * 
     * @param id
     *            - id of the user
     */

    public static void promote(Long id)
    {
        User usr = User.getById(id);
        usr.setAdmin(!usr.admin);
        Ebean.update(usr);
    }

    /**
     * Activates or Deactivates the User and Updates the DB the Method checks the actual state and sets the opposite
     * 
     * @param id
     *            - id of the user
     */
    public static boolean activate(Long id)
    {
        User usr = User.getById(id);
        usr.setActive(!usr.isActive());
        Ebean.update(usr);
        return usr.isActive();

    }

    /**
     *  Converts the Userobject to a String which contains in one line:<br/>
     *  UserID, Forename, Surname, Mail and (the crypted) Password<br/>
     *  The fields are separated by a whitespace 
     *  
     */
    @Override
    public String toString()
    {
        return id + " " + forename + " " + " " + surname + " " + mail + " " + passwd;
    }

}
