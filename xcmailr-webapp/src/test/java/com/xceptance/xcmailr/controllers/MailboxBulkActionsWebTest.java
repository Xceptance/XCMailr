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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
import models.Mail;
import models.User;

/**
 * WebMvc slice integration test suite for mailbox bulk actions and text export endpoints
 * in {@link MailboxWebController}.
 * <p>
 * Tests cover the testing pyramid including happy paths (enable, disable, extend, reset,
 * delete, export), error cases (empty selection, invalid input, unauthenticated requests,
 * missing CSRF), and security boundary isolation (cross-tenant ID injection / IDOR defense).
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public final class MailboxBulkActionsWebTest
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
     * Resets database state and initializes test users and domains before each test execution.
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
    // 1. HAPPY PATH BATCH OPERATIONS
    // =========================================================================

    @Test
    @DisplayName("Happy path: Bulk enable activates multiple expired mailboxes and returns HTMX trigger")
    public void testBulkEnableHtmx() throws Exception
    {
        final MBox box1 = new MBox("box1", "xcmailr.test", 0L, true, owner);
        final MBox box2 = new MBox("box2", "xcmailr.test", 0L, true, owner);
        mailboxRepository.saveAll(List.of(box1, box2));

        mockMvc.perform(post("/mailboxes/bulk/enable")
                .param("mailboxIds", String.valueOf(box1.getId()), String.valueOf(box2.getId()))
                .header("HX-Request", "true")
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("HX-Trigger", "mailboxChanged"))
            .andExpect(content().string(""));

        final MBox updated1 = mailboxRepository.findById(box1.getId()).orElseThrow();
        final MBox updated2 = mailboxRepository.findById(box2.getId()).orElseThrow();
        assertFalse(updated1.isExpired());
        assertFalse(updated2.isExpired());
    }

    @Test
    @DisplayName("Happy path: Bulk enable redirects when not an HTMX request")
    public void testBulkEnableStandardRedirect() throws Exception
    {
        final MBox box = new MBox("box1", "xcmailr.test", 0L, true, owner);
        mailboxRepository.save(box);

        mockMvc.perform(post("/mailboxes/bulk/enable")
                .param("mailboxIds", String.valueOf(box.getId()))
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/mailboxes?bulkEnabled"));

        final MBox updated = mailboxRepository.findById(box.getId()).orElseThrow();
        assertFalse(updated.isExpired());
    }

    @Test
    @DisplayName("Happy path: Bulk disable deactivates multiple active mailboxes")
    public void testBulkDisableHtmx() throws Exception
    {
        final MBox box1 = new MBox("box1", "xcmailr.test", 0L, false, owner);
        final MBox box2 = new MBox("box2", "xcmailr.test", 0L, false, owner);
        mailboxRepository.saveAll(List.of(box1, box2));

        mockMvc.perform(post("/mailboxes/bulk/disable")
                .param("mailboxIds", String.valueOf(box1.getId()), String.valueOf(box2.getId()))
                .header("HX-Request", "true")
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        final MBox updated1 = mailboxRepository.findById(box1.getId()).orElseThrow();
        final MBox updated2 = mailboxRepository.findById(box2.getId()).orElseThrow();
        assertTrue(updated1.isExpired());
        assertTrue(updated2.isExpired());
    }

    @Test
    @DisplayName("Happy path: Bulk extend adds 24 hours to active expirations")
    public void testBulkExtendHtmx() throws Exception
    {
        final long initialTs = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2);
        final MBox box1 = new MBox("box1", "xcmailr.test", initialTs, false, owner);
        final MBox box2 = new MBox("box2", "xcmailr.test", initialTs, true, owner);
        mailboxRepository.saveAll(List.of(box1, box2));

        mockMvc.perform(post("/mailboxes/bulk/extend")
                .param("mailboxIds", String.valueOf(box1.getId()), String.valueOf(box2.getId()))
                .param("hours", "24")
                .header("HX-Request", "true")
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        final MBox updated1 = mailboxRepository.findById(box1.getId()).orElseThrow();
        final MBox updated2 = mailboxRepository.findById(box2.getId()).orElseThrow();
        assertTrue(updated1.getTs_Active() >= initialTs + TimeUnit.HOURS.toMillis(23));
        assertFalse(updated1.isExpired());
        assertTrue(updated2.getTs_Active() >= initialTs + TimeUnit.HOURS.toMillis(23));
        assertFalse(updated2.isExpired());
    }

    @Test
    @DisplayName("Happy path: Bulk reset counters zeroes forwards and suppressions")
    public void testBulkResetCountersHtmx() throws Exception
    {
        final MBox box1 = new MBox("box1", "xcmailr.test", 0L, false, owner);
        box1.setForwards(12);
        box1.setSuppressions(4);

        final MBox box2 = new MBox("box2", "xcmailr.test", 0L, false, owner);
        box2.setForwards(8);
        box2.setSuppressions(2);
        mailboxRepository.saveAll(List.of(box1, box2));

        mockMvc.perform(post("/mailboxes/bulk/reset-counters")
                .param("mailboxIds", String.valueOf(box1.getId()), String.valueOf(box2.getId()))
                .header("HX-Request", "true")
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        final MBox updated1 = mailboxRepository.findById(box1.getId()).orElseThrow();
        final MBox updated2 = mailboxRepository.findById(box2.getId()).orElseThrow();
        assertEquals(0, updated1.getForwards());
        assertEquals(0, updated1.getSuppressions());
        assertEquals(0, updated2.getForwards());
        assertEquals(0, updated2.getSuppressions());
    }

    @Test
    @DisplayName("Happy path: Bulk delete removes mailboxes and their associated emails")
    public void testBulkDeleteHtmx() throws Exception
    {
        final MBox box1 = new MBox("box1", "xcmailr.test", 0L, false, owner);
        final MBox box2 = new MBox("box2", "xcmailr.test", 0L, false, owner);
        mailboxRepository.saveAll(List.of(box1, box2));

        final Mail mail1 = new Mail();
        mail1.setSender("sender@test.com");
        mail1.setSubject("Hello");
        mail1.setMessage("Content".getBytes());
        mail1.setReceiveTime(System.currentTimeMillis());
        mail1.setMailbox(box1);

        final Mail mail2 = new Mail();
        mail2.setSender("sender@test.com");
        mail2.setSubject("World");
        mail2.setMessage("Content".getBytes());
        mail2.setReceiveTime(System.currentTimeMillis());
        mail2.setMailbox(box2);

        mailRepository.saveAll(List.of(mail1, mail2));

        mockMvc.perform(delete("/mailboxes/bulk")
                .param("mailboxIds", String.valueOf(box1.getId()), String.valueOf(box2.getId()))
                .header("HX-Request", "true")
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        assertTrue(mailboxRepository.findById(box1.getId()).isEmpty());
        assertTrue(mailboxRepository.findById(box2.getId()).isEmpty());
        assertTrue(mailRepository.findById(mail1.getId()).isEmpty());
        assertTrue(mailRepository.findById(mail2.getId()).isEmpty());
    }

    // =========================================================================
    // 2. PLAIN TEXT EXPORT ENDPOINTS
    // =========================================================================

    @Test
    @DisplayName("Happy path: POST /mailboxes/bulk/export streams selected addresses as plain text")
    public void testBulkExportSelectedAddresses() throws Exception
    {
        final MBox box1 = new MBox("alpha", "xcmailr.test", 0L, false, owner);
        final MBox box2 = new MBox("beta", "xcmailr.test", 0L, false, owner);
        final MBox box3 = new MBox("gamma", "xcmailr.test", 0L, false, owner);
        mailboxRepository.saveAll(List.of(box1, box2, box3));

        mockMvc.perform(post("/mailboxes/bulk/export")
                .param("mailboxIds", String.valueOf(box1.getId()), String.valueOf(box3.getId()))
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes-selected.txt\""))
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(containsString("alpha@xcmailr.test\n")))
            .andExpect(content().string(containsString("gamma@xcmailr.test\n")))
            .andExpect(content().string(not(containsString("beta@xcmailr.test"))));
    }

    @Test
    @DisplayName("Happy path: Legacy /mail/myselectedmaillist.txt supports jsonObj JSON array and map payloads")
    public void testLegacySelectedMailListJsonObj() throws Exception
    {
        final MBox box1 = new MBox("alpha", "xcmailr.test", 0L, false, owner);
        final MBox box2 = new MBox("beta", "xcmailr.test", 0L, false, owner);
        mailboxRepository.saveAll(List.of(box1, box2));

        // Legacy format 1: JSON map of {"id": true}
        final String jsonMap = "{\"" + box1.getId() + "\": true, \"" + box2.getId() + "\": false}";
        mockMvc.perform(post("/mail/myselectedmaillist.txt")
                .param("jsonObj", jsonMap)
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes-selected.txt\""))
            .andExpect(content().string(containsString("alpha@xcmailr.test\n")))
            .andExpect(content().string(not(containsString("beta@xcmailr.test"))));

        // Legacy format 2: JSON array of [id1, id2]
        final String jsonArray = "[" + box1.getId() + ", " + box2.getId() + "]";
        mockMvc.perform(post("/mail/myselectedmaillist.txt")
                .param("jsonObj", jsonArray)
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes-selected.txt\""))
            .andExpect(content().string(containsString("alpha@xcmailr.test\n")))
            .andExpect(content().string(containsString("beta@xcmailr.test\n")));
    }

    @Test
    @DisplayName("Happy path: GET /mailboxes/export/all and legacy /mail/mymaillist.txt export all user mailboxes")
    public void testExportAllMailboxes() throws Exception
    {
        final MBox activeBox = new MBox("active", "xcmailr.test", 0L, false, owner);
        final MBox expiredBox = new MBox("expired", "xcmailr.test", 0L, true, owner);
        mailboxRepository.saveAll(List.of(activeBox, expiredBox));

        // Modern endpoint
        mockMvc.perform(get("/mailboxes/export/all")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes.txt\""))
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(containsString("active@xcmailr.test\n")))
            .andExpect(content().string(containsString("expired@xcmailr.test\n")));

        // Legacy endpoint alias
        mockMvc.perform(get("/mail/mymaillist.txt")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes.txt\""))
            .andExpect(content().string(containsString("active@xcmailr.test\n")))
            .andExpect(content().string(containsString("expired@xcmailr.test\n")));
    }

    @Test
    @DisplayName("Happy path: GET /mailboxes/export/active and legacy /mail/myactivemaillist.txt export only active mailboxes")
    public void testExportActiveMailboxes() throws Exception
    {
        final MBox activeBox = new MBox("active", "xcmailr.test", 0L, false, owner);
        final MBox expiredBox = new MBox("expired", "xcmailr.test", 0L, true, owner);
        mailboxRepository.saveAll(List.of(activeBox, expiredBox));

        mockMvc.perform(get("/mailboxes/export/active")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes-active.txt\""))
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(containsString("active@xcmailr.test\n")))
            .andExpect(content().string(not(containsString("expired@xcmailr.test"))));

        mockMvc.perform(get("/mail/myactivemaillist.txt")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes-active.txt\""))
            .andExpect(content().string(containsString("active@xcmailr.test\n")))
            .andExpect(content().string(not(containsString("expired@xcmailr.test"))));
    }

    // =========================================================================
    // 3. ERROR HANDLING & SPECIAL CASES
    // =========================================================================

    @Test
    @DisplayName("Special case: Bulk operations with empty selection return clean response without modification")
    public void testBulkOperationsEmptySelection() throws Exception
    {
        // Bulk enable with empty selection
        mockMvc.perform(post("/mailboxes/bulk/enable")
                .header("HX-Request", "true")
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        // Bulk disable with empty selection
        mockMvc.perform(post("/mailboxes/bulk/disable")
                .header("HX-Request", "true")
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("HX-Trigger", "mailboxChanged"));

        // Bulk export with empty selection returns empty text
        mockMvc.perform(post("/mailboxes/bulk/export")
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes-selected.txt\""))
            .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Special case: Extending an unlimited mailbox (ts_Active == 0) preserves unlimited status")
    public void testBulkExtendUnlimitedMailboxPreservesZero() throws Exception
    {
        final MBox unlimitedBox = new MBox("unlimited", "xcmailr.test", 0L, true, owner);
        mailboxRepository.save(unlimitedBox);

        mockMvc.perform(post("/mailboxes/bulk/extend")
                .param("mailboxIds", String.valueOf(unlimitedBox.getId()))
                .param("hours", "24")
                .header("HX-Request", "true")
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk());

        final MBox reloaded = mailboxRepository.findById(unlimitedBox.getId()).orElseThrow();
        assertEquals(0L, reloaded.getTs_Active(), "Unlimited mailbox must remain unlimited (0L)");
        assertFalse(reloaded.isExpired(), "Mailbox must be unexpired after extension");
    }

    @Test
    @DisplayName("Error case: Unauthenticated bulk action requests are rejected")
    public void testUnauthenticatedBulkActionsRejected() throws Exception
    {
        mockMvc.perform(post("/mailboxes/bulk/enable")
                .param("mailboxIds", "1")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", containsString("/login")));

        mockMvc.perform(get("/mailboxes/export/all"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    @DisplayName("Error case: Missing CSRF token on bulk action results in 403 Forbidden")
    public void testMissingCsrfRejected() throws Exception
    {
        mockMvc.perform(post("/mailboxes/bulk/enable")
                .param("mailboxIds", "1")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 4. SECURITY & TENANT ISOLATION (IDOR DEFENSE)
    // =========================================================================

    @Test
    @DisplayName("Security: Bulk actions strictly ignore mailbox IDs belonging to other users (IDOR prevention)")
    public void testBulkActionsCrossTenantIsolation() throws Exception
    {
        final MBox aliceBox = new MBox("alice-box", "xcmailr.test", 0L, false, owner);
        aliceBox.setForwards(10);
        final MBox bobBox = new MBox("bob-box", "xcmailr.test", 0L, false, otherUser);
        bobBox.setForwards(10);
        mailboxRepository.saveAll(List.of(aliceBox, bobBox));

        // Alice attempts to disable Bob's mailbox alongside hers
        mockMvc.perform(post("/mailboxes/bulk/disable")
                .param("mailboxIds", String.valueOf(aliceBox.getId()), String.valueOf(bobBox.getId()))
                .header("HX-Request", "true")
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk());

        final MBox reloadedAlice = mailboxRepository.findById(aliceBox.getId()).orElseThrow();
        final MBox reloadedBob = mailboxRepository.findById(bobBox.getId()).orElseThrow();
        assertTrue(reloadedAlice.isExpired(), "Alice's mailbox should be disabled");
        assertFalse(reloadedBob.isExpired(), "Bob's mailbox must remain unchanged");

        // Alice attempts to reset Bob's counters
        mockMvc.perform(post("/mailboxes/bulk/reset-counters")
                .param("mailboxIds", String.valueOf(bobBox.getId()))
                .header("HX-Request", "true")
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk());

        final MBox bobAfterReset = mailboxRepository.findById(bobBox.getId()).orElseThrow();
        assertEquals(10, bobAfterReset.getForwards(), "Bob's forwards counter must not be reset by Alice");

        // Alice attempts to delete Bob's mailbox
        mockMvc.perform(delete("/mailboxes/bulk")
                .param("mailboxIds", String.valueOf(bobBox.getId()))
                .header("HX-Request", "true")
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk());

        assertTrue(mailboxRepository.findById(bobBox.getId()).isPresent(), "Bob's mailbox must NOT be deleted by Alice");

        // Alice attempts to export Bob's mailbox
        mockMvc.perform(post("/mailboxes/bulk/export")
                .param("mailboxIds", String.valueOf(bobBox.getId()))
                .with(csrf())
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"mailboxes-selected.txt\""))
            .andExpect(content().string(""));
    }

    /**
     * Verifies that the asynchronous mailbox table HTMX fragment renders the master checkbox,
     * per-row checkboxes, compact hybrid icon controls (toggle, +1d, kebab), and the floating batch bar,
     * while omitting Bootstrap data-bs-toggle tooltip attributes from action buttons.
     */
    @Test
    @DisplayName("Table fragment renders master checkbox, row checkboxes, compact icon controls, and floating batch bar")
    public void testMailboxTableFragmentRenderingWithRowCheckboxesAndCompactControls() throws Exception
    {
        final MBox activeBox = createMailbox(owner, "active-ui", domain, 1, false, 0, 0);
        final MBox expiredBox = createMailbox(owner, "expired-ui", domain, -1, true, 2, 1);

        mockMvc.perform(get("/mailboxes")
                .header("HX-Request", "true")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            // Master checkbox
            .andExpect(content().string(containsString("id=\"selectAllMailboxes\"")))
            .andExpect(content().string(containsString("class=\"form-check-input\"")))
            // Row checkboxes with IDs
            .andExpect(content().string(containsString("class=\"form-check-input mailbox-select\"")))
            .andExpect(content().string(containsString("value=\"" + activeBox.getId() + "\"")))
            .andExpect(content().string(containsString("value=\"" + expiredBox.getId() + "\"")))
            // Compact icon controls: power icon for active box, play icon for expired box
            .andExpect(content().string(containsString("bi bi-power")))
            .andExpect(content().string(containsString("bi bi-play-fill")))
            .andExpect(content().string(containsString("+1d")))
            .andExpect(content().string(containsString("bi bi-three-dots-vertical")))
            .andExpect(content().string(containsString("dropdown-toggle-no-caret")))
            .andExpect(content().string(not(containsString("data-bs-toggle=\"tooltip\""))))
            // Floating bottom batch bar elements
            .andExpect(content().string(containsString("id=\"floatingBatchBar\"")))
            .andExpect(content().string(containsString("id=\"bulkSelectionCount\"")))
            .andExpect(content().string(containsString("id=\"bulkDeselectBtn\"")))
            .andExpect(content().string(containsString("id=\"bulkEnableBtn\"")))
            .andExpect(content().string(containsString("hx-post=\"/mailboxes/bulk/enable\"")))
            .andExpect(content().string(containsString("id=\"bulkDisableBtn\"")))
            .andExpect(content().string(containsString("hx-post=\"/mailboxes/bulk/disable\"")))
            .andExpect(content().string(containsString("id=\"bulkExtendBtn\"")))
            .andExpect(content().string(containsString("hx-post=\"/mailboxes/bulk/extend?hours=24\"")))
            .andExpect(content().string(containsString("id=\"bulkResetBtn\"")))
            .andExpect(content().string(containsString("hx-post=\"/mailboxes/bulk/reset-counters\"")))
            .andExpect(content().string(containsString("id=\"bulkExportBtn\"")))
            .andExpect(content().string(containsString("id=\"bulkDeleteBtn\"")))
            .andExpect(content().string(containsString("hx-delete=\"/mailboxes/bulk\"")))
            .andExpect(content().string(containsString("hx-include=\"input.mailbox-select:checked\"")));
    }

    /**
     * Verifies that the full mailboxes index page renders the New Mailbox action, the table footer export utility
     * links, and client-side selection synchronization script, while omitting the header export dropdown.
     */
    @Test
    @DisplayName("Index page renders New Mailbox header, footer export links, and selection synchronization script")
    public void testMailboxIndexPageRenderingWithFooterExportLinksAndClientScript() throws Exception
    {
        createMailbox(owner, "index-test", domain, 1, false, 0, 0);

        mockMvc.perform(get("/mailboxes")
                .with(user("alice@xcmailr.test")))
            .andExpect(status().isOk())
            // Header New Mailbox button present and prominent Export dropdown omitted
            .andExpect(content().string(containsString("New Mailbox")))
            .andExpect(content().string(not(containsString("title=\"Export mailbox addresses\""))))
            // Table footer export links for All and Active mailboxes
            .andExpect(content().string(containsString("Export:")))
            .andExpect(content().string(containsString("/mailboxes/export/all")))
            .andExpect(content().string(containsString("/mailboxes/export/active")))
            // Client script selection synchronization logic
            .andExpect(content().string(containsString("updateSelectionState")))
            .andExpect(content().string(containsString("selectAllMailboxes")))
            .andExpect(content().string(containsString("floatingBatchBar")));
    }

    /**
     * Helper method to construct and persist an {@link MBox} fixture.
     *
     * @param user         the mailbox owner
     * @param address      the local part address
     * @param domain       the register domain
     * @param hoursValid   the validity hours from current time, or negative for expired
     * @param expired      whether the mailbox is expired
     * @param forwards     the number of forward events
     * @param suppressions the number of suppression events
     * @return the saved {@link MBox} entity
     */
    private MBox createMailbox(final User user, final String address, final Domain domain, final int hoursValid,
                               final boolean expired, final int forwards, final int suppressions)
    {
        final long tsActive = hoursValid < 0
            ? System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
            : (hoursValid == 0 ? 0L : System.currentTimeMillis() + TimeUnit.HOURS.toMillis(hoursValid));
        final MBox box = new MBox(address, domain.getDomainname(), tsActive, expired, user);
        box.setForwards(forwards);
        box.setSuppressions(suppressions);
        return mailboxRepository.save(box);
    }
}
