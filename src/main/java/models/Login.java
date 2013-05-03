package models;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Holds the Data for the Login Form
 * 
 * @author Patrick Thum 2012 released under Apache 2.0 License
 */
public class Login
{
    @NotEmpty
    String mail;

    String pwd;

    /**
     * @return the mailaddress
     */
    public String getMail()
    {
        return mail;
    }

    /**
     * @param mail - the mail to set
     */
    public void setMail(String mail)
    {
        this.mail = mail;
    }

    /**
     * @return the password 
     */
    public String getPwd()
    {
        return pwd;
    }

    /**
     * @param pwd the password to set
     */
    public void setPwd(String pwd)
    {
        this.pwd = pwd;
    }

}
