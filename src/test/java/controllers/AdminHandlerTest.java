package controllers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.h2.constant.SysProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.Maps;
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

    }

    @After
    public void tearDown()
    {

    }

    @Test
    public void testAccountActivation()
    {
        // get the adminaccount
        // TODO get this by application.conf
        admin = User.getUsrByMail("admin@xcmailr.test");
        formParams.put("mail", "admin@xcmailr.test");
        formParams.put("pwd", "1234");

        ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress() + "/login", headers,
                                                           formParams);

        // register a new user
        User testuser = new User("test", "user", "testuser@xcmailr.test", "1234");
        testuser.save();

        /*
         * TEST: first, the user is not active, then we activate him via the admin-menu and he's active then
         */

        assertFalse(testuser.isActive());
        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                        + "/admin/activate/" + testuser.getId(),
                                                                    headers,
                                                                    formParams);

        testuser = User.getUsrByMail("testuser@xcmailr.test");        
        assertTrue(testuser.isActive());

        /*
         * TEST: We should not be able to deactivate our own account
         */

        result = ninjaTestBrowser.makePostRequestWithFormParameters(ninjaTestServer.getServerAddress()
                                                                        + "/admin/activate/" + admin.getId(), headers,
                                                                    formParams);
        admin = User.getById(admin.getId());

        assertTrue(admin.isActive());

    }

}
