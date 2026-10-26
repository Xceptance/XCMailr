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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.AfterEach;
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
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.Domain;
import models.MBox;
import models.User;

/**
 * UI template contract test suite for the mailbox creation modal dialog (Testing Pyramid Layer 3).
 * <p>
 * Verifies HTML markup structure, accessibility attributes, segmented radio button pill rendering,
 * default pre-selection, dynamic checked state bindings, and retention across validation error re-renders.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public final class MailboxCreationModalContractTest
{
    private static final String TEST_USER_EMAIL = "modal-contract-user@xcmailr.test";
    private static final String TEST_DOMAIN_NAME = "xcmailr.test";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    private MockMvc mockMvc;
    private User testUser;
    private Domain testDomain;

    /**
     * Initializes test security context and persists fixture records before each test execution.
     */
    @BeforeEach
    public void setUp()
    {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Ensure clean state
        mailboxRepository.deleteAll();
        domainRepository.deleteAll();
        userRepository.deleteAll();

        // Seed domain fixture
        testDomain = new Domain();
        testDomain.setDomainname(TEST_DOMAIN_NAME);
        domainRepository.save(testDomain);

        // Seed user fixture
        testUser = new User();
        testUser.setMail(TEST_USER_EMAIL);
        testUser.setForename("Modal");
        testUser.setSurname("Tester");
        testUser.setPasswd("hashedpassword");
        testUser.setActive(true);
        userRepository.save(testUser);
    }

    /**
     * Cleans up persisted database records after each test run.
     */
    @AfterEach
    public void tearDown()
    {
        mailboxRepository.deleteAll();
        domainRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // HAPPY PATH CONTRACT TESTS
    // =========================================================================

    /**
     * Verifies that GET /mailboxes/new renders all 6 segmented radio button pills with correct
     * labels, values, accessibility attributes, and that 2h is checked by default while native select is absent.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Happy path: modal renders 6 radio pills with 2h selected by default and no select dropdown")
    public void testNewMailboxModal_RendersRadioPillsByDefault() throws Exception
    {
        // WHEN requesting the creation modal fragment
        mockMvc.perform(get("/mailboxes/new")
               .with(user(TEST_USER_EMAIL).roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/modal-new :: modal"))
               // Native select dropdown must NOT be present
               .andExpect(content().string(not(containsString("<select class=\"form-select\" id=\"durationSelect\""))))
               // Segmented button group container must exist with aria label
               .andExpect(content().string(containsString("aria-label=\"Mailbox active duration presets\"")))
               // Preset 1: 1 Hour (1h)
               .andExpect(content().string(containsString("id=\"duration-1\" value=\"1\"")))
               .andExpect(content().string(containsString(">1h</label>")))
               // Preset 2: 2 Hours (2h) - default checked
               .andExpect(content().string(containsString("id=\"duration-2\" value=\"2\"")))
               .andExpect(content().string(containsString(">2h</label>")))
               .andExpect(content().string(containsString("id=\"duration-2\" value=\"2\" autocomplete=\"off\" checked=\"checked\"")))
               // Preset 3: 1 Day (1d)
               .andExpect(content().string(containsString("id=\"duration-24\" value=\"24\"")))
               .andExpect(content().string(containsString(">1d</label>")))
               // Preset 4: 7 Days (7d)
               .andExpect(content().string(containsString("id=\"duration-168\" value=\"168\"")))
               .andExpect(content().string(containsString(">7d</label>")))
               // Preset 5: 30 Days (30d)
               .andExpect(content().string(containsString("id=\"duration-720\" value=\"720\"")))
               .andExpect(content().string(containsString(">30d</label>")))
               // Preset 6: Unlimited
               .andExpect(content().string(containsString("id=\"duration-0\" value=\"0\"")))
               .andExpect(content().string(containsString(">Unlimited</label>")))
               // Forwarding switch must be checked by default
               .andExpect(content().string(containsString("id=\"forwardCheck\" name=\"forwardEmails\" value=\"true\" checked=\"checked\"")));
    }

    // =========================================================================
    // ERROR RE-RENDER CONTRACT TESTS
    // =========================================================================

    /**
     * Verifies that when a validation error occurs, the re-rendered modal preserves the user-selected
     * 30-day duration pill (720h) as checked, leaving the default 2h pill unchecked.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error case: validation failure re-render retains 30d radio pill checked state")
    public void testValidationErrorReRender_Retains30DaySelection() throws Exception
    {
        // GIVEN invalid address prefix with 30-day duration (720 hours) selected and forwarding disabled
        final String invalidAddress = "invalid@@prefix";
        final int selectedDuration = 720;
        final boolean forwardEmails = false;

        // WHEN submitting the invalid form via HTMX
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user(TEST_USER_EMAIL).roles("USER"))
               .with(csrf())
               .param("address", invalidAddress)
               .param("domain", TEST_DOMAIN_NAME)
               .param("durationHours", String.valueOf(selectedDuration))
               .param("forwardEmails", String.valueOf(forwardEmails)))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/modal-new :: modal"))
               .andExpect(model().attribute("selectedDuration", selectedDuration))
               .andExpect(model().attribute("forwardEmails", forwardEmails))
               // 30d radio must be checked
               .andExpect(content().string(containsString("id=\"duration-720\" value=\"720\" autocomplete=\"off\" checked=\"checked\"")))
               // 2h radio must NOT be checked
               .andExpect(content().string(not(containsString("id=\"duration-2\" value=\"2\" autocomplete=\"off\" checked=\"checked\""))))
               // Forwarding checkbox must NOT be checked
               .andExpect(content().string(not(containsString("id=\"forwardCheck\" name=\"forwardEmails\" value=\"true\" checked=\"checked\""))));
    }

    /**
     * Verifies that when a duplicate mailbox validation error occurs, the re-rendered modal preserves
     * the 1-day duration selection (24h) and enabled forwarding.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Error case: duplicate mailbox error re-render retains 1d selection and enabled forwarding")
    public void testDuplicateAddressReRender_RetainsOneDaySelection() throws Exception
    {
        // GIVEN an existing mailbox
        final String existingAddress = "existing-box";
        final MBox existing = new MBox(existingAddress, TEST_DOMAIN_NAME, 0L, false, testUser);
        mailboxRepository.save(existing);

        // WHEN submitting a duplicate mailbox with 1-day (24h) duration and forwarding enabled
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user(TEST_USER_EMAIL).roles("USER"))
               .with(csrf())
               .param("address", existingAddress)
               .param("domain", TEST_DOMAIN_NAME)
               .param("durationHours", "24")
               .param("forwardEmails", "true"))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/modal-new :: modal"))
               .andExpect(model().attribute("selectedDuration", 24))
               .andExpect(model().attribute("forwardEmails", true))
               // 1d radio must be checked
               .andExpect(content().string(containsString("id=\"duration-24\" value=\"24\" autocomplete=\"off\" checked=\"checked\"")))
               // 2h radio must NOT be checked
               .andExpect(content().string(not(containsString("id=\"duration-2\" value=\"2\" autocomplete=\"off\" checked=\"checked\""))))
               // Forwarding switch must be checked
               .andExpect(content().string(containsString("id=\"forwardCheck\" name=\"forwardEmails\" value=\"true\" checked=\"checked\"")));
    }

    // =========================================================================
    // SPECIAL CASE CONTRACT TESTS
    // =========================================================================

    /**
     * Verifies that validation error re-render preserves the Unlimited duration selection (0 hours).
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Special case: validation failure re-render retains Unlimited radio pill checked state")
    public void testValidationErrorReRender_RetainsUnlimitedSelection() throws Exception
    {
        // GIVEN an unconfigured domain with unlimited duration
        final String unconfiguredDomain = "unregistered.test";
        final int unlimitedDuration = 0;

        // WHEN submitting the unconfigured domain form via HTMX
        mockMvc.perform(post("/mailboxes")
               .header("HX-Request", "true")
               .with(user(TEST_USER_EMAIL).roles("USER"))
               .with(csrf())
               .param("address", "validprefix")
               .param("domain", unconfiguredDomain)
               .param("durationHours", String.valueOf(unlimitedDuration))
               .param("forwardEmails", "true"))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/fragments/modal-new :: modal"))
               .andExpect(model().attribute("selectedDuration", unlimitedDuration))
               // Unlimited radio must be checked
               .andExpect(content().string(containsString("id=\"duration-0\" value=\"0\" autocomplete=\"off\" checked=\"checked\"")))
               // 2h radio must NOT be checked
               .andExpect(content().string(not(containsString("id=\"duration-2\" value=\"2\" autocomplete=\"off\" checked=\"checked\""))));
    }
}

