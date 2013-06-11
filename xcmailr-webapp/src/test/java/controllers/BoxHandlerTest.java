package controllers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import models.MBox;
import models.User;
import ninja.NinjaTest;
import ninja.utils.NinjaTestBrowser;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.Maps;

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
        formParams.put("pwd", "1234");
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

        // create a new instance of the testbrowser, because theres no other way to clear the cookies...
        ninjaTestBrowser = new NinjaTestBrowser();
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
        formParams.put("pwd", "1234");
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
        assertTrue(result.contains("class=\"error\">"));

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
        assertTrue(result.contains("class=\"error\">"));
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
        assertTrue(result.contains("class=\"error\">"));
        // check that there is a mailbox with that address
        assertNull(MBox.getByName("bbox", "xcmailr.test"));

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
        assertTrue(result.contains("class=\"error\">"));
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
        MBox mbox = new MBox("abcdefg", "xcmailr.test", 0L, false, user);
        mbox.save();
        Long id = mbox.getId() + 1;
        String expected = "";
        // increase the id until we found a "free" id or reached 1000000 (prevent an endless loop)
        while (!(MBox.getById(id) == null) && (id <= 1000000))
        {
            id += 1;
        }
        // fail if we couldn't find a free id
        if (id == 1000000)
        {
            fail();
        }
        // try to edit a mailbox that does not exist
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "mail/edit/" + id);
        expected = ninjaTestBrowser.makeRequest(getServerAddress() + "mail");
        assertTrue(expected.equals(result));

        // request with the correct page
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "mail/edit/" + mbox.getId());
        // check that the returned page contains the data of our mbox
        Map<String, String> formMap = HtmlUtils.readInputFormData(result);
        assertTrue(formMap.containsKey("address"));
        assertTrue(formMap.containsKey("domain"));
        assertTrue(formMap.containsKey("datetime"));
        formParams.clear();
        formParams.put("address", mbox.getAddress());
        formParams.put("domain", mbox.getDomain());
        formParams.put("datetime", String.valueOf(mbox.getTs_Active()));
        TestUtils.testMapEntryEquality(formParams, formMap);
        formParams.clear();

        /*
         * TEST: Form with a wrong domain
         */

        formMap.put("domain", "xcmailr.null");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/edit/" + mbox.getId(),
                                                                    headers, formMap);
        assertNull(MBox.getByName("abcdefg", "xcmailr.null"));
        expected = ninjaTestBrowser.makeRequest(getServerAddress() + "mail");
        assertTrue(expected.equals(result));

        /*
         * TEST: Form with errors
         */
        formMap.remove("address");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/edit/" + mbox.getId(),
                                                                    headers, formMap);
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: correct edit of a box
         */
        // change the local-part of the mbox and the timestamp
        formMap.put("address", "abcde");
        formMap.put("domain", "xcmailr.test");
        DateTime dt = new DateTime().plusHours(1);

        formMap.put("datetime",
                    dt.getDayOfMonth() + "." + dt.getMonthOfYear() + "." + dt.getYear() + " " + dt.getHourOfDay() + ":"
                        + dt.getMinuteOfHour());
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/edit/" + mbox.getId(),
                                                                    headers, formMap);
        expected = ninjaTestBrowser.makeRequest(getServerAddress() + "mail");
        System.out.println("\n\n\n\n"+result+"\n"+expected);
        assertTrue(expected.equals(result));

        /*
         * TEST: a wrong timestamp
         */
        formMap.put("datetime", "01.01.00");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/edit/" + mbox.getId(),
                                                                    headers, formMap);
        expected = ninjaTestBrowser.makeRequest(getServerAddress() + "mail");
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: edit of an mbox that does not exist
         */

        id = mbox.getId() + 1;

        // increase the id until we found a "free" id or reached 1000000 (prevent an endless loop)
        while (!(MBox.getById(id) == null) && (id <= 1000000))
        {
            id += 1;
        }
        // fail if we couldn't find a free id
        if (id == 1000000)
        {
            fail();
        }
        formParams.put("address", mbox.getAddress());
        formParams.put("domain", mbox.getDomain());
        formParams.put("datetime", String.valueOf(mbox.getTs_Active()));
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/edit/" + id, headers,
                                                                    formParams);
        expected = ninjaTestBrowser.makeRequest(getServerAddress() + "mail");
        assertTrue(expected.equals(result));

        /*
         * TEST: edit of an mbox that does not belong to this user
         */

        User user2 = new User("fName", "sName", "eMail@xcmailr.test", "1234", "en");
        user2.save();
        MBox mbox2 = new MBox("mbox2", "xcmailr.test", 0, false, user2);
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
        MBox mbox = new MBox("abcdefg", "xcmailr.test", 0L, false, user);
        mbox.save();
        String expected;
        formParams.clear();
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/expire/" + mbox.getId(),
                                                                    headers, formParams);
        expected = ninjaTestBrowser.makeRequest(getServerAddress() + "mail");
        assertTrue(expected.equals(result));

        /*
         * TEST: expire/reactivate a box with a timestamp in the past
         */
        // create a new mailforward (mbox) to expire
        DateTime dt = DateTime.now().minusHours(2);
        MBox mbox2 = new MBox("abcdefg", "xcmailr.test", dt.getMillis(), false, user);
        mbox2.save();
        formParams.clear();
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/expire/" + mbox2.getId(),
                                                                    headers, formParams);
        expected = ninjaTestBrowser.makeRequest(getServerAddress() + "mail/edit/" + mbox2.getId());
        assertTrue(expected.equals(result));

    }

}
