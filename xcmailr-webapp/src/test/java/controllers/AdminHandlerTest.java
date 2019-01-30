package controllers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import java.sql.Date;
import java.util.Map;

import org.apache.http.cookie.Cookie;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.avaje.ebean.Ebean;
import com.google.common.collect.Maps;

import models.Domain;
import models.MailStatistics;
import models.MailStatisticsKey;
import models.MailTransaction;
import models.User;
import ninja.NinjaTest;
import ninja.utils.NinjaMode;
import ninja.utils.NinjaProperties;
import ninja.utils.NinjaPropertiesImpl;

@RunWith(MockitoJUnitRunner.class)
public class AdminHandlerTest extends NinjaTest
{
    Map<String, String> headers = Maps.newHashMap();

    Map<String, String> formParams = Maps.newHashMap();

    NinjaProperties ninjaProperties;

    String result;

    User admin;

    @Before
    public void setUp()
    {
        // get the admin-account from the application.conf-file
        ninjaProperties = spy(new NinjaPropertiesImpl(NinjaMode.test));
        String adminAccName = ninjaProperties.get("mbox.adminaddr");
        String adminPassword = ninjaProperties.get("admin.pass");

        // get the adminaccount and login
        headers.put("Accept-Language", "en-US");
        admin = User.getUsrByMail(adminAccName);
        formParams.put("mail", adminAccName);
        formParams.put("password", adminPassword);

        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress() + "/login",
                                                                    headers, formParams);
        // make sure that the success-page is displayed

        assertTrue(result.contains("class=\"alert alert-success\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
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
                                                                    + "admin/activate/" + testUser.getId(), headers,
                                                                    formParams);
        testUser = User.getUsrByMail("testuser@xcmailr.test");
        assertTrue(testUser.isActive());
        // deactivate
        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                    + "admin/activate/" + testUser.getId(), headers,
                                                                    formParams);
        testUser = User.getUsrByMail("testuser@xcmailr.test");

