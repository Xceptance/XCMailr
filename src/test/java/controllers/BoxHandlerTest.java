package controllers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Map;

import models.MBox;
import models.User;
import ninja.NinjaTest;
import ninja.utils.NinjaTestBrowser;

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

    // TODO make the domain-field in the tests dependend on the settings in the application.conf
    @Before
    public void setUp()
    {
        // create the user (in test mode a volatile in-memory db is used)
        User u = new User("John", "Doe", "admin@ccmailr.test", "1234");
        u.setActive(true);
        u.save();

        formParams.clear();
        headers.clear();
        returnedData.clear();
        formParams.clear();

        // login
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pwd", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/login", headers, formParams);
        
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
        formParams.put("duration", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/mail/add", headers,
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
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/login", headers, formParams);
        formParams.clear();

        // add the box
        formParams.put("address", "abox");
        formParams.put("domain", "xcmailr.test");
        formParams.put("duration", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/mail/add", headers,
                                                                    formParams);

        // check that the add of the mbox was successful
        assertTrue(result.contains("class=\"success\">"));
        // check that there is a mailbox with that address
        assertNotNull(MBox.getByName("abox", "xcmailr.test"));
        // TODO check the data of the mbox

        /*
         * TEST: Try to add a box with the same data again
         */

        // add the box
        formParams.put("address", "abox");
        formParams.put("domain", "xcmailr.test");
        formParams.put("duration", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/mail/add", headers,
                                                                    formParams);

        // check that the add of the mbox was successful
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: try to add a box with no data
         */

        // add the box
        formParams.put("address", "");
        formParams.put("domain", "");
        formParams.put("duration", "");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/mail/add", headers,
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
        formParams.put("duration", "2x,3d");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/mail/add", headers,
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
        formParams.put("duration", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/mail/add", headers,
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
        formParams.put("duration", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/mail/add", headers,
                                                                    formParams);

        // check that the add of the mbox was successful
        assertTrue(result.contains("class=\"error\">"));
        // check that there is a mailbox with that address
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
        formParams.put("duration", "0");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/mail/add", headers,
                                                                    formParams);
        MBox mb = MBox.getByName("abox", "xcmailr.test");
        assertNotNull(mb);

        formParams.clear();
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/mail/delete/" + mb.getId(),
                                                                    headers, formParams);
        // verify that the overview-page is shown
        assertTrue(result.contains("<!--mailboxoverview-->"));
        // verify that the box was deleted from the db
        assertNull(MBox.getByName("abox", "xcmailr.test"));

    }

}
