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
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.Domain;
import models.MailTransaction;
import models.User;

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
    @DisplayName("Admin can view system statistics dashboard")
    public void testStatisticsDashboard() throws Exception
    {
        mockMvc.perform(get("/admin/statistics")
               .with(user("admin@xcmailr.test").roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/statistics"))
               .andExpect(model().attributeExists("totalUsers"))
               .andExpect(model().attributeExists("totalMailboxes"))
               .andExpect(model().attributeExists("totalMails"))
               .andExpect(model().attributeExists("totalTransactions"));
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
               .andExpect(content().string(containsString("Domain Name")));
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
               .andExpect(content().string(containsString("Domain-Name")));
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
}
