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
}
