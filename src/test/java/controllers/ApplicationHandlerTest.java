package controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


import java.util.Map;

import ninja.NinjaTest;
import org.apache.http.cookie.Cookie;
import org.junit.After;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.subethamail.smtp.server.SMTPServer;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationHandlerTest extends NinjaTest {
	
	Map<String, String> headers = Maps.newHashMap();
	Map<String, String> formParams = Maps.newHashMap();
	String result;

	//TODO java.net.bind exception (address already in use) for SubethaSMTP..
	
	@Before
	public void startSrv(){
			
	}
	@After
	public void stopSrv(){


	}
	
	@Test
	public void testRegistrationPart(){
		//Register a new user
		formParams.clear();
		formParams.put("forename", "John");
		formParams.put("surName", "Doe");
		formParams.put("mail", "admin@this.de");
		formParams.put("pw", "1234");
		formParams.put("pwn1", "1234");

		result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress()+"/register", headers, formParams);
		
		//check if the user was registered successfully
		assertTrue(result.contains("class=\"success\">Reg"));
		
		
	}
	
	@Test
	public void testLoginPart(){
		
		
		//register the user 
		formParams.clear();
		formParams.put("forename", "John");
		formParams.put("surName", "Doe");
		formParams.put("mail", "admin@this.de");
		formParams.put("pw", "1234");
		formParams.put("pwn1", "1234");
		
		ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress()+"/register", headers, formParams);
		//test failure, when using an unknown user
		
		formParams.clear();
		formParams.put("mail", "admin00@this.de");
		formParams.put("pwd", "1234");
		result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress()+"/login", headers, formParams);
		//no cookies
		assertEquals(0, ninjaTestBrowser.getCookies().size());
		//errormessage
		assertTrue(result.contains("class=\"error\">"));
		
		//test failure, when form is empty
		formParams.clear();
		formParams.put("mail", "");
		formParams.put("pwd", "");
		result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress()+"/login", headers, formParams);
		//no cookies
		assertEquals(0, ninjaTestBrowser.getCookies().size());
		//errormessage
		assertTrue(result.contains("class=\"error\">"));
		
		//test failure, when pwd is wrong
		formParams.clear();
		formParams.put("mail", "admin@this.de");
		formParams.put("pwd", "baum");
		result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress()+"/login", headers, formParams);
		//no cookies
		assertEquals(0, ninjaTestBrowser.getCookies().size());
		//errormessage		
		assertTrue(result.contains("class=\"error\">"));
		
		
		//test successful login
		formParams.clear();
		formParams.put("mail", "admin@this.de");
		formParams.put("pwd", "1234");
		result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress()+"/login", headers, formParams);
		
		//make sure that the success-page is displayed
		assertTrue(result.contains("class=\"success\">"));
		
		//check the cookie
        assertEquals(1, ninjaTestBrowser.getCookies().size());
        Cookie cookie = ninjaTestBrowser.getCookieWithName("XCMailr_SESSION");

        assertTrue(cookie != null);
        assertTrue(cookie.getValue().contains("___TS"));		

		
	}
	
	@Test
	public void testLogout(){
		formParams.clear();
		formParams.put("forename", "John");
		formParams.put("surName", "Doe");
		formParams.put("mail", "admin@this.de");
		formParams.put("pw", "1234");
		formParams.put("pwn1", "1234");
		

		ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress()+"/register", headers, formParams);
		
		//test successful login
		formParams.clear();
		formParams.put("mail", "admin@this.de");
		formParams.put("pwd", "1234");
		result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress()+"/login", headers, formParams);
		
		//make sure that the success-page is displayed
		assertTrue(result.contains("class=\"success\">"));
		
		//test logout
		formParams.clear();
		formParams.put("mail", "admin@this.de");
		formParams.put("pwd", "1234");
		result = ninjaTestBrowser.makeRequest(getServerAddress()+"/logout", headers);
		
		assertTrue(result.contains("class=\"success\""));
		//no cookies
		assertEquals(0, ninjaTestBrowser.getCookies().size());
		
		
		
	}
	

}
