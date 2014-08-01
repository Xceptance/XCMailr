package models;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import java.util.List;

import ninja.NinjaTest;
import ninja.utils.NinjaMode;
import ninja.utils.NinjaProperties;
import ninja.utils.NinjaPropertiesImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserTest extends NinjaTest
{
    NinjaProperties ninjaProperties;

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
        ninjaProperties = spy(new NinjaPropertiesImpl(NinjaMode.test));
        /*
         * TEST: create, persist and find a user-object
         */
        User user = new User("forename", "surname", "test@localhost.com", "1234", "en");
        user.save();
        User user2 = User.getById(user.getId());
        assertNotNull(user);
        assertNotNull(user2);

        /*
         * TEST: isAdmin and isLastAdmin-functions
         */
        String adminAccName = ninjaProperties.get("mbox.adminaddr");
        User admin = User.getUsrByMail(adminAccName);
        assertTrue(admin.isAdmin());
        assertTrue(admin.isLastAdmin());

        /*
         * TEST: Auth-Methods
         */
        assertNull(User.auth("test@localhost.com", "4321"));
        assertNotNull(User.auth("test@localhost.com", "1234"));

        assertNull(User.authById(user.getId(), "4321"));
        assertNotNull(User.authById(user.getId(), "1234"));

        /*
         * TEST: Get the Userlist
         */
        User user3 = new User("forename", "surname", "test2@localhost.com", "1234", "en");
        user3.save();
        List<User> list = User.all();

        // Remark: we've 3 accounts now, because there is an adminaccount, too
        assertTrue(list.size() == 3);

        /*
         * TEST: change the users values
         */
        user3.setActive(true);
        user3.setForename("foo");
        user3.update();
        User user4 = User.getById(user3.getId());
        assertTrue(user3.getForename().equals(user4.getForename()));
        assertTrue(user3.isActive() == user4.isActive());
        assertTrue(user3.getId() == user4.getId());
        assertTrue(user3.getMail().equals(user4.getMail()));

        /*
         * TEST: Delete a persisted User
         */
        User.delete(user2.getId());
        assertNull(User.getById(user2.getId()));

    }
}
