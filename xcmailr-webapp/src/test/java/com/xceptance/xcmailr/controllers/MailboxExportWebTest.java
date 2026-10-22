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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import models.User;

/**
 * Dedicated test suite verifying mailbox address export functionality and UI layout enhancements
 * across the testing pyramid (Happy Paths, Error Cases, Special Cases, and Template Integration).
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public final class MailboxExportWebTest
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
     * Resets database state and initializes test users and domain before each test execution.
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
    // 1. HAPPY PATH TESTS
    // =========================================================================

    @Test
    @DisplayName("Happy path: GET /mailboxes/export/all returns 200 OK, attachment header, and complete address list")
    public void testExportAllMailboxesHappyPath() throws Exception
    {
        final MBox box1 = new MBox("active-box", "xcmailr.test", 0L, false, owner);
        final MBox box2 = new MBox("expired-box", "xcmailr.test", 0L, true, owner);
        mailboxRepository.saveAll(List.of(box1, box2));

        mockMvc.perform(get("/mailboxes/export/all")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes.txt\""))
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(containsString("active-box@xcmailr.test\n")))
            .andExpect(content().string(containsString("expired-box@xcmailr.test\n")));
    }

    @Test
    @DisplayName("Happy path: Legacy GET /mail/mymaillist.txt alias returns attachment header and all addresses")
    public void testLegacyExportAllMailboxesHappyPath() throws Exception
    {
        final MBox box = new MBox("my-box", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mail/mymaillist.txt")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes.txt\""))
            .andExpect(content().string(containsString("my-box@xcmailr.test\n")));
    }

    @Test
    @DisplayName("Happy path: GET /mailboxes/export/active returns 200 OK, attachment header, and only active addresses")
    public void testExportActiveMailboxesHappyPath() throws Exception
    {
        final MBox activeBox = new MBox("active-only", "xcmailr.test", 0L, false, owner);
        final MBox expiredBox = new MBox("expired-ignored", "xcmailr.test", 0L, true, owner);
        mailboxRepository.saveAll(List.of(activeBox, expiredBox));

        mockMvc.perform(get("/mailboxes/export/active")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes-active.txt\""))
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(containsString("active-only@xcmailr.test\n")))
            .andExpect(content().string(not(containsString("expired-ignored@xcmailr.test"))));
    }

    @Test
    @DisplayName("Happy path: Legacy GET /mail/myactivemaillist.txt alias returns attachment header and active addresses")
    public void testLegacyExportActiveMailboxesHappyPath() throws Exception
    {
        final MBox activeBox = new MBox("active-legacy", "xcmailr.test", 0L, false, owner);
        final MBox expiredBox = new MBox("expired-legacy", "xcmailr.test", 0L, true, owner);
        mailboxRepository.saveAll(List.of(activeBox, expiredBox));

        mockMvc.perform(get("/mail/myactivemaillist.txt")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes-active.txt\""))
            .andExpect(content().string(containsString("active-legacy@xcmailr.test\n")))
            .andExpect(content().string(not(containsString("expired-legacy@xcmailr.test"))));
    }

    @Test
    @DisplayName("Happy path: POST /mailboxes/bulk/export returns 200 OK, attachment header, and selected addresses")
    public void testBulkExportSelectedAddressesHappyPath() throws Exception
    {
        final MBox box1 = new MBox("sel1", "xcmailr.test", 0L, false, owner);
        final MBox box2 = new MBox("sel2", "xcmailr.test", 0L, false, owner);
        final MBox box3 = new MBox("unsel", "xcmailr.test", 0L, false, owner);
        mailboxRepository.saveAll(List.of(box1, box2, box3));

        mockMvc.perform(post("/mailboxes/bulk/export")
                .param("mailboxIds", String.valueOf(box1.getId()), String.valueOf(box2.getId()))
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes-selected.txt\""))
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(containsString("sel1@xcmailr.test\n")))
            .andExpect(content().string(containsString("sel2@xcmailr.test\n")))
            .andExpect(content().string(not(containsString("unsel@xcmailr.test"))));
    }

    @Test
    @DisplayName("Happy path: Legacy POST /mail/myselectedmaillist.txt alias returns attachment header and selected addresses")
    public void testLegacyBulkExportSelectedAddressesHappyPath() throws Exception
    {
        final MBox box = new MBox("legacy-selected", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mail/myselectedmaillist.txt")
                .param("mailboxIds", String.valueOf(box.getId()))
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes-selected.txt\""))
            .andExpect(content().string(containsString("legacy-selected@xcmailr.test\n")));
    }

    // =========================================================================
    // 2. ERROR CASES & DEFENSIVE HANDLING
    // =========================================================================

    @Test
    @DisplayName("Error case: Unauthenticated requests to export endpoints are redirected to login")
    public void testUnauthenticatedExportRequestsRedirected() throws Exception
    {
        mockMvc.perform(get("/mailboxes/export/all"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", containsString("/login")));

        mockMvc.perform(get("/mailboxes/export/active"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", containsString("/login")));

        mockMvc.perform(post("/mailboxes/bulk/export")
                .param("mailboxIds", "1")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    @DisplayName("Error case: Cross-tenant isolation filters out mailbox IDs owned by other users")
    public void testCrossTenantExportIsolation() throws Exception
    {
        final MBox aliceBox = new MBox("alice-allowed", "xcmailr.test", 0L, false, owner);
        final MBox bobBox = new MBox("bob-forbidden", "xcmailr.test", 0L, false, otherUser);
        mailboxRepository.saveAll(List.of(aliceBox, bobBox));

        mockMvc.perform(post("/mailboxes/bulk/export")
                .param("mailboxIds", String.valueOf(aliceBox.getId()), String.valueOf(bobBox.getId()))
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes-selected.txt\""))
            .andExpect(content().string(containsString("alice-allowed@xcmailr.test\n")))
            .andExpect(content().string(not(containsString("bob-forbidden@xcmailr.test"))));
    }

    @Test
    @DisplayName("Error case: Malformed or unparseable JSON payload handled cleanly without 500 error")
    public void testMalformedJsonPayloadHandledGracefully() throws Exception
    {
        mockMvc.perform(post("/mailboxes/bulk/export")
                .param("jsonObj", "{malformed-json-payload")
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes-selected.txt\""))
            .andExpect(content().string(""));
    }

    // =========================================================================
    // 3. SPECIAL CASES
    // =========================================================================

    @Test
    @DisplayName("Special case: Export all with 0 user mailboxes returns 200 OK with empty body and attachment header")
    public void testExportAllZeroMailboxesReturnsEmptyBodyWithHeader() throws Exception
    {
        mockMvc.perform(get("/mailboxes/export/all")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes.txt\""))
            .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Special case: Export active with only expired mailboxes returns 200 OK with empty body and attachment header")
    public void testExportActiveWithOnlyExpiredMailboxes() throws Exception
    {
        final MBox expiredBox = new MBox("expired-box", "xcmailr.test", 0L, true, owner);
        mailboxRepository.save(expiredBox);

        mockMvc.perform(get("/mailboxes/export/active")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes-active.txt\""))
            .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Special case: Bulk export with empty selection returns 200 OK with empty body and attachment header")
    public void testBulkExportEmptySelection() throws Exception
    {
        mockMvc.perform(post("/mailboxes/bulk/export")
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes-selected.txt\""))
            .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Special case: Address formatting with tags and subdomains preserved accurately")
    public void testExportAddressFormattingPreservation() throws Exception
    {
        final Domain subDomain = new Domain("sub.dept.xcmailr.test");
        domainRepository.save(subDomain);

        final MBox taggedBox = new MBox("alice+tag123", "sub.dept.xcmailr.test", 0L, false, owner);
        mailboxRepository.save(taggedBox);

        mockMvc.perform(get("/mailboxes/export/all")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(content().string("alice+tag123@sub.dept.xcmailr.test\n"));
    }

    // =========================================================================
    // 4. TEMPLATE INTEGRATION & UI REGRESSION
    // =========================================================================

    @Test
    @DisplayName("Template integration: Dashboard header renders New Mailbox and omits Export dropdown button")
    public void testDashboardHeaderOmitsExportDropdown() throws Exception
    {
        final MBox box = new MBox("test-box", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            // Header must contain New Mailbox action button
            .andExpect(content().string(containsString("New Mailbox")))
            // Header must NOT contain prominent export dropdown button or dropdown menu items
            .andExpect(content().string(not(containsString("title=\"Export mailbox addresses\""))))
            .andExpect(content().string(not(containsString("All Mailboxes (.txt)"))))
            .andExpect(content().string(not(containsString("Active Mailboxes (.txt)"))));
    }

    @Test
    @DisplayName("Template integration: Table footer renders All and Active export utility links when totalElements > 0")
    public void testTableFooterRendersExportUtilityLinksWhenMailboxesExist() throws Exception
    {
        final MBox box = new MBox("footer-test", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            // Footer utility links must be present
            .andExpect(content().string(containsString("Export:")))
            .andExpect(content().string(containsString("href=\"/mailboxes/export/all\"")))
            .andExpect(content().string(containsString("All (.txt)")))
            .andExpect(content().string(containsString("href=\"/mailboxes/export/active\"")))
            .andExpect(content().string(containsString("Active (.txt)")));
    }

    @Test
    @DisplayName("Template integration: Empty state hides table footer export utility links")
    public void testEmptyStateHidesFooterExportLinks() throws Exception
    {
        mockMvc.perform(get("/mailboxes")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            // Empty state message present
            .andExpect(content().string(containsString("No mailboxes found matching the criteria.")))
            // Footer export section hidden when totalElements == 0
            .andExpect(content().string(not(containsString("Export:"))))
            .andExpect(content().string(not(containsString("href=\"/mailboxes/export/all\""))))
            .andExpect(content().string(not(containsString("href=\"/mailboxes/export/active\""))));
    }

    @Test
    @DisplayName("Template integration: Row action buttons omit data-bs-toggle='tooltip' and preserve semantic title and aria-label")
    public void testRowActionButtonsOmitBootstrapTooltip() throws Exception
    {
        final MBox box = new MBox("tooltip-test", "xcmailr.test", 0L, false, owner);
        mailboxRepository.save(box);

        mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            // Row action buttons preserve title and aria-label
            .andExpect(content().string(containsString("title=\"Disable mailbox\"")))
            .andExpect(content().string(containsString("aria-label=\"Disable mailbox\"")))
            .andExpect(content().string(containsString("title=\"Extend validity by 24 hours (+1d)\"")))
            .andExpect(content().string(containsString("aria-label=\"Extend validity by 24 hours (+1d)\"")))
            // Row action buttons must NOT contain data-bs-toggle="tooltip"
            .andExpect(content().string(not(containsString("data-bs-toggle=\"tooltip\""))));
    }
}

