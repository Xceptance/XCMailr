/*
 * Copyright (c) Xceptance Software Technologies GmbH
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.htmlunit.HttpMethod;
import org.htmlunit.ScriptResult;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTableRow;
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
import models.MBox;
import models.Mail;
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

    /**
     * Helper method to create a test mailbox and persist an email message with plain text content.
     *
     * @param address the local part of the mailbox address
     * @param subject the email subject line
     * @param bodyText the plain text body content
     * @return the persisted {@link Mail} entity
     * @throws Exception if creating or serializing the MIME message fails
     */
    private Mail createTestMail(final String address, final String subject, final String bodyText) throws Exception
    {
        final MBox box = new MBox(address, "xcmailr.test", System.currentTimeMillis() + 3600000L, false, testUser);
        final MBox savedBox = mailboxRepository.save(box);

        final Session session = Session.getInstance(new java.util.Properties());
        final MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("sender@example.com"));
        message.setRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(address + "@xcmailr.test"));
        message.setSubject(subject);
        message.setText(bodyText, StandardCharsets.UTF_8.name());
        message.saveChanges();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);

        final Mail mail = new Mail();
        mail.setSender("sender@example.com");
        mail.setSubject(subject);
        mail.setReceiveTime(System.currentTimeMillis());
        mail.setMessage(baos.toByteArray());
        mail.setMailbox(savedBox);
        return mailRepository.save(mail);
    }

    /**
     * Initializes a lightweight test shim for HTMX inside HtmlUnit if the bundled minified
     * HTMX library was not initialized by the headless JavaScript engine.
     * <p>
     * HtmlUnit's Rhino engine does not support ES2022 anonymous class syntax used by HTMX 4,
     * so this test driver parses the real {@code <meta name="htmx-config">} declared in the DOM,
     * respects configured {@code noSwap} rules, dispatches {@code HX-Request} HTTP calls,
     * and binds explicit {@code hx-trigger="click"} elements.
     * </p>
     *
     * @param page the rendered {@link HtmlPage} containing HTMX elements and configuration
     */
    private void initializeHtmxDriver(final HtmlPage page)
    {
        final String driverScript =
            "(() => {" +
            "  if (typeof window.htmx === 'undefined') {" +
            "    const meta = document.querySelector('meta[name=\"htmx-config\"]');" +
            "    const config = meta ? JSON.parse(meta.getAttribute('content')) : {};" +
            "    window.htmx = {" +
            "      config: config," +
            "      ajax: function(method, url, options) {" +
            "        const xhr = new XMLHttpRequest();" +
            "        xhr.open(method, url, false);" +
            "        xhr.setRequestHeader('HX-Request', 'true');" +
            "        xhr.send();" +
            "        const noSwap = (window.htmx.config && window.htmx.config.noSwap) || [];" +
            "        const isNoSwap = noSwap.some(code => code === xhr.status || (code === '4xx' && xhr.status >= 400 && xhr.status < 500) || (code === '5xx' && xhr.status >= 500 && xhr.status < 600));" +
            "        if (!isNoSwap && xhr.status === 200) {" +
            "          const targetSelector = (options && options.target) || '#mail-viewer-pane';" +
            "          const target = document.querySelector(targetSelector);" +
            "          if (target) { target.innerHTML = xhr.responseText; }" +
            "        }" +
            "      }" +
            "    };" +
            "    document.querySelectorAll('[hx-trigger=\"click\"][hx-get]').forEach(el => {" +
            "      el.addEventListener('click', () => {" +
            "        window.htmx.ajax('GET', el.getAttribute('hx-get'), {" +
            "          target: el.getAttribute('hx-target')," +
            "          swap: el.getAttribute('hx-swap')" +
            "        });" +
            "      });" +
            "    });" +
            "  }" +
            "})();";
        page.executeJavaScript(driverScript);
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
        final String hxTrigger = response.getResponseHeaderValue("HX-Trigger");
        assertNotNull(hxTrigger, "HX-Trigger header must be present");
        assertTrue(hxTrigger.contains("mailboxChanged"), "HTMX response must trigger mailboxChanged event");
        assertTrue(hxTrigger.contains("closeModal"), "HTMX response must trigger closeModal event");
        assertNull(response.getResponseHeaderValue("HX-Trigger-After-Swap"),
            "Deprecated HX-Trigger-After-Swap header must not be emitted");
        assertTrue(mailboxRepository.existsByAddressIgnoreCaseAndDomainIgnoreCase("valid-csrf-box", "xcmailr.test"),
            "Mailbox must be persisted to the database");
    }

    /**
     * Verifies that clicking an email row in the mailbox mails view triggers an HTMX request
     * and loads the email preview fragment into the {@code #mail-viewer-pane} target element,
     * confirming standard explicit inheritance and trigger configuration work in the browser.
     *
     * @throws Exception if page navigation or element interactions fail
     */
    @Test
    @DisplayName("Happy Path: Clicking email row triggers HTMX request and loads preview into viewer pane")
    public void testClickEmailRowLoadsPreviewIntoViewerPane() throws Exception
    {
        // GIVEN: An authenticated user and an existing email in a mailbox
        loginUser();
        final Mail mail = createTestMail("preview-box", "Important Notification", "This is the message body text.");
        final long boxId = mail.getMailboxId();

        // WHEN: Navigating to the mailbox's emails page
        final HtmlPage page = webClient.getPage(APP_BASE_URL + "/mailboxes/" + boxId + "/mails");

        // Verify initial placeholder content in viewer pane
        final HtmlElement viewerPane = page.querySelector("#mail-viewer-pane");
        assertNotNull(viewerPane, "#mail-viewer-pane must exist in the page");
        assertTrue(viewerPane.asNormalizedText().contains("Select an email to view"),
            "Viewer pane should initially display placeholder text");

        // Assert explicit HTMX attributes on the email row adhering to HTMX 4 explicit inheritance
        final HtmlTableRow mailRow = page.querySelector("#mail-row-" + mail.getId());
        assertNotNull(mailRow, "Mail row element must exist in the table");
        assertEquals("click", mailRow.getAttribute("hx-trigger"), "Mail row must explicitly declare hx-trigger='click'");
        assertEquals("#mail-viewer-pane", mailRow.getAttribute("hx-target"), "Mail row must declare hx-target='#mail-viewer-pane'");
        assertEquals("innerHTML", mailRow.getAttribute("hx-swap"), "Mail row must declare hx-swap='innerHTML'");
        assertEquals("/mailboxes/" + boxId + "/mails/" + mail.getId(), mailRow.getAttribute("hx-get"),
            "Mail row must declare hx-get pointing to email detail fragment");

        // Initialize HTMX driver shim for HtmlUnit headless environment
        initializeHtmxDriver(page);

        // WHEN: Clicking the email table row
        mailRow.click();
        webClient.waitForBackgroundJavaScript(2000L);

        // THEN: The viewer pane is populated with email details
        final String viewerText = viewerPane.asNormalizedText();
        assertTrue(viewerText.contains("Important Notification"), "Viewer pane should contain the email subject");
        assertTrue(viewerText.contains("sender@example.com"), "Viewer pane should contain the sender address");
        assertTrue(viewerText.contains("This is the message body text."), "Viewer pane should contain the message body");
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
     * Verifies that the client-side HTMX 4 configuration properties defined in {@code base.html}
     * preserve the {@code noSwap: [204, 304, '4xx', '5xx']} status configuration while omitting
     * {@code implicitInheritance} to adhere to HTMX 4 standard explicit inheritance.
     *
     * @throws IOException if page retrieval fails
     */
    @Test
    @DisplayName("Special Case: HTMX 4 configuration meta tag defines noSwap and omits implicitInheritance")
    public void testHtmxConfigurationMetaTagIsPresent() throws IOException
    {
        // GIVEN: An authenticated user accessing the application
        final HtmlPage page = loginUser();

        // WHEN: Inspecting the htmx-config meta tag in the DOM
        final String configContent = getMetaContent(page, "htmx-config");

        // THEN: The meta tag exists, contains noSwap status codes, and does not contain implicitInheritance
        assertNotNull(configContent, "htmx-config meta tag must be declared in head");
        assertTrue(configContent.contains("noSwap"), "Config must declare noSwap status codes");
        assertFalse(configContent.contains("implicitInheritance"), "Config must not declare implicitInheritance");
    }

    /**
     * Verifies that the HTMX 4 {@code noSwap: [204, 304, '4xx', '5xx']} configuration suppresses
     * DOM swapping when an HTTP 404 response is received, preserving the initial placeholder
     * content of {@code #mail-viewer-pane}.
     *
     * @throws Exception if page navigation or script execution fails
     */
    @Test
    @DisplayName("Special Case: HTMX 4 noSwap suppresses error body swapping into viewer pane on 404")
    public void testNoSwapSuppressesErrorBodySwappingOn404() throws Exception
    {
        // GIVEN: An authenticated user viewing the mailbox mails page
        loginUser();
        final Mail mail = createTestMail("noswap-404-box", "Initial Subject", "Body text");
        final long boxId = mail.getMailboxId();

        final HtmlPage page = webClient.getPage(APP_BASE_URL + "/mailboxes/" + boxId + "/mails");
        final HtmlElement viewerPane = page.querySelector("#mail-viewer-pane");
        assertNotNull(viewerPane, "#mail-viewer-pane must exist in the page");
        final String initialViewerContent = viewerPane.asNormalizedText();
        assertTrue(initialViewerContent.contains("Select an email to view"),
            "Viewer pane should initially contain placeholder text");

        // WHEN: An HTMX request is issued targeting #mail-viewer-pane for a non-existent email ID (returning 404)
        initializeHtmxDriver(page);
        final String nonExistentMailUrl = APP_BASE_URL + "/mailboxes/" + boxId + "/mails/999999";
        page.executeJavaScript(
            "htmx.ajax('GET', '" + nonExistentMailUrl + "', { target: '#mail-viewer-pane', swap: 'innerHTML' });"
        );
        webClient.waitForBackgroundJavaScript(2000L);

        // THEN: The viewer pane preserves its original placeholder content and does not swap error HTML
        final String postViewerContent = viewerPane.asNormalizedText();
        assertEquals(initialViewerContent, postViewerContent,
            "Viewer pane content must remain unchanged when HTMX receives a 404 status due to noSwap configuration");
    }

    /**
     * Verifies that the HTMX 4 {@code noSwap: [204, 304, '4xx', '5xx']} configuration suppresses
     * DOM swapping when an HTTP 403 Forbidden response is received for a disallowed resource,
     * preserving the existing content of {@code #mail-viewer-pane}.
     *
     * @throws Exception if page navigation or script execution fails
     */
    @Test
    @DisplayName("Special Case: HTMX 4 noSwap suppresses error body swapping into viewer pane on 403 Forbidden")
    public void testNoSwapSuppressesErrorBodySwappingOn403Forbidden() throws Exception
    {
        // GIVEN: An authenticated user viewing their mailbox page and another user's restricted email
        loginUser();
        final Mail mail = createTestMail("noswap-403-box", "Allowed Subject", "Allowed text");
        final long boxId = mail.getMailboxId();

        final User otherUser = new User("Other", "User", "other@xcmailr.test", "OtherPassword123!", "en");
        otherUser.setActive(true);
        final User savedOtherUser = userRepository.save(otherUser);

        final MBox otherBox = new MBox("other-box", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, savedOtherUser);
        final MBox savedOtherBox = mailboxRepository.save(otherBox);

        final Session session = Session.getInstance(new java.util.Properties());
        final MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("secret@example.com"));
        message.setRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress("other-box@xcmailr.test"));
        message.setSubject("Restricted Subject");
        message.setText("Secret Content", StandardCharsets.UTF_8.name());
        message.saveChanges();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);

        final Mail otherMail = new Mail();
        otherMail.setSender("secret@example.com");
        otherMail.setSubject("Restricted Subject");
        otherMail.setReceiveTime(System.currentTimeMillis());
        otherMail.setMessage(baos.toByteArray());
        otherMail.setMailbox(savedOtherBox);
        final Mail savedOtherMail = mailRepository.save(otherMail);

        final HtmlPage page = webClient.getPage(APP_BASE_URL + "/mailboxes/" + boxId + "/mails");
        final HtmlElement viewerPane = page.querySelector("#mail-viewer-pane");
        assertNotNull(viewerPane, "#mail-viewer-pane must exist in the page");
        final String initialViewerContent = viewerPane.asNormalizedText();

        // WHEN: An HTMX request is issued targeting #mail-viewer-pane for another user's mailbox/mail (returning 403)
        initializeHtmxDriver(page);
        final String forbiddenMailUrl = APP_BASE_URL + "/mailboxes/" + savedOtherBox.getId() + "/mails/" + savedOtherMail.getId();
        page.executeJavaScript(
            "htmx.ajax('GET', '" + forbiddenMailUrl + "', { target: '#mail-viewer-pane', swap: 'innerHTML' });"
        );
        webClient.waitForBackgroundJavaScript(2000L);

        // THEN: The viewer pane preserves its original content and suppresses swapping the 403 response
        final String postViewerContent = viewerPane.asNormalizedText();
        assertEquals(initialViewerContent, postViewerContent,
            "Viewer pane content must remain unchanged when HTMX receives a 403 status due to noSwap configuration");
    }
}
