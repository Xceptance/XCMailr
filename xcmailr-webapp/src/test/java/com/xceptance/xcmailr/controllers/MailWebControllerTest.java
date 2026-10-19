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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import com.xceptance.xcmailr.XcmailrApplication;
import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import jakarta.activation.DataHandler;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import models.Domain;
import models.MBox;
import models.Mail;
import models.User;

/**
 * Tests for {@link MailWebController} testing inbox listing, sanitized HTML rendering,
 * raw message download, and attachment extraction.
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public class MailWebControllerTest
{
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ITemplateEngine templateEngine;

    private User alice;
    private User bob;
    private MBox aliceBox;
    private Mail testMail;

    @BeforeEach
    public void setup() throws Exception
    {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();

        final Domain domain = new Domain("xcmailr.test");
        domainRepository.save(domain);

        alice = new User("Alice", "Smith", "alice@xcmailr.test", "Password123", "en");
        alice.setActive(true);
        userRepository.save(alice);

        bob = new User("Bob", "Jones", "bob@xcmailr.test", "Password123", "en");
        bob.setActive(true);
        userRepository.save(bob);

        aliceBox = new MBox("inbox", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, alice);
        mailboxRepository.save(aliceBox);

        // Build a MIME message with HTML and an attachment
        final Session session = Session.getInstance(new java.util.Properties());
        final MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("newsletter@sender.com"));
        message.setRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress("inbox@xcmailr.test"));
        message.setSubject("Weekly Digest");

        final MimeMultipart multipart = new MimeMultipart("mixed");

        // HTML body part with malicious script
        final MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent("<p>Welcome to <b>XCMailr</b>!</p><script>alert('xss');</script>", "text/html; charset=utf-8");
        multipart.addBodyPart(htmlPart);

        // Attachment part
        final MimeBodyPart attachPart = new MimeBodyPart();
        final ByteArrayDataSource ds = new ByteArrayDataSource("invoice-binary-data", "application/pdf");
        attachPart.setDataHandler(new DataHandler(ds));
        attachPart.setFileName("invoice.pdf");
        multipart.addBodyPart(attachPart);

        message.setContent(multipart);
        message.saveChanges();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);

        testMail = new Mail();
        testMail.setSender("newsletter@sender.com");
        testMail.setSubject("Weekly Digest");
        testMail.setReceiveTime(System.currentTimeMillis());
        testMail.setMessage(baos.toByteArray());
        testMail.setMailbox(aliceBox);
        mailRepository.save(testMail);
    }

    @Test
    @DisplayName("GET /mails should render complete HTML page with message list, recipient badge, and viewer pane")
    public void testListAllMailsFullPage() throws Exception
    {
        mockMvc.perform(get("/mails")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/all-mails"))
               .andExpect(model().attributeExists("mails"))
               .andExpect(model().attributeExists("mailboxAddressMap"))
               .andExpect(content().string(not(emptyOrNullString())))
               .andExpect(content().string(containsString("All Mails")))
               .andExpect(content().string(containsString("newsletter@sender.com")))
               .andExpect(content().string(containsString("Weekly Digest")))
               .andExpect(content().string(containsString("inbox@xcmailr.test")))
               .andExpect(content().string(containsString("id=\"mail-viewer-pane\"")))
               .andExpect(content().string(containsString("Select an email to view")));
    }

    @Test
    @DisplayName("GET /mails with HX-Request should return only mail-list fragment without outer layout tags")
    public void testListAllMailsFragment() throws Exception
    {
        mockMvc.perform(get("/mails")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/all-mail-list :: mailList"))
               .andExpect(content().string(not(emptyOrNullString())))
               .andExpect(content().string(containsString("id=\"mail-list-container\"")))
               .andExpect(content().string(containsString("newsletter@sender.com")))
               .andExpect(content().string(containsString("Weekly Digest")))
               .andExpect(content().string(containsString("inbox@xcmailr.test")))
               .andExpect(content().string(not(containsString("<html"))))
               .andExpect(content().string(not(containsString("<nav"))));
    }

    @Test
    @DisplayName("GET /mails should disambiguate emails across multiple mailboxes with respective badges")
    public void testListAllMailsMultipleMailboxes() throws Exception
    {
        final MBox secondBox = new MBox("second", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, alice);
        mailboxRepository.save(secondBox);

        final Mail secondMail = new Mail();
        secondMail.setSender("alerts@vendor.com");
        secondMail.setSubject("Critical Alert");
        secondMail.setReceiveTime(System.currentTimeMillis() + 1000L);
        secondMail.setMessage("alert body".getBytes(StandardCharsets.UTF_8));
        secondMail.setMailbox(secondBox);
        mailRepository.save(secondMail);

        mockMvc.perform(get("/mails")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("inbox@xcmailr.test")))
               .andExpect(content().string(containsString("second@xcmailr.test")))
               .andExpect(content().string(containsString("Critical Alert")))
               .andExpect(content().string(containsString("Weekly Digest")));
    }

    @Test
    @DisplayName("GET /mails with zero emails should render empty state notice")
    public void testListAllMailsEmptyInbox() throws Exception
    {
        mailRepository.deleteAll();

        mockMvc.perform(get("/mails")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("No messages received across your mailboxes yet.")))
               .andExpect(content().string(not(containsString("Weekly Digest"))));
    }

    @Test
    @DisplayName("mailboxes/fragments/all-mail-list handles unknown mailbox ID gracefully via fallback")
    public void testListAllMailsMissingMailboxMapEntryFallback() throws Exception
    {
        final Mail orphanMail = new Mail();
        orphanMail.setSender("orphan@external.com");
        orphanMail.setSubject("Orphan Message");
        orphanMail.setReceiveTime(System.currentTimeMillis());
        orphanMail.setMessage("orphan body".getBytes(StandardCharsets.UTF_8));
        orphanMail.setMailboxId(999999L);

        final JakartaServletWebApplication servletApp = JakartaServletWebApplication.buildApplication(context.getServletContext());
        final var webExchange = servletApp.buildExchange(new MockHttpServletRequest(), new MockHttpServletResponse());
        final WebContext thymeleafContext = new WebContext(webExchange);
        thymeleafContext.setVariable("mails", List.of(orphanMail));
        thymeleafContext.setVariable("mailboxAddressMap", Map.of());
        thymeleafContext.setVariable("dateFormatter", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()));

        final String html = templateEngine.process("mailboxes/fragments/all-mail-list", thymeleafContext);
        assertNotNull(html);
        assertTrue(html.contains("Unknown Mailbox"));
        assertTrue(html.contains("orphan@external.com"));
        assertTrue(html.contains("Orphan Message"));
    }

    @Test
    @DisplayName("GET /mails without authentication should redirect to login")
    public void testListAllMailsUnauthenticatedRedirectsToLogin() throws Exception
    {
        mockMvc.perform(get("/mails"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrlPattern("/**/login"));
    }

    @Test
    @DisplayName("GET /mails isolates user data and does not leak Bob's emails to Alice")
    public void testListAllMailsUserIsolation() throws Exception
    {
        final MBox bobBox = new MBox("bobinbox", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, bob);
        mailboxRepository.save(bobBox);

        final Mail bobMail = new Mail();
        bobMail.setSender("secret@bobsponsor.com");
        bobMail.setSubject("Confidential For Bob Only");
        bobMail.setReceiveTime(System.currentTimeMillis() + 500L);
        bobMail.setMessage("secret content".getBytes(StandardCharsets.UTF_8));
        bobMail.setMailbox(bobBox);
        mailRepository.save(bobMail);

        mockMvc.perform(get("/mails")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("newsletter@sender.com")))
               .andExpect(content().string(containsString("Weekly Digest")))
               .andExpect(content().string(containsString("inbox@xcmailr.test")))
               .andExpect(content().string(not(containsString("secret@bobsponsor.com"))))
               .andExpect(content().string(not(containsString("Confidential For Bob Only"))))
               .andExpect(content().string(not(containsString("bobinbox@xcmailr.test"))));
    }

    @Test
    @DisplayName("POST/PUT/DELETE /mails should return HTTP 405 Method Not Allowed")
    public void testListAllMailsDisallowedMethods() throws Exception
    {
        mockMvc.perform(post("/mails")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf()))
               .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/mails")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf()))
               .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/mails")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf()))
               .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("GET /mails contains responsive layout grid classes and touch target elements")
    public void testListAllMailsResponsiveMarkup() throws Exception
    {
        mockMvc.perform(get("/mails")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("col-12 col-lg-5")))
               .andExpect(content().string(containsString("col-12 col-lg-7")))
               .andExpect(content().string(containsString("recipient-badge")))
               .andExpect(content().string(containsString("btn-touch-target")))
               .andExpect(content().string(containsString("table-responsive")));
    }

    @Test
    @DisplayName("GET /mailboxes/{boxId}/mails should list emails for mailbox owner")
    public void testListMailsFullPage() throws Exception
    {
        mockMvc.perform(get("/mailboxes/" + aliceBox.getId() + "/mails")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/mails"))
               .andExpect(model().attributeExists("mailbox"))
               .andExpect(model().attributeExists("mails"));
    }

    @Test
    @DisplayName("GET /mailboxes/{boxId}/mails with HX-Request should return fragment")
    public void testListMailsFragment() throws Exception
    {
        mockMvc.perform(get("/mailboxes/" + aliceBox.getId() + "/mails")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mail-list :: mailList"));
    }

    /**
     * Verifies that requesting email details with an HX-Request header returns the partial
     * detail fragment for inline display in the email viewer pane.
     *
     * @throws Exception if an error occurs during request execution
     */
    @Test
    @DisplayName("GET /mailboxes/{boxId}/mails/{mailId} with HX-Request should return detail fragment")
    public void testViewMailDetailFragmentWithHtmx() throws Exception
    {
        // GIVEN: an existing mailbox and email belonging to the authenticated user alice
        final String requestUrl = "/mailboxes/" + aliceBox.getId() + "/mails/" + testMail.getId();

        // WHEN: requesting the email details with the HTMX request header
        final MvcResult result = mockMvc.perform(get(requestUrl)
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER")))
               // THEN: response status is 200 OK and renders the email detail fragment
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mail-detail :: detail"))
               .andExpect(model().attributeExists("mail"))
               .andExpect(model().attributeExists("mailbox"))
               .andExpect(model().attributeExists("sanitizedHtml"))
               .andExpect(model().attributeExists("headers"))
               .andExpect(model().attributeExists("attachments"))
               .andExpect(model().attributeExists("dateFormatter"))
               .andReturn();

        final Mail returnedMail = (Mail) result.getModelAndView().getModel().get("mail");
        assertNotNull(returnedMail);
        assertEquals(testMail.getId(), returnedMail.getId());
    }

    /**
     * Verifies that requesting a non-existent email with an HX-Request header returns 404 Not Found.
     *
     * @throws Exception if an error occurs during request execution
     */
    @Test
    @DisplayName("GET /mailboxes/{boxId}/mails/{mailId} with non-existent mailId returns 404 Not Found")
    public void testViewMailNotFoundWithHtmx() throws Exception
    {
        // GIVEN: a non-existent mail identifier
        final long nonExistentMailId = 999_999L;
        final String requestUrl = "/mailboxes/" + aliceBox.getId() + "/mails/" + nonExistentMailId;

        // WHEN: requesting non-existent mail with HX-Request header
        // THEN: response status is 404 Not Found
        mockMvc.perform(get(requestUrl)
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isNotFound());
    }

    /**
     * Verifies that a user cannot retrieve email details from another user's mailbox via HTMX.
     *
     * @throws Exception if an error occurs during request execution
     */
    @Test
    @DisplayName("GET /mailboxes/{boxId}/mails/{mailId} for another user's mailbox returns 403 Forbidden")
    public void testViewMailForbiddenForOtherUserWithHtmx() throws Exception
    {
        // GIVEN: an email belonging to alice
        final String requestUrl = "/mailboxes/" + aliceBox.getId() + "/mails/" + testMail.getId();

        // WHEN: bob attempts to request alice's email with HX-Request header
        // THEN: response status is 403 Forbidden
        mockMvc.perform(get(requestUrl)
               .header("HX-Request", "true")
               .with(user("bob@xcmailr.test").roles("USER")))
               .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /mailboxes/{boxId}/mails/{mailId} should sanitize HTML")
    public void testViewMailSanitizedHtml() throws Exception
    {
        final MvcResult result = mockMvc.perform(get("/mailboxes/" + aliceBox.getId() + "/mails/" + testMail.getId())
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/mail-view"))
               .andExpect(model().attributeExists("sanitizedHtml"))
               .andExpect(model().attributeExists("attachments"))
               .andReturn();

        final String sanitized = (String) result.getModelAndView().getModel().get("sanitizedHtml");
        assertNotNull(sanitized);
        assertTrue(sanitized.contains("Welcome to <b>XCMailr</b>!"));
        assertFalse(sanitized.contains("<script>"), "Malicious script tag must be stripped by sanitizer");
    }

    @Test
    @DisplayName("GET /mailboxes/{boxId}/mails/{mailId}/attachments/{index} should stream attachment")
    public void testDownloadAttachment() throws Exception
    {
        mockMvc.perform(get("/mailboxes/" + aliceBox.getId() + "/mails/" + testMail.getId() + "/attachments/0")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(header().string("Content-Disposition", containsString("filename=\"invoice.pdf\"")))
               .andExpect(content().string("invoice-binary-data"));
    }

    @Test
    @DisplayName("GET /mailboxes/{boxId}/mails/{mailId}/raw should download .eml RFC822")
    public void testDownloadRawEml() throws Exception
    {
        mockMvc.perform(get("/mailboxes/" + aliceBox.getId() + "/mails/" + testMail.getId() + "/raw")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(header().string("Content-Type", "message/rfc822"));
    }

    @Test
    @DisplayName("DELETE /mailboxes/{boxId}/mails/{mailId} should delete single mail")
    public void testDeleteMail() throws Exception
    {
        mockMvc.perform(delete("/mailboxes/" + aliceBox.getId() + "/mails/" + testMail.getId())
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf()))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailListChanged"));

        assertFalse(mailRepository.existsById(testMail.getId()));
    }

    @Test
    @DisplayName("DELETE /mailboxes/{boxId}/mails should purge all mails in mailbox")
    public void testPurgeMails() throws Exception
    {
        mockMvc.perform(delete("/mailboxes/" + aliceBox.getId() + "/mails")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf()))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailListChanged"));

        assertEquals(0, mailRepository.countByMailbox(aliceBox.getId()));
    }

    @Test
    @DisplayName("Bob cannot view Alice's email (403 Forbidden)")
    public void testAccessDeniedForOtherUser() throws Exception
    {
        mockMvc.perform(get("/mailboxes/" + aliceBox.getId() + "/mails/" + testMail.getId())
               .with(user("bob@xcmailr.test").roles("USER")))
               .andExpect(status().isForbidden());
    }

    // =========================================================================
    // UTF-8 Email Rendering & Attachment Tests (Pyramid Top Tier)
    // =========================================================================

    @Test
    @DisplayName("GET /mailboxes/{boxId}/mails renders unmangled UTF-8 subject and sender without mojibake")
    public void testListMailsUtf8Rendering() throws Exception
    {
        final String rawEmail = "From: \"Jürgen Schmidt\" <juergen@external.com>\r\n"
            + "To: inbox@xcmailr.test\r\n"
            + "Subject: Docker Smoke Test ü\r\n"
            + "Content-Type: text/plain; charset=UTF-8\r\n\r\n"
            + "Testing UTF-8 reception with ü and 🚀.";

        final Mail utf8Mail = new Mail();
        utf8Mail.setSender("\"Jürgen Schmidt\" <juergen@external.com>");
        utf8Mail.setSubject("Docker Smoke Test ü");
        utf8Mail.setReceiveTime(System.currentTimeMillis() + 5000L);
        utf8Mail.setMessage(rawEmail.getBytes(StandardCharsets.UTF_8));
        utf8Mail.setMailbox(aliceBox);
        mailRepository.save(utf8Mail);

        // Verify full-page rendering
        mockMvc.perform(get("/mailboxes/" + aliceBox.getId() + "/mails")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Docker Smoke Test ü")))
               .andExpect(content().string(containsString("Jürgen Schmidt")))
               .andExpect(content().string(not(containsString("Docker Smoke Test Ã¼"))));

        // Verify HTMX partial list rendering
        mockMvc.perform(get("/mailboxes/" + aliceBox.getId() + "/mails")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Docker Smoke Test ü")))
               .andExpect(content().string(containsString("Jürgen Schmidt")))
               .andExpect(content().string(not(containsString("Docker Smoke Test Ã¼"))));
    }

    @Test
    @DisplayName("GET /mailboxes/{boxId}/mails/{mailId} renders UTF-8 subject, sender, body, and raw headers")
    public void testViewMailDetailAndHeadersUtf8Rendering() throws Exception
    {
        final String rawEmail = "From: \"Élodie & 太郎\" <elodie@external.com>\r\n"
            + "To: inbox@xcmailr.test\r\n"
            + "Subject: Internationalized 🚀 Subject\r\n"
            + "Content-Type: text/plain; charset=UTF-8\r\n\r\n"
            + "Body with emojis 🚀 and accents: é, à, ç.";

        final Mail utf8Mail = new Mail();
        utf8Mail.setSender("\"Élodie & 太郎\" <elodie@external.com>");
        utf8Mail.setSubject("Internationalized 🚀 Subject");
        utf8Mail.setReceiveTime(System.currentTimeMillis() + 5000L);
        utf8Mail.setMessage(rawEmail.getBytes(StandardCharsets.UTF_8));
        utf8Mail.setMailbox(aliceBox);
        mailRepository.save(utf8Mail);

        final MvcResult result = mockMvc.perform(get("/mailboxes/" + aliceBox.getId() + "/mails/" + utf8Mail.getId())
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mail-detail :: detail"))
               .andExpect(model().attributeExists("headers"))
               .andExpect(model().attributeExists("plainText"))
               .andReturn();

        final String headers = (String) result.getModelAndView().getModel().get("headers");
        assertNotNull(headers);
        assertTrue(headers.contains("Subject: Internationalized 🚀 Subject"));
        assertTrue(headers.contains("From: \"Élodie & 太郎\" <elodie@external.com>"));

        final String plainText = (String) result.getModelAndView().getModel().get("plainText");
        assertNotNull(plainText);
        assertTrue(plainText.contains("Body with emojis 🚀 and accents: é, à, ç."));

        final String htmlResponse = result.getResponse().getContentAsString();
        assertTrue(htmlResponse.contains("Internationalized 🚀 Subject"));
    }

    @Test
    @DisplayName("GET /mailboxes/{boxId}/mails/{mailId}/attachments/{index} formats Content-Disposition for UTF-8 filenames")
    public void testDownloadAttachmentWithUtf8Filename() throws Exception
    {
        final Session session = Session.getInstance(new java.util.Properties());
        final MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("sender@external.com"));
        message.setRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress("inbox@xcmailr.test"));
        message.setSubject("UTF-8 Attachment Test");

        final MimeMultipart multipart = new MimeMultipart("mixed");

        final MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Message with German attachment name");
        multipart.addBodyPart(textPart);

        final MimeBodyPart attachPart = new MimeBodyPart();
        final ByteArrayDataSource ds = new ByteArrayDataSource("pdf-payload-bytes".getBytes(StandardCharsets.UTF_8), "application/pdf");
        attachPart.setDataHandler(new DataHandler(ds));
        attachPart.setFileName("prüfbericht.pdf");
        multipart.addBodyPart(attachPart);

        message.setContent(multipart);
        message.saveChanges();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);

        final Mail mail = new Mail();
        mail.setSender("sender@external.com");
        mail.setSubject("UTF-8 Attachment Test");
        mail.setReceiveTime(System.currentTimeMillis() + 6000L);
        mail.setMessage(baos.toByteArray());
        mail.setMailbox(aliceBox);
        mailRepository.save(mail);

        mockMvc.perform(get("/mailboxes/" + aliceBox.getId() + "/mails/" + mail.getId() + "/attachments/0")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(header().string("Content-Disposition", containsString("filename*=UTF-8''pr%C3%BCfbericht.pdf")))
               .andExpect(content().string("pdf-payload-bytes"));
    }

    @Test
    @DisplayName("GET /mailboxes/{boxId}/mails/{mailId}/attachments/{index} returns 404 for out of bounds index")
    public void testDownloadAttachmentOutOfBoundsIndexReturnsNotFound() throws Exception
    {
        mockMvc.perform(get("/mailboxes/" + aliceBox.getId() + "/mails/" + testMail.getId() + "/attachments/99")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isNotFound());

        mockMvc.perform(get("/mailboxes/" + aliceBox.getId() + "/mails/" + testMail.getId() + "/attachments/-1")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isNotFound());
    }
}
