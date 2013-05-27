package controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

import etc.HelperUtils;

public class ApplicationHandlerTest extends NinjaTest
{
    Map<String, String> headers = Maps.newHashMap();

    Map<String, String> formParams = Maps.newHashMap();

    String result;

    @Before
    public void setUp()
    {
        formParams.clear();
        headers.clear();
        headers.put("Accept-Language", "en-US");
    }

    @After
    public void tearDown()
    {

    }

    @Test
    public void testRegistrationPart()
    {
        // TEST: show the registration page
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "register");
        assertTrue(result.contains("form action=\"/register\""));

        /*
         * TEST: Register a new user correctly
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "1234");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "register", headers,
                                                                    formParams);

        // check if the user has been registered successfully
        assertTrue(result.contains("class=\"success\""));
        assertNotNull(User.getUsrByMail("admin@ccmailr.test"));

        /*
         * TEST: try to register this address again
         */
        formParams.put("forename", "Nhoj");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "register", headers,
                                                                    formParams);

        // check if the user has been registered successfully
        assertTrue(result.contains("class=\"error\""));
        User user = User.getUsrByMail("admin@ccmailr.test");
        assertTrue(user.getForename().equals("John"));

        /*
         * ------------------------------------------------------------------------------------------
         * 
         * TESTBLOCK:try to register with one or all of the fields unset
         * 
         * ------------------------------------------------------------------------------------------
         */

        /*
         * TEST: All fields unset
         */
        formParams.clear();
        formParams.put("forename", "");
        formParams.put("surName", "");
        formParams.put("mail", "");
        formParams.put("pw", "");
        formParams.put("pwn1", "");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldn't be registered
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: no forename
         */
        formParams.clear();
        formParams.put("forename", "");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldn't be registered
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: no surname
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldn't be registered
        assertTrue(result.contains("class=\"error\">"));
    }

    @Test
    public void testRegistrationMails()
    {

        /*
         * TEST: no mail-address
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldn't be registered
        assertTrue(result.contains("class=\"error\""));

        /*
         * TEST: wrong formatted mail
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin.this.de");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldn't be registered
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: wrong formatted mail2
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "@this.de");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldn't be registered
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: wrong formatted mail3
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "@");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldn't be registered
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: wrong formatted mail4
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "blubb@");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldn't be registered
        assertTrue(result.contains("class=\"error\">"));

    }

    @Test
    public void testRegistrationPasswords()
    {
        /*
         * TEST: no password
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "");
        formParams.put("pwn1", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldn't be registered
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: no password repetition
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldn't be registered
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: passwords didn't match
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "4321");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldn't be registered
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: different passwords with upper and lower-cases
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "Cats");
        formParams.put("pwn1", "cats");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldn't be registered
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: different passwords with upper and lower-cases (another way)
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "dogs");
        formParams.put("pwn1", "Dogs");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldn't be registered
        assertTrue(result.contains("class=\"error\">"));

    }

    @Test
    public void testVerification()
    {
        // register the user
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "1234");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "register", headers,
                                                                    formParams);
        // check if the user has been registered successfully
        assertTrue(result.contains("class=\"success\""));
        User user = User.getUsrByMail("admin@ccmailr.test");
        assertNotNull(user);
        // the user should be inactive
        assertFalse(user.isActive());

        /*
         * TEST: wrong verification-data
         */
        String random = HelperUtils.getRndString(5);
        // generate a new random string until its not equal to the confirmation-code
        while (user.getConfirmation().equals(random))
        {
            random = HelperUtils.getRndString(5);
        }

        result = ninjaTestBrowser.makeRequest(getServerAddress() + "verify/" + user.getId() + "/" + random);
        // the verification must not be successful
        // we expect the index-page
        String expected = ninjaTestBrowser.makeRequest(getServerAddress() + "/");
        assertTrue(result.equals(expected));

        User updateduser = User.getById(user.getId());
        // the user should not be active
        assertFalse(updateduser.isActive());

        /*
         * TEST: correct verification-data
         */
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "verify/" + user.getId() + "/"
                                              + user.getConfirmation());
        // the verification should be successful
        assertTrue(result.contains("class=\"success\""));

        updateduser = User.getById(user.getId());
        // the user should now be active
        assertTrue(updateduser.isActive());

    }

    @Test
    public void testGetLostPw()
    {
        // register the user
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "register", headers,
                                                                    formParams);
        // check if the user has been registered successfully
        User user = User.getUsrByMail("admin@ccmailr.test");
        // the user should be inactive
        assertFalse(user.isActive());

        /*
         * TEST: Get the lost-pw-form (correct confirm-token and userid)
         */
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "lostpw/" + user.getId() + "/"
                                              + user.getConfirmation());
        assertTrue(result.contains("form action=\"/lostpw"));

        /*
         * TEST: wrong verification-data
         */
        String random = HelperUtils.getRndString(5);
        // generate a new random string until its not equal to the confirmation-code
        while (user.getConfirmation().equals(random))
        {
            random = HelperUtils.getRndString(5);
        }

        result = ninjaTestBrowser.makeRequest(getServerAddress() + "lostpw/" + user.getId() + "/" + random);
        // the verification must not be successful
        // we expect the index-page
        String expected = ninjaTestBrowser.makeRequest(getServerAddress() + "/");
        assertTrue(result.equals(expected));

        User updateduser = User.getById(user.getId());
        // the user should not be active
        assertFalse(updateduser.isActive());

    }

    @Test
    public void testLostPwForm()
    {
        // register the user
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "register", headers,
                                                                    formParams);
        // check if the user has been registered successfully
        User user = User.getUsrByMail("admin@ccmailr.test");
        // the user should be inactive
        assertFalse(user.isActive());

        /*
         * TEST: Form-Error (new password not set)
         */
        formParams.clear();
        formParams.put("pw2", "abc");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "lostpw/" + user.getId() + "/"
                                                                    + user.getConfirmation(), headers, formParams);
        assertTrue(result.contains("class=\"error\""));
        assertTrue(result.contains("form action=\"/lostpw"));

        /*
         * TEST: unequal passwords
         */
        formParams.clear();
        formParams.put("pw", "123");
        formParams.put("pw2", "abc");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "lostpw/" + user.getId() + "/"
                                                                    + user.getConfirmation(), headers, formParams);
        assertTrue(result.contains("class=\"error\""));
        assertTrue(result.contains("form action=\"/lostpw"));

        User updateuser = User.getById(user.getId());
        assertFalse(updateuser.isActive());

        /*
         * TEST: correct data (equal passwords)
         */

        formParams.clear();
        formParams.put("pw", "1234");
        formParams.put("pw2", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "lostpw/" + user.getId() + "/"
                                                                    + user.getConfirmation(), headers, formParams);
        assertTrue(result.contains("class=\"success\""));
        updateuser = User.getById(user.getId());
        assertTrue(updateuser.isActive());

        /*
         * TEST: re-use of the verification link should redirect to the index-page without any action
         */
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "lostpw/" + user.getId() + "/"
                                                                    + user.getConfirmation(), headers, formParams);
        String expected = ninjaTestBrowser.makeRequest(getServerAddress());
        assertTrue(expected.equals(result));
    }

    @Test
    public void testLogin()
    {
        /*
         * TEST: get the login-page
         */

        result = ninjaTestBrowser.makeRequest(getServerAddress() + "/login");

        assertTrue(result.contains("form action=\"/login\""));

        User user = new User("John", "Doe", "admin@ccmailr.test", "1234");
        user.save();

        /*
         * TEST: try to login with data which is not registered
         */
        formParams.clear();
        formParams.put("mail", "admin00@this.de");
        formParams.put("pwd", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/login", headers, formParams);

        // now there should be no session-cookie
        assertTrue(ninjaTestBrowser.getCookieWithName("XCMailr_SESSION") == null);

        // error message
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: try to login with no data
         */
        formParams.clear();
        formParams.put("mail", "");
        formParams.put("pwd", "");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/login", headers, formParams);
        // now there should be no session-cookie
        assertTrue(ninjaTestBrowser.getCookieWithName("XCMailr_SESSION") == null);
        // and an error message
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: try to login with a wrong password
         */
        formParams.clear();
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pwd", "baum");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/login", headers, formParams);
        // now there should be no session-cookie
        assertTrue(ninjaTestBrowser.getCookieWithName("XCMailr_SESSION") == null);
        // and an error message
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: test the successful login with an inactive user
         */
        formParams.clear();
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pwd", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/login", headers, formParams);

        // make sure that the success-page is displayed
        assertTrue(result.contains("class=\"error\">"));

        // check the cookie, it should be null
        Cookie cookie = ninjaTestBrowser.getCookieWithName("XCMailr_SESSION");
        assertNull(cookie);

        /*
         * TEST: activate the user and test the successful login
         */
        user.setActive(true);
        user.update();
        formParams.clear();
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pwd", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/login", headers, formParams);

        // make sure that the success-page is displayed
        assertTrue(result.contains("class=\"success\">"));

        // check the cookie
        cookie = ninjaTestBrowser.getCookieWithName("XCMailr_SESSION");
        assertTrue(cookie != null);
        assertTrue(cookie.getValue().contains("___TS"));
        assertTrue(cookie.getValue().contains("username"));

    }

    @Test
    public void testWrongLoginAccountDeactivation()
    {
        /*
         * TEST: try to login six times with a wrong password
         */
        User user = new User("John", "Doe", "admin@ccmailr.test", "1234");
        user.setActive(true);
        user.save();
        formParams.clear();
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pwd", "12");
        for (int i = 0; i < 7; i++)
        {
            result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/login", headers,
                                                                        formParams);
        }
        // an error message should be shown
        assertTrue(result.contains("class=\"error\""));
        // we should be redirected to the forgot-password page
        assertTrue(result.contains("form action=\"/pwresend\""));

    }

    @Test
    public void testLogout()
    {
        User u = new User("John", "Doe", "admin@ccmailr.test", "1234");
        u.setActive(true);
        u.save();

        // login
        formParams.clear();
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pwd", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/login", headers, formParams);

        // make sure that the success-page is displayed and the cookie was set
        assertTrue(result.contains("class=\"success\">"));
        Cookie cookie = ninjaTestBrowser.getCookieWithName("XCMailr_SESSION");
        assertTrue(cookie != null);
        assertTrue(cookie.getValue().contains("___TS"));
        assertTrue(cookie.getValue().contains("username"));

        // test logout
        formParams.clear();
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pwd", "1234");
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "/logout", headers);

        assertTrue(result.contains("class=\"success\""));

        // check whether the cookie has been deleted
        cookie = ninjaTestBrowser.getCookieWithName("XCMailr_SESSION");
        assertTrue(cookie == null);
        assertEquals(0, ninjaTestBrowser.getCookies().size());

    }

    @Test
    public void testIndexPage()
    {

        /*
         * TEST: test if the controller shows the right index-pages
         */
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "/");
        assertTrue(result.contains("<a href=\"/register\">Register</a>"));
        // register the user
        User u = new User("John", "Doe", "admin@ccmailr.test", "1234");
        u.setActive(true);
        u.save();

        // login
        formParams.clear();
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pwd", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/login", headers, formParams);
        // make sure that the success-page is displayed and the cookie was set
        assertTrue(result.contains("class=\"success\">"));
        Cookie cookie = ninjaTestBrowser.getCookieWithName("XCMailr_SESSION");
        assertTrue(cookie != null);
        assertTrue(cookie.getValue().contains("___TS"));
        assertTrue(cookie.getValue().contains("username"));
        result = ninjaTestBrowser.makeRequest(getServerAddress() + "/");
        assertTrue(result.contains("<li><a href=\"/logout\">Logout</a></li>"));

    }


    @Test
    public void testPwResend()
    {

        result = ninjaTestBrowser.makeRequest(getServerAddress() + "pwresend");
        assertTrue(result.contains("form action=\"/pwresend\""));
        formParams.clear();
        // provoke a form-error (fields not filled)
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "pwresend", headers,
                                                                    formParams);
        // the form should be shown again
        assertTrue(result.contains("form action=\"/pwresend\""));
        // and an error message
        assertTrue(result.contains("class=\"error\""));

        //
        User user = new User("forename", "surname", "admin@ccmailr.test", "1234");
        user.setActive(true);
        user.save();

        // try an existing account
        formParams.put("mail", "admin@ccmailr.test");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "pwresend", headers,
                                                                    formParams);
        // and an success message
        assertTrue(result.contains("class=\"success\""));

        // succes should also be shown with a wrong address..
        formParams.put("mail", "admin@xcmlr123456x.t2113ee");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "pwresend", headers,
                                                                    formParams);
        // and an success message
        assertTrue(result.contains("class=\"success\""));

    }

}
