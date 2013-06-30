package models;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ninja.NinjaTest;

public class EditUsrTest extends NinjaTest
{

    @Before
    public void setUp()
    {

    }

    @After
    public void tearDown()
    {

    }

    @Test
    public void prepopulateEdtUsrTest()
    {
        User u = new User("forename", "surname", "test@localhost.com", "1234", "en");
        UserFormData edt1 = UserFormData.prepopulate(u);
        assertTrue(edt1.getFirstName().equals("forename"));
        assertTrue(edt1.getSurName().equals("surname"));
        assertTrue(edt1.getMail().equals("test@localhost.com"));

        // the password should not be written to the prepopulated EdtUsr (pwn1 and 2 too)
        assertNull(edt1.getPassword());
        assertNull(edt1.getPasswordNew1());
        assertNull(edt1.getPasswordNew2());
    }

    @Test
    public void getAsUsrTest()
    {

        UserFormData edt1 = new UserFormData();
        edt1.setFirstName("forename");
        edt1.setSurName("surname");
        edt1.setMail("mail@localhost");
        edt1.setPassword("1234");
        edt1.setPasswordNew1("1234");
        User u = edt1.getAsUser();

        assertTrue(u.getForename().equals("forename"));
        assertTrue(u.getSurname().equals("surname"));
        assertTrue(u.getMail().equals("mail@localhost"));
        assertNotNull(u.checkPasswd("1234"));

    }
}
