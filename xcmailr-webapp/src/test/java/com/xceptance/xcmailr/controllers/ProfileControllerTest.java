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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import com.xceptance.xcmailr.security.PasswordValidator;

import models.User;

/**
 * Tests for {@link ProfileController} verifying profile display, profile updating,
 * password modification, and API token generation/revocation.
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public class ProfileControllerTest
{
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordValidator passwordValidator;

    private User testUser;

    @BeforeEach
    public void setup()
    {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        userRepository.deleteAll();

        testUser = new User("John", "Doe", "john.doe@xcmailr.test", "CurrentSecret123", "en");
        testUser.setActive(true);
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("GET /profile should display profile view for authenticated user with password policy guidance")
    public void testGetProfileAuthenticated() throws Exception
    {
        mockMvc.perform(get("/profile")
               .with(user("john.doe@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attributeExists("user"))
               .andExpect(content().string(containsString("minlength=\"10\"")))
               .andExpect(content().string(containsString("maxlength=\"72\"")))
               .andExpect(content().string(containsString("At least 10 characters (up to 72)")))
               .andExpect(content().string(containsString("Password requirements:")));
    }

    @Test
    @DisplayName("GET /profile unauthenticated should redirect to /login")
    public void testGetProfileUnauthenticated() throws Exception
    {
        mockMvc.perform(get("/profile"))
               .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("POST /profile/update should update names and language")
    public void testUpdateProfile() throws Exception
    {
        mockMvc.perform(post("/profile/update")
               .with(user("john.doe@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("forename", "Jonathan")
               .param("surname", "Doerr")
               .param("language", "de"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/profile?updated"));

        final User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("Jonathan", updated.getForename());
        assertEquals("Doerr", updated.getSurname());
        assertEquals("de", updated.getLanguage());
    }

    @Test
    @DisplayName("POST /profile/change-password with correct old password should update hash")
    public void testChangePasswordSuccess() throws Exception
    {
        mockMvc.perform(post("/profile/change-password")
               .with(user("john.doe@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("currentPassword", "CurrentSecret123")
               .param("newPassword", "NewSecurePassword456!")
               .param("confirmNewPassword", "NewSecurePassword456!"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/profile?passwordChanged"));

        final User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NewSecurePassword456!", updated.getPasswd()));
    }

    @Test
    @DisplayName("POST /profile/change-password with invalid current password should return error")
    public void testChangePasswordWrongCurrent() throws Exception
    {
        mockMvc.perform(post("/profile/change-password")
               .with(user("john.doe@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("currentPassword", "WrongCurrentPassword")
               .param("newPassword", "NewSecurePassword456!")
               .param("confirmNewPassword", "NewSecurePassword456!"))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attribute("errorMessage", "Current password does not match."));
    }

    @Test
    @DisplayName("POST /profile/change-password with mismatched new passwords should return error")
    public void testChangePasswordMismatchedPasswords() throws Exception
    {
        mockMvc.perform(post("/profile/change-password")
               .with(user("john.doe@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("currentPassword", "CurrentSecret123")
               .param("newPassword", "NewSecurePassword456!")
               .param("confirmNewPassword", "DifferentPassword456!"))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attribute("errorMessage", "New passwords do not match."));
    }

    @Test
    @DisplayName("POST /profile/change-password with new password shorter than 10 chars should reject")
    public void testChangePasswordTooShort() throws Exception
    {
        mockMvc.perform(post("/profile/change-password")
               .with(user("john.doe@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("currentPassword", "CurrentSecret123")
               .param("newPassword", "Short9!")
               .param("confirmNewPassword", "Short9!"))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attribute("errorMessage", "Password must be at least 10 characters long."));
    }

    @Test
    @DisplayName("POST /profile/change-password with valid 72-character password should succeed and encode without error")
    public void testChangePasswordMaxBoundary72Chars() throws Exception
    {
        final String boundary72 = "SecureBoundaryPassword72-" + "a".repeat(40) + "-End72!";
        mockMvc.perform(post("/profile/change-password")
               .with(user("john.doe@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("currentPassword", "CurrentSecret123")
               .param("newPassword", boundary72)
               .param("confirmNewPassword", boundary72))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/profile?passwordChanged"));

        final User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches(boundary72, updated.getPasswd()), "72-char changed password must be encoded and match");
    }

    @Test
    @DisplayName("POST /profile/change-password with new password exceeding 72 chars should reject")
    public void testChangePasswordExceeding72Chars() throws Exception
    {
        final String password73 = "B".repeat(73);
        mockMvc.perform(post("/profile/change-password")
               .with(user("john.doe@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("currentPassword", "CurrentSecret123")
               .param("newPassword", password73)
               .param("confirmNewPassword", password73))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attribute("errorMessage", "Password cannot exceed 72 characters."));
    }

    @Test
    @DisplayName("POST /profile/change-password with 128-character password should reject cleanly without HTTP 500")
    public void testChangePassword128Chars() throws Exception
    {
        final String password128 = "RfYfJm8wVe3jJzOVTwsL0ibnuyWmJthLPhNYe42pJmvdxL4mAY59AofqPCnTp0RDQt1rwI0hYOBrjueAGlYm1Nnsmc98vQH19mt93jyY5zJzU1MRAieuJVdUeu5X2cmN";
        mockMvc.perform(post("/profile/change-password")
               .with(user("john.doe@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("currentPassword", "CurrentSecret123")
               .param("newPassword", password128)
               .param("confirmNewPassword", password128))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attribute("errorMessage", "Password cannot exceed 72 characters."));
    }

    @Test
    @DisplayName("POST /profile/change-password with 129-character password should reject cleanly without HTTP 500")
    public void testChangePassword129Chars() throws Exception
    {
        final String password129 = "RfYfJm8wVe3jJzOVTwsL0ibnuyWmJthLPhNYe42pJmvdxL4mAY59AofqPCnTp0RDQt1rwI0hYOBrjueAGlYm1Nnsmc98vQH19mt93jyY5zJzU1MRAieuJVdUeu5X2cmNQ";
        mockMvc.perform(post("/profile/change-password")
               .with(user("john.doe@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("currentPassword", "CurrentSecret123")
               .param("newPassword", password129)
               .param("confirmNewPassword", password129))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attribute("errorMessage", "Password cannot exceed 72 characters."));
    }

    @Test
    @DisplayName("POST /profile/change-password with multibyte password exceeding 72 bytes should reject")
    public void testChangePasswordMultibyteByteOverflow() throws Exception
    {
        final String emojiPassword = "🚀".repeat(25); // 25 characters, 100 UTF-8 bytes
        mockMvc.perform(post("/profile/change-password")
               .with(user("john.doe@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("currentPassword", "CurrentSecret123")
               .param("newPassword", emojiPassword)
               .param("confirmNewPassword", emojiPassword))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attribute("errorMessage", "Password cannot exceed 72 bytes (special or non-Latin characters use multiple bytes)."));
    }

    @Test
    @DisplayName("POST /profile/change-password should catch unexpected IllegalArgumentException from encoder and return error view")
    public void testChangePasswordDefensiveEncoderCatch() throws Exception
    {
        final PasswordEncoder failingEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
        org.mockito.Mockito.when(failingEncoder.matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                           .thenReturn(true);
        org.mockito.Mockito.when(failingEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                           .thenThrow(new IllegalArgumentException("password cannot be more than 72 bytes"));

        final ProfileController defensiveController = new ProfileController(userRepository, failingEncoder, passwordValidator);
        final MockMvc defensiveMockMvc = MockMvcBuilders.standaloneSetup(defensiveController).build();

        defensiveMockMvc.perform(post("/profile/change-password")
                        .principal(() -> "john.doe@xcmailr.test")
                        .param("currentPassword", "CurrentSecret123")
                        .param("newPassword", "ValidSecretPassword123!")
                        .param("confirmNewPassword", "ValidSecretPassword123!"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("user/profile"))
                        .andExpect(model().attribute("errorMessage", "Password cannot be processed (exceeds allowable length or byte limits)."));
    }

    @Test
    @DisplayName("POST /profile/change-password with new password containing username should reject")
    public void testChangePasswordContainsUserName() throws Exception
    {
        mockMvc.perform(post("/profile/change-password")
               .with(user("john.doe@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("currentPassword", "CurrentSecret123")
               .param("newPassword", "SecretJohnDoe2026!")
               .param("confirmNewPassword", "SecretJohnDoe2026!"))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attribute("errorMessage", "Password must not contain your name or email address."));
    }

    @Test
    @DisplayName("POST /profile/change-password with single repeated character should reject")
    public void testChangePasswordRepetition() throws Exception
    {
        mockMvc.perform(post("/profile/change-password")
               .with(user("john.doe@xcmailr.test").roles("USER"))
               .with(csrf())
               .param("currentPassword", "CurrentSecret123")
               .param("newPassword", "zzzzzzzzzzzz")
               .param("confirmNewPassword", "zzzzzzzzzzzz"))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attribute("errorMessage", "Password cannot consist of a single repeated character."));
    }

    @Test
    @DisplayName("POST /profile/api-token should generate new token")
    public void testGenerateApiToken() throws Exception
    {
        mockMvc.perform(post("/profile/api-token")
               .with(user("john.doe@xcmailr.test").roles("USER"))
               .with(csrf()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/profile?tokenCreated"));

        final User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertNotNull(updated.getApiToken());
        assertTrue(updated.getApiTokenCreationTimestamp() > 0);
    }

    @Test
    @DisplayName("POST /profile/api-token/revoke should clear token")
    public void testRevokeApiToken() throws Exception
    {
        testUser.setApiToken("token-to-revoke-123");
        testUser.setApiTokenCreationTimestamp(System.currentTimeMillis());
        userRepository.save(testUser);

        mockMvc.perform(post("/profile/api-token/revoke")
               .with(user("john.doe@xcmailr.test").roles("USER"))
               .with(csrf()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/profile?tokenRevoked"));

        final User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertNull(updated.getApiToken());
        assertEquals(0L, updated.getApiTokenCreationTimestamp());
    }
}
