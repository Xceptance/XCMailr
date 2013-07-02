package controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import models.MBox;
import models.User;
import ninja.NinjaTest;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.Maps;

import etc.HelperUtils;

public class BoxHandlerTest extends NinjaTest
{
    Map<String, String> headers = Maps.newHashMap();

    Map<String, String> formParams = Maps.newHashMap();

    Map<String, String> returnedData = Maps.newHashMap();

    Map<String, String> boxData = Maps.newHashMap();

    String result;

    User user;

    @Before
    public void setUp()
    {
        // create the user (in test mode a volatile in-memory db is used)
        user = new User("John", "Doe", "admin@ccmailr.test", "1234", "en");
        user.setActive(true);
        user.save();

        formParams.clear();
        headers.clear();
        returnedData.clear();
        formParams.clear();

        // login
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("password", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/login", headers, formParams);
        formParams.clear();
    }

    @After
    public void tearDown()
    {

    }

    @Test
    public void testGeneralBoxAdding()
    {

        /*
         * TEST: Try adding a box without having logged in
         */
        // logout
        ninjaTestBrowser.makeRequest(getServerAddress() + "/logout");
        formParams.clear();
        formParams.put("address", "abox");
        formParams.put("domain", "xcmailr.test");
        formParams.put("datetime", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/add", headers,
                                                                    formParams);

        // verify that the login-page is shown (check for menu with register-field and the login-formaction)
        assertTrue(result.contains("<a href=\"/register\">"));
        assertTrue(result.contains("form action=\"/login\""));

        // verify that the mailbox doesnt exist
        assertNull(MBox.getByName("abox", "xcmailr.test"));

        formParams.clear();

        /*
         * TEST: Try to add a box while logged-in
         */

        // log-in with the new user
        formParams.clear();
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("password", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "login", headers, formParams);
        formParams.clear();

        // add the box
        formParams.put("address", "abox");
        formParams.put("domain", "xcmailr.test");
        formParams.put("datetime", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/add", headers,
                                                                    formParams);

        // check that the add of the mbox was successful

        assertTrue(result.contains("<!--mailboxoverview-->"));
        // check that there is a mailbox with that address
        assertNotNull(MBox.getByName("abox", "xcmailr.test"));

        /*
         * TEST: Try to add a box with the same data again
         */

        // add the box
        formParams.put("address", "abox");
        formParams.put("domain", "xcmailr.test");
        formParams.put("datetime", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/add", headers,
                                                                    formParams);

        // check that the add of the mbox was successful
        assertTrue(result.contains("class=\"alert alert-error\">"));

        /*
         * TEST: try to add a box with no data
         */

        // add the box
        formParams.put("address", "");
        formParams.put("domain", "");
        formParams.put("datetime", "");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/add", headers,
                                                                    formParams);

        // check that the add of the mbox was successful
        assertTrue(result.contains("class=\"alert alert-error\">"));
        // check that there is a mailbox with that address
        assertNull(MBox.getByName("", ""));

        /*
         * TEST: try to add a box with a wrong timestamp
         */

        // add the box
        formParams.put("address", "bbox");
        formParams.put("domain", "xcmailr.test");
        formParams.put("datetime", "2x,3d");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/add", headers,
                                                                    formParams);

        // check that the add of the mbox was successful
        assertTrue(result.contains("class=\"alert alert-error\">"));
        // check that there is a mailbox with that address
        assertNull(MBox.getByName("bbox", "xcmailr.test"));

        /*
         * TEST: try to add a box with an expired timestamp
         */

