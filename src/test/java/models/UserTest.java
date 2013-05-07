package models;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ninja.NinjaTest;

public class UserTest extends NinjaTest
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
    public void UsersTest()
    {

        /*
         * TEST: create, persist and find a user-object
         */
        User u = new User("forename", "surname", "test@localhost.com", "1234");
        u.save();
        User u2 = User.getById(u.getId());
        assertNotNull(u);
        assertNotNull(u2);

        /*
         * TEST: Auth-Methods
         */
        assertNull(User.auth("test@localhost.com", "4321"));
        assertNotNull(User.auth("test@localhost.com", "1234"));

        assertNull(User.authById(u.getId(), "4321"));
        assertNotNull(User.authById(u.getId(), "1234"));

        /*
         * TEST: Get the Userlist
         */
        User u3 = new User("forename", "surname", "test2@localhost.com", "1234");
        u3.save();
        List<User> list = User.all();

        // Remark: we've 3 accounts now, because there is an adminaccount, too
        assertTrue(list.size() == 3);

        /*
         * TEST: change the users values
         */
        u3.setActive(true);
        u3.setForename("foo");
        u3.update();
        User u4 = User.getById(u3.getId());
        assertTrue(u3.getForename().equals(u4.getForename()));
        assertTrue(u3.isActive() == u4.isActive());
        assertTrue(u3.getId() == u4.getId());
        assertTrue(u3.getMail().equals(u4.getMail()));

        /*
         * TEST: Delete a persisted User
         */
        User.delete(u2.getId());
        assertNull(User.getById(u2.getId()));

    }
}
