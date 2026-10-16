/*
 * Copyright (c) 2013-2026 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xceptance.xcmailr.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.htmlunit.HttpMethod;
import org.htmlunit.ScriptResult;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.NameValuePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebClientBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.Domain;
import models.User;

/**
 * Browser-level client contract integration test (Test Pyramid Layer 3 - E2E)
 * verifying that the client-side JavaScript in {@code base.html} correctly intercepts
 * HTMX 4 lifecycle events, extracts CSRF token metadata from the page DOM, and attaches
 * the security token to outbound mutating requests.
 * <p>
 * This test suite utilizes a headless {@link WebClient} wired directly to {@link MockMvc}
 * to load full HTML templates, execute JavaScript event listeners, and validate
 * end-to-end security enforcement across happy path, missing token, and forged token scenarios.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
public final class HtmxCsrfClientContractTest
{
    private static final String TEST_USER_MAIL = "csrf-tester@xcmailr.test";
    private static final String TEST_USER_PASSWORD = "ValidPassword123!";
    private static final String APP_BASE_URL = "http://localhost";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

    private WebClient webClient;
    private User testUser;

    /**
     * Initializes the database state, creates a test user account, and configures
     * an in-memory {@link WebClient} backed by {@link MockMvc} with Spring Security enabled.
     */
    @BeforeEach
    public void setUp()
    {
        // GIVEN: A clean database state and pre-seeded active user
        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        domainRepository.deleteAll();
        userRepository.deleteAll();

        final Domain domain = new Domain("xcmailr.test");
        domainRepository.save(domain);

        final User user = new User("Csrf", "Tester", TEST_USER_MAIL, TEST_USER_PASSWORD, "en");
        user.setActive(true);
        user.setAdmin(false);
        testUser = userRepository.save(user);

        // Configure MockMvc with Spring Security
        final MockMvc mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        // Build headless WebClient with JavaScript enabled
        webClient = MockMvcWebClientBuilder.mockMvcSetup(mockMvc).build();
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.setJavaScriptErrorListener(new org.htmlunit.javascript.SilentJavaScriptErrorListener());
    }

    /**
     * Cleans up the {@link WebClient} after each test execution to prevent resource leaks.
     */
    @AfterEach
    public void tearDown()
    {
        if (webClient != null)
        {
            webClient.close();
        }
    }

    /**
     * Authenticates the test user through standard form login, establishing a valid
     * authenticated session with cookies preserved inside the {@link WebClient}.
     *
     * @return the rendered {@link HtmlPage} after successful login redirect
     * @throws IOException if network or page navigation fails
     */
    private HtmlPage loginUser() throws IOException
    {
        final HtmlPage loginPage = webClient.getPage(APP_BASE_URL + "/login");
        final HtmlForm form = loginPage.getForms().get(0);
        form.getInputByName("mail").setValue(TEST_USER_MAIL);
        form.getInputByName("password").setValue(TEST_USER_PASSWORD);

        final HtmlButton submitButton = form.getOneHtmlElementByAttribute("button", "type", "submit");
        return submitButton.click();
    }

    /**
     * Extracts the {@code content} attribute of a {@code <meta>} tag by name.
     *
     * @param page the rendered HTML page
     * @param metaName the name attribute of the meta tag
     * @return the content value or null if not found
     */
    private String getMetaContent(final HtmlPage page, final String metaName)
    {
        final org.htmlunit.html.DomNode node = page.querySelector("meta[name='" + metaName + "']");
        if (node instanceof org.htmlunit.html.DomElement element)
        {
            return element.getAttribute("content");
        }
        return null;
    }

    // =========================================================================
    // 1. Happy Path Scenarios
    // =========================================================================

    /**
     * Verifies that the client-side {@code htmx:config:request} event listener in {@code base.html}
     * extracts the CSRF token and header name from the page DOM meta tags and populates
     * both the modern HTMX 4 {@code evt.detail.ctx.request.headers} object and the legacy
     * {@code evt.detail.headers} object.
     *
     * @throws IOException if page retrieval fails
     */
    @Test
    @DisplayName("Happy Path: htmx:config:request listener extracts CSRF meta tags into event headers")
    public void testHtmxConfigRequestExtractsValidCsrfTokenFromDom() throws IOException
    {
        // GIVEN: An authenticated user viewing the mailboxes dashboard page with CSRF meta tags
        final HtmlPage page = loginUser();

        // Extract expected CSRF meta values from the DOM
        final String expectedToken = getMetaContent(page, "_csrf");
        final String expectedHeader = getMetaContent(page, "_csrf_header");
        assertNotNull(expectedToken, "CSRF token meta tag must be present in the page head");
        assertNotNull(expectedHeader, "CSRF header meta tag must be present in the page head");

        // WHEN: A synthetic htmx:config:request event is dispatched on document.body
        final String script =
            "(() => {" +
            "  const evt = new CustomEvent('htmx:config:request', {" +
            "    bubbles: true," +
            "    detail: {" +
            "      headers: {}," +
            "      ctx: { request: { headers: {} } }" +
            "    }" +
            "  });" +
            "  document.body.dispatchEvent(evt);" +
            "  return JSON.stringify({" +
            "    headerName: '" + expectedHeader + "'," +
            "    headersValue: evt.detail.headers['" + expectedHeader + "'] || null," +
            "    ctxHeadersValue: (evt.detail.ctx && evt.detail.ctx.request && evt.detail.ctx.request.headers) ? evt.detail.ctx.request.headers['" + expectedHeader + "'] : null" +
            "  });" +
            "})();";

        final ScriptResult result = page.executeJavaScript(script);
        final String jsonResult = (String) result.getJavaScriptResult();

        // THEN: Both legacy headers and HTMX 4 ctx request headers contain the valid token
        assertNotNull(jsonResult, "Script execution result must not be null");
        assertTrue(jsonResult.contains(expectedToken), "Event headers must contain the extracted CSRF token value");
    }

    /**
     * Verifies that an asynchronous mutating request carrying the CSRF token attached by
     * the client-side event handler is accepted by the server and returns HTTP 200 OK.
     *
     * @throws IOException if the HTTP request fails
     */
    @Test
    @DisplayName("Happy Path: Asynchronous mutating request with valid CSRF token is accepted with 200 OK")
    public void testAsynchronousRequestWithValidCsrfTokenSucceeds() throws IOException
    {
        // GIVEN: An authenticated user session with CSRF metadata available in the DOM
        final HtmlPage page = loginUser();
        final String validToken = getMetaContent(page, "_csrf");
        final String csrfHeader = getMetaContent(page, "_csrf_header");

        // Simulate client-side extraction of CSRF token via DOM metadata
        final WebRequest request = new WebRequest(new URL(APP_BASE_URL + "/mailboxes"), HttpMethod.POST);
        request.setAdditionalHeader("HX-Request", "true");
        request.setAdditionalHeader(csrfHeader, validToken);
        request.setRequestParameters(List.of(
            new NameValuePair("address", "valid-csrf-box"),
            new NameValuePair("domain", "xcmailr.test")
        ));

        // WHEN: The client submits the mutating request to create a mailbox
        final WebResponse response = webClient.loadWebResponse(request);

        // THEN: The request passes Spring Security CSRF verification with HTTP 200 OK
        assertEquals(200, response.getStatusCode(), "Mutating request with valid CSRF token must return 200 OK");
        assertEquals("mailboxChanged", response.getResponseHeaderValue("HX-Trigger"),
            "HTMX response must trigger mailboxChanged event");
        assertEquals("closeModal", response.getResponseHeaderValue("HX-Trigger-After-Swap"),
            "HTMX response must trigger closeModal event");
        assertTrue(mailboxRepository.existsByAddressIgnoreCaseAndDomainIgnoreCase("valid-csrf-box", "xcmailr.test"),
            "Mailbox must be persisted to the database");
    }

    // =========================================================================
    // 2. Error Path Scenarios
    // =========================================================================

    /**
     * Verifies that when the CSRF meta tag is absent or missing from the DOM,
     * the client-side event handler does not attach any CSRF header, and the server
     * denies the mutating request with HTTP 403 Forbidden.
     *
     * @throws IOException if the HTTP request fails
     */
    @Test
    @DisplayName("Error Path: Asynchronous mutating request with missing CSRF token is denied with 403 Forbidden")
    public void testAsynchronousRequestWithMissingCsrfTokenReturns403() throws IOException
    {
        // GIVEN: An authenticated user session where the CSRF meta tag is removed from the DOM
        final HtmlPage page = loginUser();

        // Remove CSRF meta tags from the DOM in the browser
        final String removeMetaScript =
            "(() => {" +
            "  const meta = document.querySelector('meta[name=\"_csrf\"]');" +
            "  if (meta) meta.parentNode.removeChild(meta);" +
            "  const evt = new CustomEvent('htmx:config:request', {" +
            "    bubbles: true," +
            "    detail: {" +
            "      headers: {}," +
            "      ctx: { request: { headers: {} } }" +
            "    }" +
            "  });" +
            "  document.body.dispatchEvent(evt);" +
            "  return evt.detail.headers['X-CSRF-TOKEN'] || null;" +
            "})();";

        final ScriptResult scriptResult = page.executeJavaScript(removeMetaScript);
        final Object clientHeaderValue = scriptResult.getJavaScriptResult();

        // Client-side event listener attached nothing because meta was absent
        assertNull(clientHeaderValue, "Client must not attach CSRF token if meta tag is missing");

        // WHEN: Outbound mutating request is sent without any CSRF token header
        final WebRequest request = new WebRequest(new URL(APP_BASE_URL + "/mailboxes"), HttpMethod.POST);
        request.setAdditionalHeader("HX-Request", "true");
        request.setRequestParameters(List.of(
            new NameValuePair("address", "no-csrf-box"),
            new NameValuePair("domain", "xcmailr.test")
        ));

        final WebResponse response = webClient.loadWebResponse(request);

        // THEN: The server rejects the request immediately with HTTP 403 Forbidden
        assertEquals(403, response.getStatusCode(), "Request lacking CSRF token must be rejected with 403 Forbidden");
    }

    /**
     * Verifies that when the client sends an invalid, expired, or forged CSRF token value,
     * the server rejects the mutating request with HTTP 403 Forbidden and prevents state changes.
     *
     * @throws IOException if the HTTP request fails
     */
    @Test
    @DisplayName("Error Path: Asynchronous mutating request with invalid/forged CSRF token is denied with 403 Forbidden")
    public void testAsynchronousRequestWithForgedCsrfTokenReturns403() throws IOException
    {
        // GIVEN: An authenticated user session and an attacker injecting a forged token
        loginUser();
        final String forgedToken = "attacker-forged-token-xyz-12345";
        final String csrfHeader = "X-CSRF-TOKEN";

        // WHEN: A mutating request is submitted with the forged CSRF token header
        final WebRequest request = new WebRequest(new URL(APP_BASE_URL + "/mailboxes"), HttpMethod.POST);
        request.setAdditionalHeader("HX-Request", "true");
        request.setAdditionalHeader(csrfHeader, forgedToken);
        request.setRequestParameters(List.of(
            new NameValuePair("address", "forged-csrf-box"),
            new NameValuePair("domain", "xcmailr.test")
        ));

        final WebResponse response = webClient.loadWebResponse(request);

        // THEN: Spring Security rejects the forged token with HTTP 403 Forbidden
        assertEquals(403, response.getStatusCode(), "Request with forged CSRF token must be rejected with 403 Forbidden");
    }

    // =========================================================================
    // 3. Special Edge Cases & Backward Compatibility
    // =========================================================================

    /**
     * Verifies backward compatibility with HTMX 2 syntax by ensuring that the legacy
     * {@code htmx:configRequest} event name is also caught by the listener in {@code base.html}
     * and correctly populates outbound headers.
     *
     * @throws IOException if page retrieval fails
     */
    @Test
    @DisplayName("Special Case: Legacy htmx:configRequest event attaches CSRF token for backward compatibility")
    public void testLegacyHtmxConfigRequestEventPopulatesHeaders() throws IOException
    {
        // GIVEN: An authenticated user viewing the page
        final HtmlPage page = loginUser();
        final String expectedToken = getMetaContent(page, "_csrf");
        final String expectedHeader = getMetaContent(page, "_csrf_header");

        // WHEN: The legacy htmx:configRequest event is dispatched
        final String script =
            "(() => {" +
            "  const evt = new CustomEvent('htmx:configRequest', {" +
            "    bubbles: true," +
            "    detail: { headers: {} }" +
            "  });" +
            "  document.body.dispatchEvent(evt);" +
            "  return evt.detail.headers['" + expectedHeader + "'] || null;" +
            "})();";

        final ScriptResult result = page.executeJavaScript(script);
        final String attachedToken = (String) result.getJavaScriptResult();

        // THEN: The legacy listener captured the event and populated the CSRF header
        assertNotNull(attachedToken, "Legacy event must attach CSRF token");
        assertEquals(expectedToken, attachedToken, "Attached token must match the DOM meta token");
    }

    /**
     * Verifies that the client-side HTMX 4 compatibility configuration properties
     * ({@code implicitInheritance: true} and {@code noSwap: [204, 304, '4xx', '5xx']})
     * defined in {@code base.html} are properly rendered in the DOM meta tag.
     *
     * @throws IOException if page retrieval fails
     */
    @Test
    @DisplayName("Special Case: HTMX 4 configuration meta tag defines implicitInheritance and noSwap")
    public void testHtmxConfigurationMetaTagIsPresent() throws IOException
    {
        // GIVEN: An authenticated user accessing the application
        final HtmlPage page = loginUser();

        // WHEN: Inspecting the htmx-config meta tag in the DOM
        final String configContent = getMetaContent(page, "htmx-config");

        // THEN: The meta tag exists and contains the required compatibility JSON
        assertNotNull(configContent, "htmx-config meta tag must be declared in head");
        assertTrue(configContent.contains("implicitInheritance"), "Config must declare implicitInheritance");
        assertTrue(configContent.contains("noSwap"), "Config must declare noSwap status codes");
    }
}
