package controllers;

import static org.junit.Assert.assertTrue;
import java.util.Map;

import models.User;
import ninja.NinjaTest;
import ninja.utils.NinjaProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class UserHandlerTest extends NinjaTest
{

    @Inject
    NinjaProperties ninjaProp;
    Map<String, String> headers = Maps.newHashMap();
    Map<String, String> formParams = Maps.newHashMap();
    Map<String, String> returnedData = Maps.newHashMap();
    Map<String, String> userData = Maps.newHashMap();
    String result;

    @Before
    public void setUp()
    {
        formParams.clear();
        headers.clear();
        returnedData.clear();
        
        User u = new User("John", "Doe", "admin@ccmailr.test", "1234");
        u.setActive(true);
        u.save();

        
        userData.put("forename", "John");
        userData.put("surName", "Doe");
        userData.put("mail", "admin@ccmailr.test");

        formParams.clear();
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
    public void testUserEditing()
    {

        /*
         * TEST: Set no forename
         */
        formParams.clear();
        formParams.put("forename", "");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "1234");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        

        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed
        System.out.println(returnedData);
        System.out.println(userData);
        assertTrue(result.contains("class=\"error\">"));

        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);

        /*
         * TEST: Set no surname
         */
        formParams.clear();
        formParams.put("forename", "John");
        formParams.put("surName", "");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "1234");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed
        assertTrue(result.contains("class=\"error\">"));

        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);

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
        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed
        assertTrue(result.contains("class=\"error\">"));

        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);

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
        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed
        assertTrue(result.contains("class=\"error\">"));

        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);

        /*
         * TEST: set no passwd
         */
        formParams.clear();
        formParams.put("forename", "Johnny");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed
        assertTrue(result.contains("class=\"error\">"));

        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);

        /*
         * TEST: set wrong passwd (reversed order)
         */
        formParams.clear();
        formParams.put("forename", "Johnny");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "4321");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed
        assertTrue(result.contains("class=\"error\">"));

        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);

        /*
         * TEST: set wrong passwd (completely different chars & length)
         */
        formParams.clear();
        formParams.put("forename", "Johnny");
        formParams.put("surName", "Doe");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "abcdef");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);

        returnedData = HtmlUtils.readInputFormData(result);
        // check that the user-data-edit had failed
        assertTrue(result.contains("class=\"error\">"));

        // the returned data should (in cause of the error) now be equal to the (unchanged)userdata
        TestUtils.testMapEntryEquality(userData, returnedData);

        /*
         * TEST: Edit the Userdata correctly (fore- and surname only)
         */
        formParams.clear();
        formParams.put("forename", "Johnny");
        formParams.put("surName", "Doey");
        formParams.put("mail", "admin@ccmailr.test");
        formParams.put("pw", "1234");

        result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress() + "/user/edit", headers,
                                                                    formParams);
        returnedData = HtmlUtils.readInputFormData(result);

        // check if the userdata-edit has been successfully changed
        assertTrue(result.contains("class=\"success\">"));

        // the returned data should now be equal to the formparams without the password
        formParams.remove("pw");
        TestUtils.testMapEntryEquality(formParams, returnedData);
    }

}
