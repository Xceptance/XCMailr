package models;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EditUsrTest
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
        User user = new User("forename", "surname", "test@localhost.com", "1234", "en");
        UserFormData userFormData1 = UserFormData.prepopulate(user);
        assertTrue(userFormData1.getFirstName().equals("forename"));
        assertTrue(userFormData1.getSurName().equals("surname"));
        assertTrue(userFormData1.getMail().equals("test@localhost.com"));

        // the password should not be written to the prepopulated EdtUsr (pwn1 and 2 too)
        assertNull(userFormData1.getPassword());
        assertNull(userFormData1.getPasswordNew1());
        assertNull(userFormData1.getPasswordNew2());
    }

    @Test
    public void getAsUsrTest()
    {

        UserFormData userFormData1 = new UserFormData();
        userFormData1.setFirstName("forename");
        userFormData1.setSurName("surname");
        userFormData1.setMail("mail@localhost");
        userFormData1.setPassword("1234");
        userFormData1.setPasswordNew1("1234");
        User user = userFormData1.getAsUser();

        assertTrue(user.getForename().equals("forename"));
        assertTrue(user.getSurname().equals("surname"));
        assertTrue(user.getMail().equals("mail@localhost"));
        assertNotNull(user.checkPasswd("1234"));

    }
}
