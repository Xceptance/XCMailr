package models;

import com.avaje.ebean.validation.NotEmpty;

public class PwData
{
    @NotEmpty
    private String pw;
    @NotEmpty
    private String pw2;
    
    public String getPw()
    {
        return pw;
    }
    public void setPw(String pw)
    {
        this.pw = pw;
    }
    public String getPw2()
    {
        return pw2;
    }
    public void setPw2(String pw2)
    {
        this.pw2 = pw2;
    }
    
    

}
