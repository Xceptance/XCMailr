package controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import models.JsonMBox;
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
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

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
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/addAddress", headers,
                                                                    formParams);

        // verify that the login-page is shown (check for menu with register-field and the login-form-action)
        assertTrue(result.contains("\"error\":\"nologin\""));

        // assertTrue(result.contains("form action=\"/login\""));

        // verify that the mailbox doesnt exist
        assertNull(MBox.getByName("abox", "xcmailr.test"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

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
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        // add the box
        formParams.put("address", "abox");
        formParams.put("domain", "xcmailr.test");
        formParams.put("datetime", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/addAddress", headers,
                                                                    formParams);

        // check that the add of the mbox was successful

        assertTrue(result.contains("\"success\":true"));
        // check that there is a mailbox with that address
        assertNotNull(MBox.getByName("abox", "xcmailr.test"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: Try to add a box with the same data again
         */

        // add the box
        formParams.put("address", "abox");
        formParams.put("domain", "xcmailr.test");
        formParams.put("datetime", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/addAddress", headers,
                                                                    formParams);

        // check that the add of the mbox was not successful
        assertTrue(result.contains("\"success\":false"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: try to add a box with no data
         */

        // add the box
        formParams.put("address", "");
        formParams.put("domain", "");
        formParams.put("datetime", "");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/addAddress", headers,
                                                                    formParams);

        // check that the add of the mbox was not successful
        assertTrue(result.contains("\"success\":false"));
        // check that there is no mailbox with that address
        assertNull(MBox.getByName("", ""));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: try to add a box with a wrong timestamp
         */

        // add the box
        formParams.put("address", "bbox");
        formParams.put("domain", "xcmailr.test");
        formParams.put("datetime", "2x,3d");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/addAddress", headers,
                                                                    formParams);

        // check that the add of the mbox was successful
        assertTrue(result.contains("\"success\":false"));
        // check that there is no mailbox with that address
        assertNull(MBox.getByName("bbox", "xcmailr.test"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: try to add a box with an expired timestamp
         */

        // add the box
        formParams.put("address", "bbbcbox");
        formParams.put("domain", "xcmailr.test");
        String ts = HelperUtils.parseStringTs(DateTime.now().minusHours(3).getMillis());
        formParams.put("datetime", ts);
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/addAddress", headers,
                                                                    formParams);

        // check that the add of the mbox was not successful
        assertTrue(result.contains("\"success\":false"));
        // check that there is no mailbox with that address
        assertNull(MBox.getByName("bbox", "xcmailr.test"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

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
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/addAddress", headers,
                                                                    formParams);

        // check that the add of the mbox failed and the overview-page is shown
        assertTrue(result.contains("\"success\":false"));
        // check that there is no mailbox with that address
        assertNull(MBox.getByName("$$³@@@..", "xcmailr.test"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: Try to add a Box with a local part that is more than 64chars long
         */
        // add the box
        formParams.put("address", "a12345678901234567890123456789012345678901234567890123456789012345678901234567890a");
        formParams.put("domain", "xcmailr.test");
        formParams.put("datetime", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/addAddress", headers,
                                                                    formParams);

        // check that the add of the mbox failed and the overview-page is shown
        assertTrue(result.contains("\"success\":false"));
        // check that there is a mailbox with that address
        assertNull(MBox.getByName("a12345678901234567890123456789012345678901234567890123456789012345678901234567890a",
                                  "xcmailr.test"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

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
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/addAddress", headers,
                                                                    formParams);

        // check that the add of the mbox failed and the overview-page is shown
        assertTrue(result.contains("\"success\":false"));
        // check that there is no mailbox with that address
        assertNull(MBox.getByName("a12345678901234567890123456789012345678901234567890123456789012345678901234567890a",
                                  "a12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                                      + "aa12345678901234567890123456789012345678901234567890123456789012345678901234567890aa"
                                      + "12345678901234567890123456789012345678901234567890123456789012345678901234567890a.test"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

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
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/addAddress", headers,
                                                                    formParams);

        // check that the add of the mbox failed and the overview-page is shown
        assertTrue(result.contains("\"success\":false"));
        // check that there is no mailbox with that address
        assertNull(MBox.getByName("abox", "xcmlr.abc"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: Try to add a Box with a domain which contains special-chars, etc.
         */
        // add the box
        formParams.put("address", "abox");
        formParams.put("domain", "xcmlr@a.abc");
        formParams.put("datetime", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/addAddress", headers,
                                                                    formParams);

        // check that the add of the mbox has failed
        assertTrue(result.contains("\"success\":false"));
        // check that there is no mailbox with that address
        assertNull(MBox.getByName("abox", "xcmlr@a.abc"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

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
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/addAddress", headers,
                                                                    formParams);
        MBox mb = MBox.getByName("abox", "xcmailr.test");
        assertNotNull(mb);
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        formParams.clear();
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/delete/" + mb.getId(),
                                                                    headers, formParams);
        // verify that the action was successful
        assertTrue(result.contains("\"success\":true"));
        // verify that the box was deleted from the db
        assertNull(MBox.getByName("abox", "xcmailr.test"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

    }

    @Test
    public void testShowBoxDialogs()
    {
        /*
         * The returned page should have a form with prepopulated values
         */
         result = ninjaTestBrowser.makeRequest(getServerAddress() + "/mail/editBoxDialog.html");
         assertFalse(result.contains("FreeMarker template error"));
         assertFalse(result.contains("<title>404 - not found</title>"));
         
         // load the data for an AddBox-Dialog
         result = ninjaTestBrowser.makeRequest(getServerAddress() + "/mail/addAddressData");
         assertFalse(result.contains("FreeMarker template error"));
         assertFalse(result.contains("<title>404 - not found</title>"));
         assertTrue(result.contains("\"currentBox\""));
         
         result = ninjaTestBrowser.makeRequest(getServerAddress() + "/mail/deleteBoxDialog.html");
         assertFalse(result.contains("FreeMarker template error"));
         assertFalse(result.contains("<title>404 - not found</title>"));
         
         result = ninjaTestBrowser.makeRequest(getServerAddress() + "/mail/newDateDialog.html");
         assertFalse(result.contains("FreeMarker template error"));
         assertFalse(result.contains("<title>404 - not found</title>"));
         
         result = ninjaTestBrowser.makeRequest(getServerAddress() + "/mail");
         assertFalse(result.contains("FreeMarker template error"));
         assertFalse(result.contains("<title>404 - not found</title>"));
         
         //get all addresses
         MBox mb = new MBox("moh", "xcmailr.test", 0, false, user);
         mb.save();
         result = ninjaTestBrowser.makeRequest(getServerAddress() + "/mail/getmails");
         assertFalse(result.contains("FreeMarker template error"));
         assertFalse(result.contains("<title>404 - not found</title>"));
         //show all mails as txt
         result = ninjaTestBrowser.makeRequest(getServerAddress() + "/mail/mymaillist.txt");
         assertFalse(result.contains("FreeMarker template error"));
         assertFalse(result.contains("<title>404 - not found</title>"));
         assertTrue(result.contains("moh@xcmailr.test"));

         //show the active mails as txt
         result = ninjaTestBrowser.makeRequest(getServerAddress() + "/mail/myactivemaillist.txt");
         assertFalse(result.contains("FreeMarker template error"));
         assertFalse(result.contains("<title>404 - not found</title>"));
         assertTrue(result.contains("moh@xcmailr.test"));
         //get all boxes
         result = ninjaTestBrowser.makeRequest(getServerAddress() + "/mail/domainlist");
         assertFalse(result.contains("FreeMarker template error"));
         assertFalse(result.contains("<title>404 - not found</title>"));
         
         
    }

    @Test
    public void testEditBox()
    {
        /*
         * TEST: the shown edit-box-page
         */
        // create a new unlimited email-forward (mbox) for editing
        MBox mailbox = new MBox("abcdefg", "xcmailr.test", 0L, false, user);
        mailbox.save();
        Long mailBoxId = mailbox.getId() + 1;
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
        formParams.clear();
        JsonMBox jsm = new JsonMBox();
        jsm.setAddress("fdjskla");
        jsm.setDatetime("0");
        jsm.setDomain("xcmailr.test");
        jsm.setId(mailBoxId);
        jsm.setExpired(false);
        jsm.setForwards(0);
        jsm.setSuppressions(0);

        result = ninjaTestBrowser.postJson(getServerAddress() + "/mail/edit/" + mailBoxId, jsm);
        // verify that the action was not successful

        assertTrue(result.contains("\"success\":false"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        formParams.clear();

        /*
         * TEST: Form with a wrong domain
         */
        jsm.prepopulateJS(mailbox);
        jsm.setDomain("xcmailr.null");
        result = ninjaTestBrowser.postJson(getServerAddress() + "mail/edit/" + mailbox.getId(), jsm);
        assertNull(MBox.getByName("abcdefg", "xcmailr.null"));
        assertTrue(result.contains("\"success\":false"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: Form with errors
         */
        jsm.setAddress(null);
        jsm.setDomain("xcmailr.test");
        result = ninjaTestBrowser.postJson(getServerAddress() + "mail/edit/" + mailbox.getId(), jsm);
        assertTrue(result.contains("\"success\":false"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: correct edit of a box
         */
        // change the local-part of the mbox and the timestamp
        jsm.prepopulateJS(mailbox);
        jsm.setAddress("abcde");
        DateTime dt = new DateTime().plusHours(1);
        jsm.setDatetime(HelperUtils.parseStringTs(dt.getMillis()));

        result = ninjaTestBrowser.postJson(getServerAddress() + "mail/edit/" + mailbox.getId(), jsm);
        assertTrue(result.contains("\"success\":true"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: a wrong timestamp
         */

        jsm.setDatetime("01-01-00");
        result = ninjaTestBrowser.postJson(getServerAddress() + "mail/edit/" + mailbox.getId(), jsm);
        assertTrue(result.contains("\"success\":false"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: edit of an mbox that does not belong to this user
         */

        User user2 = new User("fName", "sName", "eMail@xcmailr.test", "1234", "en");
        user2.save();
        MBox mbox2 = new MBox("mbox2", "xcmailr.test", 0L, false, user2);
        mbox2.save();
        jsm.prepopulateJS(mbox2);

        // try to edit this box (POST to /mail/edit/id)
        result = ninjaTestBrowser.postJson(getServerAddress() + "mail/edit/" + mailbox.getId(), jsm);
        assertTrue(result.contains("\"success\":false"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

    }

    @Test
    public void testExpireBox()
    {
        /*
         * TEST: expire a box normally
         */
        // create a new unlimited email-forward (mbox) to expire
        MBox mailbox = new MBox("abcdefg", "xcmailr.test", 0L, false, user);
        mailbox.save();
        formParams.clear();
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/expire/"
                                                                        + mailbox.getId(), headers, formParams);

        assertTrue(result.contains("\"success\":true"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: expire/reactivate a box with a timestamp in the past
         */
        // create a new mail address(mbox) to expire
        DateTime dt = DateTime.now().minusHours(2);
        MBox mailbox2 = new MBox("abcdefg", "xcmailr.test", dt.getMillis(), false, user);
        mailbox2.save();
        formParams.clear();
        // we want to reactivate the box and expect the edit-page then (because its expired)
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/expire/"
                                                                        + mailbox2.getId(), headers, formParams);
        assertTrue(result.contains("\"success\":false"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
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
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "mail/reset/"
                                                                        + mailbox.getId(), headers, formParams);
        MBox mailbox2 = MBox.getById(mailbox.getId());

        assertEquals(0, mailbox2.getSuppressions());
        assertEquals(0, mailbox2.getForwards());
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        assertTrue(result.contains("\"success\":true"));

    }

    @Test
    public void testBulkReset()
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
        Map<Long, Boolean> testmap = new HashMap<Long, Boolean>();
        testmap.put(mailbox1.getId(), true);
        testmap.put(mailbox2.getId(), true);
        testmap.put(mailbox3.getId(), true);

        result = ninjaTestBrowser.postJson(getServerAddress() + "/mail/bulkReset", testmap);

        assertTrue(result.contains("\"success\":true"));

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
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
    }

    @Test
    public void testBulkEnablePossible()
    {
        MBox mailbox1 = new MBox("mail1", "xcmailr.test", 0L, false, user);
        mailbox1.save();
        MBox mailbox2 = new MBox("mail2", "xcmailr.test", 0L, false, user);
        mailbox2.save();
        MBox mailbox3 = new MBox("mail3", "xcmailr.test", 0L, false, user);
        mailbox3.save();

        mailbox1.enable();
        mailbox2.enable();
        mailbox3.enable();
        // check that the boxes are disabled
        assertFalse(mailbox1.isActive());
        assertFalse(mailbox2.isActive());
        assertFalse(mailbox3.isActive());

        formParams.clear();
        // generate the boxid-map
        Map<Long, Boolean> testmap = new HashMap<Long, Boolean>();
        testmap.put(mailbox1.getId(), true);
        testmap.put(mailbox2.getId(), true);
        testmap.put(mailbox3.getId(), true);

        // try to enable the boxes via controller method
        result = ninjaTestBrowser.postJson(getServerAddress() + "/mail/bulkEnablePossible", testmap);
        // update the boxes
        MBox mailbox1new = MBox.getById(mailbox1.getId());
        MBox mailbox2new = MBox.getById(mailbox2.getId());
        MBox mailbox3new = MBox.getById(mailbox3.getId());

        // check for a successful change
        assertTrue(result.contains("\"success\":true"));
        // the boxes should now be active
        assertTrue(mailbox1new.isActive());
        assertTrue(mailbox2new.isActive());
        assertTrue(mailbox3new.isActive());

        // try to disable the boxes via controller method
        result = ninjaTestBrowser.postJson(getServerAddress() + "/mail/bulkEnablePossible", testmap);
        // update the boxes
        mailbox1 = MBox.getById(mailbox1.getId());
        mailbox2 = MBox.getById(mailbox2.getId());
        mailbox3 = MBox.getById(mailbox3.getId());
        // check that the boxes are still active
        assertTrue(mailbox1.isActive());
        assertTrue(mailbox2.isActive());
        assertTrue(mailbox3.isActive());

        // check the original timestamp
        assertEquals(0L, mailbox1.getTs_Active());
        assertEquals(0L, mailbox2.getTs_Active());
        assertEquals(0L, mailbox3.getTs_Active());
        assertTrue(result.contains("\"success\":true"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

    }
    @Test
    public void testBulkDisable()
    {
        MBox mailbox1 = new MBox("mail1", "xcmailr.test", 0L, false, user);
        mailbox1.save();
        MBox mailbox2 = new MBox("mail2", "xcmailr.test", 0L, false, user);
        mailbox2.save();
        MBox mailbox3 = new MBox("mail3", "xcmailr.test", 0L, false, user);
        mailbox3.save();

        mailbox3.enable();
        // check that the boxes are enabled (except mailbox3)
        assertTrue(mailbox1.isActive());
        assertTrue(mailbox2.isActive());
        assertFalse(mailbox3.isActive());

        // generate the boxid-map
        Map<Long, Boolean> testmap = new HashMap<Long, Boolean>();
        testmap.put(mailbox1.getId(), true);
        testmap.put(mailbox2.getId(), true);
        testmap.put(mailbox3.getId(), true);

        // try to enable the boxes via controller method
        result = ninjaTestBrowser.postJson(getServerAddress() + "/mail/bulkDisable", testmap);
        // update the boxes
        MBox mailbox1new = MBox.getById(mailbox1.getId());
        MBox mailbox2new = MBox.getById(mailbox2.getId());
        MBox mailbox3new = MBox.getById(mailbox3.getId());

        // check for a successful change
        assertTrue(result.contains("\"success\":true"));
        // the boxes should now be inactive
        assertFalse(mailbox1new.isActive());
        assertFalse(mailbox2new.isActive());
        assertFalse(mailbox3new.isActive());

        // try to disable the boxes via controller method again
        result = ninjaTestBrowser.postJson(getServerAddress() + "/mail/bulkDisable", testmap);
        // update the boxes
        mailbox1 = MBox.getById(mailbox1.getId());
        mailbox2 = MBox.getById(mailbox2.getId());
        mailbox3 = MBox.getById(mailbox3.getId());
        
        // check that the boxes are still inactive
        assertFalse(mailbox1.isActive());
        assertFalse(mailbox2.isActive());
        assertFalse(mailbox3.isActive());

        // check the original timestamp
        assertEquals(0L, mailbox1.getTs_Active());
        assertEquals(0L, mailbox2.getTs_Active());
        assertEquals(0L, mailbox3.getTs_Active());
        assertTrue(result.contains("\"success\":true"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

    }
    
    @Test
    public void testBulkNewDate()
    {
        MBox mailbox1 = new MBox("mail1", "xcmailr.test", 0L, false, user);
        mailbox1.save();
        MBox mailbox2 = new MBox("mail2", "xcmailr.test", 0L, false, user);
        mailbox2.save();
        MBox mailbox3 = new MBox("mail3", "xcmailr.test", 0L, false, user);
        mailbox3.save();

        mailbox1.enable();
        mailbox2.enable();
        mailbox3.enable();
        // check that the boxes are disabled
        assertFalse(mailbox1.isActive());
        assertFalse(mailbox2.isActive());
        assertFalse(mailbox3.isActive());

        formParams.clear();
        // generate the boxid-map
        Map<Long, Boolean> testmap = new HashMap<Long, Boolean>();
        testmap.put(mailbox1.getId(), true);
        testmap.put(mailbox2.getId(), true);
        testmap.put(mailbox3.getId(), true);
        
        Map<String, Object> testmap2 = new HashMap<String, Object>();
        testmap2.put("boxes", testmap);
        String timeStamp = HelperUtils.parseStringTs(DateTime.now().plusHours(1).getMillis()); 
        testmap2.put("newDateTime", timeStamp);

        // try to enable the boxes via controller method
        result = ninjaTestBrowser.postJson(getServerAddress() + "/mail/bulkNewDate", testmap2);
        // update the boxes
        MBox mailbox1new = MBox.getById(mailbox1.getId());
        MBox mailbox2new = MBox.getById(mailbox2.getId());
        MBox mailbox3new = MBox.getById(mailbox3.getId());

        // check for a successful change
        assertTrue(result.contains("\"success\":true"));
        // the boxes should now be active
        assertTrue(mailbox1new.isActive());
        assertTrue(mailbox1new.getDatetime().equals(timeStamp));
        assertTrue(mailbox2new.getDatetime().equals(timeStamp));
        assertTrue(mailbox3new.getDatetime().equals(timeStamp));

        assertTrue(result.contains("\"success\":true"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

    }


    @Test
    public void testBulkDelete()
    {
        MBox mailbox1 = new MBox("mail1", "xcmailr.test", 0L, false, user);
        mailbox1.save();
        MBox mailbox2 = new MBox("mail2", "xcmailr.test", 0L, false, user);
        mailbox2.save();
        MBox mailbox3 = new MBox("mail3", "xcmailr.test", 0L, false, user);
        mailbox3.save();

        // generate the boxid-map
        Map<Long, Boolean> testmap = new HashMap<Long, Boolean>();
        testmap.put(mailbox1.getId(), true);
        testmap.put(mailbox2.getId(), true);
        testmap.put(mailbox3.getId(), true);

        // try to enable the boxes via controller method
        result = ninjaTestBrowser.postJson(getServerAddress() + "/mail/bulkDelete", testmap);
        // update the boxes
        MBox mailbox1new = MBox.getById(mailbox1.getId());
        MBox mailbox2new = MBox.getById(mailbox2.getId());
        MBox mailbox3new = MBox.getById(mailbox3.getId());

        // check for a successful change
        assertTrue(result.contains("\"success\":true"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        // the boxes should now be active
        assertNull(mailbox1new);
        assertNull(mailbox2new);
        assertNull(mailbox3new);
    }
}
