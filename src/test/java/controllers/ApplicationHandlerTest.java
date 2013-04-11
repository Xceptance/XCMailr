package controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import ninja.NinjaTest;
import ninja.session.SessionCookie;

import org.apache.http.HttpResponse;
import org.apache.http.cookie.Cookie;
import org.h2.constant.SysProperties;
import org.junit.Test;

import com.google.common.collect.Maps;

public class ApplicationHandlerTest extends NinjaTest {
	
	Map<String, String> headers = Maps.newHashMap();
	Map<String, String> formParams = Maps.newHashMap();
	String result;
	
	@Test
	public void testRegistrationPart(){
		formParams.clear();
		formParams.put("forename", "John");
		formParams.put("surName", "Doe");
		formParams.put("mail", "admin@this.de");
		formParams.put("pw", "1234");
		formParams.put("pwn1", "1234");
		
		result = ninjaTestBrowser.makePostRequestWithFormParameters(getServerAddress()+"/register", headers, formParams);
		
		
		//erfolgreich registriert
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
        Cookie cookie = ninjaTestBrowser.getCookieWithName("CCMailr_SESSION");

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
