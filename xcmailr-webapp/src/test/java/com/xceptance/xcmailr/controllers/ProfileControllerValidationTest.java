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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.XcmailrApplication;
import com.xceptance.xcmailr.models.User;
import com.xceptance.xcmailr.repositories.UserRepository;
import com.xceptance.xcmailr.security.UserPrincipal;

/**
 * Integration test suite following the Test Pyramid strategy to verify server-side validation
 * for user profile updates in {@link ProfileController}.
 * <p>
 * Written test-driven (TDD) to prove that unvalidated inbound requests (bypassing client UI attributes)
 * are rejected at the controller boundary with field-level binding errors, returning HTTP 200 with the
 * profile view instead of unhandled HTTP 500 server crashes or database constraint violations.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public class ProfileControllerValidationTest
{
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private UserPrincipal userPrincipal;

    /**
     * Initializes MockMvc and sets up an active test user in the database before each test.
     */
    @BeforeEach
    public void setup()
    {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        userRepository.deleteAll();

        testUser = new User("John", "Doe", "john.doe.validation@xcmailr.test", passwordEncoder.encode("CurrentSecret123!"), "en");
        testUser.setActive(true);
        testUser = userRepository.save(testUser);
        userPrincipal = new UserPrincipal(testUser);
    }

    // =========================================================================
    // HAPPY PATH INTEGRATION TESTS
    // =========================================================================

    /**
     * Verifies that submitting valid updated details succeeds and redirects to profile with confirmation.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: valid profile update redirects to profile with updated flag and updates database")
    public void testUpdateProfile_ValidInput_SuccessRedirect() throws Exception
    {
        mockMvc.perform(post("/profile/update")
               .with(user(userPrincipal))
               .with(csrf())
               .param("forename", "Jonathan")
               .param("surname", "Doerr")
               .param("language", "de"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/profile?updated"));

        final User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("Jonathan", updatedUser.getForename());
        assertEquals("Doerr", updatedUser.getSurname());
        assertEquals("de", updatedUser.getLanguage());
    }

    // =========================================================================
    // VALIDATION ERROR CASES: MANDATORY & BLANK FIELDS
    // =========================================================================

    /**
     * Verifies that submitting a blank or whitespace-only forename fails server-side validation
     * and re-renders the profile view with field error on 'forename'.
     *
     * @param blankForename invalid candidate forename string
     * @throws Exception if mock MVC request execution fails
     */
    @ParameterizedTest(name = "Validation failure: blank forename \"{0}\" is rejected")
    @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
    @DisplayName("Error case: blank or whitespace-only forename should trigger field validation error")
    public void testUpdateProfile_BlankForename_ValidationFailure(final String blankForename) throws Exception
    {
        mockMvc.perform(post("/profile/update")
               .with(user(userPrincipal))
               .with(csrf())
               .param("forename", blankForename)
               .param("surname", "Doe")
               .param("language", "en"))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attributeHasFieldErrors("profileForm", "forename"));

        final User retainedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("John", retainedUser.getForename(), "Database must remain unaltered on validation failure");
    }

    /**
     * Verifies that submitting a blank or whitespace-only surname fails server-side validation
     * and re-renders the profile view with field error on 'surname'.
     *
     * @param blankSurname invalid candidate surname string
     * @throws Exception if mock MVC request execution fails
     */
    @ParameterizedTest(name = "Validation failure: blank surname \"{0}\" is rejected")
    @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
    @DisplayName("Error case: blank or whitespace-only surname should trigger field validation error")
    public void testUpdateProfile_BlankSurname_ValidationFailure(final String blankSurname) throws Exception
    {
        mockMvc.perform(post("/profile/update")
               .with(user(userPrincipal))
               .with(csrf())
               .param("forename", "John")
               .param("surname", blankSurname)
               .param("language", "en"))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attributeHasFieldErrors("profileForm", "surname"));

        final User retainedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("Doe", retainedUser.getSurname(), "Database must remain unaltered on validation failure");
    }

    // =========================================================================
    // BOUNDARY & MAXIMUM LENGTH VALIDATION CASES
    // =========================================================================

    /**
     * Verifies that forename exceeding 255 characters is rejected with a validation error.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Boundary case: forename exceeding 255 characters should trigger field validation error")
    public void testUpdateProfile_ForenameExceeds255Chars_ValidationFailure() throws Exception
    {
        final String oversizedForename = "F".repeat(256);

        mockMvc.perform(post("/profile/update")
               .with(user(userPrincipal))
               .with(csrf())
               .param("forename", oversizedForename)
               .param("surname", "Doe")
               .param("language", "en"))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attributeHasFieldErrors("profileForm", "forename"));

        final User retainedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("John", retainedUser.getForename(), "Database must remain unaltered on validation failure");
    }

    /**
     * Verifies that surname exceeding 255 characters is rejected with a validation error.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Boundary case: surname exceeding 255 characters should trigger field validation error")
    public void testUpdateProfile_SurnameExceeds255Chars_ValidationFailure() throws Exception
    {
        final String oversizedSurname = "S".repeat(256);

        mockMvc.perform(post("/profile/update")
               .with(user(userPrincipal))
               .with(csrf())
               .param("forename", "John")
               .param("surname", oversizedSurname)
               .param("language", "en"))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attributeHasFieldErrors("profileForm", "surname"));

        final User retainedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("Doe", retainedUser.getSurname(), "Database must remain unaltered on validation failure");
    }

    // =========================================================================
    // SPECIAL CASES: EXACT BOUNDARIES, UTF-8, XSS, SQL INJECTION STRINGS
    // =========================================================================

    /**
     * Verifies that updating forename and surname to exact boundary of 255 characters succeeds.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Special case: forename and surname of exactly 255 characters should pass validation")
    public void testUpdateProfile_Exact255Characters_Success() throws Exception
    {
        final String exact255 = "A".repeat(255);

        mockMvc.perform(post("/profile/update")
               .with(user(userPrincipal))
               .with(csrf())
               .param("forename", exact255)
               .param("surname", exact255)
               .param("language", "de"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/profile?updated"));

        final User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(255, updatedUser.getForename().length());
        assertEquals(255, updatedUser.getSurname().length());
    }

    /**
     * Verifies that multi-byte UTF-8 international names are updated cleanly.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Special case: multi-byte UTF-8 international names should pass validation")
    public void testUpdateProfile_Utf8MultibyteCharacters_Success() throws Exception
    {
        mockMvc.perform(post("/profile/update")
               .with(user(userPrincipal))
               .with(csrf())
               .param("forename", "Frédéric")
               .param("surname", "Chopin-Groß")
               .param("language", "de"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/profile?updated"));

        final User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("Frédéric", updatedUser.getForename());
        assertEquals("Chopin-Groß", updatedUser.getSurname());
    }

    /**
     * Verifies that script/HTML injection payload in profile fields is stored cleanly as text.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Special case: XSS payload in profile fields should be stored safely as text")
    public void testUpdateProfile_XssPayload_StoredSafelyAsPlainText() throws Exception
    {
        final String xssPayload = "<script>alert('profile-xss')</script>";

        mockMvc.perform(post("/profile/update")
               .with(user(userPrincipal))
               .with(csrf())
               .param("forename", xssPayload)
               .param("surname", "Tester")
               .param("language", "en"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/profile?updated"));

        final User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(xssPayload, updatedUser.getForename());
    }

    /**
     * Verifies that SQL control characters in profile fields are stored cleanly as text.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Special case: SQL control characters in profile fields should be stored safely as text")
    public void testUpdateProfile_SqlInjectionPayload_StoredSafelyAsPlainText() throws Exception
    {
        final String sqlPayload = "D'Angelo' OR 1=1;--";

        mockMvc.perform(post("/profile/update")
               .with(user(userPrincipal))
               .with(csrf())
               .param("forename", sqlPayload)
               .param("surname", "Tester")
               .param("language", "en"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/profile?updated"));

        final User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(sqlPayload, updatedUser.getForename());
    }
}
