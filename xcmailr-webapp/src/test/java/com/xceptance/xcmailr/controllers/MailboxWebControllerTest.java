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

import static org.hamcrest.Matchers.containsString;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.XcmailrApplication;
import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;
import com.xceptance.xcmailr.security.UserPrincipal;

import models.Domain;
import models.MBox;
import models.Mail;
import models.User;

/**
 * Integration slice test suite for {@link MailboxWebController} representing the middle layer
 * of the test pyramid.
 * <p>
 * Tests controller endpoints via Spring MockMvc across happy paths, error cases,
 * and special edge cases, including asynchronous HTMX partial renderings, CSRF validation,
 * ownership isolation, and fallback standard form workflows.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public final class MailboxWebControllerTest
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

    /**
     * Initializes the mock MVC environment and populates base test fixture records.
     */
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

    // =========================================================================
    // HAPPY PATH INTEGRATION TESTS
    // =========================================================================

    /**
     * Verifies that GET /mailboxes renders the complete HTML index dashboard for an authenticated user.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: GET /mailboxes renders full index view with model attributes")
    public void testGetDashboardFullPage() throws Exception
    {
        // GIVEN
        final MBox box = new MBox("temp1", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        mailboxRepository.save(box);

        // WHEN & THEN
        mockMvc.perform(get("/mailboxes")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/index"))
               .andExpect(model().attributeExists("mailboxes"))
               .andExpect(model().attributeExists("page"))
               .andExpect(model().attributeExists("domains"))
               .andExpect(model().attributeDoesNotExist("mailCounts"))
               .andExpect(model().attribute("sort", "id"))
               .andExpect(model().attribute("dir", "desc"));
    }

    /**
     * Verifies that the full dashboard page renders all five status filter options,
     * correctly setting the selected attribute on the active filter option.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: Full index dashboard renders status filter options including EXPIRING and UNLIMITED")
    public void testGetDashboardFullPage_StatusFilterOptions() throws Exception
    {
        // WHEN & THEN - request with EXPIRING selected
        mockMvc.perform(get("/mailboxes")
               .param("status", "EXPIRING")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/index"))
               .andExpect(content().string(containsString("<option value=\"ALL\">All Statuses</option>")))
               .andExpect(content().string(containsString("<option value=\"ACTIVE\">Active Only</option>")))
               .andExpect(content().string(containsString("<option value=\"EXPIRED\">Expired Only</option>")))
               .andExpect(content().string(containsString("<option value=\"EXPIRING\" selected=\"selected\">Expiring Only</option>")))
               .andExpect(content().string(containsString("<option value=\"UNLIMITED\">Unlimited Only</option>")));
    }

    /**
     * Verifies that GET /mailboxes with the HX-Request header returns only the mailbox table fragment.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: GET /mailboxes with HX-Request header renders partial table fragment")
    public void testGetDashboardHtmxFragment() throws Exception
    {
        // GIVEN
        final MBox box = new MBox("temp2", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        mailboxRepository.save(box);

        // WHEN & THEN
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(model().attributeExists("mailboxes"))
               .andExpect(model().attributeDoesNotExist("mailCounts"))
               .andExpect(model().attribute("sort", "id"))
               .andExpect(model().attribute("dir", "desc"));
    }

    /**
     * Verifies that GET /mailboxes/new returns the dynamic creation modal fragment.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: GET /mailboxes/new returns modal fragment with domain options and suggested address")
    public void testNewMailboxModal() throws Exception
    {
        // GIVEN
        final String username = "alice@xcmailr.test";

        // WHEN & THEN
        mockMvc.perform(get("/mailboxes/new")
               .with(user(username).roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/modal-new :: modal"))
               .andExpect(model().attributeExists("domains", "suggestedAddress"))
               .andExpect(model().attribute("selectedDuration", 2))
               .andExpect(model().attribute("forwardEmails", true));
    }

    /**
     * Verifies that POST /mailboxes via HTMX creates a mailbox and triggers client-side refresh events.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: POST /mailboxes via HTMX returns 200 with HX-Trigger and HX-Trigger-After-Swap headers")
    public void testCreateMailboxHtmx() throws Exception
    {
        // GIVEN
        final String address = "htmx-box";
        final String domainName = "xcmailr.test";

        // WHEN & THEN
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", address)
               .param("domain", domainName)
               .param("durationHours", "1")
               .param("forwardEmails", "false"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged, closeModal"))
               .andExpect(header().doesNotExist("HX-Trigger-After-Swap"));

        final MBox persistedBox = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(address, domainName).orElse(null);
        assertNotNull(persistedBox, "Mailbox must be stored in database");
        assertFalse(persistedBox.isForwardEmails(), "Forwarding should be disabled as configured");
        assertEquals(owner.getId(), persistedBox.getUsr().getId());
    }

    /**
     * Verifies that POST /mailboxes with forwardEmails=true creates and persists a mailbox with forwarding enabled.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: POST /mailboxes with forwardEmails=true creates and persists mailbox with forwarding enabled")
    public void testCreateMailboxWithForwardingEnabled() throws Exception
    {
        // GIVEN
        final String address = "forward-enabled-box";
        final String domainName = "xcmailr.test";

        // WHEN & THEN
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", address)
               .param("domain", domainName)
               .param("durationHours", "2")
               .param("forwardEmails", "true"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged, closeModal"));

        final MBox persistedBox = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(address, domainName).orElse(null);
        assertNotNull(persistedBox, "Mailbox must be stored in database");
        assertTrue(persistedBox.isForwardEmails(), "Forwarding should be enabled as configured");
        assertEquals(owner.getId(), persistedBox.getUsr().getId());
    }

    /**
     * Verifies that POST /mailboxes without forwardEmails parameter (unchecked checkbox) defaults to forwarding disabled.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: POST /mailboxes without forwardEmails parameter creates and persists mailbox with forwarding disabled")
    public void testCreateMailboxWithoutForwardingParamDefaultsToFalse() throws Exception
    {
        // GIVEN unchecked HTML checkbox results in omitted forwardEmails form parameter
        final String address = "forward-omitted-box";
        final String domainName = "xcmailr.test";

        // WHEN & THEN
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", address)
               .param("domain", domainName)
               .param("durationHours", "2"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged, closeModal"));

        final MBox persistedBox = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(address, domainName).orElse(null);
        assertNotNull(persistedBox, "Mailbox must be stored in database");
        assertFalse(persistedBox.isForwardEmails(), "Forwarding should default to disabled when checkbox is omitted");
        assertEquals(owner.getId(), persistedBox.getUsr().getId());
    }

    /**
     * Verifies that POST /mailboxes/{id}/toggle inverts the expired state of an existing mailbox.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: POST /mailboxes/{id}/toggle inverts expired status and emits HX-Trigger")
    public void testToggleMailbox() throws Exception
    {
        // GIVEN
        final MBox box = new MBox("toggle-box", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        mailboxRepository.save(box);

        // WHEN
        mockMvc.perform(post("/mailboxes/" + box.getId() + "/toggle")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf()))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        // THEN
        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertTrue(updated.isExpired(), "Mailbox must now be marked as expired");
    }

    /**
     * Verifies that POST /mailboxes/{id}/extend with 24 hours adds time and reactivates an expired mailbox.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: POST /mailboxes/{id}/extend with +24h extends expiration and reactivates mailbox")
    public void testExtendMailbox24Hours() throws Exception
    {
        // GIVEN
        final long initialTs = System.currentTimeMillis() + 1000L;
        final MBox box = new MBox("extend-24h-box", "xcmailr.test", initialTs, true, owner);
        mailboxRepository.save(box);

        // WHEN
        mockMvc.perform(post("/mailboxes/" + box.getId() + "/extend")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("hours", "24"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        // THEN
        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertFalse(updated.isExpired(), "Mailbox must be reactivated upon extension");
        assertTrue(updated.getTs_Active() > initialTs + TimeUnit.HOURS.toMillis(23),
            "Active timestamp must be incremented by at least 23 hours");
    }

    /**
     * Verifies that POST /mailboxes/{id}/edit updates forwarding and expiration attributes.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: POST /mailboxes/{id}/edit updates mailbox forwarding preferences and status")
    public void testEditMailbox() throws Exception
    {
        // GIVEN
        final MBox box = new MBox("edit-box", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        box.setForwardEmails(true);
        mailboxRepository.save(box);

        // WHEN
        mockMvc.perform(post("/mailboxes/" + box.getId() + "/edit")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("forwardEmails", "false")
               .param("active", "false"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged, closeModal"))
               .andExpect(header().doesNotExist("HX-Trigger-After-Swap"));

        // THEN
        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertFalse(updated.isForwardEmails(), "Forwarding should now be disabled");
        assertTrue(updated.isExpired(), "Mailbox should be set to expired");
    }

    /**
     * Verifies that DELETE /mailboxes/{id} removes the mailbox and cascades to associated emails.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: DELETE /mailboxes/{id} removes mailbox and cascades deletion to associated messages")
    public void testDeleteMailboxCascade() throws Exception
    {
        // GIVEN
        final MBox box = new MBox("cascade-box", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        mailboxRepository.save(box);

        final Mail mail = new Mail();
        mail.setSender("sender@test.com");
        mail.setSubject("Cascade Test");
        mail.setReceiveTime(System.currentTimeMillis());
        mail.setMessage("payload".getBytes());
        mail.setMailbox(box);
        mailRepository.save(mail);

        // WHEN
        mockMvc.perform(delete("/mailboxes/" + box.getId())
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf()))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        // THEN
        assertFalse(mailboxRepository.existsById(box.getId()), "Mailbox must be removed from repository");
        assertEquals(0, mailRepository.countByMailbox(box.getId()), "Associated mail records must be deleted");
    }

    // =========================================================================
    // ERROR CASES INTEGRATION TESTS
    // =========================================================================

    /**
     * Verifies that unauthenticated GET requests to /mailboxes redirect to the login page.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error case: unauthenticated GET /mailboxes redirects to login")
    public void testGetDashboardUnauthenticated() throws Exception
    {
        // GIVEN no authenticated user session

        // WHEN & THEN
        mockMvc.perform(get("/mailboxes"))
               .andExpect(status().is3xxRedirection());
    }

    /**
     * Verifies that creating a mailbox with invalid characters returns the modal fragment with an error attribute.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error case: POST /mailboxes with invalid characters returns modal fragment with error")
    public void testCreateMailboxHtmxInvalidAddress() throws Exception
    {
        // GIVEN an invalid address containing illegal symbols
        final String invalidAddress = "invalid@@address";

        // WHEN & THEN
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", invalidAddress)
               .param("domain", "xcmailr.test"))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/modal-new :: modal"))
               .andExpect(model().attribute("error", "Invalid mailbox address characters."))
               .andExpect(model().attribute("selectedDuration", 2))
               .andExpect(model().attribute("forwardEmails", false));
    }

    /**
     * Verifies that attempting to create a mailbox with an unconfigured domain returns an error fragment.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error case: POST /mailboxes with unconfigured domain returns modal fragment with error")
    public void testCreateMailboxHtmxInvalidDomain() throws Exception
    {
        // GIVEN an unconfigured domain
        final String unconfiguredDomain = "unregistered-domain.test";

        // WHEN & THEN
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", "valid-box")
               .param("domain", unconfiguredDomain))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/modal-new :: modal"))
               .andExpect(model().attribute("error", "Specified domain is not configured or whitelisted."))
               .andExpect(model().attribute("selectedDuration", 2))
               .andExpect(model().attribute("forwardEmails", false));
    }

    /**
     * Verifies that attempting to create a mailbox with an already existing address returns a duplicate error.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error case: POST /mailboxes with duplicate address returns duplicate warning fragment")
    public void testCreateMailboxHtmxDuplicateAddress() throws Exception
    {
        // GIVEN an existing mailbox record
        final MBox existing = new MBox("duplicate-box", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(existing);

        // WHEN & THEN
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", "duplicate-box")
               .param("domain", "xcmailr.test"))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/modal-new :: modal"))
               .andExpect(model().attribute("error", "A mailbox with this address already exists."))
               .andExpect(model().attribute("selectedDuration", 2))
               .andExpect(model().attribute("forwardEmails", false));
    }

    /**
     * Verifies that a user cannot delete a mailbox belonging to another user, yielding HTTP 403 Forbidden.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error case: cross-user DELETE attempt returns 403 Forbidden")
    public void testOwnershipSecurityCrossUserDelete() throws Exception
    {
        // GIVEN a mailbox owned by Alice
        final MBox box = new MBox("alice-delete-target", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        mailboxRepository.save(box);

        // WHEN Bob attempts to delete Alice's mailbox
        mockMvc.perform(delete("/mailboxes/" + box.getId())
               .header("HX-Request", "true")
               .with(user("bob@xcmailr.test").roles("USER"))
               .with(csrf()))
               .andExpect(status().isForbidden());

        // THEN
        assertTrue(mailboxRepository.existsById(box.getId()), "Mailbox must not be deleted by unauthorized user");
    }

    /**
     * Verifies that a user cannot toggle a mailbox belonging to another user, yielding HTTP 403 Forbidden.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error case: cross-user POST toggle attempt returns 403 Forbidden")
    public void testOwnershipSecurityCrossUserToggle() throws Exception
    {
        // GIVEN a mailbox owned by Alice
        final MBox box = new MBox("alice-toggle-target", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        mailboxRepository.save(box);

        // WHEN Bob attempts to toggle Alice's mailbox
        mockMvc.perform(post("/mailboxes/" + box.getId() + "/toggle")
               .header("HX-Request", "true")
               .with(user("bob@xcmailr.test").roles("USER"))
               .with(csrf()))
               .andExpect(status().isForbidden());

        // THEN
        final MBox unchanged = mailboxRepository.findById(box.getId()).orElseThrow();
        assertFalse(unchanged.isExpired(), "Mailbox expired status must not be modified by cross-user call");
    }

    /**
     * Verifies that a user cannot edit a mailbox belonging to another user, yielding HTTP 403 Forbidden.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error case: cross-user POST edit attempt returns 403 Forbidden")
    public void testOwnershipSecurityCrossUserEdit() throws Exception
    {
        // GIVEN a mailbox owned by Alice
        final MBox box = new MBox("alice-edit-target", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        box.setForwardEmails(true);
        mailboxRepository.save(box);

        // WHEN Bob attempts to edit Alice's mailbox
        mockMvc.perform(post("/mailboxes/" + box.getId() + "/edit")
               .header("HX-Request", "true")
               .with(user("bob@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("forwardEmails", "false")
               .param("active", "false"))
               .andExpect(status().isForbidden());

        // THEN
        final MBox unchanged = mailboxRepository.findById(box.getId()).orElseThrow();
        assertTrue(unchanged.isForwardEmails(), "Mailbox forward settings must not be modified by unauthorized user");
    }

    /**
     * Verifies that creation validation failure due to duplicate address re-renders modal without closeModal trigger.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error case: POST /mailboxes duplicate address retains modal and omits closeModal trigger")
    public void testCreateMailboxValidationFailureOmitsCloseModal() throws Exception
    {
        // GIVEN an existing mailbox
        final MBox existing = new MBox("dupe-test", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        mailboxRepository.save(existing);

        // WHEN attempting to create duplicate mailbox via HTMX
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", "dupe-test")
               .param("domain", "xcmailr.test")
               .param("durationHours", "1"))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/modal-new :: modal"))
               .andExpect(model().attribute("error", "A mailbox with this address already exists."))
               .andExpect(header().doesNotExist("HX-Trigger"))
               .andExpect(header().doesNotExist("HX-Trigger-After-Swap"));
    }

    /**
     * Verifies that POST /mailboxes/{id}/edit on a non-existent mailbox returns 404 without emitting closeModal trigger.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error case: POST /mailboxes/{id}/edit on non-existent mailbox returns 404 and omits closeModal trigger")
    public void testUpdateMailboxNonExistentReturnsNotFound() throws Exception
    {
        // GIVEN a non-existent mailbox ID
        final long nonExistentId = 999999L;

        // WHEN attempting to edit non-existent mailbox via HTMX
        mockMvc.perform(post("/mailboxes/" + nonExistentId + "/edit")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("forwardEmails", "false")
               .param("active", "false"))
               .andExpect(status().isNotFound())
               .andExpect(header().doesNotExist("HX-Trigger"))
               .andExpect(header().doesNotExist("HX-Trigger-After-Swap"));
    }

    // =========================================================================
    // SPECIAL EDGE CASES INTEGRATION TESTS
    // =========================================================================

    /**
     * Verifies that standard non-HTMX form submissions redirect to /mailboxes?created upon success.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Special case: non-HTMX browser fallback form submission redirects with query param")
    public void testCreateMailboxFormSubmitNonHtmx() throws Exception
    {
        // GIVEN a traditional browser form submission without HX-Request header
        final String address = "standard-browser-box";

        // WHEN & THEN
        mockMvc.perform(post("/mailboxes")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", address)
               .param("domain", "xcmailr.test")
               .param("durationHours", "12")
               .param("forwardEmails", "true"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/mailboxes?created"));

        final MBox box = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(address, "xcmailr.test").orElse(null);
        assertNotNull(box, "Mailbox must be persisted via standard browser form submit");
        assertTrue(box.isForwardEmails());
    }

    /**
     * Verifies that standard non-HTMX form submission without forwardEmails parameter defaults to forwarding disabled.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Special case: non-HTMX browser fallback form submission without forwardEmails defaults to disabled")
    public void testCreateMailboxFormSubmitNonHtmxOmittedForwardingDefaultsToFalse() throws Exception
    {
        // GIVEN a traditional browser form submission without forwardEmails checkbox
        final String address = "standard-browser-no-forward-box";

        // WHEN & THEN
        mockMvc.perform(post("/mailboxes")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", address)
               .param("domain", "xcmailr.test")
               .param("durationHours", "6"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/mailboxes?created"));

        final MBox box = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(address, "xcmailr.test").orElse(null);
        assertNotNull(box, "Mailbox must be persisted via standard browser form submit");
        assertFalse(box.isForwardEmails(), "Forwarding should default to disabled for non-HTMX form submissions");
    }

    /**
     * Verifies that POST /mailboxes with explicit forwardEmails=false persists with forwarding disabled.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Special case: POST /mailboxes with explicit forwardEmails=false persists with forwarding disabled")
    public void testCreateMailboxExplicitForwardingDisabled() throws Exception
    {
        // GIVEN explicit forwardEmails=false parameter
        final String address = "explicit-false-box";

        // WHEN & THEN
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", address)
               .param("domain", "xcmailr.test")
               .param("durationHours", "1")
               .param("forwardEmails", "false"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged, closeModal"));

        final MBox persistedBox = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(address, "xcmailr.test").orElse(null);
        assertNotNull(persistedBox, "Mailbox must be stored in database");
        assertFalse(persistedBox.isForwardEmails(), "Forwarding should be disabled");
    }

    /**
     * Verifies that leaving the mailbox address parameter blank auto-generates a valid address and creates the mailbox.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Special case: blank address triggers auto-generation and succeeds via HTMX")
    public void testCreateMailboxBlankAddressAutoGenerates() throws Exception
    {
        // GIVEN a blank address parameter
        final long countBefore = mailboxRepository.count();

        // WHEN
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", "")
               .param("domain", "xcmailr.test")
               .param("durationHours", "1"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged, closeModal"))
               .andExpect(header().doesNotExist("HX-Trigger-After-Swap"));

        // THEN
        final long countAfter = mailboxRepository.count();
        assertEquals(countBefore + 1, countAfter, "One auto-generated mailbox should be persisted");
    }

    /**
     * Verifies that HTMX live search query parameters filter the table fragment correctly.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Special case: HTMX live search filters mailbox table by search query")
    public void testSearchMailboxesHtmxFilter() throws Exception
    {
        // GIVEN two mailboxes with distinct addresses
        final MBox alpha = new MBox("alpha-search", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        final MBox beta = new MBox("beta-search", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        mailboxRepository.save(alpha);
        mailboxRepository.save(beta);

        // WHEN searching for 'alpha'
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .param("search", "alpha")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(model().attributeExists("mailboxes"));
    }

    /**
     * Verifies that an expired mailbox granted an extension transitions back to active status with extended validity.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Special case: extending an expired mailbox transitions expired status back to false")
    public void testExtendExpiredMailboxReactivationGracePeriod() throws Exception
    {
        // GIVEN an expired mailbox
        final long expiredTs = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2);
        final MBox expiredBox = new MBox("expired-grace-box", "xcmailr.test", expiredTs, true, owner);
        mailboxRepository.save(expiredBox);
        assertTrue(expiredBox.isExpired());

        // WHEN extending by 24 hours
        mockMvc.perform(post("/mailboxes/" + expiredBox.getId() + "/extend")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("hours", "24"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        // THEN
        final MBox reactivated = mailboxRepository.findById(expiredBox.getId()).orElseThrow();
        assertFalse(reactivated.isExpired(), "Expired mailbox must be reactivated");
        assertTrue(reactivated.getTs_Active() > System.currentTimeMillis(), "Validity must extend into the future");
    }

    /**
     * Verifies that the rendered mailbox table outputs dropdown action toggles configured with
     * Popper viewport boundary and fixed positioning strategy to prevent overflow clipping
     * in responsive table containers.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Special case: mailbox action dropdown toggle includes fixed popper and viewport boundary attributes")
    public void testMailboxActionDropdownPopperConfiguration() throws Exception
    {
        // GIVEN a persisted mailbox for the user
        final MBox box = new MBox("dropdown-test", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        mailboxRepository.save(box);

        // WHEN requesting the mailbox table fragment
        // THEN the dropdown toggle button must define viewport boundary and fixed Popper strategy
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("data-bs-boundary=\"viewport\"")))
               .andExpect(content().string(containsString("data-bs-popper-config='{\"strategy\":\"fixed\"}'")));
    }

    /**
     * Verifies happy path creation of a mailbox with 30-day preset duration (720 hours).
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: POST /mailboxes with 30-day duration sets expiration to 720 hours in future")
    public void testCreateMailbox_PresetDuration30Days_OffsetsCorrectly() throws Exception
    {
        // GIVEN
        final String address = "thirty-days-box";
        final String domainName = "xcmailr.test";
        final int durationHours = 720;
        final long before = System.currentTimeMillis();

        // WHEN
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", address)
               .param("domain", domainName)
               .param("durationHours", String.valueOf(durationHours))
               .param("forwardEmails", "true"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged, closeModal"));

        // THEN
        final long after = System.currentTimeMillis();
        final MBox box = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(address, domainName).orElseThrow();
        assertFalse(box.isExpired(), "Mailbox must be active");
        assertTrue(box.isForwardEmails(), "Forwarding must be enabled");
        final long minExpected = before + TimeUnit.HOURS.toMillis(durationHours);
        final long maxExpected = after + TimeUnit.HOURS.toMillis(durationHours);
        assertTrue(box.getTs_Active() >= minExpected && box.getTs_Active() <= maxExpected,
                   "Expiration timestamp must be offset by exactly 720 hours");
    }

    /**
     * Verifies happy path creation of an unlimited mailbox (0 hours).
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: POST /mailboxes with unlimited duration sets expiration to 0L")
    public void testCreateMailbox_PresetDurationUnlimited_PersistsZeroTimestamp() throws Exception
    {
        // GIVEN
        final String address = "unlimited-box";
        final String domainName = "xcmailr.test";

        // WHEN
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", address)
               .param("domain", domainName)
               .param("durationHours", "0")
               .param("forwardEmails", "false"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged, closeModal"));

        // THEN
        final MBox box = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(address, domainName).orElseThrow();
        assertFalse(box.isExpired(), "Mailbox must be active");
        assertFalse(box.isForwardEmails(), "Forwarding must be disabled");
        assertEquals(0L, box.getTs_Active(), "Unlimited mailbox must persist ts_active = 0L");
    }

    /**
     * Verifies that when durationHours parameter is omitted, controller defaults to 2 hours.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: POST /mailboxes with omitted durationHours defaults to 2 hours")
    public void testCreateMailbox_DefaultDuration_ResolvesToTwoHours() throws Exception
    {
        // GIVEN
        final String address = "default-duration-box";
        final String domainName = "xcmailr.test";
        final long before = System.currentTimeMillis();

        // WHEN
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", address)
               .param("domain", domainName)
               .param("forwardEmails", "true"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged, closeModal"));

        // THEN
        final long after = System.currentTimeMillis();
        final MBox box = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(address, domainName).orElseThrow();
        final long minExpected = before + TimeUnit.HOURS.toMillis(2);
        final long maxExpected = after + TimeUnit.HOURS.toMillis(2);
        assertTrue(box.getTs_Active() >= minExpected && box.getTs_Active() <= maxExpected,
                   "Default duration must be 2 hours");
    }

    /**
     * Verifies that validation error re-renders retain selectedDuration and forwardEmails in the model.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error case: validation failure re-render preserves custom selectedDuration and forwardEmails")
    public void testCreateMailbox_ValidationError_PreservesSelectedDurationAndForwarding() throws Exception
    {
        // GIVEN an invalid address, custom duration (30 days = 720h), and forwarding disabled
        final String invalidAddress = "invalid@@address";
        final String domainName = "xcmailr.test";
        final int customDuration = 720;
        final boolean customForwarding = false;

        // WHEN & THEN
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", invalidAddress)
               .param("domain", domainName)
               .param("durationHours", String.valueOf(customDuration))
               .param("forwardEmails", String.valueOf(customForwarding)))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/modal-new :: modal"))
               .andExpect(model().attribute("error", "Invalid mailbox address characters."))
               .andExpect(model().attribute("selectedDuration", customDuration))
               .andExpect(model().attribute("forwardEmails", customForwarding))
               .andExpect(model().attribute("selectedDomain", domainName));
    }

    /**
     * Verifies that negative duration hours evaluate safely to unlimited (0L) without errors.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Edge case: POST /mailboxes with negative duration evaluates to unlimited (0L)")
    public void testCreateMailbox_NegativeDuration_PersistsZeroTimestamp() throws Exception
    {
        // GIVEN
        final String address = "negative-duration-box";
        final String domainName = "xcmailr.test";

        // WHEN
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("address", address)
               .param("domain", domainName)
               .param("durationHours", "-10"))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "mailboxChanged, closeModal"));

        // THEN
        final MBox box = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(address, domainName).orElseThrow();
        assertEquals(0L, box.getTs_Active(), "Negative duration must evaluate to 0L");
    }

    // =========================================================================
    // Tier 1 Unit Tests: Whitelist Mapping, Parameter Handling & Sort Resolution
    // =========================================================================

    /**
     * Verifies that all valid column whitelist keys map to their expected JPA Sort orders and directions.
     */
    @Test
    @DisplayName("Happy path: Whitelist columns resolve to expected Spring Data Sort specifications")
    public void testResolveSort_HappyPath_AllColumnsAndDirections()
    {
        // Address ASC -> composite order: address ASC, domain ASC (case-insensitive)
        final Sort addressAsc = MailboxWebController.resolveSort("address", "asc");
        final Sort.Order addressOrderAsc = addressAsc.getOrderFor("address");
        final Sort.Order domainOrderAsc = addressAsc.getOrderFor("domain");
        assertNotNull(addressOrderAsc, "Address order must not be null");
        assertNotNull(domainOrderAsc, "Domain order must not be null");
        assertEquals(Sort.Direction.ASC, addressOrderAsc.getDirection());
        assertEquals(Sort.Direction.ASC, domainOrderAsc.getDirection());
        assertTrue(addressOrderAsc.isIgnoreCase(), "Address order must be case-insensitive");
        assertTrue(domainOrderAsc.isIgnoreCase(), "Domain order must be case-insensitive");

        // Address DESC -> composite order: address DESC, domain DESC (case-insensitive)
        final Sort addressDesc = MailboxWebController.resolveSort("address", "desc");
        final Sort.Order addressOrderDesc = addressDesc.getOrderFor("address");
        final Sort.Order domainOrderDesc = addressDesc.getOrderFor("domain");
        assertNotNull(addressOrderDesc, "Address order must not be null");
        assertNotNull(domainOrderDesc, "Domain order must not be null");
        assertEquals(Sort.Direction.DESC, addressOrderDesc.getDirection());
        assertEquals(Sort.Direction.DESC, domainOrderDesc.getDirection());
        assertTrue(addressOrderDesc.isIgnoreCase(), "Address order must be case-insensitive");
        assertTrue(domainOrderDesc.isIgnoreCase(), "Domain order must be case-insensitive");

        // Status -> expired
        final Sort statusAsc = MailboxWebController.resolveSort("status", "asc");
        final Sort.Order statusOrder = statusAsc.getOrderFor("expired");
        assertNotNull(statusOrder, "Status order on 'expired' property must not be null");
        assertEquals(Sort.Direction.ASC, statusOrder.getDirection());

        // Expires -> ts_Active (Perspective A)
        final Sort expiresDesc = MailboxWebController.resolveSort("expires", "desc");
        final Sort.Order expiresOrder = expiresDesc.getOrderFor("ts_Active");
        assertNotNull(expiresOrder, "Expires order on 'ts_Active' property must not be null");
        assertEquals(Sort.Direction.DESC, expiresOrder.getDirection());

        // Forwards -> forwards
        final Sort forwardsAsc = MailboxWebController.resolveSort("forwards", "asc");
        final Sort.Order forwardsOrder = forwardsAsc.getOrderFor("forwards");
        assertNotNull(forwardsOrder, "Forwards order must not be null");
        assertEquals(Sort.Direction.ASC, forwardsOrder.getDirection());

        // Suppressions -> suppressions
        final Sort suppressionsDesc = MailboxWebController.resolveSort("suppressions", "desc");
        final Sort.Order suppressionsOrder = suppressionsDesc.getOrderFor("suppressions");
        assertNotNull(suppressionsOrder, "Suppressions order must not be null");
        assertEquals(Sort.Direction.DESC, suppressionsOrder.getDirection());
    }

    /**
     * Verifies that resolveSort implements Perspective A (Pure Infinity: +infinity) for the "expires" column.
     * <p>
     * On ASC: orders by CASE WHEN ts_Active = 0 THEN 1 ELSE 0 END ASC, then ts_Active ASC so finite timestamps
     * come first (soonest first) and permanent mailboxes (ts_Active = 0) sort at the end (+infinity).
     * </p>
     * <p>
     * On DESC: orders by CASE WHEN ts_Active = 0 THEN 0 ELSE 1 END ASC, then ts_Active DESC so permanent mailboxes
     * (ts_Active = 0) sort at the very top (+infinity) followed by finite timestamps in descending order.
     * </p>
     */
    @Test
    @DisplayName("Happy path: Perspective A (+infinity) expiration sort specifications generated for ASC and DESC")
    public void testResolveSort_HappyPath_PerspectiveA_ExpiresSortOrders()
    {
        // 1. ASC: finite timestamps soonest first, unlimited (0) at end (+infinity)
        final Sort expiresAsc = MailboxWebController.resolveSort("expires", "asc");
        final List<Sort.Order> ascOrders = new ArrayList<>();
        expiresAsc.iterator().forEachRemaining(ascOrders::add);

        assertEquals(2, ascOrders.size(), "Expires ASC sort specification must contain exactly 2 order clauses");

        final Sort.Order ascCaseOrder = ascOrders.get(0);
        assertEquals("(CASE WHEN ts_Active = 0 THEN 1 ELSE 0 END)", ascCaseOrder.getProperty(),
                     "First order clause must be CASE partitioning finite (0) before unlimited (1)");
        assertEquals(Sort.Direction.ASC, ascCaseOrder.getDirection(),
                     "CASE clause must be sorted ASC");

        final Sort.Order ascEpochOrder = ascOrders.get(1);
        assertEquals("ts_Active", ascEpochOrder.getProperty(),
                     "Second order clause must be ts_Active epoch timestamp");
        assertEquals(Sort.Direction.ASC, ascEpochOrder.getDirection(),
                     "ts_Active must be sorted ASC for chronological expiration");

        // 2. DESC: unlimited (0) at top (+infinity), finite timestamps descending
        final Sort expiresDesc = MailboxWebController.resolveSort("expires", "desc");
        final List<Sort.Order> descOrders = new ArrayList<>();
        expiresDesc.iterator().forEachRemaining(descOrders::add);

        assertEquals(2, descOrders.size(), "Expires DESC sort specification must contain exactly 2 order clauses");

        final Sort.Order descCaseOrder = descOrders.get(0);
        assertEquals("(CASE WHEN ts_Active = 0 THEN 0 ELSE 1 END)", descCaseOrder.getProperty(),
                     "First order clause must be CASE partitioning unlimited (0) before finite (1)");
        assertEquals(Sort.Direction.ASC, descCaseOrder.getDirection(),
                     "CASE clause must be sorted ASC so unlimited (weight 0) comes first");

        final Sort.Order descEpochOrder = descOrders.get(1);
        assertEquals("ts_Active", descEpochOrder.getProperty(),
                     "Second order clause must be ts_Active epoch timestamp");
        assertEquals(Sort.Direction.DESC, descEpochOrder.getDirection(),
                     "ts_Active must be sorted DESC for reverse chronological expiration");
    }

    /**
     * Special case: Case-insensitive address sorting configuration.
     * Verifies that resolveSort configures ignoreCase on both address and domain orders.
     */
    @Test
    @DisplayName("Special case: Address sorting configures case-insensitive orders")
    public void testResolveSort_SpecialCase_AddressCaseInsensitive()
    {
        final Sort ascSort = MailboxWebController.resolveSort("address", "asc");
        final Sort.Order ascAddress = ascSort.getOrderFor("address");
        final Sort.Order ascDomain = ascSort.getOrderFor("domain");
        assertNotNull(ascAddress, "Address ASC order must not be null");
        assertNotNull(ascDomain, "Domain ASC order must not be null");
        assertTrue(ascAddress.isIgnoreCase(), "Address ASC order must be case-insensitive");
        assertTrue(ascDomain.isIgnoreCase(), "Domain ASC order must be case-insensitive");

        final Sort descSort = MailboxWebController.resolveSort("address", "desc");
        final Sort.Order descAddress = descSort.getOrderFor("address");
        final Sort.Order descDomain = descSort.getOrderFor("domain");
        assertNotNull(descAddress, "Address DESC order must not be null");
        assertNotNull(descDomain, "Domain DESC order must not be null");
        assertTrue(descAddress.isIgnoreCase(), "Address DESC order must be case-insensitive");
        assertTrue(descDomain.isIgnoreCase(), "Domain DESC order must be case-insensitive");
    }

    /**
     * Verifies that resolveSortField correctly validates whitelisted column names.
     */
    @Test
    @DisplayName("Happy path: Whitelist columns normalized correctly by resolveSortField")
    public void testResolveSortField_HappyPath_AllColumns()
    {
        assertEquals("address", MailboxWebController.resolveSortField("address"));
        assertEquals("status", MailboxWebController.resolveSortField("status"));
        assertEquals("expires", MailboxWebController.resolveSortField("expires"));
        assertEquals("forwards", MailboxWebController.resolveSortField("forwards"));
        assertEquals("suppressions", MailboxWebController.resolveSortField("suppressions"));
    }

    /**
     * Verifies that resolveSortDirection correctly parses direction strings.
     */
    @Test
    @DisplayName("Happy path: Direction strings resolved cleanly")
    public void testResolveSortDirection_HappyPath()
    {
        assertEquals("asc", MailboxWebController.resolveSortDirection("asc"));
        assertEquals("desc", MailboxWebController.resolveSortDirection("desc"));
    }

    /**
     * Verifies that resolveStatus correctly recognizes all whitelisted status filter tokens.
     */
    @Test
    @DisplayName("Happy path: Whitelisted status filter tokens are mapped correctly by resolveStatus")
    public void testResolveStatus_HappyPath_AllStatuses()
    {
        // Valid status tokens in uppercase
        assertEquals("ALL", MailboxWebController.resolveStatus("ALL"), "ALL should resolve to ALL");
        assertEquals("ACTIVE", MailboxWebController.resolveStatus("ACTIVE"), "ACTIVE should resolve to ACTIVE");
        assertEquals("EXPIRED", MailboxWebController.resolveStatus("EXPIRED"), "EXPIRED should resolve to EXPIRED");
        assertEquals("EXPIRING", MailboxWebController.resolveStatus("EXPIRING"), "EXPIRING should resolve to EXPIRING");
        assertEquals("UNLIMITED", MailboxWebController.resolveStatus("UNLIMITED"), "UNLIMITED should resolve to UNLIMITED");
    }

    /**
     * Verifies that resolveStatus handles case variations and leading/trailing whitespace cleanly.
     */
    @Test
    @DisplayName("Special case: Status filter tokens match case-insensitively and ignore whitespace")
    public void testResolveStatus_SpecialCase_CaseInsensitivityAndWhitespace()
    {
        // Case variations
        assertEquals("ALL", MailboxWebController.resolveStatus("all"), "Lowercase all should resolve to ALL");
        assertEquals("ACTIVE", MailboxWebController.resolveStatus("active"), "Lowercase active should resolve to ACTIVE");
        assertEquals("EXPIRED", MailboxWebController.resolveStatus("expired"), "Lowercase expired should resolve to EXPIRED");
        assertEquals("EXPIRING", MailboxWebController.resolveStatus("expiring"), "Lowercase expiring should resolve to EXPIRING");
        assertEquals("UNLIMITED", MailboxWebController.resolveStatus("unlimited"), "Lowercase unlimited should resolve to UNLIMITED");
        assertEquals("EXPIRING", MailboxWebController.resolveStatus("ExPiRiNg"), "Mixed case should resolve to EXPIRING");
        assertEquals("UNLIMITED", MailboxWebController.resolveStatus("UnLiMiTeD"), "Mixed case should resolve to UNLIMITED");

        // Leading/trailing whitespace
        assertEquals("ALL", MailboxWebController.resolveStatus("   ALL   "), "Padded ALL should resolve to ALL");
        assertEquals("ACTIVE", MailboxWebController.resolveStatus("\tACTIVE\n"), "Whitespace padded ACTIVE should resolve to ACTIVE");
        assertEquals("EXPIRING", MailboxWebController.resolveStatus("  expiring  "), "Padded lowercase should resolve to EXPIRING");
        assertEquals("UNLIMITED", MailboxWebController.resolveStatus("  unlimited  "), "Padded lowercase should resolve to UNLIMITED");
    }

    /**
     * Verifies that null, empty, or whitespace-only status values safely default to ALL.
     */
    @Test
    @DisplayName("Error case: Null, empty, and blank status inputs safely default to ALL")
    public void testResolveStatus_ErrorCase_NullAndEmptyDefaultsToAll()
    {
        assertEquals("ALL", MailboxWebController.resolveStatus(null), "null must fall back to ALL");
        assertEquals("ALL", MailboxWebController.resolveStatus(""), "Empty string must fall back to ALL");
        assertEquals("ALL", MailboxWebController.resolveStatus("   "), "Whitespace-only must fall back to ALL");
        assertEquals("ALL", MailboxWebController.resolveStatus("\t\n"), "Tabs and newlines must fall back to ALL");
    }

    /**
     * Verifies that unrecognized, invalid, or malicious status filter strings safely default to ALL.
     */
    @Test
    @DisplayName("Error case: Unknown, invalid, and injection status strings safely fall back to ALL")
    public void testResolveStatus_ErrorCase_InvalidAndMaliciousStringsDefaultToAll()
    {
        final String[] invalidStatuses = {
            "INVALID",
            "NONEXISTENT",
            "DELETED",
            "PENDING",
            "ALL; DROP TABLE mailboxes;--",
            "' OR 1=1 --",
            "<script>alert(1)</script>",
            "expiring_soon",
            "unlimited_boxes"
        };

        for (final String invalidStatus : invalidStatuses)
        {
            assertEquals("ALL", MailboxWebController.resolveStatus(invalidStatus),
                         "Invalid status '" + invalidStatus + "' must fall back to ALL");
        }
    }

    /**
     * Verifies that listMailboxes populates requested sort and dir attributes into the model.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: GET /mailboxes with custom sort and dir parameters populates model")
    public void testListMailboxes_HappyPath_SortParametersPopulatedInModel() throws Exception
    {
        // GIVEN a mailbox
        final MBox box = new MBox("sorting-test", "xcmailr.test", System.currentTimeMillis() + 3600000L, false, owner);
        mailboxRepository.save(box);

        // WHEN & THEN
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .param("sort", "address")
               .param("dir", "asc"))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(model().attribute("sort", "address"))
               .andExpect(model().attribute("dir", "asc"));
    }

    /**
     * Verifies that GET /mailboxes with status=EXPIRING filters to active mailboxes with finite lifespan.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: GET /mailboxes with status=EXPIRING returns only active finite mailboxes")
    public void testListMailboxes_HappyPath_StatusExpiring() throws Exception
    {
        // GIVEN: an active expiring mailbox, an active unlimited mailbox, and an expired finite mailbox
        final long futureEpoch = System.currentTimeMillis() + 3600000L;
        final long pastEpoch = System.currentTimeMillis() - 3600000L;
        final MBox expiring = new MBox("expiring-box", "xcmailr.test", futureEpoch, false, owner);
        final MBox unlimited = new MBox("unlimited-box", "xcmailr.test", 0L, false, owner);
        final MBox expired = new MBox("expired-box", "xcmailr.test", pastEpoch, true, owner);
        mailboxRepository.save(expiring);
        mailboxRepository.save(unlimited);
        mailboxRepository.save(expired);

        // WHEN & THEN
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .param("status", "EXPIRING"))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(model().attribute("status", "EXPIRING"))
               .andExpect(content().string(containsString("expiring-box@xcmailr.test")))
               .andExpect(content().string(org.hamcrest.Matchers.not(containsString("unlimited-box@xcmailr.test"))))
               .andExpect(content().string(org.hamcrest.Matchers.not(containsString("expired-box@xcmailr.test"))));
    }

    /**
     * Verifies that GET /mailboxes with status=UNLIMITED filters to active permanent mailboxes.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: GET /mailboxes with status=UNLIMITED returns only active permanent mailboxes")
    public void testListMailboxes_HappyPath_StatusUnlimited() throws Exception
    {
        // GIVEN: an active expiring mailbox, an active unlimited mailbox, and an expired finite mailbox
        final long futureEpoch = System.currentTimeMillis() + 3600000L;
        final long pastEpoch = System.currentTimeMillis() - 3600000L;
        final MBox expiring = new MBox("expiring-box", "xcmailr.test", futureEpoch, false, owner);
        final MBox unlimited = new MBox("unlimited-box", "xcmailr.test", 0L, false, owner);
        final MBox expired = new MBox("expired-box", "xcmailr.test", pastEpoch, true, owner);
        mailboxRepository.save(expiring);
        mailboxRepository.save(unlimited);
        mailboxRepository.save(expired);

        // WHEN & THEN
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .param("status", "UNLIMITED"))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(model().attribute("status", "UNLIMITED"))
               .andExpect(content().string(containsString("unlimited-box@xcmailr.test")))
               .andExpect(content().string(org.hamcrest.Matchers.not(containsString("expiring-box@xcmailr.test"))))
               .andExpect(content().string(org.hamcrest.Matchers.not(containsString("expired-box@xcmailr.test"))));
    }

    /**
     * Verifies that GET /mailboxes with status=EXPIRING and search filters combined.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: GET /mailboxes with status=EXPIRING and search filters matching expiring mailboxes")
    public void testListMailboxes_HappyPath_StatusExpiringAndSearch() throws Exception
    {
        // GIVEN: two expiring mailboxes with different names
        final long futureEpoch = System.currentTimeMillis() + 3600000L;
        final MBox expiring1 = new MBox("match-expiring", "xcmailr.test", futureEpoch, false, owner);
        final MBox expiring2 = new MBox("other-expiring", "xcmailr.test", futureEpoch, false, owner);
        mailboxRepository.save(expiring1);
        mailboxRepository.save(expiring2);

        // WHEN & THEN
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .param("status", "EXPIRING")
               .param("search", "match"))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(model().attribute("status", "EXPIRING"))
               .andExpect(content().string(containsString("match-expiring@xcmailr.test")))
               .andExpect(content().string(org.hamcrest.Matchers.not(containsString("other-expiring@xcmailr.test"))));
    }

    /**
     * Verifies that GET /mailboxes with status=UNLIMITED and search filters combined.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: GET /mailboxes with status=UNLIMITED and search filters matching unlimited mailboxes")
    public void testListMailboxes_HappyPath_StatusUnlimitedAndSearch() throws Exception
    {
        // GIVEN: two unlimited mailboxes with different names
        final MBox unlimited1 = new MBox("match-unlimited", "xcmailr.test", 0L, false, owner);
        final MBox unlimited2 = new MBox("other-unlimited", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(unlimited1);
        mailboxRepository.save(unlimited2);

        // WHEN & THEN
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .param("status", "UNLIMITED")
               .param("search", "match"))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(model().attribute("status", "UNLIMITED"))
               .andExpect(content().string(containsString("match-unlimited@xcmailr.test")))
               .andExpect(content().string(org.hamcrest.Matchers.not(containsString("other-unlimited@xcmailr.test"))));
    }

    /**
     * Verifies that GET /mailboxes with an invalid status filter safely falls back to ALL.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error case: GET /mailboxes with invalid status filter falls back to ALL")
    public void testListMailboxes_ErrorCase_InvalidStatusFallsBackToAll() throws Exception
    {
        // GIVEN: a mailbox
        final MBox box = new MBox("any-status-box", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(box);

        // WHEN & THEN
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .param("status", "MALICIOUS_OR_INVALID"))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(model().attribute("status", "ALL"))
               .andExpect(content().string(containsString("any-status-box@xcmailr.test")));
    }

    /**
     * Verifies that GET /mailboxes with an empty mailbox collection renders the empty state
     * with 7-column colspan and omits the mailCounts model attribute.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Special case: Empty mailbox collection omits mailCounts and renders 7-column empty state")
    public void testListMailboxes_SpecialCase_EmptyDataset_OmitsMailCountsAndRendersEmptyState() throws Exception
    {
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(model().attributeDoesNotExist("mailCounts"))
               .andExpect(content().string(containsString("<td colspan=\"7\" class=\"text-center text-muted py-4\">")))
               .andExpect(content().string(containsString("<tr class=\"align-middle\">")))
               .andExpect(content().string(containsString("<span class=\"d-inline-flex align-items-center py-1\">Actions</span>")))
               .andExpect(content().string(not(containsString("<th>Mails</th>"))));
    }

    /**
     * Verifies that unknown or non-whitelisted sort keys safely fall back to ID descending.
     */
    @Test
    @DisplayName("Error case: Unrecognized sort keys fall back safely to id DESC")
    public void testResolveSort_ErrorCase_UnrecognizedSortKeyFallsBackToIdDesc()
    {
        final Sort sortResult = MailboxWebController.resolveSort("nonexistent_column", "asc");
        final Sort.Order idOrder = sortResult.getOrderFor("id");
        assertNotNull(idOrder, "Fallback must order by 'id'");
        assertEquals(Sort.Direction.DESC, idOrder.getDirection(), "Fallback must be DESC");
        assertEquals("id", MailboxWebController.resolveSortField("nonexistent_column"));
    }

    /**
     * Verifies that SQL injection attempts or unexpected characters fall back safely to ID descending.
     */
    @Test
    @DisplayName("Error case: SQL injection attempts fall back safely to id DESC")
    public void testResolveSort_ErrorCase_SqlInjectionAttemptsFallBackToIdDesc()
    {
        final String[] maliciousInputs = {
            "id; DROP TABLE mailboxes;--",
            "' OR 1=1 --",
            "address, id DESC",
            "SELECT * FROM users",
            "../etc/passwd"
        };

        for (final String maliciousInput : maliciousInputs)
        {
            final Sort sortResult = MailboxWebController.resolveSort(maliciousInput, "asc");
            final Sort.Order idOrder = sortResult.getOrderFor("id");
            assertNotNull(idOrder, "Malicious input '" + maliciousInput + "' must fall back to 'id'");
            assertEquals(Sort.Direction.DESC, idOrder.getDirection(), "Fallback must be DESC");
            assertEquals("id", MailboxWebController.resolveSortField(maliciousInput));
        }
    }

    /**
     * Verifies that malformed direction strings safely default to "desc".
     */
    @Test
    @DisplayName("Error case: Malformed direction values safely fall back to desc")
    public void testResolveSort_ErrorCase_MalformedDirectionFallsBackToDesc()
    {
        final String[] invalidDirs = {"sideways", "123", "none", "", "asc;drop", "null"};
        for (final String invalidDir : invalidDirs)
        {
            assertEquals("desc", MailboxWebController.resolveSortDirection(invalidDir),
                         "Invalid direction '" + invalidDir + "' must fall back to 'desc'");
            final Sort resolved = MailboxWebController.resolveSort("expires", invalidDir);
            final Sort.Order expiresOrder = resolved.getOrderFor("ts_Active");
            assertNotNull(expiresOrder, "Expires order must not be null");
            assertEquals(Sort.Direction.DESC, expiresOrder.getDirection(),
                         "Invalid direction must fall back to DESC for expires");
        }
    }

    /**
     * Verifies that GET /mailboxes with an invalid sort key and malformed direction gracefully handles
     * the request, defaulting to id and desc in model.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error case: GET /mailboxes with invalid sort parameter falls back to id and desc in model")
    public void testListMailboxes_ErrorCase_InvalidSortFallback() throws Exception
    {
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .with(user("alice@xcmailr.test").roles("USER"))
               .param("sort", "malicious_injection; DROP TABLE--")
               .param("dir", "invalid_dir"))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(model().attribute("sort", "id"))
               .andExpect(model().attribute("dir", "desc"));
    }

    /**
     * Verifies that sort keys and directions are case-insensitively parsed.
     */
    @Test
    @DisplayName("Special case: Column names and directions are matched case-insensitively")
    public void testResolveSort_SpecialCase_CaseInsensitiveMatching()
    {
        assertEquals("address", MailboxWebController.resolveSortField("ADDRESS"));
        assertEquals("status", MailboxWebController.resolveSortField("StAtUs"));
        assertEquals("expires", MailboxWebController.resolveSortField("ExPiReS"));
        assertEquals("forwards", MailboxWebController.resolveSortField("FORWARDS"));
        assertEquals("suppressions", MailboxWebController.resolveSortField("SuPpReSsIoNs"));

        assertEquals("asc", MailboxWebController.resolveSortDirection("ASC"));
        assertEquals("asc", MailboxWebController.resolveSortDirection("aSc"));

        final Sort sortResult = MailboxWebController.resolveSort("EXPIRES", "ASC");
        final Sort.Order expiresOrder = sortResult.getOrderFor("ts_Active");
        assertNotNull(expiresOrder, "ts_Active order must not be null");
        assertEquals(Sort.Direction.ASC, expiresOrder.getDirection());
    }

    /**
     * Verifies that null, empty, or whitespace sort and direction parameters fall back safely.
     */
    @Test
    @DisplayName("Special case: Null and blank parameters fall back safely without exceptions")
    public void testResolveSort_SpecialCase_NullAndBlankInputsFallBackToIdDesc()
    {
        // Null inputs
        assertEquals("id", MailboxWebController.resolveSortField(null));
        assertEquals("desc", MailboxWebController.resolveSortDirection(null));
        final Sort nullSort = MailboxWebController.resolveSort(null, null);
        assertNotNull(nullSort.getOrderFor("id"));
        assertEquals(Sort.Direction.DESC, nullSort.getOrderFor("id").getDirection());

        // Empty string inputs
        assertEquals("id", MailboxWebController.resolveSortField(""));
        assertEquals("desc", MailboxWebController.resolveSortDirection(""));
        final Sort emptySort = MailboxWebController.resolveSort("", "");
        assertNotNull(emptySort.getOrderFor("id"));
        assertEquals(Sort.Direction.DESC, emptySort.getOrderFor("id").getDirection());

        // Whitespace inputs
        assertEquals("id", MailboxWebController.resolveSortField("   "));
        assertEquals("desc", MailboxWebController.resolveSortDirection("   "));
        final Sort blankSort = MailboxWebController.resolveSort("   ", "   ");
        assertNotNull(blankSort.getOrderFor("id"));
        assertEquals(Sort.Direction.DESC, blankSort.getOrderFor("id").getDirection());
    }

    // =========================================================================
    // LOCALIZED PAGINATION RANGE INTEGRATION TESTS (TIER 2)
    // =========================================================================

    /**
     * Helper method to populate a given number of distinct test mailboxes for the specified user.
     *
     * @param count the number of mailboxes to create
     * @param targetUser the owning user
     */
    private void createMailboxes(final int count, final User targetUser)
    {
        final List<MBox> boxes = new ArrayList<>(count);
        for (int i = 1; i <= count; i++)
        {
            final MBox box = new MBox("box-" + i, "xcmailr.test", System.currentTimeMillis() + 3600000L, false, targetUser);
            boxes.add(box);
        }
        mailboxRepository.saveAll(boxes);
    }

    /**
     * Verifies happy path: multi-page dataset (17 mailboxes, default page size 15) renders
     * the item range summary "Showing 1–15 of 17 mailboxes" on page 1 for an English-preference user.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy Path: Multi-page mailbox table renders 'Showing 1–15 of 17 mailboxes' on page 1")
    public void testListMailboxes_HappyPath_MultiPageRangeDisplayEnglish() throws Exception
    {
        // GIVEN: 17 mailboxes owned by Alice (English locale preference)
        createMailboxes(17, owner);

        // WHEN: requesting the first page (page 0) of the mailbox dashboard
        // THEN: verify HTTP 200 and localized range summary in table footer
        mockMvc.perform(get("/mailboxes")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/index"))
               .andExpect(content().string(containsString("Showing 1–15 of 17 mailboxes")))
               .andExpect(content().string(not(containsString("total mailboxes"))));
    }

    /**
     * Verifies special case: subsequent page (page 2 of 2) displays the correct offset range
     * "Showing 16–17 of 17 mailboxes" for an English-preference user.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Special Case: Page 2 of multi-page mailbox table renders 'Showing 16–17 of 17 mailboxes'")
    public void testListMailboxes_SpecialCase_SubsequentPageRangeDisplayEnglish() throws Exception
    {
        // GIVEN: 17 mailboxes owned by Alice
        createMailboxes(17, owner);

        // WHEN: navigating to the second page (page 1)
        // THEN: verify HTTP 200 and localized range summary reflecting elements 16 to 17
        mockMvc.perform(get("/mailboxes")
               .param("page", "1")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/index"))
               .andExpect(content().string(containsString("Showing 16–17 of 17 mailboxes")));
    }

    /**
     * Verifies special case: single mailbox dataset displays singular phrasing
     * "Showing 1 of 1 mailbox" rather than range phrasing.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Special Case: Single mailbox dataset renders singular phrasing 'Showing 1 of 1 mailbox'")
    public void testListMailboxes_SpecialCase_SingleItemDisplayEnglish() throws Exception
    {
        // GIVEN: exactly 1 mailbox owned by Alice
        createMailboxes(1, owner);

        // WHEN: requesting the mailbox dashboard
        // THEN: verify singular phrasing is displayed and range phrasing is absent
        mockMvc.perform(get("/mailboxes")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Showing 1 of 1 mailbox")))
               .andExpect(content().string(not(containsString("Showing 1–1"))));
    }

    /**
     * Verifies happy path: multi-page dataset renders German range string
     * "Zeige 1–15 von 17 Postfächern" when user account language is configured to German ("de").
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy Path: User with German language preference receives 'Zeige 1–15 von 17 Postfächern'")
    public void testListMailboxes_HappyPath_GermanLocaleRangeDisplay() throws Exception
    {
        // GIVEN: user profile set to German language preference and 17 mailboxes
        owner.setLanguage("de");
        userRepository.save(owner);
        createMailboxes(17, owner);

        // WHEN: requesting the mailbox dashboard
        // THEN: verify German localized pagination summary
        mockMvc.perform(get("/mailboxes")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Zeige 1–15 von 17 Postfächern")));
    }

    /**
     * Verifies special case: single mailbox dataset renders German singular phrasing
     * "Zeige 1 von 1 Postfach" when user language is German ("de").
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Special Case: User with German language preference receives singular 'Zeige 1 von 1 Postfach'")
    public void testListMailboxes_SpecialCase_GermanLocaleSingleItemDisplay() throws Exception
    {
        // GIVEN: user profile set to German language preference and 1 mailbox
        owner.setLanguage("de");
        userRepository.save(owner);
        createMailboxes(1, owner);

        // WHEN: requesting the mailbox dashboard
        // THEN: verify German singular pagination summary
        mockMvc.perform(get("/mailboxes")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Zeige 1 von 1 Postfach")));
    }

    /**
     * Verifies error / fallback case: unsupported user language preference (e.g. "fr")
     * safely falls back to standard English localization without errors.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error/Fallback Case: Unsupported language code falls back to English range display")
    public void testListMailboxes_ErrorCase_UnsupportedLocaleFallsBackToEnglish() throws Exception
    {
        // GIVEN: user profile with unsupported language "fr" and 17 mailboxes
        owner.setLanguage("fr");
        userRepository.save(owner);
        createMailboxes(17, owner);

        // WHEN: requesting the mailbox dashboard
        // THEN: verify fallback to English pagination summary
        mockMvc.perform(get("/mailboxes")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Showing 1–15 of 17 mailboxes")));
    }

    /**
     * Verifies special case: HTMX partial table swaps (header HX-Request: true) return
     * the table fragment with the correct localized item range summary.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Special Case: HTMX partial table swap retains localized range display")
    public void testListMailboxes_SpecialCase_HtmxPaginationPartialSwapPreservesRange() throws Exception
    {
        // GIVEN: 17 mailboxes owned by Alice
        createMailboxes(17, owner);

        // WHEN: requesting second page via asynchronous HTMX request
        // THEN: verify partial table fragment is returned containing the range summary
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .param("page", "1")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(content().string(containsString("Showing 16–17 of 17 mailboxes")));
    }

    /**
     * Verifies edge case: empty mailbox dataset renders empty state view
     * and completely omits the pagination range summary.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Edge Case: Empty mailbox dataset renders empty state and omits range summary")
    public void testListMailboxes_EdgeCase_EmptyDatasetRendersEmptyStateWithoutPaginationSummary() throws Exception
    {
        // GIVEN: zero mailboxes for Alice (empty dataset)

        // WHEN: requesting the mailbox dashboard
        // THEN: verify empty state message is present and pagination range summaries are omitted
        mockMvc.perform(get("/mailboxes")
               .with(user("alice@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("No mailboxes found matching the criteria.")))
               .andExpect(content().string(not(containsString("Showing"))))
               .andExpect(content().string(not(containsString("Zeige"))));
    }

    /**
     * Verifies happy path: switching preferred language in the profile from German to English
     * immediately reflects on subsequent mailbox dashboard full-page and HTMX pagination views.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy Path: Profile language update from German to English immediately localizes mailbox dashboard and HTMX swaps")
    public void testListMailboxes_HappyPath_ProfileLanguageUpdateFromGermanToEnglishTakesImmediateEffect() throws Exception
    {
        // GIVEN: 17 mailboxes owned by Alice whose profile begins with German preference
        owner.setLanguage("de");
        userRepository.save(owner);
        createMailboxes(17, owner);
        final UserPrincipal alicePrincipal = new UserPrincipal(owner);

        // 1. Initial full-page request resolves German
        mockMvc.perform(get("/mailboxes")
               .with(user(alicePrincipal)))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Zeige 1–15 von 17 Postfächern")));

        // 2. User updates profile language to English in /profile
        mockMvc.perform(post("/profile/update")
               .with(user(alicePrincipal))
               .with(csrf())
               .param("forename", "Alice")
               .param("surname", "Tester")
               .param("language", "en"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/profile?updated"));

        // 3. Subsequent full-page request immediately renders English pagination summary without re-login
        mockMvc.perform(get("/mailboxes")
               .with(user(alicePrincipal)))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Showing 1–15 of 17 mailboxes")))
               .andExpect(content().string(not(containsString("Zeige"))));

        // 4. Subsequent asynchronous HTMX pagination request renders English partial fragment
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .param("page", "1")
               .with(user(alicePrincipal)))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(content().string(containsString("Showing 16–17 of 17 mailboxes")))
               .andExpect(content().string(not(containsString("Zeige"))));
    }

    /**
     * Verifies happy path: switching preferred language in the profile from English to German
     * immediately reflects on subsequent mailbox dashboard full-page and HTMX pagination views.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy Path: Profile language update from English to German immediately localizes mailbox dashboard and HTMX swaps")
    public void testListMailboxes_HappyPath_ProfileLanguageUpdateFromEnglishToGermanTakesImmediateEffect() throws Exception
    {
        // GIVEN: 17 mailboxes owned by Alice whose profile begins with English preference
        owner.setLanguage("en");
        userRepository.save(owner);
        createMailboxes(17, owner);
        final UserPrincipal alicePrincipal = new UserPrincipal(owner);

        // 1. Initial full-page request resolves English
        mockMvc.perform(get("/mailboxes")
               .with(user(alicePrincipal)))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Showing 1–15 of 17 mailboxes")));

        // 2. User updates profile language to German in /profile
        mockMvc.perform(post("/profile/update")
               .with(user(alicePrincipal))
               .with(csrf())
               .param("forename", "Alice")
               .param("surname", "Tester")
               .param("language", "de"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/profile?updated"));

        // 3. Subsequent full-page request immediately renders German pagination summary without re-login
        mockMvc.perform(get("/mailboxes")
               .with(user(alicePrincipal)))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Zeige 1–15 von 17 Postfächern")))
               .andExpect(content().string(not(containsString("Showing"))));

        // 4. Subsequent asynchronous HTMX pagination request renders German partial fragment
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .param("page", "1")
               .with(user(alicePrincipal)))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(content().string(containsString("Zeige 16–17 von 17 Postfächern")))
               .andExpect(content().string(not(containsString("Showing"))));
    }

    /**
     * Verifies OWASP tenant isolation: one user updating their profile language does not
     * affect or contaminate the locale resolution or pagination summary of another tenant.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Security Case (OWASP): Language change for one user maintains strict tenant isolation without leaking to other users")
    public void testListMailboxes_SecurityCase_TenantIsolationMaintainedAcrossUserUpdates() throws Exception
    {
        // GIVEN: Alice with German language and Charlie with English language
        owner.setLanguage("de");
        userRepository.save(owner);
        createMailboxes(17, owner);
        final UserPrincipal alicePrincipal = new UserPrincipal(owner);

        final User charlie = new User("Charlie", "Brown", "charlie@xcmailr.test", "Password123", "en");
        charlie.setActive(true);
        userRepository.save(charlie);
        createMailboxes(17, charlie);
        final UserPrincipal charliePrincipal = new UserPrincipal(charlie);

        // WHEN: Alice accesses dashboard, German is resolved
        mockMvc.perform(get("/mailboxes")
               .with(user(alicePrincipal)))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Zeige 1–15 von 17 Postfächern")));

        // AND: Charlie accesses dashboard in the same application run, English is strictly resolved
        mockMvc.perform(get("/mailboxes")
               .with(user(charliePrincipal)))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Showing 1–15 of 17 mailboxes")))
               .andExpect(content().string(not(containsString("Zeige"))));
    }

    /**
     * Verifies special case: sorting and filtering parameters (search, sort, dir)
     * are strictly preserved across localized HTMX partial swaps following a profile language update.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Special Case: Preserves sort and filter params across localized HTMX swaps after profile language update")
    public void testListMailboxes_SpecialCase_PreservesSortAndFilterParamsAcrossLocalizedSwap() throws Exception
    {
        // GIVEN: Alice starts with German language preference and 17 mailboxes
        owner.setLanguage("de");
        userRepository.save(owner);
        createMailboxes(17, owner);
        final UserPrincipal alicePrincipal = new UserPrincipal(owner);

        // 1. Initial request in German with custom sort parameters
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .param("sort", "address")
               .param("dir", "asc")
               .with(user(alicePrincipal)))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(model().attribute("sort", "address"))
               .andExpect(model().attribute("dir", "asc"))
               .andExpect(content().string(containsString("Zeige 1–15 von 17 Postfächern")));

        // 2. User updates profile language to English in /profile
        mockMvc.perform(post("/profile/update")
               .with(user(alicePrincipal))
               .with(csrf())
               .param("forename", "Alice")
               .param("surname", "Tester")
               .param("language", "en"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/profile?updated"));

        // 3. Subsequent HTMX request with sort preserves sort filters while rendering in English
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .param("sort", "address")
               .param("dir", "asc")
               .with(user(alicePrincipal)))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(model().attribute("sort", "address"))
               .andExpect(model().attribute("dir", "asc"))
               .andExpect(content().string(containsString("Showing 1–15 of 17 mailboxes")))
               .andExpect(content().string(not(containsString("Zeige"))));

        // 4. Subsequent HTMX request with search and sort preserves search filter while rendering in English
        mockMvc.perform(get("/mailboxes")
               .header("HX-Request", "true")
               .param("search", "box-1")
               .param("sort", "address")
               .param("dir", "asc")
               .with(user(alicePrincipal)))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/mailbox-table :: table"))
               .andExpect(model().attribute("search", "box-1"))
               .andExpect(model().attribute("sort", "address"))
               .andExpect(model().attribute("dir", "asc"))
               .andExpect(content().string(containsString("Showing 1–9 of 9 mailboxes")))
               .andExpect(content().string(not(containsString("Zeige"))));
    }
}

