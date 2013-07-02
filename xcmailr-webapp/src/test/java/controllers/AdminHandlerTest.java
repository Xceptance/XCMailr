package controllers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.http.cookie.Cookie;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.Maps;

import models.MailTransaction;
import models.User;
import ninja.NinjaTest;

public class AdminHandlerTest extends NinjaTest
{
    Map<String, String> headers = Maps.newHashMap();

    Map<String, String> formParams = Maps.newHashMap();

    String result;

    User admin;

    @Before
    public void setUp()
    {

        // get the adminaccount and login
        headers.put("Accept-Language", "en-US");
        admin = User.getUsrByMail("admin@xcmailr.test");
        formParams.put("mail", "admin@xcmailr.test");
        formParams.put("password", "1234");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress() + "/login",
                                                                    headers, formParams);
        // make sure that the success-page is displayed

        assertTrue(result.contains("class=\"alert alert-success\">"));

        // check the cookie
        Cookie cookie = ninjaTestBrowser.getCookieWithName("XCMailr_SESSION");

        assertTrue(cookie != null);
        assertTrue(cookie.getValue().contains("___TS"));
        assertTrue(cookie.getValue().contains("username"));

        formParams.clear();

    }

    @After
    public void tearDown()
    {

    }

    @Test
    public void testAccountActivation()
    {

        // "register" a new user
        User testUser = new User("test", "user", "testuser@xcmailr.test", "1234", "en");
        testUser.save();

        /*
         * TEST: first, the user is not active, then we activate him via the admin-menu and he's active then
         */

        assertFalse(testUser.isActive());
        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                        + "admin/activate/" + testUser.getId(),
                                                                    headers,
                                                                    formParams);
        testUser = User.getUsrByMail("testuser@xcmailr.test");
        assertTrue(testUser.isActive());
        // deactivate
        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                        + "admin/activate/" + testUser.getId(),
                                                                    headers,
                                                                    formParams);
        testUser = User.getUsrByMail("testuser@xcmailr.test");

        assertFalse(testUser.isActive());

        /*
         * TEST: promote and demote the testuser
         */
        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                        + "admin/promote/" + testUser.getId(), headers,
                                                                    formParams);

        testUser = User.getUsrByMail("testuser@xcmailr.test");
        assertTrue(testUser.isAdmin());
        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                        + "admin/promote/" + testUser.getId(), headers,
                                                                    formParams);
        testUser = User.getUsrByMail("testuser@xcmailr.test");
        assertFalse(testUser.isAdmin());

        /*
         * TEST: delete the testuser
         */
        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                        + "admin/delete/" + testUser.getId(), headers,
                                                                    formParams);
        testUser = User.getUsrByMail("testuser@xcmailr.test");
        assertNull(testUser);
        /*
         * TEST: We should not be able to deactivate our own account
         */

        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                        + "admin/activate/" + admin.getId(), headers,
                                                                    formParams);
        admin = User.getById(admin.getId());
        assertTrue(admin.isActive());

        /*
         * TEST: We should not be able to delete our own account
         */

        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                        + "admin/delete/" + admin.getId(), headers,
                                                                    formParams);
        admin = User.getById(admin.getId());
        assertNotNull(admin);

        /*
         * TEST: We should not be able to demote our own account (as last admin)
         */

        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                        + "admin/promote/" + admin.getId(), headers,
                                                                    formParams);
        admin = User.getById(admin.getId());
        assertTrue(admin.isAdmin());

    }

    @Test
    public void showStatistics()
    {

        result = ninjaTestBrowser.makeRequest(getServerAddress() + "admin/users");
        assertTrue(result.contains("<li class=\"active\"><a href=\"/admin/users\">"));

        result = ninjaTestBrowser.makeRequest(getServerAddress() + "admin/summedtx");
        assertTrue(result.contains("<li class=\"active\"><a href=\"/admin/summedtx\">"));

        result = ninjaTestBrowser.makeRequest(getServerAddress() + "admin/mtxs");
        assertTrue(result.contains("<li class=\"active\"><a href=\"/admin/mtxs\">"));

    }

    @Test
    public void testShowAdmin()
    {
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "admin/");
        assertTrue(!result.contains("<li class=\"active\">"));
    }

    @Test
    public void testDeleteMTX()
    {
        // create some transactions
        MailTransaction mtx1 = new MailTransaction(200, "test@abc", "", "somewhere");
        MailTransaction mtx2 = new MailTransaction(100, "tsest@abc", "", "somewhere");
        mtx1.saveTx();
        mtx2.saveTx();
        // delete all entries
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "admin/mtxs/delete/-1");
        // check if they're gone
        mtx1 = MailTransaction.getById(mtx1.getId());
        mtx2 = MailTransaction.getById(mtx2.getId());

        assertNull(mtx1);
        assertNull(mtx2);

    }

}
