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
    @DisplayName("GET /profile should display profile view for authenticated user")
    public void testGetProfileAuthenticated() throws Exception
    {
        mockMvc.perform(get("/profile")
               .with(user("john.doe@xcmailr.test").roles("USER")))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attributeExists("user"));
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
               .andExpect(model().attributeExists("errorMessage"));
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
