package models;

import com.avaje.ebean.validation.NotEmpty;

/**
 * The Model for the Password-Reset-Form
 * 
 * @author Patrick Thum
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
