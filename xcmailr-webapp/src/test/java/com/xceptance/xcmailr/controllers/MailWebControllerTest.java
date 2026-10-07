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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
    @DisplayName("GET /mails should list all emails across mailboxes for authenticated user")
    public void testListAllMailsFullPage() throws Exception
    {
        mockMvc.perform(get("/mails")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/all-mails"))
               .andExpect(model().attributeExists("mails"))
               .andExpect(model().attributeExists("mailboxAddressMap"));
    }

    @Test
    @DisplayName("GET /mails with HX-Request should return all-mail-list fragment")
    public void testListAllMailsFragment() throws Exception
    {
        mockMvc.perform(get("/mails")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/all-mail-list :: mailList"));
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
               .andExpect(header().string("Content-Disposition", "attachment; filename=\"invoice.pdf\""))
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
}
