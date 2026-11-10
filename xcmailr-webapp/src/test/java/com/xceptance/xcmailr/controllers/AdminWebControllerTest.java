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
import static org.hamcrest.Matchers.matchesPattern;
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

import java.util.List;

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
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import com.xceptance.xcmailr.models.Domain;
import com.xceptance.xcmailr.models.MailTransaction;
import com.xceptance.xcmailr.models.User;

/**
 * Integration tests for {@link AdminWebController} testing administrative controls,
 * RBAC enforcement, user management, domain whitelisting, and transaction auditing.
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public class AdminWebControllerTest
{
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private MailTransactionRepository transactionRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

    private User admin;
    private User adminDe;
    private User regularUser;

    @BeforeEach
    public void setup()
    {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        transactionRepository.deleteAll();
        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();

        admin = new User("Super", "Admin", "admin@xcmailr.test", "AdminPass123", "en");
        admin.setActive(true);
        admin.setAdmin(true);
        userRepository.save(admin);

        adminDe = new User("German", "Admin", "admin-de@xcmailr.test", "AdminPass123", "de");
        adminDe.setActive(true);
        adminDe.setAdmin(true);
        userRepository.save(adminDe);

        regularUser = new User("Regular", "User", "user@xcmailr.test", "UserPass123", "en");
        regularUser.setActive(true);
        regularUser.setAdmin(false);
        userRepository.save(regularUser);
    }

    @Test
    @DisplayName("Non-admin user cannot access admin dashboard (403 Forbidden)")
    public void testRbacDeniesRegularUser() throws Exception
    {
        mockMvc.perform(get("/admin/users")
               .with(user("user@xcmailr.test").roles("USER")))
               .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can view paginated users list")
    public void testListUsers() throws Exception
    {
        mockMvc.perform(get("/admin/users")
               .with(user("admin@xcmailr.test").roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/users"))
               .andExpect(model().attributeExists("users"))
               .andExpect(model().attributeExists("page"));
    }

    @Test
    @DisplayName("Admin can toggle user active state")
    public void testToggleUserActive() throws Exception
    {
        mockMvc.perform(post("/admin/users/" + regularUser.getId() + "/toggle-active")
               .header("HX-Request", "true")
               .with(user("admin@xcmailr.test").roles("ADMIN"))
               .with(csrf()))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "userListChanged"));

        final User updated = userRepository.findById(regularUser.getId()).orElseThrow();
        assertFalse(updated.isActive(), "Regular user should now be inactive");
    }

    @Test
    @DisplayName("Admin cannot deactivate own account (400 Bad Request)")
    public void testAdminCannotDeactivateSelf() throws Exception
    {
        mockMvc.perform(post("/admin/users/" + admin.getId() + "/toggle-active")
               .with(user("admin@xcmailr.test").roles("ADMIN"))
               .with(csrf()))
               .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin can promote user to administrator")
    public void testToggleUserAdmin() throws Exception
    {
        mockMvc.perform(post("/admin/users/" + regularUser.getId() + "/toggle-admin")
               .header("HX-Request", "true")
               .with(user("admin@xcmailr.test").roles("ADMIN"))
               .with(csrf()))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "userListChanged"));

        final User updated = userRepository.findById(regularUser.getId()).orElseThrow();
        assertTrue(updated.isAdmin(), "Regular user should now be an admin");
    }

    @Test
    @DisplayName("Admin can delete user account")
    public void testDeleteUser() throws Exception
    {
        mockMvc.perform(delete("/admin/users/" + regularUser.getId())
               .header("HX-Request", "true")
               .with(user("admin@xcmailr.test").roles("ADMIN"))
               .with(csrf()))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "userListChanged"));

        assertFalse(userRepository.existsById(regularUser.getId()), "User should be deleted");
    }

    @Test
    @DisplayName("Admin can list and add domain to whitelist")
    public void testDomainWhitelistManagement() throws Exception
    {
        mockMvc.perform(post("/admin/domains")
               .with(user("admin@xcmailr.test").roles("ADMIN"))
               .with(csrf())
               .param("domainName", "newdomain.test"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/admin/domains?created"));

        assertTrue(domainRepository.existsByDomainnameIgnoreCase("newdomain.test"));

        final Domain domain = domainRepository.findByDomainnameIgnoreCase("newdomain.test").orElseThrow();

        mockMvc.perform(delete("/admin/domains/" + domain.getId())
               .header("HX-Request", "true")
               .with(user("admin@xcmailr.test").roles("ADMIN"))
               .with(csrf()))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "domainListChanged"));

        assertFalse(domainRepository.existsById(domain.getId()), "Domain should be deleted");
    }

    @Test
    @DisplayName("Admin can view transaction log and purge records")
    public void testTransactionsAuditAndPurge() throws Exception
    {
        final MailTransaction tx = new MailTransaction(300, "sender@external.com", "box@xcmailr.test", "user@xcmailr.test");
        tx.setTs(System.currentTimeMillis() - 86400000L * 10); // 10 days ago
        transactionRepository.save(tx);

        mockMvc.perform(get("/admin/transactions")
               .with(user("admin@xcmailr.test").roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/transactions"))
               .andExpect(model().attributeExists("transactions"))
               .andExpect(model().attributeExists("statusSummary"));

        // Purge records older than 7 days
        mockMvc.perform(post("/admin/transactions/purge")
               .with(user("admin@xcmailr.test").roles("ADMIN"))
               .with(csrf())
               .param("days", "7"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/admin/transactions?purged"));

        assertEquals(0, transactionRepository.count(), "Old transaction should be purged");
    }

    @Test
    @DisplayName("Admin can view system statistics dashboard including 24h and 7d chart series")
    public void testStatisticsDashboard() throws Exception
    {
        mockMvc.perform(get("/admin/statistics")
               .with(user("admin@xcmailr.test").roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/statistics"))
               .andExpect(model().attributeExists("totalUsers"))
               .andExpect(model().attributeExists("totalMailboxes"))
               .andExpect(model().attributeExists("totalMails"))
               .andExpect(model().attributeExists("totalTransactions"))
               .andExpect(model().attributeExists("chart24h"))
               .andExpect(model().attributeExists("chart7d"))
               .andExpect(content().string(containsString("id=\"chart-last-24h\"")))
               .andExpect(content().string(containsString("id=\"chart-last-7d\"")))
               .andExpect(content().string(containsString("/webjars/chart.js/4.6.1/dist/chart.umd.js")))
               .andExpect(result -> {
                   final Object c24 = result.getModelAndView().getModel().get("chart24h");
                   assertNotNull(c24);
                   assertTrue(c24 instanceof com.xceptance.xcmailr.dto.ChartDataDto);
                   final com.xceptance.xcmailr.dto.ChartDataDto dto24 = (com.xceptance.xcmailr.dto.ChartDataDto) c24;
                   assertEquals(24, dto24.labels().size());
                   assertEquals(24, dto24.forwarded().size());
                   assertEquals(24, dto24.dropped().size());

                   final Object c7 = result.getModelAndView().getModel().get("chart7d");
                   assertNotNull(c7);
                   assertTrue(c7 instanceof com.xceptance.xcmailr.dto.ChartDataDto);
                   final com.xceptance.xcmailr.dto.ChartDataDto dto7 = (com.xceptance.xcmailr.dto.ChartDataDto) c7;
                   assertEquals(7, dto7.labels().size());
                   assertEquals(7, dto7.forwarded().size());
                   assertEquals(7, dto7.dropped().size());
               });
    }

    /**
     * Verifies that the admin statistics dashboard renders both canvas elements, includes the Chart.js
     * WebJar asset, and embeds well-formed JSON datasets for both 24h and 7d series.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin statistics dashboard renders HTML with chart canvases, WebJar script, and JSON payloads (Happy Path)")
    public void testAdminStatisticsChartRenderingAndPayloads() throws Exception
    {
        final long now = System.currentTimeMillis();
        final MailTransaction fwdTx = new MailTransaction(300, "sender@ext.com", "target@xcmailr.test", null);
        fwdTx.setTs(now - 3600_000L);
        transactionRepository.save(fwdTx);

        final MailTransaction dropTx = new MailTransaction(200, "sender@ext.com", "target@xcmailr.test", null);
        dropTx.setTs(now - 7200_000L);
        transactionRepository.save(dropTx);

        mockMvc.perform(get("/admin/statistics")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/statistics"))
               .andExpect(content().string(containsString("<canvas id=\"chart-last-24h\"></canvas>")))
               .andExpect(content().string(containsString("<canvas id=\"chart-last-7d\"></canvas>")))
               .andExpect(content().string(containsString("/webjars/chart.js/4.6.1/dist/chart.umd.js")))
               .andExpect(content().string(containsString("const chart24hData = {")))
               .andExpect(content().string(containsString("const chart7dData = {")))
               .andExpect(content().string(containsString("\"forwarded\":")))
               .andExpect(content().string(containsString("\"dropped\":")));
    }

    // =========================================================================
    // Testing Pyramid: Tier 2 - Localization Happy Path (English & German)
    // =========================================================================

    /**
     * Verifies that the user management view renders localized text in English.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin users list renders localized strings in English (Happy Path)")
    public void testAdminUsersLocalizedEnglish() throws Exception
    {
        mockMvc.perform(get("/admin/users")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/users"))
               .andExpect(content().string(containsString("User Management")))
               .andExpect(content().string(containsString("Manage registered user accounts")))
               .andExpect(content().string(containsString("Email")))
               .andExpect(content().string(containsString("Actions")))
               .andExpect(content().string(containsString("You")));
    }

    /**
     * Verifies that the user management view renders localized text in German.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin users list renders localized strings in German (Happy Path)")
    public void testAdminUsersLocalizedGerman() throws Exception
    {
        mockMvc.perform(get("/admin/users")
               .with(user(adminDe.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/users"))
               .andExpect(content().string(containsString("Benutzerverwaltung")))
               .andExpect(content().string(containsString("Registrierte Benutzerkonten")))
               .andExpect(content().string(containsString("E-Mail")))
               .andExpect(content().string(containsString("Aktionen")))
               .andExpect(content().string(containsString("Sie")));
    }

    /**
     * Verifies that the domain whitelist view renders localized text in English.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin domains whitelist renders localized strings in English (Happy Path)")
    public void testAdminDomainsLocalizedEnglish() throws Exception
    {
        mockMvc.perform(get("/admin/domains")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/domains"))
               .andExpect(content().string(containsString("Domain Whitelist")))
               .andExpect(content().string(containsString("Add Domain")))
               .andExpect(content().string(containsString("Domain Name")))
               .andExpect(content().string(containsString("title=\"Valid fully qualified domain name\"")));
    }

    /**
     * Verifies that the domain whitelist view renders localized text in German.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin domains whitelist renders localized strings in German (Happy Path)")
    public void testAdminDomainsLocalizedGerman() throws Exception
    {
        mockMvc.perform(get("/admin/domains")
               .with(user(adminDe.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/domains"))
               .andExpect(content().string(containsString("Domain-Positivliste")))
               .andExpect(content().string(containsString("Domain hinzufügen")))
               .andExpect(content().string(containsString("Domain-Name")))
               .andExpect(content().string(containsString("title=\"Gültiger vollqualifizierter Domänenname\"")));
    }

    /**
     * Verifies that the transaction audit view renders localized text in English.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin transactions audit log renders localized strings in English (Happy Path)")
    public void testAdminTransactionsLocalizedEnglish() throws Exception
    {
        mockMvc.perform(get("/admin/transactions")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/transactions"))
               .andExpect(content().string(containsString("Transaction Audit Log")))
               .andExpect(content().string(containsString("Older than 7 days")))
               .andExpect(content().string(containsString("Purge")))
               .andExpect(content().string(containsString("Status Overview:")));
    }

    /**
     * Verifies that the transaction audit view renders localized text in German.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin transactions audit log renders localized strings in German (Happy Path)")
    public void testAdminTransactionsLocalizedGerman() throws Exception
    {
        mockMvc.perform(get("/admin/transactions")
               .with(user(adminDe.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/transactions"))
               .andExpect(content().string(containsString("Transaktions-Audit-Protokoll")))
               .andExpect(content().string(containsString("Älter als 7 Tage")))
               .andExpect(content().string(containsString("Bereinigen")))
               .andExpect(content().string(containsString("Statusübersicht:")));
    }

    /**
     * Verifies that the system statistics dashboard renders localized text in English.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin statistics dashboard renders localized strings in English (Happy Path)")
    public void testAdminStatisticsLocalizedEnglish() throws Exception
    {
        mockMvc.perform(get("/admin/statistics")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/statistics"))
               .andExpect(content().string(containsString("Email Statistics")))
               .andExpect(content().string(containsString("Registered Users")))
               .andExpect(content().string(containsString("Active Mailboxes")))
               .andExpect(content().string(containsString("Stored Messages")))
               .andExpect(content().string(containsString("Logged Transactions")))
               .andExpect(content().string(containsString("Last 24 Hours Activity")))
               .andExpect(content().string(containsString("Hourly resolution")))
               .andExpect(content().string(containsString("Last 7 Days Activity")))
               .andExpect(content().string(containsString("Daily resolution")))
               .andExpect(content().string(containsString("Transaction Delivery Breakdown")));
    }

    /**
     * Verifies that the system statistics dashboard renders localized text in German.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin statistics dashboard renders localized strings in German (Happy Path)")
    public void testAdminStatisticsLocalizedGerman() throws Exception
    {
        mockMvc.perform(get("/admin/statistics")
               .with(user(adminDe.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/statistics"))
               .andExpect(content().string(containsString("E-Mail-Statistiken")))
               .andExpect(content().string(containsString("Registrierte Benutzer")))
               .andExpect(content().string(containsString("Aktive Postfächer")))
               .andExpect(content().string(containsString("Gespeicherte Nachrichten")))
               .andExpect(content().string(containsString("Protokollierte Transaktionen")))
               .andExpect(content().string(containsString("Aktivität der letzten 24 Stunden")))
               .andExpect(content().string(containsString("Stündliche Auflösung")))
               .andExpect(content().string(containsString("Aktivität der letzten 7 Tage")))
               .andExpect(content().string(containsString("Tägliche Auflösung")))
               .andExpect(content().string(containsString("Transaktions-Zustellungsübersicht")));
    }

    // =========================================================================
    // Testing Pyramid: Tier 2 - Special Cases & Fallback Behavior
    // =========================================================================

    /**
     * Verifies that admin views gracefully fall back to default English when requested by a user
     * with an unsupported locale.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin views fall back to English on unsupported locale (Special Case)")
    public void testAdminFallbackToEnglishOnUnsupportedLocale() throws Exception
    {
        final User adminUnsupported = new User("Italian", "Admin", "admin-it@xcmailr.test", "AdminPass123", "it");
        adminUnsupported.setActive(true);
        adminUnsupported.setAdmin(true);
        userRepository.save(adminUnsupported);

        mockMvc.perform(get("/admin/users")
               .with(user(adminUnsupported.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/users"))
               .andExpect(content().string(containsString("User Management")))
               .andExpect(content().string(containsString("Manage registered user accounts")));
    }

    // =========================================================================
    // Testing Pyramid: Tier 2 - Security (OWASP XSS Sanitization)
    // =========================================================================

    /**
     * Verifies that potentially malicious XSS payloads stored in user attributes are strictly escaped
     * in the rendered administrative HTML table, preventing cross-site scripting vulnerabilities.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin user list sanitizes XSS payloads in user attributes (Security Case)")
    public void testAdminUserXssSanitizationInLocalizedView() throws Exception
    {
        final String xssPayload = "<script>alert('xss-admin')</script>";
        final User maliciousUser = new User("Malicious", xssPayload, "malicious@xcmailr.test", "UserPass123", "de");
        maliciousUser.setActive(true);
        maliciousUser.setAdmin(false);
        userRepository.save(maliciousUser);

        mockMvc.perform(get("/admin/users")
               .with(user(adminDe.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(content().string(not(containsString("<script>alert('xss-admin')</script>"))))
               .andExpect(content().string(containsString("&lt;script&gt;alert(&#39;xss-admin&#39;)&lt;/script&gt;")));
    }

    // =========================================================================
    // Table Pagination & Display Count Selector Tests (OpenSpec Task 5.2)
    // =========================================================================

    /**
     * Happy Path: Verifies that /admin/users renders a 7-slot sliding window pagination bar
     * with ellipsis when there are more than 7 pages.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Happy Path: /admin/users renders 7-slot sliding pagination with ellipsis")
    public void testAdminUsersPagination7SlotSequenceWithEllipsis() throws Exception
    {
        for (int i = 1; i <= 18; i++)
        {
            final User u = new User("Test" + i, "User" + i, "user" + i + "@xcmailr.test", "Pass1234", "en");
            u.setActive(true);
            userRepository.save(u);
        }

        mockMvc.perform(get("/admin/users")
               .header("HX-Request", "true")
               .param("page", "4")
               .param("size", "2")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("aria-label=\"User pagination\"")))
               .andExpect(content().string(containsString("&hellip;")))
               .andExpect(content().string(containsString("Display Count")));
    }

    /**
     * Happy Path: Verifies that /admin/users Display Count presets switch page density
     * and clamp size safely (including size=0 clamping to 1000).
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Happy Path: /admin/users Display Count presets switch page density")
    public void testAdminUsersDisplayCountPresets() throws Exception
    {
        mockMvc.perform(get("/admin/users")
               .header("HX-Request", "true")
               .param("size", "50")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(model().attribute("size", 50))
               .andExpect(content().string(matchesPattern("(?s).*<li[^>]*class=\"[^\"]*page-item active[^\"]*\"[^>]*>\\s*<button[^>]*size=50.*")));

        mockMvc.perform(get("/admin/users")
               .header("HX-Request", "true")
               .param("size", "0")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(model().attribute("size", 1000))
               .andExpect(content().string(matchesPattern("(?s).*<li[^>]*class=\"[^\"]*page-item active[^\"]*\"[^>]*>\\s*<button[^>]*size=0.*")));
    }

    /**
     * Happy Path: Verifies that /admin/users preserves pagination bar and Display Count selector
     * on single-page results while disabling Previous and Next buttons.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Happy Path: /admin/users single page keeps controls visible with disabled Previous and Next")
    public void testAdminUsersSinglePageDisabledControls() throws Exception
    {
        mockMvc.perform(get("/admin/users")
               .header("HX-Request", "true")
               .param("size", "15")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("aria-label=\"User pagination\"")))
               .andExpect(content().string(containsString("Display Count")))
               .andExpect(content().string(matchesPattern("(?s).*<li[^>]*class=\"[^\"]*page-item disabled[^\"]*\"[^>]*>\\s*<button[^>]*page=-1.*")))
               .andExpect(content().string(matchesPattern("(?s).*<li[^>]*class=\"[^\"]*page-item disabled[^\"]*\"[^>]*>\\s*<button[^>]*page=1.*")));
    }

    /**
     * Happy Path: Verifies that /admin/transactions renders 7-slot sliding pagination and Display Count selector.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Happy Path: /admin/transactions renders 7-slot pagination and Display Count selector")
    public void testAdminTransactionsPaginationAndDisplayCount() throws Exception
    {
        for (int i = 1; i <= 20; i++)
        {
            final MailTransaction tx = new MailTransaction(100, "sender" + i + "@ext.com", "target" + i + "@xcmailr.test", null);
            transactionRepository.save(tx);
        }

        mockMvc.perform(get("/admin/transactions")
               .header("HX-Request", "true")
               .param("page", "4")
               .param("size", "2")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("aria-label=\"Transaction pagination\"")))
               .andExpect(content().string(containsString("&hellip;")))
               .andExpect(content().string(containsString("Display Count")));

        mockMvc.perform(get("/admin/transactions")
               .header("HX-Request", "true")
               .param("size", "50")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(model().attribute("size", 50))
               .andExpect(content().string(matchesPattern("(?s).*<li[^>]*class=\"[^\"]*page-item active[^\"]*\"[^>]*>\\s*<button[^>]*size=50.*")));
    }

    // =========================================================================
    // Layer 1: Unit Tests for Whitelist Sorting Logic (Testing Pyramid)
    // =========================================================================

    /**
     * Happy Path: Verifies that resolveSortField maps all whitelisted column keys correctly.
     */
    @Test
    @DisplayName("Layer 1 Unit: resolveSortField maps whitelisted column names correctly")
    public void testResolveSortField_HappyPath()
    {
        assertEquals("email", AdminWebController.resolveSortField("email"));
        assertEquals("email", AdminWebController.resolveSortField("mail"));
        assertEquals("name", AdminWebController.resolveSortField("name"));
        assertEquals("status", AdminWebController.resolveSortField("status"));
        assertEquals("role", AdminWebController.resolveSortField("role"));
    }

    /**
     * Error Cases: Verifies that resolveSortField safely normalizes null, empty, or unknown columns to 'email'.
     */
    @Test
    @DisplayName("Layer 1 Unit: resolveSortField safely falls back to 'email' on invalid or missing input")
    public void testResolveSortField_ErrorCases()
    {
        assertEquals("email", AdminWebController.resolveSortField(null));
        assertEquals("email", AdminWebController.resolveSortField(""));
        assertEquals("email", AdminWebController.resolveSortField("   "));
        assertEquals("email", AdminWebController.resolveSortField("unknown"));
        assertEquals("email", AdminWebController.resolveSortField("password"));
        assertEquals("email", AdminWebController.resolveSortField("id; DROP TABLE users;--"));
    }

    /**
     * Special Cases: Verifies that resolveSortField handles whitespace and mixed-case strings.
     */
    @Test
    @DisplayName("Layer 1 Unit: resolveSortField handles mixed-case and trimmed inputs")
    public void testResolveSortField_SpecialCases()
    {
        assertEquals("email", AdminWebController.resolveSortField("EMAIL"));
        assertEquals("email", AdminWebController.resolveSortField("  mAiL  "));
        assertEquals("name", AdminWebController.resolveSortField("NaMe"));
        assertEquals("status", AdminWebController.resolveSortField("STATUS "));
        assertEquals("role", AdminWebController.resolveSortField("  RoLe  "));
    }

    /**
     * Happy Path: Verifies that resolveSortDirection correctly resolves 'asc' and 'desc'.
     */
    @Test
    @DisplayName("Layer 1 Unit: resolveSortDirection resolves 'asc' and 'desc'")
    public void testResolveSortDirection_HappyPath()
    {
        assertEquals("asc", AdminWebController.resolveSortDirection("asc"));
        assertEquals("desc", AdminWebController.resolveSortDirection("desc"));
    }

    /**
     * Error Cases: Verifies that resolveSortDirection safely normalizes null, empty, or invalid directions to 'desc'.
     */
    @Test
    @DisplayName("Layer 1 Unit: resolveSortDirection safely falls back to 'desc' on invalid or missing input")
    public void testResolveSortDirection_ErrorCases()
    {
        assertEquals("desc", AdminWebController.resolveSortDirection(null));
        assertEquals("desc", AdminWebController.resolveSortDirection(""));
        assertEquals("desc", AdminWebController.resolveSortDirection("   "));
        assertEquals("desc", AdminWebController.resolveSortDirection("invalid"));
        assertEquals("desc", AdminWebController.resolveSortDirection("1; DROP TABLE users;--"));
    }

    /**
     * Special Cases: Verifies that resolveSortDirection handles mixed-case direction strings.
     */
    @Test
    @DisplayName("Layer 1 Unit: resolveSortDirection handles mixed-case inputs")
    public void testResolveSortDirection_SpecialCases()
    {
        assertEquals("asc", AdminWebController.resolveSortDirection("ASC"));
        assertEquals("asc", AdminWebController.resolveSortDirection("  AsC  "));
        assertEquals("desc", AdminWebController.resolveSortDirection("DESC"));
        assertEquals("desc", AdminWebController.resolveSortDirection("dEsC"));
    }

    /**
     * Happy Path: Verifies that resolveSort constructs valid Sort instances for all whitelisted columns.
     */
    @Test
    @DisplayName("Layer 1 Unit: resolveSort constructs Sort orders for all whitelisted columns")
    public void testResolveSort_HappyPath()
    {
        // 1. Email ASC: single order by mail ASC ignoreCase()
        final Sort emailAsc = AdminWebController.resolveSort("email", "asc");
        final List<Sort.Order> emailAscOrders = emailAsc.toList();
        assertEquals(1, emailAscOrders.size());
        assertEquals("mail", emailAscOrders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, emailAscOrders.get(0).getDirection());
        assertTrue(emailAscOrders.get(0).isIgnoreCase());

        // 2. Email DESC: single order by mail DESC ignoreCase()
        final Sort emailDesc = AdminWebController.resolveSort("email", "desc");
        final List<Sort.Order> emailDescOrders = emailDesc.toList();
        assertEquals(1, emailDescOrders.size());
        assertEquals("mail", emailDescOrders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, emailDescOrders.get(0).getDirection());
        assertTrue(emailDescOrders.get(0).isIgnoreCase());

        // 3. Name ASC: composite surname ASC ignoreCase(), forename ASC ignoreCase(), tiebreaker mail ASC ignoreCase()
        final Sort nameAsc = AdminWebController.resolveSort("name", "asc");
        final List<Sort.Order> nameAscOrders = nameAsc.toList();
        assertEquals(3, nameAscOrders.size());
        assertEquals("surname", nameAscOrders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, nameAscOrders.get(0).getDirection());
        assertTrue(nameAscOrders.get(0).isIgnoreCase());
        assertEquals("forename", nameAscOrders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, nameAscOrders.get(1).getDirection());
        assertTrue(nameAscOrders.get(1).isIgnoreCase());
        assertEquals("mail", nameAscOrders.get(2).getProperty());
        assertEquals(Sort.Direction.ASC, nameAscOrders.get(2).getDirection());
        assertTrue(nameAscOrders.get(2).isIgnoreCase());

        // 4. Name DESC: composite surname DESC ignoreCase(), forename DESC ignoreCase(), tiebreaker mail ASC ignoreCase()
        final Sort nameDesc = AdminWebController.resolveSort("name", "desc");
        final List<Sort.Order> nameDescOrders = nameDesc.toList();
        assertEquals(3, nameDescOrders.size());
        assertEquals("surname", nameDescOrders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, nameDescOrders.get(0).getDirection());
        assertTrue(nameDescOrders.get(0).isIgnoreCase());
        assertEquals("forename", nameDescOrders.get(1).getProperty());
        assertEquals(Sort.Direction.DESC, nameDescOrders.get(1).getDirection());
        assertTrue(nameDescOrders.get(1).isIgnoreCase());
        assertEquals("mail", nameDescOrders.get(2).getProperty());
        assertEquals(Sort.Direction.ASC, nameDescOrders.get(2).getDirection());
        assertTrue(nameDescOrders.get(2).isIgnoreCase());

        // 5. Status ASC: active DESC (active before inactive), tiebreaker mail ASC ignoreCase()
        final Sort statusAsc = AdminWebController.resolveSort("status", "asc");
        final List<Sort.Order> statusAscOrders = statusAsc.toList();
        assertEquals(2, statusAscOrders.size());
        assertEquals("active", statusAscOrders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, statusAscOrders.get(0).getDirection());
        assertEquals("mail", statusAscOrders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, statusAscOrders.get(1).getDirection());
        assertTrue(statusAscOrders.get(1).isIgnoreCase());

        // 6. Status DESC: active ASC (inactive before active), tiebreaker mail ASC ignoreCase()
        final Sort statusDesc = AdminWebController.resolveSort("status", "desc");
        final List<Sort.Order> statusDescOrders = statusDesc.toList();
        assertEquals(2, statusDescOrders.size());
        assertEquals("active", statusDescOrders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, statusDescOrders.get(0).getDirection());
        assertEquals("mail", statusDescOrders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, statusDescOrders.get(1).getDirection());
        assertTrue(statusDescOrders.get(1).isIgnoreCase());

        // 7. Role ASC: admin DESC (admin before user), tiebreaker mail ASC ignoreCase()
        final Sort roleAsc = AdminWebController.resolveSort("role", "asc");
        final List<Sort.Order> roleAscOrders = roleAsc.toList();
        assertEquals(2, roleAscOrders.size());
        assertEquals("admin", roleAscOrders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, roleAscOrders.get(0).getDirection());
        assertEquals("mail", roleAscOrders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, roleAscOrders.get(1).getDirection());
        assertTrue(roleAscOrders.get(1).isIgnoreCase());

        // 8. Role DESC: admin ASC (user before admin), tiebreaker mail ASC ignoreCase()
        final Sort roleDesc = AdminWebController.resolveSort("role", "desc");
        final List<Sort.Order> roleDescOrders = roleDesc.toList();
        assertEquals(2, roleDescOrders.size());
        assertEquals("admin", roleDescOrders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, roleDescOrders.get(0).getDirection());
        assertEquals("mail", roleDescOrders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, roleDescOrders.get(1).getDirection());
        assertTrue(roleDescOrders.get(1).isIgnoreCase());
    }

    /**
     * Error Cases: Verifies that resolveSort handles null or invalid column names by falling back to email sort.
     */
    @Test
    @DisplayName("Layer 1 Unit: resolveSort safely falls back on invalid parameters")
    public void testResolveSort_ErrorCases()
    {
        final Sort invalidColumn = AdminWebController.resolveSort("nonexistent", "asc");
        final List<Sort.Order> orders = invalidColumn.toList();
        assertEquals(1, orders.size());
        assertEquals("mail", orders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(0).getDirection());
        assertTrue(orders.get(0).isIgnoreCase());
    }

    /**
     * Special Cases: Verifies case-insensitivity of inputs and secondary tiebreaker stability.
     */
    @Test
    @DisplayName("Layer 1 Unit: resolveSort normalizes mixed case and guarantees secondary tiebreakers")
    public void testResolveSort_SpecialCases()
    {
        final Sort mixedCase = AdminWebController.resolveSort("  NAME  ", "DESC");
        final List<Sort.Order> orders = mixedCase.toList();
        assertEquals(3, orders.size());
        assertEquals("surname", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());
        assertEquals("forename", orders.get(1).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(1).getDirection());
        assertEquals("mail", orders.get(2).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(2).getDirection());
        assertTrue(orders.get(2).isIgnoreCase());
    }

    /**
     * Layer 1 Integration: Verifies that listUsers endpoint populates model attributes 'sort' and 'dir'.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Layer 1 Web: listUsers populates normalized sort and dir in model")
    public void testListUsers_ModelAttributesPopulated() throws Exception
    {
        mockMvc.perform(get("/admin/users")
               .header("HX-Request", "true")
               .param("sort", "name")
               .param("dir", "desc")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(model().attribute("sort", "name"))
               .andExpect(model().attribute("dir", "desc"));

        mockMvc.perform(get("/admin/users")
               .header("HX-Request", "true")
               .param("sort", "invalid_col")
               .param("dir", "invalid_dir")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(model().attribute("sort", "email"))
               .andExpect(model().attribute("dir", "desc"));
    }
}