        // add the box
        formParams.put("address", "bbbcbox");
        formParams.put("domain", "xcmailr.test");
        String ts = HelperUtils.parseStringTs(DateTime.now().minusHours(3).getMillis());
        formParams.put("datetime", ts);
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/add", headers,
                                                                    formParams);

        // check that the add of the mbox was successful
        assertTrue(result.contains("class=\"alert alert-error\">"));
        // check that there is a mailbox with that address
        assertNull(MBox.getByName("bbox", "xcmailr.test"));
    }

    @Test
    public void testAddBoxAddressField()
    {
        /*
         * TEST: Try to add a Box with an invalid local part
         */
        // add the box
        formParams.put("address", "$$³@@@..");
        formParams.put("domain", "xcmailr.test");
        formParams.put("datetime", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/add", headers,
                                                                    formParams);

        // check that the add of the mbox failed and the overview-page is shown
        assertTrue(result.contains("class=\"alert alert-error\">"));
        // check that there is a mailbox with that address
        assertNull(MBox.getByName("$$³@@@..", "xcmailr.test"));

        /*
         * TEST: Try to add a Box with a local part that is more than 64chars long
         */
        // add the box
        formParams.put("address", "a12345678901234567890123456789012345678901234567890123456789012345678901234567890a");
        formParams.put("domain", "xcmailr.test");
        formParams.put("datetime", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/add", headers,
                                                                    formParams);

        // check that the add of the mbox failed and the overview-page is shown
        assertTrue(result.contains("class=\"alert alert-error\">"));
        // check that there is a mailbox with that address
        assertNull(MBox.getByName("a12345678901234567890123456789012345678901234567890123456789012345678901234567890a",
                                  "xcmailr.test"));

        /*
         * TEST: Try to add a Box which has more than 254 chars
         */
        // add the box
        formParams.put("address", "a12345678901234567890123456789012345678901234567890123456789012345678901234567890a");
        formParams.put("domain",
                       "a12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                           + "aa12345678901234567890123456789012345678901234567890123456789012345678901234567890aa"
                           + "12345678901234567890123456789012345678901234567890123456789012345678901234567890a.test");
        formParams.put("datetime", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/add", headers,
                                                                    formParams);

        // check that the add of the mbox failed and the overview-page is shown
        assertTrue(result.contains("class=\"alert alert-error\">"));
        // check that there is a mailbox with that address
        assertNull(MBox.getByName("a12345678901234567890123456789012345678901234567890123456789012345678901234567890a",
                                  "a12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                                      + "aa12345678901234567890123456789012345678901234567890123456789012345678901234567890aa"
                                      + "12345678901234567890123456789012345678901234567890123456789012345678901234567890a.test"));

    }

    @Test
    public void testAddBoxDomainField()
    {

        /*
         * TEST: Try to add a Box with a domain which is not set in application.conf
         */
        // add the box
        formParams.put("address", "abox");
        formParams.put("domain", "xcmlr.abc");
        formParams.put("datetime", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/add", headers,
                                                                    formParams);

        // check that the add of the mbox failed and the overview-page is shown
        assertTrue(result.contains("<!--mailboxoverview-->"));
        // check that there is a mailbox with that address
        assertNull(MBox.getByName("abox", "xcmlr.abc"));

        /*
         * TEST: Try to add a Box with a domain which contains special-chars, etc.
         */
        // add the box
        formParams.put("address", "abox");
        formParams.put("domain", "xcmlr@a.abc");
        formParams.put("datetime", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/add", headers,
                                                                    formParams);

        // check that the add of the mbox has failed
        assertTrue(result.contains("class=\"alert alert-error\">"));
        // check that there is no mailbox with that address
        assertNull(MBox.getByName("abox", "xcmlr@a.abc"));

    }

    @Test
    public void testDeleteBox()
    {

        /*
         * TEST: Delete a Mailbox
         */
        // add the box
        formParams.put("address", "abox");
        formParams.put("domain", "xcmailr.test");
        formParams.put("datetime", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/add", headers,
                                                                    formParams);
        MBox mb = MBox.getByName("abox", "xcmailr.test");
        assertNotNull(mb);

        formParams.clear();
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/delete/" + mb.getId(),
                                                                    headers, formParams);
        // verify that the overview-page is shown
        assertTrue(result.contains("<!--mailboxoverview-->"));
        // verify that the box was deleted from the db
        assertNull(MBox.getByName("abox", "xcmailr.test"));

    }

    @Test
    public void testShowAddBox()
    {
        /*
         * The returned page should have a form with prepopulated values
         */
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "/mail/add");
        Map<String, String> formMap = HtmlUtils.readInputFormData(result);
        assertTrue(formMap.containsKey("address"));
        assertTrue(formMap.containsKey("domain"));
        assertTrue(formMap.containsKey("datetime"));
    }

    @Test
    public void testEditBox()
    {
        /*
         * TEST: the shown edit-box-page
         */
        // create a new mailforward (mbox) for editing
        MBox mailbox = new MBox("abcdefg", "xcmailr.test", 0L, false, user);
        mailbox.save();
        Long mailBoxId = mailbox.getId() + 1;
        String expected = "";
        // increase the id until we found a "free" id or reached 1000000 (prevent an endless loop)
        while (!(MBox.getById(mailBoxId) == null) && (mailBoxId <= 1000000))
        {
            mailBoxId += 1;
        }
        // fail if we couldn't find a free id
        if (mailBoxId == 1000000)
        {
            fail();
        }
        // try to edit a mailbox that does not exist
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "mail/edit/" + mailBoxId);
        expected = ninjaTestBrowser.makeRequest(getServerAddress() + "mail");
        assertTrue(expected.equals(result));

        // request with the correct page
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "mail/edit/" + mailbox.getId());
        // check that the returned page contains the data of our mbox
        Map<String, String> formMap = HtmlUtils.readInputFormData(result);

        assertTrue(formMap.containsKey("address"));
        assertTrue(formMap.containsKey("domain"));
        assertTrue(formMap.containsKey("datetime"));
        formParams.clear();
        formParams.put("address", mailbox.getAddress());
        formParams.put("domain", mailbox.getDomain());
        formParams.put("datetime", String.valueOf(mailbox.getTs_Active()));
        TestUtils.testMapEntryEquality(formParams, formMap);
        formParams.clear();

        /*
         * TEST: Form with a wrong domain
         */

        formMap.put("domain", "xcmailr.null");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/edit/" + mailbox.getId(),
                                                                    headers, formMap);
        assertNull(MBox.getByName("abcdefg", "xcmailr.null"));
        expected = ninjaTestBrowser.makeRequest(getServerAddress() + "mail");
        assertTrue(expected.equals(result));

        /*
         * TEST: Form with errors
         */
        formMap.remove("address");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/edit/" + mailbox.getId(),
                                                                    headers, formMap);
        assertTrue(result.contains("class=\"alert alert-error\">"));

        /*
         * TEST: correct edit of a box
         */
        // change the local-part of the mbox and the timestamp
        formMap.put("address", "abcde");
        formMap.put("domain", "xcmailr.test");
        DateTime dt = new DateTime().plusHours(1);

        formMap.put("datetime",
                    dt.getYear() + "-" + dt.getMonthOfYear() + "-" + dt.getDayOfMonth() + " " + dt.getHourOfDay() + ":"
                        + dt.getMinuteOfHour());
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/edit/" + mailbox.getId(),
                                                                    headers, formMap);
        expected = ninjaTestBrowser.makeRequest(getServerAddress() + "mail");

        assertTrue(expected.equals(result));

        /*
         * TEST: a wrong timestamp
         */
        formMap.put("datetime", "01-01-00");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/edit/" + mailbox.getId(),
                                                                    headers, formMap);
        expected = ninjaTestBrowser.makeRequest(getServerAddress() + "mail");
        assertTrue(result.contains("class=\"alert alert-error\">"));

        /*
         * TEST: edit of an mbox that does not exist
         */

        mailBoxId = mailbox.getId() + 1;

        // increase the id until we found a "free" id or reached 1000000 (prevent an endless loop)
        while ((MBox.getById(mailBoxId) != null) && (mailBoxId <= 1000000))
        {
            mailBoxId += 1;
        }
        // fail if we couldn't find a free id
        if (mailBoxId == 1000000)
        {
            fail();
        }
        formParams.put("address", mailbox.getAddress());
        formParams.put("domain", mailbox.getDomain());
        formParams.put("datetime", String.valueOf(mailbox.getTs_Active()));
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/edit/" + mailBoxId, headers,
                                                                    formParams);
        expected = ninjaTestBrowser.makeRequest(getServerAddress() + "mail");
        assertTrue(expected.equals(result));

        /*
         * TEST: edit of an mbox that does not belong to this user
         */

        User user2 = new User("fName", "sName", "eMail@xcmailr.test", "1234", "en");
        user2.save();
        MBox mbox2 = new MBox("mbox2", "xcmailr.test", 0L, false, user2);
        mbox2.save();

        // display the edit-page

        // try to edit a mailbox that does not exist
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "mail/edit/" + mbox2.getId());
        expected = ninjaTestBrowser.makeRequest(getServerAddress() + "mail");
        assertTrue(expected.equals(result));

        // try to edit this box (POST to /mail/edit/id)
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/edit/" + mbox2.getId(),
                                                                    headers, formParams);
        expected = ninjaTestBrowser.makeRequest(getServerAddress() + "mail");

    }

    @Test
    public void testExpireBox()
    {
        /*
         * TEST: expire a box normally
         */
        // create a new mailforward (mbox) to expire
        MBox mailbox = new MBox("abcdefg", "xcmailr.test", 0L, false, user);
        mailbox.save();
        String expected;
        formParams.clear();
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/expire/" + mailbox.getId(),
                                                                    headers, formParams);
        expected = ninjaTestBrowser.makeRequest(getServerAddress() + "mail");
        assertTrue(expected.equals(result));

        /*
         * TEST: expire/reactivate a box with a timestamp in the past
         */
        // create a new mail address(mbox) to expire
        DateTime dt = DateTime.now().minusHours(2);
        MBox mailbox2 = new MBox("abcdefg", "xcmailr.test", dt.getMillis(), false, user);
        mailbox2.save();
        formParams.clear();
        // we want to reactivate the box and expect the edit-page then (because its expired)
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/expire/" + mailbox2.getId(),
                                                                    headers, formParams);
        assertTrue(result.contains("id=\"editBoxForm\""));

    }

    @Test
    public void testResetBoxCounters()
    {
        MBox mailbox = new MBox("abcdefg", "xcmailr.test", 0L, false, user);
        mailbox.save();
        // increase the suppressions and forwards
        mailbox.increaseForwards();
        mailbox.increaseForwards();
        mailbox.increaseForwards();
        mailbox.increaseSuppressions();
        mailbox.increaseSuppressions();
        mailbox.increaseSuppressions();
        mailbox.update();
        assertEquals(3, mailbox.getSuppressions());
        assertEquals(3, mailbox.getForwards());

        formParams.clear();
        // try to reset the box via controller method
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/reset/" + mailbox.getId(),
                                                                    headers, formParams);

        MBox mailbox2 = MBox.getById(mailbox.getId());

        assertEquals(0, mailbox2.getSuppressions());
        assertEquals(0, mailbox2.getForwards());

    }

    @Test
    public void testBulkChange()
    {
        MBox mailbox1 = new MBox("mail1", "xcmailr.test", 0L, false, user);
        mailbox1.setForwards(3);
        mailbox1.setSuppressions(3);
        mailbox1.save();
        MBox mailbox2 = new MBox("mail2", "xcmailr.test", 0L, false, user);
        mailbox2.setForwards(2);
        mailbox2.setSuppressions(2);
        mailbox2.save();

        MBox mailbox3 = new MBox("mail3", "xcmailr.test", 0L, false, user);
        mailbox3.setForwards(1);
        mailbox3.setSuppressions(1);
        mailbox3.save();

        // check the correct numbers
        assertEquals(3, mailbox1.getSuppressions());
        assertEquals(3, mailbox1.getForwards());
        assertEquals(2, mailbox2.getSuppressions());
        assertEquals(2, mailbox2.getForwards());
        assertEquals(1, mailbox3.getSuppressions());
        assertEquals(1, mailbox3.getForwards());

        // check that the boxes are active
        assertTrue(mailbox1.isActive());
        assertTrue(mailbox2.isActive());
        assertTrue(mailbox3.isActive());

        formParams.clear();
        // try to reset the boxes via controller method
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "mail/bulkChange?action=reset&ids=" + mailbox1.getId()
                                              + "," + mailbox2.getId() + "," + mailbox3.getId());

        // update the boxes
        MBox mailbox1new = MBox.getById(mailbox1.getId());
        MBox mailbox2new = MBox.getById(mailbox2.getId());
        MBox mailbox3new = MBox.getById(mailbox3.getId());
        // check that the boxes are disabled
        assertEquals(0, mailbox1new.getSuppressions());
        assertEquals(0, mailbox1new.getForwards());
        assertEquals(0, mailbox2new.getSuppressions());
        assertEquals(0, mailbox2new.getForwards());
        assertEquals(0, mailbox3new.getSuppressions());
        assertEquals(0, mailbox3new.getForwards());

        mailbox3new.enable();
        mailbox3new.update();
        // try to disable the boxes via controller method
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "mail/bulkChange?action=enable&ids=" + mailbox1.getId()
                                              + "," + mailbox2.getId() + "," + mailbox3.getId());

        // update the boxes
        mailbox1 = MBox.getById(mailbox1.getId());
        mailbox2 = MBox.getById(mailbox2.getId());
        mailbox3 = MBox.getById(mailbox3.getId());
        // check that the boxes are active
        assertFalse(mailbox1.isActive());
        assertFalse(mailbox2.isActive());
        assertTrue(mailbox3.isActive());

        // check the original timestamp
        assertEquals(0L, mailbox1.getTs_Active());
        assertEquals(0L, mailbox2.getTs_Active());
        assertEquals(0L, mailbox3.getTs_Active());
        DateTime dt = DateTime.now().plusHours(3);
        String duration = HelperUtils.parseStringTs(dt.getMillis());

        URI uri;
        try
        {
            URI uriServer = getServerAddressAsUri();
            uri = new URI(uriServer.getScheme(), uriServer.getHost() + ":" + uriServer.getPort(), "/mail/bulkChange",
                          "action=change&ids=" + mailbox1.getId() + "," + mailbox2.getId() + "," + mailbox3.getId()
                              + "&duration=" + duration, null);

            String request = uri.toASCIIString();

            // try to change the validity-period of the boxes via controller method
            result = ninjaTestBrowser.makeRequest(request);
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
        }
        // update the boxes
        mailbox1 = MBox.getById(mailbox1.getId());
        mailbox2 = MBox.getById(mailbox2.getId());
        mailbox3 = MBox.getById(mailbox3.getId());

        // check the new timestamp
        long timeStamp = HelperUtils.parseTimeString(duration);

        assertEquals(timeStamp, mailbox1.getTs_Active());
        assertEquals(timeStamp, mailbox2.getTs_Active());
        assertEquals(timeStamp, mailbox3.getTs_Active());

        // try to delete the boxes via controller method
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "mail/bulkChange?action=delete&ids=" + mailbox1.getId()
                                              + "," + mailbox2.getId() + "," + mailbox3.getId());

        mailbox1 = MBox.getById(mailbox1.getId());
        mailbox2 = MBox.getById(mailbox2.getId());
        mailbox3 = MBox.getById(mailbox3.getId());

        assertNull(mailbox1);
        assertNull(mailbox2);
        assertNull(mailbox3);
    }
}
