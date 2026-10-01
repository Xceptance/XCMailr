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
package controllers.restapi;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import com.xceptance.xcmailr.util.JakartaMimeMessageParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import jakarta.mail.internet.MimeMessage;
import models.MBox;
import models.Mail;
import models.User;

/**
 * Integration characterization tests for {@code /api/v1/mails} endpoints verifying
 * compatibility with legacy JSON schemas, query filtering, attachment streaming, and security.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = com.xceptance.xcmailr.XcmailrApplication.class)
@ActiveProfiles("test")
@Transactional
public class MailApiIT
{
    private static final String API_PREFIX = "/api/v1/mails";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

    private MockMvc mockMvc;
    private User testUser;
    private String token;
    private MBox userMailbox;
    private Mail multiPartMail;
    private Mail attachmentMail;

    @BeforeEach
    public void setUp() throws Exception
    {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                                 .apply(springSecurity())
                                 .build();

        testUser = new User("Jane", "Doe", "jane.doe@xcmailr.test", "$2a$10$abcdefghijklmnopqrstuv", "en");
        testUser.setActive(true);
        testUser.setApiToken("test-mail-token-" + System.nanoTime());
        testUser = userRepository.save(testUser);
        token = testUser.getApiToken();

        userMailbox = new MBox("janemailbox", "xcmailr.test", System.currentTimeMillis() + 3600000, false, testUser);
        userMailbox = mailboxRepository.save(userMailbox);

        multiPartMail = createMailFromResource(userMailbox, "/testmails/multiPart.eml", 1000);
        attachmentMail = createMailFromResource(userMailbox, "/testmails/MailWithAttachments.eml", 2000);
    }

    private Mail createMailFromResource(final MBox mailbox, final String resourcePath, final long timeOffset) throws Exception
    {
        final URL url = getClass().getResource(resourcePath);
        final byte[] bytes;
        try (final InputStream in = url.openStream())
        {
            bytes = IOUtils.toByteArray(in);
        }

        final Mail mail = new Mail();
        mail.setMailbox(mailbox);
        mail.setMessage(bytes);
        mail.setReceiveTime(System.currentTimeMillis() + timeOffset);

        try
        {
            final jakarta.mail.internet.MimeMessage mime = JakartaMimeMessageParser.createMimeMessage(bytes);
            mail.setSender(mime.getFrom() != null && mime.getFrom().length > 0 ? mime.getFrom()[0].toString() : "sender@test.com");
            mail.setSubject(mime.getSubject() != null ? mime.getSubject() : "");
        }
        catch (final Exception e)
        {
            mail.setSender("sender@test.com");
            mail.setSubject("Test Mail");
        }

        return mailRepository.save(mail);
    }

    @Test
    @DisplayName("listMails on empty mailbox returns 200 OK with empty array")
    public void testListMailsEmptyMailbox() throws Exception
    {
        final MBox emptyBox = new MBox("emptybox", "xcmailr.test", System.currentTimeMillis() + 3600000, false, testUser);
        mailboxRepository.save(emptyBox);

        mockMvc.perform(get(API_PREFIX)
               .param("mailboxAddress", "emptybox@xcmailr.test")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("listMails on non-empty mailbox returns 200 OK with all mails")
    public void testListMailsNonEmptyMailbox() throws Exception
    {
        mockMvc.perform(get(API_PREFIX)
               .param("mailboxAddress", userMailbox.getFullAddress())
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("listMails with lastMatch=true returns only the most recent mail")
    public void testListMailsWithLastMatchOnly() throws Exception
    {
        mockMvc.perform(get(API_PREFIX)
               .param("mailboxAddress", userMailbox.getFullAddress())
               .param("lastMatch", "true")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(1)))
               .andExpect(jsonPath("$[0].subject", is(attachmentMail.getSubject())));
    }

    @Test
    @DisplayName("listMails with subject regex filter returns matching mails")
    public void testListMailsWithSubjectFilter() throws Exception
    {
        mockMvc.perform(get(API_PREFIX)
               .param("mailboxAddress", userMailbox.getFullAddress())
               .param("subject", "attachments")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(1)))
               .andExpect(jsonPath("$[0].subject", is(attachmentMail.getSubject())));

        mockMvc.perform(get(API_PREFIX)
               .param("mailboxAddress", userMailbox.getFullAddress())
               .param("subject", "nonexistent-subject")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("listMails with invalid subject regex pattern returns 400 Bad Request")
    public void testListMailsInvalidSubjectPattern() throws Exception
    {
        mockMvc.perform(get(API_PREFIX)
               .param("mailboxAddress", userMailbox.getFullAddress())
               .param("subject", "[unclosed-bracket")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors[0].parameter", is("subject")));
    }

    @Test
    @DisplayName("listMails with invalid mailbox address returns 400 Bad Request")
    public void testListMailsInvalidMailboxAddress() throws Exception
    {
        mockMvc.perform(get(API_PREFIX)
               .param("mailboxAddress", "invalid-address")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors[0].parameter", is("mailboxAddress")));
    }

    @Test
    @DisplayName("listMails on non-existent mailbox returns 404 Not Found")
    public void testListMailsUnknownMailbox() throws Exception
    {
        mockMvc.perform(get(API_PREFIX)
               .param("mailboxAddress", "unknown@xcmailr.test")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("listMails on another user's mailbox returns 403 Forbidden")
    public void testListMailsSomeoneElsesMailbox() throws Exception
    {
        final User otherUser = new User("Other", "User", "other@xcmailr.test", "hash", "en");
        otherUser.setActive(true);
        otherUser.setApiToken("other-token-" + System.nanoTime());
        userRepository.save(otherUser);

        final MBox otherBox = new MBox("otherbox", "xcmailr.test", System.currentTimeMillis() + 3600000, false, otherUser);
        mailboxRepository.save(otherBox);

        mockMvc.perform(get(API_PREFIX)
               .param("mailboxAddress", "otherbox@xcmailr.test")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.errors[0].parameter", is("mailboxAddress")));
    }

    @Test
    @DisplayName("getMail returns details for existing mail")
    public void testGetMailSuccess() throws Exception
    {
        mockMvc.perform(get(API_PREFIX + "/" + attachmentMail.getId())
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id", is((int) attachmentMail.getId())))
               .andExpect(jsonPath("$.subject", is(attachmentMail.getSubject())));
    }

    @Test
    @DisplayName("getMail with invalid ID returns 400 Bad Request")
    public void testGetMailInvalidId() throws Exception
    {
        mockMvc.perform(get(API_PREFIX + "/not-a-number")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors[0].parameter", is("mailId")));
    }

    @Test
    @DisplayName("getMail for non-existent mail returns 404 Not Found")
    public void testGetMailNotFound() throws Exception
    {
        mockMvc.perform(get(API_PREFIX + "/99999999")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getMail for another user's mail returns 403 Forbidden")
    public void testGetMailSomeoneElsesMail() throws Exception
    {
        final User otherUser = new User("Other5", "User", "other5@xcmailr.test", "hash", "en");
        otherUser.setActive(true);
        otherUser.setApiToken("other5-token-" + System.nanoTime());
        userRepository.save(otherUser);

        final MBox otherBox = new MBox("otherbox5", "xcmailr.test", System.currentTimeMillis() + 3600000, false, otherUser);
        mailboxRepository.save(otherBox);

        final Mail otherMail = createMailFromResource(otherBox, "/testmails/multiPart.eml", 1000);

        mockMvc.perform(get(API_PREFIX + "/" + otherMail.getId())
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.errors[0].parameter", is("mailId")));
    }

    @Test
    @DisplayName("getMailAttachment returns binary content with appropriate content type")
    public void testGetMailAttachmentSuccess() throws Exception
    {
        mockMvc.perform(get(API_PREFIX + "/" + attachmentMail.getId() + "/attachments/test.pdf")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("application/pdf")));

        mockMvc.perform(get(API_PREFIX + "/" + attachmentMail.getId() + "/attachments/test.png")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("image/png")));
    }

    @Test
    @DisplayName("getMailAttachment with non-existent attachment returns 404 Not Found")
    public void testGetMailAttachmentNotFound() throws Exception
    {
        mockMvc.perform(get(API_PREFIX + "/" + attachmentMail.getId() + "/attachments/nonexistent.pdf")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deleteMail deletes mail and returns 204 No Content")
    public void testDeleteMailSuccess() throws Exception
    {
        final Mail doomed = createMailFromResource(userMailbox, "/testmails/multiPart.eml", 5000);
        assertTrue(mailRepository.existsById(doomed.getId()));

        mockMvc.perform(delete(API_PREFIX + "/" + doomed.getId())
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isNoContent());

        assertFalse(mailRepository.existsById(doomed.getId()));
    }

    @Test
    @DisplayName("deleteMail for non-existent mail returns 404 Not Found")
    public void testDeleteMailNotFound() throws Exception
    {
        mockMvc.perform(delete(API_PREFIX + "/99999999")
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deleteMail for another user's mail returns 403 Forbidden")
    public void testDeleteMailSomeoneElsesMail() throws Exception
    {
        final User otherUser = new User("Other6", "User", "other6@xcmailr.test", "hash", "en");
        otherUser.setActive(true);
        otherUser.setApiToken("other6-token-" + System.nanoTime());
        userRepository.save(otherUser);

        final MBox otherBox = new MBox("otherbox6", "xcmailr.test", System.currentTimeMillis() + 3600000, false, otherUser);
        mailboxRepository.save(otherBox);

        final Mail otherMail = createMailFromResource(otherBox, "/testmails/multiPart.eml", 1000);

        mockMvc.perform(delete(API_PREFIX + "/" + otherMail.getId())
               .header("Authorization", "Bearer " + token))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.errors[0].parameter", is("mailId")));
    }
}
