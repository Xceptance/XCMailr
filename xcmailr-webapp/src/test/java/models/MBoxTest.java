/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ninja.NinjaTest;

public class MBoxTest extends NinjaTest
{
    User user;

    MBox mailbox;

    @Before
    public void setUp()
    {
        user = new User("forename", "surname", "test@localhost.com", "1234", "en");
        user.save();
        mailbox = new MBox("test", "xcmailr.test", 0, false, user);
        mailbox.save();
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
        User user2 = new User("forename2", "surname2", "test2@localhost.com", "1234", "en");
        user2.save();

        MBox mailbox2 = MBox.getById(mailbox.getId());
        assertNotNull(mailbox2);
        assertEquals(mailbox.getFullAddress(), mailbox2.getFullAddress());

        // test case-insensitive handling of mail-addresses
        mailbox2 = MBox.getByName(StringUtils.capitalize(mailbox.getAddress()), mailbox.getDomain().toUpperCase());
        assertNotNull(mailbox2);
        assertEquals(mailbox.getFullAddress(), mailbox2.getFullAddress());
        assertEquals(mailbox.getId(), mailbox2.getId());

        /*
         * TEST: Get the MBox-list of all Users and a specific User
         */
        MBox mailbox3 = new MBox("test2", "xcmailr.test", 0, false, user);
        mailbox3.save();
        MBox mailbox4 = new MBox("test3", "xcmailr.test", 0, false, user2);
        mailbox4.save();
        List<MBox> list = MBox.all();

        // in general we should have 3 MBoxes now
        assertTrue(list.size() == 3);

        // the first user should now have 2 boxes
        list = MBox.allUser(user.getId());
        assertTrue(list.size() == 2);

        /*
         * TEST: change the MBox values
         */
        mailbox.disable();
        mailbox.setAddress("test001");
        mailbox.update();
        mailbox2 = MBox.getById(mailbox.getId());

        // we executed the enable()-Method, which switches the expiration-flag to the opposite of the current state
        // now it should be expired
        assertTrue(mailbox2.isExpired());
        assertTrue(mailbox2.getAddress().equals("test001"));

        /*
         * TEST: Delete a persisted MBox
         */
        MBox.delete(mailbox2.getId());
        assertNull(MBox.getById(mailbox2.getId()));
    }

    @Test
    public void getTsAsStringTest()
    {

        assertTrue(mailbox.getDatetime().equals("unlimited"));
        // year, month, day, hour, minute
        DateTime dt = new DateTime(2013, 1, 1, 1, 1);
        mailbox.setTs_Active(dt.getMillis());
        assertTrue(mailbox.getDatetime().equals("2013-01-01 01:01"));

        dt = new DateTime(2013, 1, 12, 11, 1);
        mailbox.setTs_Active(dt.getMillis());
        assertTrue(mailbox.getDatetime().equals("2013-01-12 11:01"));

        dt = new DateTime(2013, 11, 11, 11, 11);
        mailbox.setTs_Active(dt.getMillis());
        assertTrue(mailbox.getDatetime().equals("2013-11-11 11:11"));

    }

    @Test
    public void boxToUserTest()
    {
        User user2 = new User("fname", "sName", "eMail@localhost.com", "pw", "en");
        user2.save();

        assertTrue(mailbox.belongsTo(user.getId()));
        assertFalse(mailbox.belongsTo(user2.getId()));

        assertTrue(MBox.boxToUser(mailbox.getId(), user.getId()));
        assertFalse(MBox.boxToUser(mailbox.getId(), user2.getId()));
    }

    @Test
    public void expiredByTimestampTest()
    {
        // in this moment, the box should be active and unlimited
        assertFalse(mailbox.isExpiredByTimestamp());

        // box active and expiring in 2h
        mailbox.setTs_Active(DateTime.now().plusHours(2).getMillis());
        assertFalse(mailbox.isExpiredByTimestamp());

        // expired and ts in the past
        mailbox.setTs_Active(DateTime.now().minusHours(2).getMillis());
        mailbox.disable();
        assertTrue(mailbox.isExpiredByTimestamp());
    }

}
