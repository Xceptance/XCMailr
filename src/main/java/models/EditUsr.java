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

    public String getForename()
    {
        return forename;
    }

    public void setForeName(String foreName)
    {
        this.forename = foreName;
    }

    public String getSurName()
    {
        return surName;
    }

    public void setSurName(String surName)
    {
        this.surName = surName;
    }

    public String getMail()
    {
        return mail;
    }

    public void setMail(String mail)
    {
        this.mail = mail;
    }

    public String getPw()
    {
        return pw;
    }

    public void setPw(String pw)
    {
        this.pw = pw;
    }

    public String getPwn1()
    {
        return pwn1;
    }

    public void setPwn1(String pwn1)
    {
        this.pwn1 = pwn1;
    }

    public String getPwn2()
    {
        return pwn2;
    }

    public void setPwn2(String pwn2)
    {
        this.pwn2 = pwn2;
    }

    public User getAsUser()
    {
        return new User(forename, surName, mail, pwn1);
    }

    public static EditUsr prepopulate(User u)
    {
        EditUsr eDat = new EditUsr();
        eDat.setForeName(u.getForename());
        eDat.setMail(u.getMail());
        eDat.setSurName(u.getSurname());
        return eDat;
    }

}
