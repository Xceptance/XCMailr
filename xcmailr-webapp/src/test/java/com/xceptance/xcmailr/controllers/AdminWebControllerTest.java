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
}
