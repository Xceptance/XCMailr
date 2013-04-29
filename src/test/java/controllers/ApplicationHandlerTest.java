package controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import models.User;
import ninja.NinjaTest;
import org.apache.http.cookie.Cookie;
import org.junit.After;

import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.Maps;

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
        /*
         * TEST: Register a new user correctly
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "1234");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        System.out.println(result);
        // check if the user was registered successfully
        assertTrue(result.contains("class=\"success\">Reg"));

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
        // check if the user couldnt be registered
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
        // check if the user couldnt be registered
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
        // check if the user couldnt be registered
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: no mailaddress
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldnt be registered
        assertTrue(result.contains("error")); // a 500 validation-error page will be shown

        /*
         * TEST: no pw
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "");
        formParams.put("pwn1", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldnt be registered
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: no pw repetition
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldnt be registered
        assertTrue(result.contains("class=\"error\">"));

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
        // check if the user couldnt be registered
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
        // check if the user couldnt be registered
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
        // check if the user couldnt be registered
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
        // check if the user couldnt be registered
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: passwords didnt match
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "4321");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldnt be registered
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: different pwds with upper and lowercases
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "Cats");
        formParams.put("pwn1", "cats");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldnt be registered
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: different pwds with upper and lowercases (another way)
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "dogs");
        formParams.put("pwn1", "Dogs");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        // check if the user couldnt be registered
        assertTrue(result.contains("class=\"error\">"));

    }

    @Test
    public void testLoginPart()
    {
        User u = new User("John", "Doe", "admin@ccmailr.test", "1234");
        u.setActive(true);
        u.save();

        /*
         * TEST: try to login with data which is not registered
         */
        formParams.clear();
        formParams.put("mail", "admin00@this.de");
        formParams.put("pwd", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/login", headers, formParams);

        // now there should be no session-cookie
        assertTrue(ninjaTestBrowser.getCookieWithName("XCMailr_SESSION") == null);

        // errormessage
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
        // and an errormessage
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
        // and an errormessage
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: test the successful login
         */
        formParams.clear();
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pwd", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/login", headers, formParams);

        // make sure that the success-page is displayed
        assertTrue(result.contains("class=\"success\">"));

        // check the cookie
        Cookie cookie = ninjaTestBrowser.getCookieWithName("XCMailr_SESSION");

        assertTrue(cookie != null);
        assertTrue(cookie.getValue().contains("___TS"));
        assertTrue(cookie.getValue().contains("id"));

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
        assertTrue(cookie.getValue().contains("id"));

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
     * TODO find a way to manipulate the cookie
     *
     *
     * Try to get to the index-page for the logged-in users
     */
    
     /*
     * TEST: Meanwhile: test if the controller shows the right index-pages
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
     assertTrue(cookie.getValue().contains("id"));
     result = ninjaTestBrowser.makeRequest(getServerAddress() + "/");
     assertTrue(result.contains("<li><a href=\"/logout\">Logout</a></li>"));
    
     }
    // TODO test the pwresend-method
    // TODO add tests for the activations..

}
