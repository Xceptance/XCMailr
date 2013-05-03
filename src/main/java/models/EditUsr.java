package models;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Holds all data for the Edit Form (also used in the registration process)
 * 
 * @author Patrick Thum 2012 released under Apache 2.0 License
 */

public class EditUsr
{
    @NotEmpty
    public String forename;

    @NotEmpty
    private String surName;

    @Email
    private String mail;

    @NotEmpty
    private String pw;

    private String pwn1;

    private String pwn2;

    /**
     * @return the forename
     */
    public String getForename()
    {
        return forename;
    }

    /**
     * @param foreName
     *            - the forename to set
     */
    public void setForeName(String foreName)
    {
        this.forename = foreName;
    }

    /**
     * @return the surname
     */
    public String getSurName()
    {
        return surName;
    }

    /**
     * @param surName
     *            - the surname to set
     */
    public void setSurName(String surName)
    {
        this.surName = surName;
    }

    /**
     * @return the mailaddress
     */
    public String getMail()
    {
        return mail;
    }

    /**
     * @param mail
     *            - the mailaddress to set
     */
    public void setMail(String mail)
    {
        this.mail = mail;
    }

    /**
     * @return the password
     */
    public String getPw()
    {
        return pw;
    }

    /**
     * @param pw
     *            - the password to set
     */
    public void setPw(String pw)
    {
        this.pw = pw;
    }

    /**
     * @return the new password
     */
    public String getPwn1()
    {
        return pwn1;
    }

    /**
     * @param pwn1
     *            - the new password to set
     */
    public void setPwn1(String pwn1)
    {
        this.pwn1 = pwn1;
    }

    /**
     * @return the repetition of the new password
     */
    public String getPwn2()
    {
        return pwn2;
    }

    /**
     * @param pwn2
     *            the repetition of the new password to set
     */
    public void setPwn2(String pwn2)
    {
        this.pwn2 = pwn2;
    }

    /**
     * This Method will create a Userobject by the given data<br/>
     * Be careful: The pwn1-field will be used as password for the User-object <br/>
     * 
     * @return a user-object instantiated with the given data
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
     * @return - an EditUsr-Object filled with the userdata
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
