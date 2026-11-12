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
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.xceptance.xcmailr.dto.ChartDataDto;
import com.xceptance.xcmailr.dto.DomainStatisticsDto;
import com.xceptance.xcmailr.models.Domain;
import com.xceptance.xcmailr.models.MBox;
import com.xceptance.xcmailr.models.MailStatistics;
import com.xceptance.xcmailr.models.MailStatisticsKey;
import com.xceptance.xcmailr.models.MailTransaction;
import com.xceptance.xcmailr.models.User;
import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailStatisticsRepository;
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

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

    @Autowired
    private MailStatisticsRepository mailStatisticsRepository;

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

        mailStatisticsRepository.deleteAll();
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
    @DisplayName("Admin activating inactive user clears lingering confirmation token and timestamp")
    public void testAdminActivatingUserClearsConfirmationTokens() throws Exception
    {
        final User inactiveWithToken = new User("Inactive", "Pending", "inactive_pending@xcmailr.test", "password", "en");
        inactiveWithToken.setActive(false);
        inactiveWithToken.setConfirmation("pending-token-999");
        inactiveWithToken.setTs_confirm(System.currentTimeMillis() - 3600_000L);
        userRepository.save(inactiveWithToken);

        mockMvc.perform(post("/admin/users/" + inactiveWithToken.getId() + "/toggle-active")
               .header("HX-Request", "true")
               .with(user("admin@xcmailr.test").roles("ADMIN"))
               .with(csrf()))
               .andExpect(status().isOk())
               .andExpect(header().string("HX-Trigger", "userListChanged"));

        final User activated = userRepository.findById(inactiveWithToken.getId()).orElseThrow();
        assertTrue(activated.isActive(), "User should now be active");
        assertNull(activated.getConfirmation(), "Confirmation token should be cleared upon activation");
        assertNull(activated.getTs_confirm(), "Confirmation timestamp should be cleared upon activation");
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
        final MBox activeMb = new MBox("active", "xcmailr.test", 0L, false, admin);
        mailboxRepository.save(activeMb);

        final MBox expiredMb = new MBox("expired", "xcmailr.test", 0L, true, admin);
        mailboxRepository.save(expiredMb);

        mockMvc.perform(get("/admin/statistics")
               .with(user("admin@xcmailr.test").roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/statistics"))
               .andExpect(model().attributeExists("totalUsers"))
               .andExpect(model().attribute("totalUsers", 3L))
               .andExpect(model().attribute("adminCount", 2L))
               .andExpect(model().attribute("regularCount", 1L))
               .andExpect(model().attribute("totalMailboxes", 2L))
               .andExpect(model().attribute("activeMailboxCount", 1L))
               .andExpect(model().attribute("expiredMailboxCount", 1L))
               .andExpect(model().attributeExists("totalMails"))
               .andExpect(model().attributeExists("totalTransactions"))
               .andExpect(model().attributeExists("totalVolume24h"))
               .andExpect(model().attributeExists("totalVolume7d"))
               .andExpect(model().attributeExists("chart24h"))
               .andExpect(model().attributeExists("chart7d"))
               .andExpect(model().attributeExists("todaySenderDomains"))
               .andExpect(model().attributeExists("weekSenderDomains"))
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
                   assertEquals(24, dto24.stored().size());
                   assertEquals(24, dto24.dropped().size());

                   final Object c7 = result.getModelAndView().getModel().get("chart7d");
                   assertNotNull(c7);
                   assertTrue(c7 instanceof com.xceptance.xcmailr.dto.ChartDataDto);
                   final com.xceptance.xcmailr.dto.ChartDataDto dto7 = (com.xceptance.xcmailr.dto.ChartDataDto) c7;
                   assertEquals(7, dto7.labels().size());
                   assertEquals(7, dto7.forwarded().size());
                   assertEquals(7, dto7.stored().size());
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

        final java.time.LocalDate todayLd = java.time.LocalDate.now();
        final java.time.LocalTime nowLt = java.time.LocalTime.now();
        final int currentQuarterHour = (nowLt.getHour() * 4) + (nowLt.getMinute() / 15);
        final java.sql.Date today = java.sql.Date.valueOf(todayLd);
        final MailStatisticsKey statKey = new MailStatisticsKey(today, currentQuarterHour, "ext.com", "xcmailr.test");
        final MailStatistics stat = new MailStatistics(statKey, 1, 1, 0);
        mailStatisticsRepository.save(stat);

        mockMvc.perform(get("/admin/statistics")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/statistics"))
               .andExpect(model().attribute("totalVolume24h", 2L))
               .andExpect(model().attribute("totalVolume7d", 2L))
               .andExpect(content().string(containsString("<canvas id=\"chart-last-24h\"></canvas>")))
               .andExpect(content().string(containsString("<canvas id=\"chart-last-7d\"></canvas>")))
               .andExpect(content().string(containsString("id=\"chart-fallback-24h\"")))
               .andExpect(content().string(containsString("id=\"chart-fallback-7d\"")))
               .andExpect(content().string(containsString("/webjars/chart.js/4.6.1/dist/chart.umd.js")))
               .andExpect(content().string(containsString("const chart24hData = {")))
               .andExpect(content().string(containsString("const chart7dData = {")))
               .andExpect(content().string(containsString("\"forwarded\":")))
               .andExpect(content().string(containsString("\"stored\":")))
               .andExpect(content().string(containsString("\"dropped\":")))
               .andExpect(content().string(containsString("labelStored = ")))
               .andExpect(content().string(containsString("function initCharts()")))
               .andExpect(content().string(containsString("createBarChartConfig")))
               .andExpect(content().string(containsString("type: 'bar'")))
               .andExpect(content().string(containsString("stacked: true")))
               .andExpect(content().string(containsString("Forwarded (300):")))
               .andExpect(content().string(containsString("Inactive (200):")))
               .andExpect(content().string(containsString("Total: 2")))
               .andExpect(content().string(containsString("Chart library could not be loaded.")))
               .andExpect(content().string(containsString("chart.options.scales.x.border.color = theme.borderColor;")))
               .andExpect(content().string(containsString("backgroundColor: theme.isDark ? '#212529' : '#ffffff'")))
               .andExpect(content().string(containsString("document.addEventListener('DOMContentLoaded', initCharts)")));
    }

    /**
     * Verifies that the admin statistics dashboard renders localized German chart labels and fallback message.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin statistics dashboard renders localized German chart labels and fallback (Happy Path)")
    public void testAdminStatisticsChartRenderingGerman() throws Exception
    {
        mockMvc.perform(get("/admin/statistics")
               .with(user(adminDe.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/statistics"))
               .andExpect(content().string(containsString("Aktivität der letzten 24 Stunden")))
               .andExpect(content().string(containsString("Aktivität der letzten 7 Tage")))
               .andExpect(content().string(containsString("Diagramm-Bibliothek konnte nicht geladen werden.")))
               .andExpect(content().string(containsString("\"Weitergeleitet\"")))
               .andExpect(content().string(containsString("\"Gespeichert\"")))
               .andExpect(content().string(containsString("\"Verworfen\"")));
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
               .andExpect(content().string(containsString("First Name")))
               .andExpect(content().string(containsString("Last Name")))
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
               .andExpect(content().string(containsString("Vorname")))
               .andExpect(content().string(containsString("Nachname")))
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
               .andExpect(content().string(containsString("2 Admins · 1 Standard")))
               .andExpect(content().string(containsString("Active Mailboxes")))
               .andExpect(content().string(containsString("0 expired (0 total)")))
               .andExpect(content().string(containsString("Stored Messages")))
               .andExpect(content().string(containsString("Logged Transactions")))
               .andExpect(content().string(containsString("Transaction Delivery Status:")))
               .andExpect(content().string(containsString("24-Hour Email Activity")))
               .andExpect(content().string(containsString("Today")))
               .andExpect(content().string(containsString("Last 24 Hours Activity")))
               .andExpect(content().string(containsString("Hourly resolution")))
               .andExpect(content().string(containsString("7-Day Email Activity")))
               .andExpect(content().string(containsString("Weekly Trend")))
               .andExpect(content().string(containsString("Last 7 Days Activity")))
               .andExpect(content().string(containsString("Daily resolution")))
               .andExpect(content().string(containsString("Total: 0")))
               .andExpect(content().string(containsString("Todays (last 24 hours) sender domains")))
               .andExpect(content().string(containsString("Last 7 days sender domains")))
               .andExpect(content().string(containsString("Sender Domain")))
               .andExpect(content().string(containsString("Drops")))
               .andExpect(content().string(containsString("Forwards")))
               .andExpect(content().string(containsString("Stored")))
               .andExpect(content().string(containsString("Total")));
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
               .andExpect(content().string(containsString("2 Admins · 1 Standard")))
               .andExpect(content().string(containsString("Aktive Postfächer")))
               .andExpect(content().string(containsString("0 abgelaufen (0 gesamt)")))
               .andExpect(content().string(containsString("Gespeicherte Nachrichten")))
               .andExpect(content().string(containsString("Protokollierte Transaktionen")))
               .andExpect(content().string(containsString("Transaktions-Zustellungsstatus:")))
               .andExpect(content().string(containsString("24-Stunden-Aktivität")))
               .andExpect(content().string(containsString("Heute")))
               .andExpect(content().string(containsString("Aktivität der letzten 24 Stunden")))
               .andExpect(content().string(containsString("Stündliche Auflösung")))
               .andExpect(content().string(containsString("7-Tage-Aktivität")))
               .andExpect(content().string(containsString("Wochenverlauf")))
               .andExpect(content().string(containsString("Aktivität der letzten 7 Tage")))
               .andExpect(content().string(containsString("Tägliche Auflösung")))
               .andExpect(content().string(containsString("Gesamt: 0")))
               .andExpect(content().string(containsString("Heutige (letzte 24 Stunden) Absenderdomains")))
               .andExpect(content().string(containsString("Absenderdomains der letzten 7 Tage")))
               .andExpect(content().string(containsString("Absenderdomain")))
               .andExpect(content().string(containsString("Verworfen")))
               .andExpect(content().string(containsString("Weitergeleitet")))
               .andExpect(content().string(containsString("Gespeichert")))
               .andExpect(content().string(containsString("Gesamt")));
    }

    /**
     * Verifies that the sender domain breakdown tables render domain rows when data is present
     * and render the localized empty state message when no data is recorded.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin statistics dashboard renders sender domain breakdown tables and empty state handling")
    public void testAdminStatisticsSenderDomainsHappyPathAndEmptyState() throws Exception
    {
        // 1. Special Case: Empty state when no statistics records exist
        mockMvc.perform(get("/admin/statistics")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("No sender domain activity recorded for this time window.")));

        // 2. Happy Path: Insert MailStatistics record for today
        final java.sql.Date today = java.sql.Date.valueOf(java.time.LocalDate.now());
        final MailStatisticsKey key = new MailStatisticsKey(today, 40, "alpha-sender.com", "xcmailr.test");
        final MailStatistics stat = new MailStatistics(key, 5, 10, 15);
        mailStatisticsRepository.save(stat);

        mockMvc.perform(get("/admin/statistics")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("alpha-sender.com")))
               .andExpect(content().string(containsString("30"))); // totalCount = 5 + 10 + 15 = 30
    }

    /**
     * Verifies that the admin statistics dashboard populates {@code weekSenderDomains} in the model
     * with daily breakdown information including non-null dates for sender domains.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin statistics dashboard model contains daily date breakdown in weekSenderDomains (Happy Path)")
    public void testAdminStatisticsWeeklySenderDomainsContainsDateInformation() throws Exception
    {
        final java.time.LocalDate todayLd = java.time.LocalDate.now();
        final java.sql.Date today = java.sql.Date.valueOf(todayLd);
        final java.sql.Date twoDaysAgo = java.sql.Date.valueOf(todayLd.minusDays(2));

        final MailStatisticsKey keyToday = new MailStatisticsKey(today, 10, "today-domain.com", "xcmailr.test");
        final MailStatistics statToday = new MailStatistics(keyToday, 1, 2, 3);
        mailStatisticsRepository.save(statToday);

        final MailStatisticsKey keyPast = new MailStatisticsKey(twoDaysAgo, 20, "past-domain.com", "xcmailr.test");
        final MailStatistics statPast = new MailStatistics(keyPast, 4, 5, 6);
        mailStatisticsRepository.save(statPast);

        mockMvc.perform(get("/admin/statistics")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(model().attributeExists("weekSenderDomains"))
               .andExpect(result -> {
                   final Object weekDomainsObj = result.getModelAndView().getModel().get("weekSenderDomains");
                   assertNotNull(weekDomainsObj);
                   assertTrue(weekDomainsObj instanceof List<?>);
                   @SuppressWarnings("unchecked")
                   final List<DomainStatisticsDto> weekDomains = (List<DomainStatisticsDto>) weekDomainsObj;
                   assertFalse(weekDomains.isEmpty());
                   assertEquals(2, weekDomains.size());

                   // Verify first record (today, ordered by date DESC)
                   final DomainStatisticsDto first = weekDomains.get(0);
                   assertEquals(today, first.date());
                   assertEquals("today-domain.com", first.domain());
                   assertEquals(1L, first.dropCount());
                   assertEquals(2L, first.forwardCount());
                   assertEquals(3L, first.storeCount());

                   // Verify second record (two days ago)
                   final DomainStatisticsDto second = weekDomains.get(1);
                   assertEquals(twoDaysAgo, second.date());
                   assertEquals("past-domain.com", second.domain());
                   assertEquals(4L, second.dropCount());
                   assertEquals(5L, second.forwardCount());
                   assertEquals(6L, second.storeCount());
               });
    }

    /**
     * Verifies that the admin statistics dashboard populates {@code chart24h} and {@code chart7d} model
     * attributes with metrics derived directly from {@code MAIL_STATISTICS}, ensuring parity with domain breakdown tables.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin statistics dashboard populates chart24h and chart7d model attributes from MAIL_STATISTICS (Happy Path)")
    public void testAdminStatisticsChartsPopulatedFromMailStatistics() throws Exception
    {
        final java.time.LocalDate todayLd = java.time.LocalDate.now();
        final java.time.LocalTime nowLt = java.time.LocalTime.now();
        final int currentQuarterHour = (nowLt.getHour() * 4) + (nowLt.getMinute() / 15);

        final java.sql.Date today = java.sql.Date.valueOf(todayLd);
        final java.sql.Date twoDaysAgo = java.sql.Date.valueOf(todayLd.minusDays(2));

        // Insert statistics records directly into MAIL_STATISTICS (and zero rows in mailtransactions)
        final MailStatisticsKey keyToday = new MailStatisticsKey(today, currentQuarterHour, "today-chart.com", "xcmailr.test");
        final MailStatistics statToday = new MailStatistics(keyToday, 100, 200, 300);
        mailStatisticsRepository.save(statToday);

        final MailStatisticsKey keyPast = new MailStatisticsKey(twoDaysAgo, 20, "past-chart.com", "xcmailr.test");
        final MailStatistics statPast = new MailStatistics(keyPast, 40, 50, 60);
        mailStatisticsRepository.save(statPast);

        mockMvc.perform(get("/admin/statistics")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(model().attributeExists("chart24h"))
               .andExpect(model().attributeExists("chart7d"))
               .andExpect(result -> {
                   final Object chart24hObj = result.getModelAndView().getModel().get("chart24h");
                   assertNotNull(chart24hObj);
                   assertTrue(chart24hObj instanceof ChartDataDto);
                   final ChartDataDto chart24h = (ChartDataDto) chart24hObj;
                   assertEquals(24, chart24h.labels().size());

                   final long sum24hForwarded = chart24h.forwarded().stream().mapToLong(Long::longValue).sum();
                   final long sum24hStored = chart24h.stored().stream().mapToLong(Long::longValue).sum();
                   final long sum24hDropped = chart24h.dropped().stream().mapToLong(Long::longValue).sum();
                   assertEquals(200L, sum24hForwarded);
                   assertEquals(300L, sum24hStored);
                   assertEquals(100L, sum24hDropped);

                   final Object chart7dObj = result.getModelAndView().getModel().get("chart7d");
                   assertNotNull(chart7dObj);
                   assertTrue(chart7dObj instanceof ChartDataDto);
                   final ChartDataDto chart7d = (ChartDataDto) chart7dObj;
                   assertEquals(7, chart7d.labels().size());

                   final long sum7dForwarded = chart7d.forwarded().stream().mapToLong(Long::longValue).sum();
                   final long sum7dStored = chart7d.stored().stream().mapToLong(Long::longValue).sum();
                   final long sum7dDropped = chart7d.dropped().stream().mapToLong(Long::longValue).sum();
                   assertEquals(250L, sum7dForwarded);
                   assertEquals(360L, sum7dStored);
                   assertEquals(140L, sum7dDropped);
               });
    }

    /**
     * Verifies that the admin statistics dashboard renders sortable table headers, chevron icons,
     * ISO date sort attributes, and localized column headers and sort tooltips in both English
     * and German locales.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin statistics dashboard renders sortable headers, chevrons, and localized date/domain columns (Happy Path)")
    public void testAdminStatisticsSenderDomainsSortableMarkupAndLocalization() throws Exception
    {
        // 1. Empty state verification (before records exist)
        mockMvc.perform(get("/admin/statistics")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("colspan=\"6\"")))
               .andExpect(content().string(containsString("colspan=\"5\"")));

        // 2. Insert statistics record for today
        final java.time.LocalDate todayLd = java.time.LocalDate.now();
        final java.sql.Date sampleDate = java.sql.Date.valueOf(todayLd);
        final int currentHourSlot = java.time.LocalTime.now().getHour() * 4;

        final MailStatisticsKey key = new MailStatisticsKey(sampleDate, currentHourSlot, "sortable-domain.com", "xcmailr.test");
        final MailStatistics stat = new MailStatistics(key, 10, 20, 30);
        mailStatisticsRepository.save(stat);

        // 3. English Locale Verification with data
        mockMvc.perform(get("/admin/statistics")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("table-sortable")))
               .andExpect(content().string(containsString("sortable-header")))
               .andExpect(content().string(containsString("data-sort-col=\"0\"")))
               .andExpect(content().string(containsString("data-sort-type=\"date\"")))
               .andExpect(content().string(containsString("data-sort-type=\"string\"")))
               .andExpect(content().string(containsString("data-sort-type=\"number\"")))
               .andExpect(content().string(containsString("bi-chevron-down sort-icon text-primary")))
               .andExpect(content().string(containsString("bi-chevron-expand sort-icon text-body-secondary opacity-50")))
               .andExpect(content().string(containsString("data-sort-value=\"" + todayLd.toString() + "\"")))
               .andExpect(content().string(containsString("Date / Day")))
               .andExpect(content().string(containsString("Sender Domain")))
               .andExpect(content().string(containsString("Sort by Date")))
               .andExpect(content().string(containsString("Sort by Sender Domain")))
               .andExpect(content().string(containsString("Sort by Total")));

        // 4. German Locale Verification with data
        mockMvc.perform(get("/admin/statistics")
               .with(user(adminDe.getMail()).roles("ADMIN"))
               .locale(java.util.Locale.GERMAN))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Datum / Tag")))
               .andExpect(content().string(containsString("Absenderdomain")))
               .andExpect(content().string(containsString("Nach Datum sortieren")))
               .andExpect(content().string(containsString("Nach Absenderdomain sortieren")))
               .andExpect(content().string(containsString("Nach Gesamtzahl sortieren")));
    }

    /**
     * Verifies that status 250 transactions render the Stored (250) badge in transaction logs.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin transactions audit log renders Stored (250) badge for local stored deliveries")
    public void testAdminTransactionsStored250Badge() throws Exception
    {
        final MailTransaction tx250 = new MailTransaction(250, "sender@test.com", "box@xcmailr.test", null);
        tx250.setTs(System.currentTimeMillis());
        transactionRepository.save(tx250);

        mockMvc.perform(get("/admin/transactions")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Stored (250)")));
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
        assertEquals("firstname", AdminWebController.resolveSortField("firstname"));
        assertEquals("firstname", AdminWebController.resolveSortField("forename"));
        assertEquals("lastname", AdminWebController.resolveSortField("lastname"));
        assertEquals("lastname", AdminWebController.resolveSortField("surname"));
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
        assertEquals("email", AdminWebController.resolveSortField("name")); // retired key safely falls back to email
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
        assertEquals("firstname", AdminWebController.resolveSortField("  FiRsTNaMe "));
        assertEquals("firstname", AdminWebController.resolveSortField("FoReNaMe"));
        assertEquals("lastname", AdminWebController.resolveSortField("  LaStNaMe "));
        assertEquals("lastname", AdminWebController.resolveSortField("SuRnAmE"));
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

        // 3. First Name ASC: forename ASC ignoreCase(), surname ASC ignoreCase(), tiebreaker mail ASC ignoreCase()
        final Sort firstNameAsc = AdminWebController.resolveSort("firstname", "asc");
        final List<Sort.Order> firstNameAscOrders = firstNameAsc.toList();
        assertEquals(3, firstNameAscOrders.size());
        assertEquals("forename", firstNameAscOrders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, firstNameAscOrders.get(0).getDirection());
        assertTrue(firstNameAscOrders.get(0).isIgnoreCase());
        assertEquals("surname", firstNameAscOrders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, firstNameAscOrders.get(1).getDirection());
        assertTrue(firstNameAscOrders.get(1).isIgnoreCase());
        assertEquals("mail", firstNameAscOrders.get(2).getProperty());
        assertEquals(Sort.Direction.ASC, firstNameAscOrders.get(2).getDirection());
        assertTrue(firstNameAscOrders.get(2).isIgnoreCase());

        // 4. First Name DESC: forename DESC ignoreCase(), surname DESC ignoreCase(), tiebreaker mail ASC ignoreCase()
        final Sort firstNameDesc = AdminWebController.resolveSort("forename", "desc");
        final List<Sort.Order> firstNameDescOrders = firstNameDesc.toList();
        assertEquals(3, firstNameDescOrders.size());
        assertEquals("forename", firstNameDescOrders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, firstNameDescOrders.get(0).getDirection());
        assertTrue(firstNameDescOrders.get(0).isIgnoreCase());
        assertEquals("surname", firstNameDescOrders.get(1).getProperty());
        assertEquals(Sort.Direction.DESC, firstNameDescOrders.get(1).getDirection());
        assertTrue(firstNameDescOrders.get(1).isIgnoreCase());
        assertEquals("mail", firstNameDescOrders.get(2).getProperty());
        assertEquals(Sort.Direction.ASC, firstNameDescOrders.get(2).getDirection());
        assertTrue(firstNameDescOrders.get(2).isIgnoreCase());

        // 5. Last Name ASC: surname ASC ignoreCase(), forename ASC ignoreCase(), tiebreaker mail ASC ignoreCase()
        final Sort lastNameAsc = AdminWebController.resolveSort("lastname", "asc");
        final List<Sort.Order> lastNameAscOrders = lastNameAsc.toList();
        assertEquals(3, lastNameAscOrders.size());
        assertEquals("surname", lastNameAscOrders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, lastNameAscOrders.get(0).getDirection());
        assertTrue(lastNameAscOrders.get(0).isIgnoreCase());
        assertEquals("forename", lastNameAscOrders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, lastNameAscOrders.get(1).getDirection());
        assertTrue(lastNameAscOrders.get(1).isIgnoreCase());
        assertEquals("mail", lastNameAscOrders.get(2).getProperty());
        assertEquals(Sort.Direction.ASC, lastNameAscOrders.get(2).getDirection());
        assertTrue(lastNameAscOrders.get(2).isIgnoreCase());

        // 6. Last Name DESC: surname DESC ignoreCase(), forename DESC ignoreCase(), tiebreaker mail ASC ignoreCase()
        final Sort lastNameDesc = AdminWebController.resolveSort("surname", "desc");
        final List<Sort.Order> lastNameDescOrders = lastNameDesc.toList();
        assertEquals(3, lastNameDescOrders.size());
        assertEquals("surname", lastNameDescOrders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, lastNameDescOrders.get(0).getDirection());
        assertTrue(lastNameDescOrders.get(0).isIgnoreCase());
        assertEquals("forename", lastNameDescOrders.get(1).getProperty());
        assertEquals(Sort.Direction.DESC, lastNameDescOrders.get(1).getDirection());
        assertTrue(lastNameDescOrders.get(1).isIgnoreCase());
        assertEquals("mail", lastNameDescOrders.get(2).getProperty());
        assertEquals(Sort.Direction.ASC, lastNameDescOrders.get(2).getDirection());
        assertTrue(lastNameDescOrders.get(2).isIgnoreCase());

        // 7. Status ASC: active DESC (active before inactive), tiebreaker mail ASC ignoreCase()
        final Sort statusAsc = AdminWebController.resolveSort("status", "asc");
        final List<Sort.Order> statusAscOrders = statusAsc.toList();
        assertEquals(2, statusAscOrders.size());
        assertEquals("active", statusAscOrders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, statusAscOrders.get(0).getDirection());
        assertEquals("mail", statusAscOrders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, statusAscOrders.get(1).getDirection());
        assertTrue(statusAscOrders.get(1).isIgnoreCase());

        // 8. Status DESC: active ASC (inactive before active), tiebreaker mail ASC ignoreCase()
        final Sort statusDesc = AdminWebController.resolveSort("status", "desc");
        final List<Sort.Order> statusDescOrders = statusDesc.toList();
        assertEquals(2, statusDescOrders.size());
        assertEquals("active", statusDescOrders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, statusDescOrders.get(0).getDirection());
        assertEquals("mail", statusDescOrders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, statusDescOrders.get(1).getDirection());
        assertTrue(statusDescOrders.get(1).isIgnoreCase());

        // 9. Role ASC: admin DESC (admin before user), tiebreaker mail ASC ignoreCase()
        final Sort roleAsc = AdminWebController.resolveSort("role", "asc");
        final List<Sort.Order> roleAscOrders = roleAsc.toList();
        assertEquals(2, roleAscOrders.size());
        assertEquals("admin", roleAscOrders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, roleAscOrders.get(0).getDirection());
        assertEquals("mail", roleAscOrders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, roleAscOrders.get(1).getDirection());
        assertTrue(roleAscOrders.get(1).isIgnoreCase());

        // 10. Role DESC: admin ASC (user before admin), tiebreaker mail ASC ignoreCase()
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
        final Sort mixedCaseFirst = AdminWebController.resolveSort("  FiRsTnAmE  ", "DESC");
        final List<Sort.Order> firstOrders = mixedCaseFirst.toList();
        assertEquals(3, firstOrders.size());
        assertEquals("forename", firstOrders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, firstOrders.get(0).getDirection());
        assertEquals("surname", firstOrders.get(1).getProperty());
        assertEquals(Sort.Direction.DESC, firstOrders.get(1).getDirection());
        assertEquals("mail", firstOrders.get(2).getProperty());
        assertEquals(Sort.Direction.ASC, firstOrders.get(2).getDirection());
        assertTrue(firstOrders.get(2).isIgnoreCase());

        final Sort mixedCaseLast = AdminWebController.resolveSort("  LaStNaMe  ", "DESC");
        final List<Sort.Order> lastOrders = mixedCaseLast.toList();
        assertEquals(3, lastOrders.size());
        assertEquals("surname", lastOrders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, lastOrders.get(0).getDirection());
        assertEquals("forename", lastOrders.get(1).getProperty());
        assertEquals(Sort.Direction.DESC, lastOrders.get(1).getDirection());
        assertEquals("mail", lastOrders.get(2).getProperty());
        assertEquals(Sort.Direction.ASC, lastOrders.get(2).getDirection());
        assertTrue(lastOrders.get(2).isIgnoreCase());
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
               .param("sort", "firstname")
               .param("dir", "desc")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(model().attribute("sort", "firstname"))
               .andExpect(model().attribute("dir", "desc"));

        mockMvc.perform(get("/admin/users")
               .header("HX-Request", "true")
               .param("sort", "lastname")
               .param("dir", "asc")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(model().attribute("sort", "lastname"))
               .andExpect(model().attribute("dir", "asc"));

        mockMvc.perform(get("/admin/users")
               .header("HX-Request", "true")
               .param("sort", "name")
               .param("dir", "desc")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(model().attribute("sort", "email"))
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

    /**
     * WebMvc Slice Test: Verifies that sort=firstname and sort=lastname render directional chevrons
     * on the respective First Name and Last Name column headers, while unwhitelisted sort keys safely
     * fall back to email sorting.
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Layer 1 Web: listUsers renders directional chevrons for sort=firstname and sort=lastname")
    public void testListUsers_SortingRenderedHeaders() throws Exception
    {
        // 1. First Name ASC
        mockMvc.perform(get("/admin/users")
               .header("HX-Request", "true")
               .param("sort", "firstname")
               .param("dir", "asc")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(model().attribute("sort", "firstname"))
               .andExpect(model().attribute("dir", "asc"))
               .andExpect(content().string(containsString("sort=firstname")))
               .andExpect(content().string(containsString("bi-chevron-up sort-icon text-primary")))
               .andExpect(content().string(containsString("First Name")))
               .andExpect(content().string(containsString("Last Name")));

        // 2. Last Name DESC
        mockMvc.perform(get("/admin/users")
               .header("HX-Request", "true")
               .param("sort", "lastname")
               .param("dir", "desc")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(model().attribute("sort", "lastname"))
               .andExpect(model().attribute("dir", "desc"))
               .andExpect(content().string(containsString("sort=lastname")))
               .andExpect(content().string(containsString("bi-chevron-down sort-icon text-primary")));

        // 3. Retired sort=name falls back safely to email ascending/descending
        mockMvc.perform(get("/admin/users")
               .header("HX-Request", "true")
               .param("sort", "name")
               .param("dir", "asc")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(model().attribute("sort", "email"))
               .andExpect(model().attribute("dir", "asc"));

        // 4. Empty state colspan check
        mockMvc.perform(get("/admin/users")
               .header("HX-Request", "true")
               .param("search", "nonexistent-user-query-12345")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("<td colspan=\"6\"")));
    }

    /**
     * Verifies that the admin statistics dashboard populates the 24-hour sender domain table
     * with data aggregated across the sliding 24-hour window (including yesterday afternoon and today).
     *
     * @throws Exception if request execution fails
     */
    @Test
    @DisplayName("Admin statistics dashboard populates sliding 24-hour sender domains table across date boundary")
    public void testAdminStatisticsSliding24HourSenderDomains() throws Exception
    {
        final java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
        final java.sql.Date today = java.sql.Date.valueOf(now.toLocalDate());
        final java.sql.Date yesterday = java.sql.Date.valueOf(now.toLocalDate().minusDays(1));
        final int currentHour = now.getHour();
        final int startQuarterHour = (currentHour + 1) * 4;

        // Domain 1: traffic yesterday afternoon (within sliding 24h window if startQuarterHour <= 95)
        if (startQuarterHour <= 95)
        {
            final MailStatisticsKey yestKey = new MailStatisticsKey(yesterday, startQuarterHour, "yesterday-sender.com", "xcmailr.test");
            mailStatisticsRepository.save(new MailStatistics(yestKey, 10, 20, 30));
        }

        // Domain 2: traffic today at current hour (within sliding 24h window)
        final int todayQuarter = currentHour * 4;
        final MailStatisticsKey todayKey = new MailStatisticsKey(today, todayQuarter, "today-sender.com", "xcmailr.test");
        mailStatisticsRepository.save(new MailStatistics(todayKey, 5, 15, 20));

        mockMvc.perform(get("/admin/statistics")
               .with(user(admin.getMail()).roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(model().attributeExists("todaySenderDomains"))
               .andExpect(result -> {
                   final Object domainStatsObj = result.getModelAndView().getModel().get("todaySenderDomains");
                   assertNotNull(domainStatsObj);
                   assertTrue(domainStatsObj instanceof List<?>);
                   @SuppressWarnings("unchecked")
                   final List<DomainStatisticsDto> domainStats = (List<DomainStatisticsDto>) domainStatsObj;
                   assertFalse(domainStats.isEmpty());
                   final boolean hasTodayDomain = domainStats.stream()
                       .anyMatch(d -> "today-sender.com".equals(d.domain()));
                   assertTrue(hasTodayDomain, "today-sender.com must be present in todaySenderDomains");
                   if (startQuarterHour <= 95)
                   {
                       final boolean hasYestDomain = domainStats.stream()
                           .anyMatch(d -> "yesterday-sender.com".equals(d.domain()));
                       assertTrue(hasYestDomain, "yesterday-sender.com must be present in sliding 24h window");
                   }
               });
    }
}
