package models;

import static org.junit.Assert.*;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ninja.NinjaTest;

public class MBoxTest extends NinjaTest
{
    User u;

    MBox mb;

    @Before
    public void setUp()
    {
        u = new User("forename", "surname", "test@localhost.com", "1234");
        u.save();
        mb = new MBox("test", "xcmailr.test", 0, false, u);
        mb.save();
    }

    @After
    public void tearDown()
    {

    }

    @Test
    public void BoxTest()
    {

        /*
         * TEST: create, persist and find a user-object
         */
        User u2 = new User("forename2", "surname2", "test2@localhost.com", "1234");

        u.save();
        u2.save();

        MBox mb2 = MBox.getById(mb.getId());
        assertNotNull(mb);
        assertNotNull(mb2);

        /*
         * TEST: Get the MBox-list of all Users and a specific User
         */
        MBox mb3 = new MBox("test2", "xcmailr.test", 0, false, u);
        mb3.save();
        MBox mb4 = new MBox("test3", "xcmailr.test", 0, false, u2);
        mb4.save();
        List<MBox> list = MBox.all();

        // in general we should have 3 MBoxes now
        assertTrue(list.size() == 3);

        // the first user should now have 2 boxes
        list = MBox.allUser(u.getId());
        assertTrue(list.size() == 2);

        /*
         * TEST: change the MBox values
         */
        mb.enable();
        mb.setAddress("test001");
        mb.update();
        mb2 = MBox.getById(mb.getId());

        // we executed the enable()-Method, which switches the expiration-flag to the opposite of the current state
        // now it should be expired
        assertTrue(mb2.isExpired());
        assertTrue(mb2.getAddress().equals("test001"));

        /*
         * TEST: Delete a persisted MBox
         */
        MBox.delete(mb2.getId());
        assertNull(MBox.getById(mb2.getId()));
    }

    @Test
    public void getTsAsStringTest()
    {

        assertTrue(mb.getTSAsString().equals("unlimited"));
        // year, month, day, hour, minute
        DateTime dt = new DateTime(2013, 1, 1, 1, 1);
        mb.setTs_Active(dt.getMillis());
        assertTrue(mb.getTSAsString().equals("01.01.2013 01:01"));

        dt = new DateTime(2013, 1, 12, 11, 1);
        mb.setTs_Active(dt.getMillis());
        assertTrue(mb.getTSAsString().equals("12.01.2013 11:01"));

        dt = new DateTime(2013, 11, 11, 11, 11);
        mb.setTs_Active(dt.getMillis());
        assertTrue(mb.getTSAsString().equals("11.11.2013 11:11"));

    }

    @Test
    public void boxToUserTest()
    {
        User u2 = new User("fname", "sName", "eMail@localhost", "pw");
        u2.save();

        assertTrue(mb.belongsTo(u.getId()));
        assertFalse(mb.belongsTo(u2.getId()));

        assertTrue(MBox.boxToUser(mb.getId(), u.getId()));
        assertFalse(MBox.boxToUser(mb.getId(), u2.getId()));
    }

}
