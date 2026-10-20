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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.repositories.UserRepository;

import models.User;

/**
 * Integration test verifying backward compatibility for existing users in the production database.
 * <p>
 * In accordance with OWASP ASVS v4.0 and NIST SP 800-63B guidance, new password complexity and length
 * rules (minimum 10 characters, context exclusions, repetition limits) are enforced strictly on credential
 * creation and update flows (Registration, Password Reset, Profile Password Change). Existing accounts
 * with legacy passwords (e.g. 4-8 characters, repetitive characters, or containing user identity terms)
 * MUST remain fully valid and authenticatable during form login so that modernization does not lock out
 * existing production users.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class LegacyPasswordAuthenticationIntegrationTest
{
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    /**
     * Initializes MockMvc with Spring Security filters before each test execution.
     */
    @BeforeEach
    public void setUp()
    {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        userRepository.deleteAll();
    }

    /**
     * Verifies that a legacy user with a 4-character password (e.g., "1234") hashed with legacy jbcrypt
     * authenticates successfully via {@code POST /login} and redirects to the home page.
     *
     * @throws Exception if an error occurs during HTTP dispatch
     */
    @Test
    @DisplayName("Existing user with 4-character legacy password authenticates successfully")
    public void testLegacyShortPasswordAuthenticationSucceeds() throws Exception
    {
        final String rawPassword = "1234";
        // User constructor invokes hashPasswd(rawPassword) using legacy BCrypt.hashpw(...)
        final User legacyUser = new User("Alice", "Smith", "alice.legacy@xcmailr.test", rawPassword, "en");
        legacyUser.setActive(true);
        userRepository.save(legacyUser);

        mockMvc.perform(post("/login")
               .with(csrf())
               .param("mail", "alice.legacy@xcmailr.test")
               .param("password", rawPassword))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/"))
               .andExpect(authenticated().withUsername("alice.legacy@xcmailr.test"));
    }

    /**
     * Verifies that a legacy user whose password contains repetitive characters (e.g. "aaaaaa")
     * authenticates successfully, proving repetition checks do not block login.
     *
     * @throws Exception if an error occurs during HTTP dispatch
     */
    @Test
    @DisplayName("Existing user with repetitive legacy password authenticates successfully")
    public void testLegacyRepetitivePasswordAuthenticationSucceeds() throws Exception
    {
        final String rawPassword = "aaaaaa";
        final User legacyUser = new User("Bob", "Miller", "bob.repeater@xcmailr.test", rawPassword, "en");
        legacyUser.setActive(true);
        userRepository.save(legacyUser);

        mockMvc.perform(post("/login")
               .with(csrf())
               .param("mail", "bob.repeater@xcmailr.test")
               .param("password", rawPassword))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/"))
               .andExpect(authenticated().withUsername("bob.repeater@xcmailr.test"));
    }

    /**
     * Verifies that a legacy user whose password matches their own name or email local part (e.g. "carol")
     * authenticates successfully, proving context exclusion checks do not block login.
     *
     * @throws Exception if an error occurs during HTTP dispatch
     */
    @Test
    @DisplayName("Existing user with contextual name-based legacy password authenticates successfully")
    public void testLegacyContextualPasswordAuthenticationSucceeds() throws Exception
    {
        final String rawPassword = "carol";
        final User legacyUser = new User("Carol", "Taylor", "carol@xcmailr.test", rawPassword, "en");
        legacyUser.setActive(true);
        userRepository.save(legacyUser);

        mockMvc.perform(post("/login")
               .with(csrf())
               .param("mail", "carol@xcmailr.test")
               .param("password", rawPassword))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/"))
               .andExpect(authenticated().withUsername("carol@xcmailr.test"));
    }

    /**
     * Verifies that supplying an incorrect password for a legacy account is rejected with a redirect
     * to {@code /login?error}.
     *
     * @throws Exception if an error occurs during HTTP dispatch
     */
    @Test
    @DisplayName("Legacy user with incorrect password fails authentication")
    public void testLegacyUserWrongPasswordFails() throws Exception
    {
        final String rawPassword = "pass";
        final User legacyUser = new User("Dave", "Wilson", "dave@xcmailr.test", rawPassword, "en");
        legacyUser.setActive(true);
        userRepository.save(legacyUser);

        mockMvc.perform(post("/login")
               .with(csrf())
               .param("mail", "dave@xcmailr.test")
               .param("password", "wrongPass"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?error"))
               .andExpect(unauthenticated());
    }

    /**
     * Verifies that an unconfirmed legacy user attempting login is rejected with a redirect
     * to {@code /login?unconfirmed}.
     *
     * @throws Exception if an error occurs during HTTP dispatch
     */
    @Test
    @DisplayName("Unconfirmed legacy user fails authentication with unconfirmed redirect")
    public void testLegacyUserUnconfirmedFails() throws Exception
    {
        final String rawPassword = "mypassword";
        final User legacyUser = new User("Eve", "Brown", "eve@xcmailr.test", rawPassword, "en");
        legacyUser.setActive(false);
        userRepository.save(legacyUser);

        mockMvc.perform(post("/login")
               .with(csrf())
               .param("mail", "eve@xcmailr.test")
               .param("password", rawPassword))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?unconfirmed"))
               .andExpect(unauthenticated());
    }
}