        assertFalse(testUser.isActive());
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
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
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        /*
         * TEST: delete the testuser
         */
        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress() + "admin/delete/"
                                                                    + testUser.getId(), headers, formParams);
        testUser = User.getUsrByMail("testuser@xcmailr.test");
        assertNull(testUser);
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        /*
         * TEST: We should not be able to deactivate our own account
         */

        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                    + "admin/activate/" + admin.getId(), headers,
                                                                    formParams);
        admin = User.getById(admin.getId());
        assertTrue(admin.isActive());
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        /*
         * TEST: We should not be able to delete our own account
         */

        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress() + "admin/delete/"
                                                                    + admin.getId(), headers, formParams);
        admin = User.getById(admin.getId());
        assertNotNull(admin);
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        /*
         * TEST: We should not be able to demote our own account (as last admin)
         */

        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                    + "admin/promote/" + admin.getId(), headers,
                                                                    formParams);
        admin = User.getById(admin.getId());
        assertTrue(admin.isAdmin());
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
    }

    @Test
    public void showStatistics()
    {
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "admin/users");
        assertTrue(result.contains("<a class=\"list-group-item active view-user\" href=\"/admin/users\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "admin/summedtx");
        assertTrue(result.contains("<a class=\"list-group-item active show-summedStatistics\" href=\"/admin/summedtx\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "admin/mtxs");
        assertTrue(result.contains("<a class=\"list-group-item active show-transactions\" href=\"/admin/mtxs\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

    }

    @Test
    public void testShowAdmin()
    {
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "admin");
        assertTrue(!result.contains("<li class=\"active\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
    }

    @Test
    public void testDeleteMTX()
    {
        // create some transactions
        MailTransaction mtx1 = new MailTransaction(200, "test@abc", "", "somewhere");
        MailTransaction mtx2 = new MailTransaction(100, "tsest@abc", "", "somewhere");
        mtx1.save();
        mtx2.save();
        // delete all entries
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "admin/mtxs/delete/-1");
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        // check if they're gone
        mtx1 = MailTransaction.getById(mtx1.getId());
        mtx2 = MailTransaction.getById(mtx2.getId());

        assertNull(mtx1);
        assertNull(mtx2);

    }

    @Test
    public void testJsonUserSearch()
    {
        /*
         * TEST: no search string
         */
        formParams.clear();
        formParams.put("s", "");
        result = ninjaTestBrowser.makeRequest(ninjaTestServer.getServerAddress() + "admin/usersearch?s=");
        // if no search-string was delivered, then the result should be empty
        assertTrue(result.equals("[]"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: one search string
         */
        formParams.clear();
        formParams.put("s", "");
        result = ninjaTestBrowser.makeRequest(ninjaTestServer.getServerAddress() + "admin/usersearch?s=admi");
        // if no search-string was delivered, then the result should be empty
        assertTrue(result.contains("admin@xcmailr.test"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
    }

    @Test
    public void testDomainWhitelist()
    {
        /*
         * TEST: the domain whitelist is empty
         */
        result = ninjaTestBrowser.makeRequest(ninjaTestServer.getServerAddress() + "admin/whitelist");
        assertTrue(result.contains("No domains defined in this whitelist. The registration is open to all domains."));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: add a domain via backend to the whitlist and test for it
         */
        new Domain("foobar.test").save();
        result = ninjaTestBrowser.makeRequest(ninjaTestServer.getServerAddress() + "admin/whitelist");

        assertFalse(result.contains("No domains defined in this whitelist. The registration is open to all domains."));
        assertTrue(result.contains("foobar.test"));

        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
    }

    @Test
    public void testAddWhitelistDomain() throws Exception
    {
        /*
         * TEST: add an empty domain
         */
        formParams.clear();
        formParams.put("domainName", "");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                    + "admin/whitelist/add", headers, formParams);

        assertTrue(result.contains("No domains defined in this whitelist. The registration is open to all domains."));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: add "abc.de" as domain
         */
        formParams.put("domainName", "abc.de");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                    + "admin/whitelist/add", headers, formParams);

        assertTrue(result.contains("abc.de"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: add again "abc.de" as domain
         */
        formParams.put("domainName", "abc.de");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                    + "admin/whitelist/add", headers, formParams);

        assertTrue(result.contains("abc.de"));
        assertTrue(result.lastIndexOf("abc.de") == result.indexOf("abc.de"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: add "123.45" as domain
         */
        formParams.put("domainName", "123.45");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                    + "admin/whitelist/add", headers, formParams);

        assertFalse(result.contains("123.45"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
    }

    @Test
    public void testRemoveWhitelistDomain() throws Exception
    {
        /*
         * TEST: add a domain via backend and remove it via frontend
         */
        Domain domain = new Domain("foobar.test");
        domain.save();

        result = ninjaTestBrowser.makeRequest(ninjaTestServer.getServerAddress()
                                              + "admin/whitelist/remove?action=deleteDomain&domainId="
                                              + String.valueOf(domain.getId()));

        assertTrue(result.contains("No domains defined in this whitelist."));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: add a domain via backend and remove it via frontend
         */
        domain = new Domain("foobar2.test");
        domain.save();

        formParams.put("removeDomainsSelection", String.valueOf(domain.getId()));

        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                    + "admin/whitelist/remove", headers, formParams);

        assertTrue(result.contains("Do you want to delete all users with email addresses containing the domain foobar2.test?"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
    }

    @Test
    public void testShowEmailStatistics() throws Exception
    {
        /*
         * TEST: show email statistics
         */
        result = ninjaTestBrowser.makeRequest(ninjaTestServer.getServerAddress() + "admin/emailStatistics");

        assertTrue(result.contains("Todays (last 24 hours) sender domains"));
        assertTrue(result.contains("Last 7 days sender domains"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: get first page for the day
         */
        result = ninjaTestBrowser.makeJsonRequest(ninjaTestServer.getServerAddress()
                                                  + "admin/emailSenderPage?scope=day&offset=0&limit=10");
        assertTrue("{\"total\":0,\"rows\":[]}".equals(result));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: get first page for the week
         */
        result = ninjaTestBrowser.makeJsonRequest(ninjaTestServer.getServerAddress()
                                                  + "admin/emailSenderPage?scope=week&offset=0&limit=10");

        assertTrue("{\"total\":0,\"rows\":[]}".equals(result));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: get second page for the week
         */
        result = ninjaTestBrowser.makeJsonRequest(ninjaTestServer.getServerAddress()
                                                  + "admin/emailSenderPage?scope=week&offset=10&limit=10");

        assertTrue("{\"total\":0,\"rows\":[]}".equals(result));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: check invalid scope
         */
        result = ninjaTestBrowser.makeJsonRequest(ninjaTestServer.getServerAddress()
                                                  + "admin/emailSenderPage?scope=month&offset=0&limit=10");

        assertTrue("null".equals(result));

        /*
         * TEST: get first page for the day
         */
        MailStatistics mailStatistics = new MailStatistics();
        MailStatisticsKey mailStatisticsKey = new MailStatisticsKey(new Date(System.currentTimeMillis()), 0,
                                                                    "fromDomain.com", "targetDomain.com");
        mailStatistics.setKey(mailStatisticsKey);
        mailStatistics.setDropCount(13);
        mailStatistics.setForwardCount(5);
        Ebean.save(mailStatistics);

        result = ninjaTestBrowser.makeJsonRequest(ninjaTestServer.getServerAddress()
                                                  + "admin/emailSenderPage?scope=day&offset=0&limit=10");

        assertTrue("{\"total\":1,\"rows\":[{\"id\":0,\"fromDomain\":\"fromDomain.com\",\"droppedCount\":13,\"forwardedCount\":5}]}".equals(result));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

    }
}
