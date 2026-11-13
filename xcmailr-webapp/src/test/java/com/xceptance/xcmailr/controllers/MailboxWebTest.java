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
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.xceptance.xcmailr.models.Domain;
import com.xceptance.xcmailr.models.MBox;
import com.xceptance.xcmailr.models.User;
import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

/**
 * WebMvc slice integration test suite for mailbox validity extension controls
 * and endpoints in {@link MailboxWebController}.
 * <p>
 * Tests cover the testing pyramid for Layer 2:
 * <ul>
 *   <li>Happy path HTMX and non-HTMX requests for {@code POST /mailboxes/{id}/edit} and {@code POST /mailboxes/{id}/extend}</li>
 *   <li>Error and security isolation cases (cross-tenant access, non-existent mailbox, CSRF protection, authentication)</li>
 *   <li>Template fragment rendering for Edit Mailbox modal duration pills (English and German)</li>
 *   <li>Template fragment rendering for table quick action direct buttons and kebab menu options (English and German)</li>
 * </ul>
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public final class MailboxWebTest
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

    private User alice;
    private User bob;
    private User hansGerman;
    private Domain domain;

    /**
     * Resets repository fixtures and configures MockMvc with Spring Security before each test.
     */
    @BeforeEach
    public void setUp()
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

        alice = new User("Alice", "Smith", "alice@xcmailr.test", "Password123", "en");
        alice.setActive(true);
        userRepository.save(alice);

        bob = new User("Bob", "Jones", "bob@xcmailr.test", "Password123", "en");
        bob.setActive(true);
        userRepository.save(bob);

        hansGerman = new User("Hans", "Muster", "hans@xcmailr.test", "Password123", "de");
        hansGerman.setActive(true);
        userRepository.save(hansGerman);
    }

    // =========================================================================
    // 1. HAPPY PATH TESTS: POST /mailboxes/{id}/edit WITH extendHours (Task 3.1)
    // =========================================================================

    /**
     * Verifies happy path: HTMX request to edit mailbox with extendHours=24 extends validity by 24 hours.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy Path: HTMX POST /mailboxes/{id}/edit with extendHours=24 extends validity and returns HX-Trigger")
    public void testUpdateMailbox_extendHours24_htmx() throws Exception
    {
        final long initialExpiration = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2);
        final MBox box = new MBox("test-box-24", "xcmailr.test", initialExpiration, false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/edit")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("extendHours", "24")
               .param("active", "true")
               .param("forwardEmails", "true")
               .param("comment", "Extended by 1 day"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged, closeModal"));

        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertEquals(initialExpiration + TimeUnit.HOURS.toMillis(24), updated.getTs_Active(), 1000L);
        assertFalse(updated.isExpired());
        assertTrue(updated.isActive());
        assertEquals("Extended by 1 day", updated.getComment());
    }

    /**
     * Verifies happy path: HTMX request to edit mailbox with extendHours=168 extends validity by 7 days.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy Path: HTMX POST /mailboxes/{id}/edit with extendHours=168 extends validity by 7 days")
    public void testUpdateMailbox_extendHours168_htmx() throws Exception
    {
        final long initialExpiration = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(5);
        final MBox box = new MBox("test-box-168", "xcmailr.test", initialExpiration, false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/edit")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("extendHours", "168")
               .param("active", "true"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged, closeModal"));

        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertEquals(initialExpiration + TimeUnit.HOURS.toMillis(168), updated.getTs_Active(), 1000L);
        assertFalse(updated.isExpired());
    }

    /**
     * Verifies happy path: HTMX request to edit mailbox with extendHours=720 extends validity by 30 days.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy Path: HTMX POST /mailboxes/{id}/edit with extendHours=720 extends validity by 30 days")
    public void testUpdateMailbox_extendHours720_htmx() throws Exception
    {
        final long initialExpiration = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);
        final MBox box = new MBox("test-box-720", "xcmailr.test", initialExpiration, false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/edit")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("extendHours", "720")
               .param("active", "true"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged, closeModal"));

        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertEquals(initialExpiration + TimeUnit.HOURS.toMillis(720), updated.getTs_Active(), 1000L);
        assertFalse(updated.isExpired());
    }

    /**
     * Verifies happy path: HTMX request to edit mailbox with extendHours=-1 sets validity to unlimited.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy Path: HTMX POST /mailboxes/{id}/edit with extendHours=-1 sets validity to unlimited")
    public void testUpdateMailbox_extendHoursUnlimited_htmx() throws Exception
    {
        final long initialExpiration = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2);
        final MBox box = new MBox("test-box-unlimited", "xcmailr.test", initialExpiration, false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/edit")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("extendHours", "-1")
               .param("active", "true"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged, closeModal"));

        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertEquals(0L, updated.getTs_Active());
        assertFalse(updated.isExpired());
    }

    /**
     * Verifies happy path: HTMX request to edit mailbox with extendHours=0 preserves current expiration timestamp.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy Path: HTMX POST /mailboxes/{id}/edit with extendHours=0 preserves current expiration")
    public void testUpdateMailbox_extendHoursKeepCurrent_htmx() throws Exception
    {
        final long initialExpiration = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(10);
        final MBox box = new MBox("test-box-keep", "xcmailr.test", initialExpiration, false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/edit")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("extendHours", "0")
               .param("active", "true")
               .param("comment", "Keep expiration"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged, closeModal"));

        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertEquals(initialExpiration, updated.getTs_Active());
        assertEquals("Keep expiration", updated.getComment());
    }

    /**
     * Verifies happy path: non-HTMX standard form submit to edit mailbox redirects to /mailboxes.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy Path: Non-HTMX POST /mailboxes/{id}/edit redirects to /mailboxes")
    public void testUpdateMailbox_nonHtmx_redirectsToMailboxes() throws Exception
    {
        final MBox box = new MBox("test-box-redirect", "xcmailr.test", System.currentTimeMillis() + 10000L, false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/edit")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("extendHours", "24")
               .param("active", "true"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/mailboxes?updated"));
    }

    // =========================================================================
    // 2. HAPPY PATH TESTS: POST /mailboxes/{id}/extend (Task 3.1)
    // =========================================================================

    /**
     * Verifies happy path: HTMX request to extend mailbox by 24 hours.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy Path: HTMX POST /mailboxes/{id}/extend?hours=24 extends validity and returns HX-Trigger")
    public void testExtendMailbox_hours24_htmx() throws Exception
    {
        final long initialExpiration = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(4);
        final MBox box = new MBox("extend-24", "xcmailr.test", initialExpiration, false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/extend")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("hours", "24"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertEquals(initialExpiration + TimeUnit.HOURS.toMillis(24), updated.getTs_Active(), 1000L);
        assertFalse(updated.isExpired());
    }

    /**
     * Verifies happy path: HTMX request to extend mailbox by 168 hours (+7d).
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy Path: HTMX POST /mailboxes/{id}/extend?hours=168 extends validity by 7 days")
    public void testExtendMailbox_hours168_htmx() throws Exception
    {
        final long initialExpiration = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(4);
        final MBox box = new MBox("extend-168", "xcmailr.test", initialExpiration, false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/extend")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("hours", "168"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertEquals(initialExpiration + TimeUnit.HOURS.toMillis(168), updated.getTs_Active(), 1000L);
    }

    /**
     * Verifies happy path: HTMX request to extend mailbox by 720 hours (+30d).
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy Path: HTMX POST /mailboxes/{id}/extend?hours=720 extends validity by 30 days")
    public void testExtendMailbox_hours720_htmx() throws Exception
    {
        final long initialExpiration = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(4);
        final MBox box = new MBox("extend-720", "xcmailr.test", initialExpiration, false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/extend")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("hours", "720"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertEquals(initialExpiration + TimeUnit.HOURS.toMillis(720), updated.getTs_Active(), 1000L);
    }

    /**
     * Verifies happy path: HTMX request to extend mailbox with hours=-1 sets validity to unlimited.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy Path: HTMX POST /mailboxes/{id}/extend?hours=-1 sets validity to unlimited")
    public void testExtendMailbox_hoursUnlimited_htmx() throws Exception
    {
        final long initialExpiration = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(4);
        final MBox box = new MBox("extend-unlimited", "xcmailr.test", initialExpiration, false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/extend")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("hours", "-1"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertEquals(0L, updated.getTs_Active());
        assertFalse(updated.isExpired());
    }

    /**
     * Verifies happy path: non-HTMX request to extend mailbox redirects to /mailboxes.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy Path: Non-HTMX POST /mailboxes/{id}/extend redirects to /mailboxes")
    public void testExtendMailbox_nonHtmx_redirectsToMailboxes() throws Exception
    {
        final MBox box = new MBox("extend-redirect", "xcmailr.test", System.currentTimeMillis() + 10000L, false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/extend")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("hours", "24"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/mailboxes?extended"));
    }

    // =========================================================================
    // 3. SECURITY AND ERROR CASES (Task 3.1)
    // =========================================================================

    /**
     * Verifies security case: editing another tenant's mailbox is rejected with HTTP 403 Forbidden.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Security Case: POST /mailboxes/{id}/edit on another tenant's mailbox returns 403 Forbidden")
    public void testUpdateMailbox_crossTenant_forbidden() throws Exception
    {
        final MBox bobsBox = new MBox("bobs-box", "xcmailr.test", System.currentTimeMillis() + 10000L, false, bob);
        mailboxRepository.save(bobsBox);

        mockMvc.perform(post("/mailboxes/" + bobsBox.getId() + "/edit")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("extendHours", "24"))
               .andExpect(status().isForbidden());
    }

    /**
     * Verifies security case: extending another tenant's mailbox is rejected with HTTP 403 Forbidden.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Security Case: POST /mailboxes/{id}/extend on another tenant's mailbox returns 403 Forbidden")
    public void testExtendMailbox_crossTenant_forbidden() throws Exception
    {
        final MBox bobsBox = new MBox("bobs-extend", "xcmailr.test", System.currentTimeMillis() + 10000L, false, bob);
        mailboxRepository.save(bobsBox);

        mockMvc.perform(post("/mailboxes/" + bobsBox.getId() + "/extend")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("hours", "24"))
               .andExpect(status().isForbidden());
    }

    /**
     * Verifies error case: editing a non-existent mailbox returns HTTP 404 Not Found.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error Case: POST /mailboxes/{id}/edit for non-existent mailbox returns 404 Not Found")
    public void testUpdateMailbox_notFound_404() throws Exception
    {
        mockMvc.perform(post("/mailboxes/999999/edit")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("extendHours", "24"))
               .andExpect(status().isNotFound());
    }

    /**
     * Verifies error case: extending a non-existent mailbox returns HTTP 404 Not Found.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error Case: POST /mailboxes/{id}/extend for non-existent mailbox returns 404 Not Found")
    public void testExtendMailbox_notFound_404() throws Exception
    {
        mockMvc.perform(post("/mailboxes/999999/extend")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("hours", "24"))
               .andExpect(status().isNotFound());
    }

    /**
     * Verifies security case: editing a mailbox without CSRF token returns HTTP 403 Forbidden.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Security Case: POST /mailboxes/{id}/edit without CSRF token returns 403 Forbidden")
    public void testUpdateMailbox_missingCsrf_forbidden() throws Exception
    {
        final MBox box = new MBox("csrf-box", "xcmailr.test", System.currentTimeMillis() + 10000L, false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/edit")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .param("extendHours", "24"))
               .andExpect(status().isForbidden());
    }

    /**
     * Verifies security case: extending a mailbox without CSRF token returns HTTP 403 Forbidden.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Security Case: POST /mailboxes/{id}/extend without CSRF token returns 403 Forbidden")
    public void testExtendMailbox_missingCsrf_forbidden() throws Exception
    {
        final MBox box = new MBox("csrf-extend", "xcmailr.test", System.currentTimeMillis() + 10000L, false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/extend")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .param("hours", "24"))
               .andExpect(status().isForbidden());
    }

    /**
     * Verifies security case: unauthenticated request to edit mailbox redirects to login.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Security Case: Unauthenticated POST /mailboxes/{id}/edit redirects to login")
    public void testUpdateMailbox_unauthenticated_redirectsToLogin() throws Exception
    {
        final MBox box = new MBox("unauth-box", "xcmailr.test", System.currentTimeMillis() + 10000L, false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/edit")
               .with(csrf())
               .param("extendHours", "24"))
               .andExpect(status().is3xxRedirection());
    }

    /**
     * Verifies security case: unauthenticated request to extend mailbox redirects to login.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Security Case: Unauthenticated POST /mailboxes/{id}/extend redirects to login")
    public void testExtendMailbox_unauthenticated_redirectsToLogin() throws Exception
    {
        final MBox box = new MBox("unauth-extend", "xcmailr.test", System.currentTimeMillis() + 10000L, false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/" + box.getId() + "/extend")
               .with(csrf())
               .param("hours", "24"))
               .andExpect(status().is3xxRedirection());
    }

    // =========================================================================
    // 4. TEMPLATE RENDERING: EDIT MAILBOX MODAL FRAGMENT (Task 3.2)
    // =========================================================================

    /**
     * Verifies that the Edit Mailbox modal fragment renders all duration radio button pills with
     * default checked state on "Keep current" in English.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("UI Rendering: GET /mailboxes/{id}/edit renders duration radio pill group in English")
    public void testEditMailboxModal_rendersDurationRadioGroupEnglish() throws Exception
    {
        final MBox box = new MBox("modal-test", "xcmailr.test", System.currentTimeMillis() + TimeUnit.HOURS.toMillis(5), false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes/" + box.getId() + "/edit")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Extend Validity By")))
               .andExpect(content().string(containsString("name=\"extendHours\"")))
               .andExpect(content().string(containsString("id=\"extend-0\"")))
               .andExpect(content().string(containsString("checked")))
               .andExpect(content().string(containsString("Keep current")))
               .andExpect(content().string(containsString("id=\"extend-24\"")))
               .andExpect(content().string(containsString("+1d")))
               .andExpect(content().string(containsString("id=\"extend-168\"")))
               .andExpect(content().string(containsString("+7d")))
               .andExpect(content().string(containsString("id=\"extend-720\"")))
               .andExpect(content().string(containsString("+30d")))
               .andExpect(content().string(containsString("id=\"extend-unlimited\"")))
               .andExpect(content().string(containsString("Unlimited")));
    }

    /**
     * Verifies that the Edit Mailbox modal fragment renders all duration radio button pills with
     * German localized labels when viewed by a German user.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("UI Rendering: GET /mailboxes/{id}/edit renders duration radio pill group in German")
    public void testEditMailboxModal_rendersDurationRadioGroupGerman() throws Exception
    {
        final MBox box = new MBox("modal-de", "xcmailr.test", System.currentTimeMillis() + TimeUnit.HOURS.toMillis(5), false, hansGerman);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes/" + box.getId() + "/edit")
               .with(user("hans@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Gültigkeit verlängern um")))
               .andExpect(content().string(containsString("Gegenwärtig beibehalten")))
               .andExpect(content().string(containsString("+1d")))
               .andExpect(content().string(containsString("+7d")))
               .andExpect(content().string(containsString("+30d")))
               .andExpect(content().string(containsString("Unbegrenzt")));
    }

    // =========================================================================
    // 5. TEMPLATE RENDERING: MAILBOX TABLE QUICK ACTIONS (Task 3.3)
    // =========================================================================

    /**
     * Verifies that the mailbox table renders direct 1-click extension buttons and dropdown items in English.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("UI Rendering: GET /mailboxes renders direct extension quick actions and dropdown in English")
    public void testMailboxTable_rendersDirectExtensionButtonsEnglish() throws Exception
    {
        final MBox box = new MBox("table-actions-en", "xcmailr.test", System.currentTimeMillis() + TimeUnit.HOURS.toMillis(5), false, alice);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               // Direct action buttons on desktop:
               .andExpect(content().string(containsString("hx-post=\"/mailboxes/" + box.getId() + "/extend?hours=24\"")))
               .andExpect(content().string(containsString("title=\"Extend validity by 24 hours (+1d)\"")))
               .andExpect(content().string(containsString("+1d")))
               .andExpect(content().string(containsString("hx-post=\"/mailboxes/" + box.getId() + "/extend?hours=168\"")))
               .andExpect(content().string(containsString("title=\"Extend validity by 7 days (+7d)\"")))
               .andExpect(content().string(containsString("+7d")))
               .andExpect(content().string(containsString("hx-post=\"/mailboxes/" + box.getId() + "/extend?hours=720\"")))
               .andExpect(content().string(containsString("title=\"Extend validity by 30 days (+30d)\"")))
               .andExpect(content().string(containsString("+30d")))
               .andExpect(content().string(containsString("hx-post=\"/mailboxes/" + box.getId() + "/extend?hours=-1\"")))
               .andExpect(content().string(containsString("title=\"Set validity to unlimited\"")))
               .andExpect(content().string(containsString("bi-infinity")))
               // Mobile dropdown menu items:
               .andExpect(content().string(containsString("Extend 24 Hours (+1d)")))
               .andExpect(content().string(containsString("Extend 7 Days (+7d)")))
               .andExpect(content().string(containsString("Extend 30 Days (+30d)")))
               .andExpect(content().string(containsString("Set Unlimited")));
    }

    /**
     * Verifies that the mailbox table renders direct 1-click extension buttons and dropdown items in German.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("UI Rendering: GET /mailboxes renders direct extension quick actions and dropdown in German")
    public void testMailboxTable_rendersDirectExtensionButtonsGerman() throws Exception
    {
        final MBox box = new MBox("table-actions-de", "xcmailr.test", System.currentTimeMillis() + TimeUnit.HOURS.toMillis(5), false, hansGerman);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
               .with(user("hans@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               // Direct action buttons on desktop tooltips:
               .andExpect(content().string(containsString("title=\"Gültigkeit um 24 Stunden verlängern (+1d)\"")))
               .andExpect(content().string(containsString("title=\"Gültigkeit um 7 Tage verlängern (+7d)\"")))
               .andExpect(content().string(containsString("title=\"Gültigkeit um 30 Tage verlängern (+30d)\"")))
               .andExpect(content().string(containsString("title=\"Gültigkeit auf unbegrenzt setzen\"")))
               // Mobile dropdown menu items in German:
               .andExpect(content().string(containsString("24 Stunden verlängern (+1d)")))
               .andExpect(content().string(containsString("Um 7 Tage verlängern (+7d)")))
               .andExpect(content().string(containsString("Um 30 Tage verlängern (+30d)")))
               .andExpect(content().string(containsString("Auf unbegrenzt setzen")));
    }
}
