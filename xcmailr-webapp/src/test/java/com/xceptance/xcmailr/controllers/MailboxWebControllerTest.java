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
               .andExpect(model().attributeExists("domains"));
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
               .andExpect(model().attributeExists("mailboxes"));
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
               .andExpect(model().attributeExists("domains", "suggestedAddress"));
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
               .andExpect(header().string("HX-Trigger", "mailboxChanged"))
               .andExpect(header().string("HX-Trigger-After-Swap", "closeModal"));

        final MBox persistedBox = mailboxRepository.findByAddressIgnoreCaseAndDomainIgnoreCase(address, domainName).orElse(null);
        assertNotNull(persistedBox, "Mailbox must be stored in database");
        assertFalse(persistedBox.isForwardEmails(), "Forwarding should be disabled as configured");
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
               .andExpect(header().string("HX-Trigger", "mailboxChanged"));

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
               .andExpect(model().attribute("error", "Invalid mailbox address characters."));
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
               .andExpect(model().attribute("error", "Specified domain is not configured or whitelisted."));
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
               .andExpect(model().attribute("error", "A mailbox with this address already exists."));
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
               .andExpect(header().string("HX-Trigger", "mailboxChanged"))
               .andExpect(header().string("HX-Trigger-After-Swap", "closeModal"));

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
}

