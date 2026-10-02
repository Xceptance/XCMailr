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
package com.xceptance.xcmailr.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.User;

/**
 * Integration slice test verifying role-based navigation visibility, OWASP Broken Access Control
 * (A01:2021) server-side enforcement, offcanvas mobile navigation structure, and dark mode controls.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WebNavigationSecurityTest
{
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

    private MockMvc mockMvc;
    private User admin;
    private User standardUser;

    /**
     * Initializes MockMvc with Spring Security filters and seeds database users.
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

        // Seed administrator user
        final User newAdmin = new User("Admin", "Owner", "admin@xcmailr.test", "AdminPass123", "en");
        newAdmin.setActive(true);
        newAdmin.setAdmin(true);
        admin = userRepository.save(newAdmin);

        // Seed standard authenticated user
        final User newUser = new User("Standard", "User", "standard@xcmailr.test", "UserPass123", "en");
        newUser.setActive(true);
        newUser.setAdmin(false);
        standardUser = userRepository.save(newUser);
    }

    // =========================================================================
    // Happy Path Navigation Rendering Tests
    // =========================================================================

    /**
     * Verifies that anonymous visitors see only public navigation links (login/register)
     * and do not see protected mailbox, admin, or user profile menus.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Anonymous visitor sees public navigation links and no protected areas")
    public void testAnonymousNavigationRendering() throws Exception
    {
        mockMvc.perform(get("/login"))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Login")))
               .andExpect(content().string(containsString("Register")))
               .andExpect(content().string(not(containsString("Administration"))))
               .andExpect(content().string(not(containsString("href=\"/admin/users\""))))
               .andExpect(content().string(not(containsString("href=\"/admin/domains\""))))
               .andExpect(content().string(not(containsString("href=\"/profile\""))))
               .andExpect(content().string(not(containsString("Log Out"))));
    }

    /**
     * Verifies that authenticated standard users see personal mailbox and profile controls,
     * but the administration menu and all administrative endpoint links are strictly omitted.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Standard authenticated user sees mailboxes and profile, but not admin menus")
    public void testStandardUserNavigationRendering() throws Exception
    {
        final UserPrincipal principal = new UserPrincipal(standardUser);

        mockMvc.perform(get("/").with(user(principal)))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Mailboxes")))
               .andExpect(content().string(containsString("href=\"/profile\"")))
               .andExpect(content().string(containsString("Log Out")))
               .andExpect(content().string(not(containsString("Administration"))))
               .andExpect(content().string(not(containsString("href=\"/admin/users\""))))
               .andExpect(content().string(not(containsString("href=\"/admin/domains\""))))
               .andExpect(content().string(not(containsString("href=\"/admin/transactions\""))))
               .andExpect(content().string(not(containsString("href=\"/admin/statistics\""))));
    }

    /**
     * Verifies that authenticated administrators see mailboxes, profile, and the full
     * administrative dropdown containing all admin section links.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Administrator sees mailboxes, profile, and complete administration menu")
    public void testAdminUserNavigationRendering() throws Exception
    {
        final UserPrincipal principal = new UserPrincipal(admin);

        mockMvc.perform(get("/").with(user(principal)))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("Mailboxes")))
               .andExpect(content().string(containsString("Administration")))
               .andExpect(content().string(containsString("href=\"/admin/users\"")))
               .andExpect(content().string(containsString("href=\"/admin/domains\"")))
               .andExpect(content().string(containsString("href=\"/admin/transactions\"")))
               .andExpect(content().string(containsString("href=\"/admin/statistics\"")))
               .andExpect(content().string(containsString("href=\"/profile\"")));
    }

    // =========================================================================
    // OWASP Broken Access Control (A01:2021) Security Tests
    // =========================================================================

    /**
     * Verifies that authenticated non-admin users attempting direct URL access to administrative
     * endpoints are rejected with HTTP 403 Forbidden.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Non-admin user directly requesting administrative endpoints receives HTTP 403 Forbidden")
    public void testRbacDeniesRegularUserAccessToAdminEndpoints() throws Exception
    {
        final UserPrincipal principal = new UserPrincipal(standardUser);

        // Test all four primary administrative management subpaths
        mockMvc.perform(get("/admin/users").with(user(principal)))
               .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/domains").with(user(principal)))
               .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/transactions").with(user(principal)))
               .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/statistics").with(user(principal)))
               .andExpect(status().isForbidden());
    }

    /**
     * Verifies that anonymous visitors attempting direct URL access to administrative endpoints
     * are redirected to the login page (HTTP 302 Found).
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Anonymous requester directly accessing admin endpoints is redirected to login")
    public void testAnonymousAccessToAdminEndpointsRedirectsToLogin() throws Exception
    {
        mockMvc.perform(get("/admin/users"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrlPattern("/**/login"));

        mockMvc.perform(get("/admin/domains"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrlPattern("/**/login"));
    }

    /**
     * Verifies that state-altering administrative POST requests without a valid CSRF token
     * are rejected with HTTP 403 Forbidden in accordance with OWASP CSRF defenses.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("State-altering administrative request without CSRF token is rejected with HTTP 403 Forbidden")
    public void testCsrfTokenRequiredForAdminAction() throws Exception
    {
        final UserPrincipal principal = new UserPrincipal(admin);

        // Attempt admin action without csrf() token helper
        mockMvc.perform(post("/admin/users/" + standardUser.getId() + "/toggle-active")
               .with(user(principal)))
               .andExpect(status().isForbidden());

        // Same request with valid CSRF token succeeds or processes redirect
        mockMvc.perform(post("/admin/users/" + standardUser.getId() + "/toggle-active")
               .with(user(principal))
               .with(csrf()))
               .andExpect(status().is3xxRedirection());
    }

    // =========================================================================
    // Responsive Layout & Theme Controls Verification
    // =========================================================================

    /**
     * Verifies that the rendered layout includes the Bootstrap 5 Offcanvas drawer structure,
     * theme mode switcher button, and corporate Xceptance theme stylesheet.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Rendered page contains responsive offcanvas drawer, theme toggle, and theme CSS")
    public void testResponsiveLayoutAndThemeMarkup() throws Exception
    {
        final UserPrincipal principal = new UserPrincipal(standardUser);

        mockMvc.perform(get("/").with(user(principal)))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("id=\"navbarOffcanvas\"")))
               .andExpect(content().string(containsString("class=\"offcanvas offcanvas-end offcanvas-navbar")))
               .andExpect(content().string(containsString("id=\"themeToggle\"")))
               .andExpect(content().string(containsString("xceptance-theme.css")))
               .andExpect(content().string(containsString("bootstrap-icons.min.css")));
    }
}

