package controllers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Map;

import models.User;
import ninja.NinjaTest;
import org.apache.http.cookie.Cookie;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.Maps;

public class UserHandlerTest extends NinjaTest
{

    Map<String, String> headers = Maps.newHashMap();

    Map<String, String> formParams = Maps.newHashMap();

    Map<String, String> returnedData = Maps.newHashMap();

    Map<String, String> userData = Maps.newHashMap();

    String result;

    User user;

    @Before
    public void setUp()
    {
        formParams.clear();
        headers.clear();
        returnedData.clear();

        user = new User("John", "Doe", "admin@localhost.test", "1234", "en");
        user.setActive(true);
        user.save();

        userData.put("firstName", "John");
        userData.put("surName", "Doe");
        userData.put("mail", "admin@localhost.test");
        userData.put("language", "en");
        formParams.clear();
        formParams.put("mail", "admin@localhost.test");
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
    public void testUserEditing()
    {

        /*
         * TEST: Set no firstName
         */
        formParams.clear();
        formParams.put("firstName", "");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@localhost.test");
        formParams.put("password", "1234");
        formParams.put("language", "en");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);

        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed
        assertTrue(result.contains("class=\"alert alert-error\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);

        /*
         * TEST: Set no surname
         */
        formParams.clear();
        formParams.put("firstName", "John");
        formParams.put("surName", "");
        formParams.put("mail", "admin@localhost.test");
        formParams.put("password", "1234");
        formParams.put("language", "en");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed
        assertTrue(result.contains("class=\"alert alert-error\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);

        /*
         * TEST: Set a wrong formatted mail
         */
        formParams.clear();
        formParams.put("firstName", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "@this.de");
        formParams.put("password", "1234");
        formParams.put("language", "en");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed
        assertTrue(result.contains("class=\"alert alert-error\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);

        /*
         * TEST: Set a domain that belongs to this domains
         */
        formParams.clear();
        formParams.put("firstName", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "abc@xcmailr.test");
        formParams.put("password", "1234");
        formParams.put("language", "en");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed
        assertTrue(result.contains("class=\"alert alert-error\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);

        /*
         * TEST: Set a wrong formatted mail, again
         */
        formParams.clear();
        formParams.put("firstName", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin.this.de");
        formParams.put("password", "1234");
        formParams.put("language", "en");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed
        assertTrue(result.contains("class=\"alert alert-error\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);

        /*
         * TEST: set no passwd
         */
        formParams.clear();
        formParams.put("firstName", "Johnny");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@localhost.test");
        formParams.put("password", "");
        formParams.put("language", "en");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed
        assertTrue(result.contains("class=\"alert alert-error\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);

        /*
         * TEST: set wrong passwd (reversed order)
         */
        formParams.clear();
        formParams.put("firstName", "Johnny");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@localhost.test");
        formParams.put("password", "4321");
        formParams.put("language", "en");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed
        assertTrue(result.contains("class=\"alert alert-error\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);

        /*
         * TEST: set wrong passwd (completely different chars & length)
         */
        formParams.clear();
        formParams.put("firstName", "Johnny");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@localhost.test");
        formParams.put("password", "abcdef");
        formParams.put("language", "en");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);

        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed
        assertTrue(result.contains("class=\"alert alert-error\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);

        /*
         * TEST: set too short new password
         */
        formParams.clear();
        formParams.put("firstName", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@localhost.test");
        formParams.put("password", "1234");
        formParams.put("passwordNew1", "1");
        formParams.put("passwordNew2", "1");
        formParams.put("language", "en");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);

        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed

        assertTrue(result.contains("class=\"alert alert-error\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);
        
        /*
         * TEST: Try to change the mail to an existing address
         */
        User anotherUser = new User("first", "last", "user@localhost.test", "1234", "en");
        anotherUser.save();
        
        formParams.clear();
        formParams.put("firstName", "Johnny");
        formParams.put("surName", "Doey");
        formParams.put("mail", "user@localhost.test");
        formParams.put("password", "1234");
        formParams.put("language", "en");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed

        assertTrue(result.contains("class=\"alert alert-error\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));  
        
        /*
         * TEST: Edit the Userdata correctly (fore- and surname only)
         */
        formParams.clear();
        formParams.put("firstName", "Johnny");
        formParams.put("surName", "Doey");
        formParams.put("mail", "admin@localhost.test");
        formParams.put("password", "1234");
        formParams.put("language", "en");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        returnedData = HtmlUtils.readInputFormData(result);

        // check if the userdata-edit has been successfully changed
        assertTrue(result.contains("class=\"alert alert-success\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        // the returned data should now be equal to the formparams without the password
        formParams.remove("password");

        TestUtils.testMapEntryEquality(formParams, returnedData);

        /*
         * TEST: Edit the Userdata correctly (fore- and surname and passwords)
         */
        formParams.clear();
        formParams.put("firstName", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@localhost.test");
        formParams.put("password", "1234");
        formParams.put("passwordNew1", "4321");
        formParams.put("passwordNew2", "4321");
        formParams.put("language", "en");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        returnedData = HtmlUtils.readInputFormData(result);

        // check if the userdata-edit has been successfully changed
        assertTrue(result.contains("class=\"alert alert-success\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        // the returned data should now be equal to the formparams without the password
        formParams.remove("password");
        formParams.remove("passwordNew1");
        formParams.remove("passwordNew2");

        TestUtils.testMapEntryEquality(formParams, returnedData);

    }

    @Test
    public void testUserWrongNewPws()
    {
        /*
         * TEST: Edit the Userdata with two new passwords which were not equal
         */
        formParams.clear();
        formParams.put("firstName", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@localhost.test");
        formParams.put("password", "1234");
        formParams.put("passwordNew1", "4321");
        formParams.put("passwordNew2", "1234");
        formParams.put("language", "en");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        returnedData = HtmlUtils.readInputFormData(result);

        // check if the userdata-edit has been successfully changed

        assertTrue(result.contains("class=\"alert alert-error\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));
        // the returned data should now be equal to the formparams without the password
        formParams.remove("password");
        formParams.remove("passwordNew1");
        formParams.remove("passwordNew2");

        TestUtils.testMapEntryEquality(formParams, returnedData);
    }

    @Test
    public void testDeleteUser()
    {

        /*
         * TEST: try a delete with a wrong password
         */
        formParams.clear();
        formParams.put("password", "1222");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/delete", headers,
                                                                    formParams);
        User user2 = User.getById(user.getId());

        assertTrue(user2 != null);
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: try a delete with no password
         */
        formParams.clear();
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/delete", headers,
                                                                    formParams);
        user2 = User.getById(user.getId());

        assertTrue(user2 != null);
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: correct deletion
         */
        formParams.clear();
        formParams.put("password", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/delete", headers,
                                                                    formParams);
        user2 = User.getById(user.getId());

        assertNull(user2);
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        /*
         * TEST: try to delete the last admin-account
         */
        // after the deletion of the user, we should now be logged out and there should be no cookie
        Cookie cookie = ninjaTestBrowser.getCookieWithName("XCMailr_SESSION");
        assertNull(cookie);
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        // login with the admin-account
        formParams.clear();
        formParams.put("mail", "admin@xcmailr.test");
        formParams.put("password", "1234");
        user2 = User.getUsrByMail("admin@xcmailr.test");
        assertTrue(user2.isLastAdmin());
        
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/login", headers, formParams);
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

        // now try to delete the account
        formParams.clear();
        formParams.put("password", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/delete", headers,
                                                                    formParams);
        user2 = User.getUsrByMail("admin@xcmailr.test");

        assertTrue(user2 != null);
        assertTrue(result.contains("class=\"alert alert-error\">"));
        assertFalse(result.contains("FreeMarker template error"));
        assertFalse(result.contains("<title>404 - not found</title>"));

    }

}
