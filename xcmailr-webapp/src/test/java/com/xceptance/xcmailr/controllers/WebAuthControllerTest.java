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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Locale;
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

    @Autowired
    private com.xceptance.xcmailr.services.OutboundMailService outboundMailService;

    @Autowired
    private com.xceptance.xcmailr.security.PasswordValidator passwordValidator;

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
    @DisplayName("GET /register should display registration view with password requirements")
    public void testRegisterPage() throws Exception
    {
        mockMvc.perform(get("/register"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(content().string(containsString("minlength=\"10\"")))
               .andExpect(content().string(containsString("maxlength=\"72\"")))
               .andExpect(content().string(containsString("At least 10 characters (up to 72)")))
               .andExpect(content().string(containsString("Password requirements:")));
    }

    @Test
    @DisplayName("GET /pwreset/{token} with valid token should display reset password view with requirements")
    public void testResetPasswordPage() throws Exception
    {
        final User user = new User("Token", "User", "token.user@xcmailr.test", passwordEncoder.encode("secret1234"), "en");
        user.setActive(true);
        user.setConfirmation("view-test-token");
        user.setTs_confirm(System.currentTimeMillis() + 86_400_000L);
        userRepository.save(user);

        mockMvc.perform(get("/pwreset/view-test-token"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/reset-password"))
               .andExpect(content().string(containsString("minlength=\"10\"")))
               .andExpect(content().string(containsString("maxlength=\"72\"")))
               .andExpect(content().string(containsString("At least 10 characters (up to 72)")))
               .andExpect(content().string(containsString("Password requirements:")));
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

    /**
     * Verifies backward compatibility: ensures existing database user credentials hashed with
     * standard BCrypt (prior to password policy alignment) continue to authenticate cleanly via /login.
     */
    @Test
    @DisplayName("POST /login with pre-existing legacy BCrypt hash in database should authenticate successfully")
    public void testLegacyBcryptHashLoginSuccess() throws Exception
    {
        // Simulate an existing database user created before password policy alignment
        final User user = new User();
        user.setForename("Legacy");
        user.setSurname("User");
        user.setMail("legacy.user@xcmailr.test");
        // Explicitly set a standard BCrypt hash in the database
        user.setPasswd(passwordEncoder.encode("LegacySecret123!"));
        user.setActive(true);
        userRepository.save(user);

        // Verify successful authentication and redirection to dashboard root
        mockMvc.perform(post("/login")
               .with(csrf())
               .param("mail", "legacy.user@xcmailr.test")
               .param("password", "LegacySecret123!"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/"));
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
               .param("password", "ValidSecret123!")
               .param("confirmPassword", "ValidSecret123!"))
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
               .param("password", "ValidSecret123!"))
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
               .param("password", "ValidSecret123!"))
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
    @DisplayName("POST /register with password shorter than 10 characters should reject")
    public void testRegisterShortPassword() throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Short")
               .param("surname", "Pass")
               .param("mail", "shortpass@xcmailr.test")
               .param("password", "Short9!")
               .param("confirmPassword", "Short9!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attribute("errorMessage", "Password must be at least 10 characters long."));
    }

    @Test
    @DisplayName("POST /register with valid 72-character password should succeed and encode without error")
    public void testRegisterPasswordMaxBoundary72Chars() throws Exception
    {
        final String boundary72 = "SecureLimitPassword72---" + "a".repeat(40) + "-End72!";
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Boundary")
               .param("surname", "User")
               .param("mail", "boundary72@xcmailr.test")
               .param("password", boundary72)
               .param("confirmPassword", boundary72))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?registered"));

        final User user = userRepository.findByMailIgnoreCase("boundary72@xcmailr.test").orElseThrow();
        assertTrue(passwordEncoder.matches(boundary72, user.getPasswd()), "72-char password must be encoded and match");
    }

    @Test
    @DisplayName("POST /register with password exceeding 72 characters should reject")
    public void testRegisterPasswordExceeding72Chars() throws Exception
    {
        final String password73 = "A".repeat(73);
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Long")
               .param("surname", "Pass")
               .param("mail", "longpass73@xcmailr.test")
               .param("password", password73)
               .param("confirmPassword", password73))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attribute("errorMessage", "Password cannot exceed 72 characters."));
    }

    @Test
    @DisplayName("POST /register with 128-character password should reject cleanly without HTTP 500")
    public void testRegisterPassword128Chars() throws Exception
    {
        final String password128 = "RfYfJm8wVe3jJzOVTwsL0ibnuyWmJthLPhNYe42pJmvdxL4mAY59AofqPCnTp0RDQt1rwI0hYOBrjueAGlYm1Nnsmc98vQH19mt93jyY5zJzU1MRAieuJVdUeu5X2cmN";
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Manual")
               .param("surname", "Case128")
               .param("mail", "case128@xcmailr.test")
               .param("password", password128)
               .param("confirmPassword", password128))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attribute("errorMessage", "Password cannot exceed 72 characters."));
    }

    @Test
    @DisplayName("POST /register with 129-character password should reject cleanly without HTTP 500")
    public void testRegisterPassword129Chars() throws Exception
    {
        final String password129 = "RfYfJm8wVe3jJzOVTwsL0ibnuyWmJthLPhNYe42pJmvdxL4mAY59AofqPCnTp0RDQt1rwI0hYOBrjueAGlYm1Nnsmc98vQH19mt93jyY5zJzU1MRAieuJVdUeu5X2cmNQ";
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Manual")
               .param("surname", "Case129")
               .param("mail", "case129@xcmailr.test")
               .param("password", password129)
               .param("confirmPassword", password129))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attribute("errorMessage", "Password cannot exceed 72 characters."));
    }

    @Test
    @DisplayName("POST /register with multibyte password exceeding 72 bytes should reject")
    public void testRegisterPasswordMultibyteByteOverflow() throws Exception
    {
        final String emojiPassword = "🚀".repeat(25); // 25 characters, 100 UTF-8 bytes
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Emoji")
               .param("surname", "User")
               .param("mail", "emoji@xcmailr.test")
               .param("password", emojiPassword)
               .param("confirmPassword", emojiPassword))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attribute("errorMessage", "Password cannot exceed 72 bytes (special or non-Latin characters use multiple bytes)."));
    }

    @Test
    @DisplayName("POST /register should catch unexpected IllegalArgumentException from encoder and return error view")
    public void testRegisterDefensiveEncoderCatch() throws Exception
    {
        final PasswordEncoder failingEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
        org.mockito.Mockito.when(failingEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                           .thenThrow(new IllegalArgumentException("password cannot be more than 72 bytes"));

        final WebAuthController defensiveController = new WebAuthController(userRepository, failingEncoder, outboundMailService, properties, passwordValidator);
        final MockMvc defensiveMockMvc = MockMvcBuilders.standaloneSetup(defensiveController).build();

        defensiveMockMvc.perform(post("/register")
                        .param("forename", "Defensive")
                        .param("surname", "User")
                        .param("mail", "defensive@xcmailr.test")
                        .param("password", "ValidSecretPassword123!")
                        .param("confirmPassword", "ValidSecretPassword123!"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("auth/register"))
                        .andExpect(model().attribute("errorMessage", "Password cannot be processed (exceeds allowable length or byte limits)."));
    }

    @Test
    @DisplayName("POST /register with password containing forename should reject")
    public void testRegisterPasswordContainsForename() throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Alexander")
               .param("surname", "Hamilton")
               .param("mail", "ah@xcmailr.test")
               .param("password", "MyAlexanderPass!")
               .param("confirmPassword", "MyAlexanderPass!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attribute("errorMessage", "Password must not contain your name or email address."));
    }

    @Test
    @DisplayName("POST /register with password containing surname should reject")
    public void testRegisterPasswordContainsSurname() throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Alexander")
               .param("surname", "Hamilton")
               .param("mail", "ah@xcmailr.test")
               .param("password", "MyHamiltonPass!")
               .param("confirmPassword", "MyHamiltonPass!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attribute("errorMessage", "Password must not contain your name or email address."));
    }

    @Test
    @DisplayName("POST /register with password containing mail local-part should reject")
    public void testRegisterPasswordContainsMailLocalPart() throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Alexander")
               .param("surname", "Hamilton")
               .param("mail", "alex.hamilton@xcmailr.test")
               .param("password", "alex.hamilton2026!")
               .param("confirmPassword", "alex.hamilton2026!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attribute("errorMessage", "Password must not contain your name or email address."));
    }

    @Test
    @DisplayName("POST /register with single repeating character should reject")
    public void testRegisterPasswordRepetition() throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Trivial")
               .param("surname", "Pass")
               .param("mail", "trivial@xcmailr.test")
               .param("password", "aaaaaaaaaaaa")
               .param("confirmPassword", "aaaaaaaaaaaa"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attribute("errorMessage", "Password cannot consist of a single repeated character."));
    }

    @Test
    @DisplayName("POST /pwreset/{token} with password shorter than 10 characters should reject")
    public void testResetPasswordShortPassword() throws Exception
    {
        final User user = new User("Grace", "Hopper", "grace@xcmailr.test", passwordEncoder.encode("oldPassword123!"), "en");
        user.setActive(true);
        user.setConfirmation("reset-token-grace");
        user.setTs_confirm(System.currentTimeMillis() + 86_400_000L);
        userRepository.save(user);

        mockMvc.perform(post("/pwreset/reset-token-grace")
               .with(csrf())
               .param("password", "Short9!")
               .param("confirmPassword", "Short9!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/reset-password"))
               .andExpect(model().attribute("errorMessage", "Password must be at least 10 characters long."));
    }

    @Test
    @DisplayName("POST /pwreset/{token} with password containing user context should reject")
    public void testResetPasswordContainsUserName() throws Exception
    {
        final User user = new User("Grace", "Hopper", "grace.hopper@xcmailr.test", passwordEncoder.encode("oldPassword123!"), "en");
        user.setActive(true);
        user.setConfirmation("reset-token-grace2");
        user.setTs_confirm(System.currentTimeMillis() + 86_400_000L);
        userRepository.save(user);

        mockMvc.perform(post("/pwreset/reset-token-grace2")
               .with(csrf())
               .param("password", "NewHopperSecret2026!")
               .param("confirmPassword", "NewHopperSecret2026!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/reset-password"))
               .andExpect(model().attribute("errorMessage", "Password must not contain your name or email address."));
    }

    @Test
    @DisplayName("POST /pwreset/{token} with mismatched passwords should reject")
    public void testResetPasswordMismatchedPasswords() throws Exception
    {
        final User user = new User("Grace", "Hopper", "grace.hopper2@xcmailr.test", passwordEncoder.encode("oldPassword123!"), "en");
        user.setActive(true);
        user.setConfirmation("reset-token-grace3");
        user.setTs_confirm(System.currentTimeMillis() + 86_400_000L);
        userRepository.save(user);

        mockMvc.perform(post("/pwreset/reset-token-grace3")
               .with(csrf())
               .param("password", "ValidSecretPassword123!")
               .param("confirmPassword", "DifferentPassword123!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/reset-password"))
               .andExpect(model().attribute("errorMessage", "Passwords do not match."));
    }

    @Test
    @DisplayName("POST /pwreset/{token} with single repeating character should reject")
    public void testResetPasswordRepetition() throws Exception
    {
        final User user = new User("Grace", "Hopper", "grace.hopper3@xcmailr.test", passwordEncoder.encode("oldPassword123!"), "en");
        user.setActive(true);
        user.setConfirmation("reset-token-grace4");
        user.setTs_confirm(System.currentTimeMillis() + 86_400_000L);
        userRepository.save(user);

        mockMvc.perform(post("/pwreset/reset-token-grace4")
               .with(csrf())
               .param("password", "111111111111")
               .param("confirmPassword", "111111111111"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/reset-password"))
               .andExpect(model().attribute("errorMessage", "Password cannot consist of a single repeated character."));
    }

    @Test
    @DisplayName("POST /pwreset/{token} with valid 72-character password should succeed and encode without error")
    public void testResetPasswordMaxBoundary72Chars() throws Exception
    {
        final User user = new User("Boundary", "Reset", "boundary.reset@xcmailr.test", passwordEncoder.encode("oldPassword123!"), "en");
        user.setActive(true);
        user.setConfirmation("reset-token-72chars");
        user.setTs_confirm(System.currentTimeMillis() + 86_400_000L);
        userRepository.save(user);

        final String boundary72 = "SecureLimitPassword72---" + "a".repeat(40) + "-End72!";
        mockMvc.perform(post("/pwreset/reset-token-72chars")
               .with(csrf())
               .param("password", boundary72)
               .param("confirmPassword", boundary72))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?passwordReset"));

        final User updated = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches(boundary72, updated.getPasswd()), "72-char reset password must be encoded and match");
    }

    @Test
    @DisplayName("POST /pwreset/{token} with 128-character password should reject cleanly")
    public void testResetPassword128Chars() throws Exception
    {
        final User user = new User("Grace", "Hopper", "grace.hopper128@xcmailr.test", passwordEncoder.encode("oldPassword123!"), "en");
        user.setActive(true);
        user.setConfirmation("reset-token-128");
        user.setTs_confirm(System.currentTimeMillis() + 86_400_000L);
        userRepository.save(user);

        final String password128 = "RfYfJm8wVe3jJzOVTwsL0ibnuyWmJthLPhNYe42pJmvdxL4mAY59AofqPCnTp0RDQt1rwI0hYOBrjueAGlYm1Nnsmc98vQH19mt93jyY5zJzU1MRAieuJVdUeu5X2cmN";
        mockMvc.perform(post("/pwreset/reset-token-128")
               .with(csrf())
               .param("password", password128)
               .param("confirmPassword", password128))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/reset-password"))
               .andExpect(model().attribute("errorMessage", "Password cannot exceed 72 characters."));
    }

    @Test
    @DisplayName("POST /pwreset/{token} with 129-character password should reject cleanly")
    public void testResetPassword129Chars() throws Exception
    {
        final User user = new User("Grace", "Hopper", "grace.hopper129@xcmailr.test", passwordEncoder.encode("oldPassword123!"), "en");
        user.setActive(true);
        user.setConfirmation("reset-token-129");
        user.setTs_confirm(System.currentTimeMillis() + 86_400_000L);
        userRepository.save(user);

        final String password129 = "RfYfJm8wVe3jJzOVTwsL0ibnuyWmJthLPhNYe42pJmvdxL4mAY59AofqPCnTp0RDQt1rwI0hYOBrjueAGlYm1Nnsmc98vQH19mt93jyY5zJzU1MRAieuJVdUeu5X2cmNQ";
        mockMvc.perform(post("/pwreset/reset-token-129")
               .with(csrf())
               .param("password", password129)
               .param("confirmPassword", password129))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/reset-password"))
               .andExpect(model().attribute("errorMessage", "Password cannot exceed 72 characters."));
    }

    @Test
    @DisplayName("POST /pwreset/{token} should catch unexpected IllegalArgumentException from encoder and return error view")
    public void testResetPasswordDefensiveEncoderCatch() throws Exception
    {
        final User user = new User("Defensive", "Reset", "defensive.reset@xcmailr.test", "oldPass12345", "en");
        user.setActive(true);
        user.setConfirmation("defensive-token-123");
        user.setTs_confirm(System.currentTimeMillis() + 86_400_000L);
        userRepository.save(user);

        final PasswordEncoder failingEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
        org.mockito.Mockito.when(failingEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                           .thenThrow(new IllegalArgumentException("password cannot be more than 72 bytes"));

        final WebAuthController defensiveController = new WebAuthController(userRepository, failingEncoder, outboundMailService, properties, passwordValidator);
        final MockMvc defensiveMockMvc = MockMvcBuilders.standaloneSetup(defensiveController).build();

        defensiveMockMvc.perform(post("/pwreset/defensive-token-123")
                        .param("password", "ValidSecretPassword123!")
                        .param("confirmPassword", "ValidSecretPassword123!"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("auth/reset-password"))
                        .andExpect(model().attribute("errorMessage", "Password cannot be processed (exceeds allowable length or byte limits)."));
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

    /**
     * Verifies that user registration rejection due to mismatched passwords returns a localized
     * German error alert when requested with German locale.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Localization: POST /register with Locale.GERMAN and mismatched passwords returns German error alert")
    public void testRegister_Localization_GermanLocale_PasswordMismatch() throws Exception
    {
        mockMvc.perform(post("/register")
               .locale(Locale.GERMAN)
               .with(csrf())
               .param("forename", "Hans")
               .param("surname", "Mueller")
               .param("mail", "hans.mueller@xcmailr.test")
               .param("password", "SecretPassword123!")
               .param("confirmPassword", "DifferentPassword123!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attribute("errorMessage", "Die Passwörter stimmen nicht überein."))
               .andExpect(content().string(containsString("Die Passwörter stimmen nicht überein.")));
    }

    /**
     * Verifies that user registration rejection due to short password policy violation returns a localized
     * German error alert when requested with German locale.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Localization: POST /register with Locale.GERMAN and short password returns German validation alert")
    public void testRegister_Localization_GermanLocale_ShortPassword() throws Exception
    {
        mockMvc.perform(post("/register")
               .locale(Locale.GERMAN)
               .with(csrf())
               .param("forename", "Hans")
               .param("surname", "Mueller")
               .param("mail", "hans.mueller@xcmailr.test")
               .param("password", "Short9!")
               .param("confirmPassword", "Short9!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attribute("errorMessage", "Das Passwort muss mindestens 10 Zeichen lang sein."))
               .andExpect(content().string(containsString("Das Passwort muss mindestens 10 Zeichen lang sein.")));
    }

    /**
     * Verifies that user registration rejection due to duplicate email address returns a localized
     * German error alert when requested with German locale.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Localization: POST /register with Locale.GERMAN and duplicate email returns German error alert")
    public void testRegister_Localization_GermanLocale_DuplicateEmail() throws Exception
    {
        final User existing = new User("Existing", "User", "existing@xcmailr.test", "Password123!", "de");
        existing.setActive(true);
        userRepository.save(existing);

        mockMvc.perform(post("/register")
               .locale(Locale.GERMAN)
               .with(csrf())
               .param("forename", "Neuer")
               .param("surname", "Benutzer")
               .param("mail", "existing@xcmailr.test")
               .param("password", "ValidSecretPassword123!")
               .param("confirmPassword", "ValidSecretPassword123!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attribute("errorMessage", "Ein Konto mit dieser E-Mail-Adresse existiert bereits."))
               .andExpect(content().string(containsString("Ein Konto mit dieser E-Mail-Adresse existiert bereits.")));
    }

    /**
     * Verifies that user registration rejection with default English locale returns standard English error message.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Localization: POST /register with default Locale returns English error alert")
    public void testRegister_Localization_EnglishDefault_PasswordMismatch() throws Exception
    {
        mockMvc.perform(post("/register")
               .locale(Locale.ENGLISH)
               .with(csrf())
               .param("forename", "John")
               .param("surname", "Doe")
               .param("mail", "john.unique@xcmailr.test")
               .param("password", "SecretPassword123!")
               .param("confirmPassword", "MismatchPassword123!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attribute("errorMessage", "Passwords do not match."))
               .andExpect(content().string(containsString("Passwords do not match.")));
    }

    /**
     * Verifies that password reset submission rejection due to mismatched passwords returns a localized
     * German error alert when requested with German locale.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Localization: POST /pwreset/{token} with Locale.GERMAN and mismatched passwords returns German error alert")
    public void testResetPassword_Localization_GermanLocale_PasswordMismatch() throws Exception
    {
        final User user = new User("Reset", "German", "reset.german@xcmailr.test", "ExistingPassword123!", "de");
        user.setActive(true);
        user.setConfirmation("reset-token-de-mismatch");
        user.setTs_confirm(System.currentTimeMillis() + 3600_000L);
        userRepository.save(user);

        mockMvc.perform(post("/pwreset/reset-token-de-mismatch")
               .locale(Locale.GERMAN)
               .with(csrf())
               .param("password", "NewValidSecret123!")
               .param("confirmPassword", "CompletelyDifferent123!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/reset-password"))
               .andExpect(model().attribute("errorMessage", "Die Passwörter stimmen nicht überein."))
               .andExpect(content().string(containsString("Die Passwörter stimmen nicht überein.")));
    }

    /**
     * Verifies that password reset submission rejection due to password policy violation returns a localized
     * German validation error alert when requested with German locale.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Localization: POST /pwreset/{token} with Locale.GERMAN and short password returns German validation alert")
    public void testResetPassword_Localization_GermanLocale_QualityViolation() throws Exception
    {
        final User user = new User("Reset", "German", "reset.quality@xcmailr.test", "ExistingPassword123!", "de");
        user.setActive(true);
        user.setConfirmation("reset-token-de-quality");
        user.setTs_confirm(System.currentTimeMillis() + 3600_000L);
        userRepository.save(user);

        mockMvc.perform(post("/pwreset/reset-token-de-quality")
               .locale(Locale.GERMAN)
               .with(csrf())
               .param("password", "Short9!")
               .param("confirmPassword", "Short9!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/reset-password"))
               .andExpect(model().attribute("errorMessage", "Das Passwort muss mindestens 10 Zeichen lang sein."))
               .andExpect(content().string(containsString("Das Passwort muss mindestens 10 Zeichen lang sein.")));
    }

    /**
     * Verifies template rendering contract for registration view with German locale:
     * forename, surname, target email, password placeholders, and mobile navigation aria-label.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Localization Contract: GET /register with Locale.GERMAN renders localized placeholders and aria-label")
    public void testRegisterForm_LocalizationContract_GermanPlaceholders() throws Exception
    {
        mockMvc.perform(get("/register")
               .locale(Locale.GERMAN))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(content().string(containsString("placeholder=\"Erika\"")))
               .andExpect(content().string(containsString("placeholder=\"Mustermann\"")))
               .andExpect(content().string(containsString("placeholder=\"zieladresse@ihredomain.de\"")))
               .andExpect(content().string(containsString("placeholder=\"Mindestens 10 Zeichen\"")))
               .andExpect(content().string(containsString("placeholder=\"Passwort wiederholen\"")))
               .andExpect(content().string(containsString("aria-label=\"Navigation umschalten\"")));
    }

    /**
     * Verifies template rendering contract for registration view with English locale (default):
     * forename, surname, target email, password placeholders, and mobile navigation aria-label.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Localization Contract: GET /register with Locale.ENGLISH renders English placeholders and aria-label")
    public void testRegisterForm_LocalizationContract_EnglishPlaceholders() throws Exception
    {
        mockMvc.perform(get("/register")
               .locale(Locale.ENGLISH))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(content().string(containsString("placeholder=\"Jane\"")))
               .andExpect(content().string(containsString("placeholder=\"Doe\"")))
               .andExpect(content().string(containsString("placeholder=\"forward-target@yourdomain.com\"")))
               .andExpect(content().string(containsString("placeholder=\"At least 10 characters\"")))
               .andExpect(content().string(containsString("placeholder=\"Repeat password\"")))
               .andExpect(content().string(containsString("aria-label=\"Toggle navigation\"")));
    }

    /**
     * Verifies template rendering contract for reset password view with German locale:
     * new password and confirm password placeholders.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Localization Contract: GET /pwreset/{token} with Locale.GERMAN renders localized placeholders")
    public void testResetPasswordForm_LocalizationContract_GermanPlaceholders() throws Exception
    {
        final User user = new User("Form", "Test", "form.test@xcmailr.test", "Password123!", "de");
        user.setActive(true);
        user.setConfirmation("reset-token-form-de");
        user.setTs_confirm(System.currentTimeMillis() + 3600_000L);
        userRepository.save(user);

        mockMvc.perform(get("/pwreset/reset-token-form-de")
               .locale(Locale.GERMAN))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/reset-password"))
               .andExpect(content().string(containsString("placeholder=\"Mindestens 10 Zeichen\"")))
               .andExpect(content().string(containsString("placeholder=\"Neues Passwort wiederholen\"")));
    }

    /**
     * Verifies OWASP ASVS / XSS mitigation standards: user-supplied input submitted in registration form
     * is automatically escaped with HTML entities by Thymeleaf when re-rendered on validation failure.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("OWASP ASVS XSS: Injected script tag in registration form fields is automatically escaped in rendered HTML")
    public void testRegister_OwaspAsvs_HtmlEntityEscaping() throws Exception
    {
        final String maliciousForename = "<script>alert('xss-forename')</script>";
        final String maliciousSurname = "\"><img src=x onerror=alert(1)>";

        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", maliciousForename)
               .param("surname", maliciousSurname)
               .param("mail", "xss.test@xcmailr.test")
               .param("password", "SecretPassword123!")
               .param("confirmPassword", "MismatchPassword123!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               // Raw unescaped malicious scripts must NOT be present in output
               .andExpect(content().string(not(containsString("<script>alert('xss-forename')</script>"))))
               .andExpect(content().string(not(containsString("\"><img src=x onerror=alert(1)>"))))
               // Escaped entities must be present
               .andExpect(content().string(containsString("&lt;script&gt;alert(&#39;xss-forename&#39;)&lt;/script&gt;")))
               .andExpect(content().string(containsString("&quot;&gt;&lt;img src=x onerror=alert(1)&gt;")));
    }
}
