package controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import ninja.NinjaTest;
import ninja.utils.NinjaProperties;

import org.apache.http.cookie.Cookie;
import org.junit.After;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class UserHandlerTest extends NinjaTest
{

    @Inject
    NinjaProperties ninjaProp;

    Map<String, String> headers = Maps.newHashMap();

    Map<String, String> formParams = Maps.newHashMap();

    String result;

    @Before
    public void setUp()
    {
        formParams.clear();
        headers.clear();
        ninjaTestBrowser.getCookies().clear();
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@this.de");
        formParams.put("pw", "1234");
        formParams.put("pwn1", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/register", headers,
                                                                    formParams);
        formParams.clear();
        formParams.put("mail", "admin@this.de");
        formParams.put("pwd", "1234");
        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/login", headers, formParams);

    }

    @After
    public void tearDown()
    {

    }

    // TODO check the returned data (does it show only the error-page and saves the data nevertheless?)
    @Test
    public void testUserEditing()
    {
        /*
         * TEST: Set no forename
         */
        formParams.clear();
        formParams.put("forename", "");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@this.de");
        formParams.put("pw", "1234");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        // check if the user-data-edit had failed
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: Set no surname
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "");
        formParams.put("mail", "admin@this.de");
        formParams.put("pw", "1234");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        // check if the user-data-edit had failed
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: Set a wrong formatted mail
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "@this.de");
        formParams.put("pw", "1234");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        // check if the user-data-edit had failed
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: Set a wrong formatted mail, again
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin.this.de");
        formParams.put("pw", "1234");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        // check if the user-data-edit had failed
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: set no passwd
         */
        formParams.clear();
        formParams.put("forename", "Johnny");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@this.de");
        formParams.put("pw", "");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        // check if the user-data-edit had failed
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: set wrong passwd (reversed order)
         */
        formParams.clear();
        formParams.put("forename", "Johnny");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@this.de");
        formParams.put("pw", "4321");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        // check if the user-data-edit had failed
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: set wrong passwd (completely different chars & length)
         */
        formParams.clear();
        formParams.put("forename", "Johnny");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@this.de");
        formParams.put("pw", "abcdef");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);

        // check if the user-data-edit had failed
        assertTrue(result.contains("class=\"error\">"));

        /*
         * TEST: Edit the Userdata correctly (fore- and surname only)
         */
        formParams.clear();
        formParams.put("forename", "Johnny");
        formParams.put("surName", "Doey");
        formParams.put("mail", "admin@this.de");
        formParams.put("pw", "1234");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);

        // check if the userdata-edit has been successfully changed
        assertTrue(result.contains("class=\"success\">"));

    }

}
