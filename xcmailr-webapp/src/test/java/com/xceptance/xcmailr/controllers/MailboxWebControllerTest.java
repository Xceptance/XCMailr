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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.XcmailrApplication;
import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.Domain;
import models.MBox;
import models.Mail;
import models.User;

/**
 * Integration tests for {@link MailboxWebController} testing dashboard views, HTMX live search,
 * and CRUD operations with security enforcement.
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public class MailboxWebControllerTest
{
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User otherUser;
    private Domain domain;

    @BeforeEach
    public void setup()
    {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();

        domain = new Domain("xcmailr.test");
        domainRepository.save(domain);

        owner = new User("Alice", "Smith", "alice@xcmailr.test", "Password123", "en");
        owner.setActive(true);
        userRepository.save(owner);

        otherUser = new User("Bob", "Jones", "bob@xcmailr.test", "Password123", "en");
        otherUser.setActive(true);
        userRepository.save(otherUser);
    }

    @Test
    @DisplayName("GET /mailboxes should render full index page for authenticated user")
    public void testGetDashboardFullPage() throws Exception
    {
        final MBox box = new MBox("temp1", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/index"))
               .andExpect(model().attributeExists("mailboxes"))
               .andExpect(model().attributeExists("page"))
               .andExpect(model().attributeExists("domains"));
    }

    @Test
    @DisplayName("GET /mailboxes with HX-Request header should render table fragment")
    public void testGetDashboardHtmxFragment() throws Exception
    {
        final MBox box = new MBox("temp2", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(model().attributeExists("mailboxes"));
    }

    @Test
    @DisplayName("GET /mailboxes unauthenticated should redirect to /login")
    public void testGetDashboardUnauthenticated() throws Exception
    {
        mockMvc.perform(get("/mailboxes"))
               .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("POST /mailboxes via form should create mailbox and redirect")
    public void testCreateMailboxFormSubmit() throws Exception
    {
        mockMvc.perform(post("/mailboxes")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", "custom-box")
               .param("domain", "xcmailr.test")
               .param("durationHours", "24")
               .param("forwardEmails", "true"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/mailboxes?created"));

        final MBox box = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("custom-box", "xcmailr.test").orElse(null);
        assertNotNull(box, "Mailbox must be persisted in database");
        assertEquals(owner.getId(), box.getUsr().getId());
        assertTrue(box.isForwardEmails());
        assertFalse(box.isExpired());
    }

    @Test
    @DisplayName("GET /mailboxes/new should return modal fragment with domains and suggested address")
    public void testNewMailboxModal() throws Exception
    {
        mockMvc.perform(get("/mailboxes/new")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/modal-new :: modal"))
               .andExpect(model().attributeExists("domains", "suggestedAddress"));
    }

    @Test
    @DisplayName("POST /mailboxes via HTMX should return 200 with HX-Trigger and HX-Trigger-After-Swap headers")
    public void testCreateMailboxHtmx() throws Exception
    {
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", "htmx-box")
               .param("domain", "xcmailr.test")
               .param("durationHours", "1")
               .param("forwardEmails", "false"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged"))
               .andExpect(header().string("HX-Trigger-After-Swap", "closeModal"));

        final MBox box = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase("htmx-box", "xcmailr.test").orElse(null);
        assertNotNull(box);
        assertFalse(box.isForwardEmails());
    }

    @Test
    @DisplayName("POST /mailboxes via HTMX with invalid address should return modal with error")
    public void testCreateMailboxHtmxInvalidAddress() throws Exception
    {
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", "invalid@@address")
               .param("domain", "xcmailr.test"))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/modal-new :: modal"))
               .andExpect(model().attribute("error", "Invalid mailbox address characters."));
    }

    @Test
    @DisplayName("POST /mailboxes via HTMX with unknown domain should return modal with error")
    public void testCreateMailboxHtmxInvalidDomain() throws Exception
    {
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", "valid-box")
               .param("domain", "unknown-domain.test"))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/modal-new :: modal"))
               .andExpect(model().attribute("error", "Specified domain is not configured or whitelisted."));
    }

    @Test
    @DisplayName("POST /mailboxes via HTMX with duplicate address should return modal with error")
    public void testCreateMailboxHtmxDuplicateAddress() throws Exception
    {
        final MBox existing = new MBox("existing-box", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(existing);

        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", "existing-box")
               .param("domain", "xcmailr.test"))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/modal-new :: modal"))
               .andExpect(model().attribute("error", "A mailbox with this address already exists."));
    }

    @Test
    @DisplayName("POST /mailboxes/{id}/toggle should invert expired status")
    public void testToggleMailbox() throws Exception
    {
        final MBox box = new MBox("toggle-box", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/toggle")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf()))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertTrue(updated.isExpired(), "Mailbox should now be expired");
    }

    @Test
    @DisplayName("POST /mailboxes/{id}/extend should add hours to expiration")
    public void testExtendMailbox() throws Exception
    {
        final long initialTs = System.currentTimeMillis() + 1000L;
        final MBox box = new MBox("extend-box", "xcmailr.test", initialTs, true, owner);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/extend")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("hours", "48"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertFalse(updated.isExpired(), "Mailbox should be reactivated upon extension");
        assertTrue(updated.getTs_Active() > initialTs + TimeUnit.HOURS.toMillis(47));
    }

    @Test
    @DisplayName("POST /mailboxes/{id}/edit should update forward settings")
    public void testEditMailbox() throws Exception
    {
        final MBox box = new MBox("edit-box", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        box.setForwardEmails(true);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/edit")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("forwardEmails", "false")
               .param("active", "false"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertFalse(updated.isForwardEmails());
        assertTrue(updated.isExpired());
    }

    @Test
    @DisplayName("DELETE /mailboxes/{id} should remove mailbox and its emails")
    public void testDeleteMailbox() throws Exception
    {
        final MBox box = new MBox("delete-box", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        mailboxRepository.save(box);

        final Mail mail = new Mail();
        mail.setSender("sender@test.com");
        mail.setSubject("Test");
        mail.setReceiveTime(System.currentTimeMillis());
        mail.setMessage("test".getBytes());
        mail.setMailbox(box);
        mailRepository.save(mail);

        mockMvc.perform(delete("/mailboxes/" + box.getId())
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf()))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        assertFalse(mailboxRepository.existsById(box.getId()), "Mailbox must be deleted");
        assertEquals(0, mailRepository.countByMailbox(box.getId()), "Associated mails must be deleted");
    }

    @Test
    @DisplayName("Unauthorized user cannot modify another user's mailbox")
    public void testOwnershipSecurity() throws Exception
    {
        final MBox box = new MBox("alice-box", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        mailboxRepository.save(box);

        // Bob tries to delete Alice's mailbox
        mockMvc.perform(delete("/mailboxes/" + box.getId())
               .header("HX-Request", "true")
               .with(user("bob@xcmailr.test").roles("USER"))
               .with(csrf()))
               .andExpect(status().isForbidden());

        assertTrue(mailboxRepository.existsById(box.getId()), "Mailbox must not be deleted by another user");
    }
}
