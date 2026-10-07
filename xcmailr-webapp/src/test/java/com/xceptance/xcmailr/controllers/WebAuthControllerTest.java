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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.XcmailrApplication;
import com.xceptance.xcmailr.config.XcmailrProperties;
import com.xceptance.xcmailr.repositories.UserRepository;

import models.User;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Tests for {@link WebAuthController} verifying user registration, email confirmation,
 * forgot password, and reset password flows.
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public class WebAuthControllerTest
{
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private XcmailrProperties properties;

    @BeforeEach
    public void setup()
    {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /login should display login view")
    public void testLoginPage() throws Exception
    {
        mockMvc.perform(get("/login"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("GET /register should display registration view")
    public void testRegisterPage() throws Exception
    {
        mockMvc.perform(get("/register"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"));
    }

    @Test
    @DisplayName("POST /register should create inactive user with confirmation token")
    public void testRegisterSuccess() throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Alice")
               .param("surname", "Wonderland")
               .param("mail", "alice@xcmailr.test")
               .param("password", "SecureSecret123!")
               .param("confirmPassword", "SecureSecret123!"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?registered"));

        final User user = userRepository.findByMailIgnoreCase("alice@xcmailr.test").orElse(null);
        assertNotNull(user, "Registered user should exist");
        assertFalse(user.isActive(), "User should be inactive before email confirmation");
        assertNotNull(user.getConfirmation(), "Confirmation token must be set");
        assertTrue(passwordEncoder.matches("SecureSecret123!", user.getPasswd()), "Password must be encoded");
    }

    @Test
    @DisplayName("POST /register with mismatched passwords should return error")
    public void testRegisterMismatchedPasswords() throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Bob")
               .param("surname", "Builder")
               .param("mail", "bob@xcmailr.test")
               .param("password", "Password123")
               .param("confirmPassword", "DifferentPassword123"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("GET /confirm/{token} should activate user")
    public void testConfirmAccount() throws Exception
    {
        final User user = new User("Carol", "Danvers", "carol@xcmailr.test", passwordEncoder.encode("secret"), "en");
        user.setActive(false);
        user.setConfirmation("valid-confirm-token-xyz");
        user.setTs_confirm(System.currentTimeMillis() + 86_400_000L);
        userRepository.save(user);

        mockMvc.perform(get("/confirm/valid-confirm-token-xyz"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?confirmed"));

        final User activated = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(activated.isActive(), "User should now be active");
    }

    @Test
    @DisplayName("POST /forgot-password should generate reset token for existing user")
    public void testForgotPassword() throws Exception
    {
        final User user = new User("David", "Copperfield", "david@xcmailr.test", passwordEncoder.encode("secret"), "en");
        user.setActive(true);
        userRepository.save(user);

        mockMvc.perform(post("/forgot-password")
               .with(csrf())
               .param("mail", "david@xcmailr.test"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?resetRequested"));

        final User updated = userRepository.findById(user.getId()).orElseThrow();
        assertNotNull(updated.getConfirmation(), "Confirmation token for reset must be set");
    }

    @Test
    @DisplayName("POST /pwreset/{token} should update password with valid token")
    public void testResetPassword() throws Exception
    {
        final User user = new User("Eve", "Polastri", "eve@xcmailr.test", passwordEncoder.encode("oldPassword"), "en");
        user.setActive(true);
        user.setConfirmation("reset-token-abc");
        user.setTs_confirm(System.currentTimeMillis() + 86_400_000L);
        userRepository.save(user);

        mockMvc.perform(post("/pwreset/reset-token-abc")
               .with(csrf())
               .param("password", "BrandNewPassword456!")
               .param("confirmPassword", "BrandNewPassword456!"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?passwordReset"));

        final User updated = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("BrandNewPassword456!", updated.getPasswd()), "Password should be updated");
    }

    @Test
    @DisplayName("POST /login with valid credentials should authenticate and redirect to /")
    public void testFormLoginSuccess() throws Exception
    {
        final User user = new User("Frank", "Castle", "frank@xcmailr.test", "ValidSecret123", "en");
        user.setActive(true);
        userRepository.save(user);

        mockMvc.perform(post("/login")
               .with(csrf())
               .param("mail", "frank@xcmailr.test")
               .param("password", "ValidSecret123"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("POST /login with invalid credentials should redirect to /login?error")
    public void testFormLoginBadCredentials() throws Exception
    {
        final User user = new User("Frank", "Castle", "frank@xcmailr.test", "ValidSecret123", "en");
        user.setActive(true);
        userRepository.save(user);

        mockMvc.perform(post("/login")
               .with(csrf())
               .param("mail", "frank@xcmailr.test")
               .param("password", "WrongPassword"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("Roundtrip: Email confirmation flow (register -> login blocked with ?unconfirmed -> confirm token -> login succeeds)")
    public void testEmailConfirmationRoundtrip() throws Exception
    {
        properties.getApp().setRequireConfirmation(true);

        // Step 1: Self-service registration
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Hans")
               .param("surname", "Kanns")
               .param("mail", "hans.kanns@varmail.de")
               .param("password", "123456")
               .param("confirmPassword", "123456"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?registered"));

        final User registeredUser = userRepository.findByMailIgnoreCase("hans.kanns@varmail.de").orElse(null);
        assertNotNull(registeredUser, "User should be registered");
        assertFalse(registeredUser.isActive(), "User must be inactive before confirmation");
        assertNotNull(registeredUser.getConfirmation(), "Confirmation token must be generated");
        final String token = registeredUser.getConfirmation();

        // Step 2: Attempt login prior to activation with valid password -> redirected to /login?unconfirmed
        mockMvc.perform(post("/login")
               .with(csrf())
               .param("mail", "hans.kanns@varmail.de")
               .param("password", "123456"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?unconfirmed"));

        // Step 3: Activate account via confirmation token link
        mockMvc.perform(get("/confirm/" + token))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?confirmed"));

        final User activatedUser = userRepository.findByMailIgnoreCase("hans.kanns@varmail.de").orElse(null);
        assertNotNull(activatedUser, "User should exist");
        assertTrue(activatedUser.isActive(), "User must be active after confirmation");
        assertNull(activatedUser.getConfirmation(), "Confirmation token must be cleared after use");

        // Step 4: Login with activated credentials -> successfully authenticated and redirected to /
        mockMvc.perform(post("/login")
               .with(csrf())
               .param("mail", "hans.kanns@varmail.de")
               .param("password", "123456"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("Roundtrip: Auto-activation flow when requireConfirmation=false (register -> login?ready -> immediate login succeeds)")
    public void testAutoActivationRoundtrip() throws Exception
    {
        final boolean previousSetting = properties.getApp().isRequireConfirmation();
        try
        {
            properties.getApp().setRequireConfirmation(false);

            // Step 1: Self-service registration with auto-activation
            mockMvc.perform(post("/register")
                   .with(csrf())
                   .param("forename", "Auto")
                   .param("surname", "User")
                   .param("mail", "auto.active@varmail.de")
                   .param("password", "SecureSecret123!")
                   .param("confirmPassword", "SecureSecret123!"))
                   .andExpect(status().is3xxRedirection())
                   .andExpect(redirectedUrl("/login?ready"));

            final User registeredUser = userRepository.findByMailIgnoreCase("auto.active@varmail.de").orElse(null);
            assertNotNull(registeredUser, "User should be registered");
            assertTrue(registeredUser.isActive(), "User must be active immediately when requireConfirmation is false");
            assertNull(registeredUser.getConfirmation(), "Confirmation token must not be generated");

            // Step 2: Immediate login without confirmation link -> succeeds and redirects to /
            mockMvc.perform(post("/login")
                   .with(csrf())
                   .param("mail", "auto.active@varmail.de")
                   .param("password", "SecureSecret123!"))
                   .andExpect(status().is3xxRedirection())
                   .andExpect(redirectedUrl("/"));
        }
        finally
        {
            properties.getApp().setRequireConfirmation(previousSetting);
        }
    }

    @Test
    @DisplayName("Anti-enumeration: Login with wrong password on unconfirmed account must redirect to /login?error")
    public void testUnconfirmedLoginWithWrongPasswordAntiEnumeration() throws Exception
    {
        final User unconfirmed = new User("Target", "Victim", "victim@xcmailr.test", "RealSecretPassword123", "en");
        unconfirmed.setActive(false);
        unconfirmed.setConfirmation("target-token-uuid");
        unconfirmed.setTs_confirm(System.currentTimeMillis() + 3600_000L);
        userRepository.save(unconfirmed);

        // Attacker attempts login with wrong password
        mockMvc.perform(post("/login")
               .with(csrf())
               .param("mail", "victim@xcmailr.test")
               .param("password", "GuessedWrongPassword"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?error")); // Must NOT redirect to /login?unconfirmed (CWE-204)
    }

    @Test
    @DisplayName("POST /register with password shorter than 6 characters should reject")
    public void testRegisterShortPassword() throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Short")
               .param("surname", "Pass")
               .param("mail", "shortpass@xcmailr.test")
               .param("password", "12345")
               .param("confirmPassword", "12345"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attribute("errorMessage", "Password must be at least 6 characters long."));
    }

    @Test
    @DisplayName("POST /register with duplicate email case-insensitively should reject")
    public void testRegisterDuplicateEmailCaseInsensitive() throws Exception
    {
        final User existing = new User("Original", "User", "duplicate@xcmailr.test", "Password123", "en");
        existing.setActive(true);
        userRepository.save(existing);

        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Another")
               .param("surname", "User")
               .param("mail", "DUPLICATE@XCMAILR.TEST")
               .param("password", "DifferentPass123")
               .param("confirmPassword", "DifferentPass123"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attribute("errorMessage", "An account with this email address already exists."));
    }

    @Test
    @DisplayName("GET /confirm/{token} with non-existent token should redirect to /login?invalidToken")
    public void testConfirmAccountInvalidToken() throws Exception
    {
        mockMvc.perform(get("/confirm/non-existent-token-xyz"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?invalidToken"));
    }

    @Test
    @DisplayName("GET /confirm/{token} with expired token should redirect to /login?expiredToken")
    public void testConfirmAccountExpiredToken() throws Exception
    {
        final User user = new User("Expired", "User", "expired@xcmailr.test", "secretPassword123", "en");
        user.setActive(false);
        user.setConfirmation("expired-token-1234");
        user.setTs_confirm(System.currentTimeMillis() - 60_000L); // expired 1 minute ago
        userRepository.save(user);

        mockMvc.perform(get("/confirm/expired-token-1234"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?expiredToken"));

        final User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertFalse(reloaded.isActive(), "User must remain inactive after expired confirmation attempt");
    }

    @Test
    @DisplayName("POST /login with upper-case email should authenticate active user")
    public void testFormLoginCaseInsensitiveEmail() throws Exception
    {
        final User user = new User("Case", "Test", "case.test@xcmailr.test", "Secret123", "en");
        user.setActive(true);
        userRepository.save(user);

        mockMvc.perform(post("/login")
               .with(csrf())
               .param("mail", "CASE.TEST@XCMAILR.TEST")
               .param("password", "Secret123"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/"));
    }
}
