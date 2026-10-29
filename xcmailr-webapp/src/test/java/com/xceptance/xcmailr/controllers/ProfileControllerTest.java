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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import com.xceptance.xcmailr.XcmailrApplication;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;
import com.xceptance.xcmailr.security.PasswordValidator;
import com.xceptance.xcmailr.security.UserPrincipal;

import models.MBox;
import models.Mail;
import models.User;

/**
 * Tests for {@link ProfileController} verifying profile display, profile updating,
 * password modification, API token generation/revocation, and self-service account deletion.
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
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

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

        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
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

    /**
     * Verifies happy path: updating profile synchronizes both the persistent database record
     * and the in-session UserPrincipal held in the Spring Security context.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy Path: POST /profile/update synchronizes in-session UserPrincipal user state")
    public void testUpdateProfile_HappyPath_SynchronizesInSessionUserPrincipal() throws Exception
    {
        // GIVEN: an authenticated session backed by a UserPrincipal with English preference
        final UserPrincipal userPrincipal = new UserPrincipal(testUser);

        // WHEN: user submits profile update changing language to German
        mockMvc.perform(post("/profile/update")
               .with(user(userPrincipal))
               .with(csrf())
               .param("forename", "Jonathan")
               .param("surname", "Doerr")
               .param("language", "de"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/profile?updated"));

        // THEN: persistent database entity is updated
        final User dbUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("Jonathan", dbUser.getForename());
        assertEquals("Doerr", dbUser.getSurname());
        assertEquals("de", dbUser.getLanguage());

        // AND: in-session UserPrincipal user reference is synchronized immediately
        assertNotNull(userPrincipal.getUser(), "In-session user reference must not be null");
        assertEquals("Jonathan", userPrincipal.getUser().getForename(), "In-session forename must be updated");
        assertEquals("Doerr", userPrincipal.getUser().getSurname(), "In-session surname must be updated");
        assertEquals("de", userPrincipal.getUser().getLanguage(), "In-session language preference must be updated");
    }

    /**
     * Verifies OWASP privilege escalation prevention: updating profile attributes synchronizes only
     * non-sensitive fields and leaves admin status and authorities strictly intact.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Security Case (OWASP): POST /profile/update prevents privilege escalation and preserves roles")
    public void testUpdateProfile_SecurityCase_PreventsPrivilegeEscalation() throws Exception
    {
        // GIVEN: a standard non-administrator user
        testUser.setAdmin(false);
        userRepository.save(testUser);
        final UserPrincipal userPrincipal = new UserPrincipal(testUser);

        // WHEN: user updates personal details
        mockMvc.perform(post("/profile/update")
               .with(user(userPrincipal))
               .with(csrf())
               .param("forename", "Jonathan")
               .param("surname", "Doerr")
               .param("language", "de"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/profile?updated"));

        // THEN: admin privilege remains false and authorities remain standard USER role
        final User dbUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertFalse(dbUser.isAdmin(), "Database user admin flag must remain false");
        assertFalse(userPrincipal.getUser().isAdmin(), "In-session user admin flag must remain false");
        assertEquals(1, userPrincipal.getAuthorities().size(), "Authorities count must not change");
        assertEquals("ROLE_USER", userPrincipal.getAuthorities().iterator().next().getAuthority(), "User role must remain standard ROLE_USER");
    }

    /**
     * Verifies OWASP CSRF protection: profile update without CSRF token is rejected with HTTP 403 Forbidden.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Security Case (OWASP): POST /profile/update without CSRF token is rejected with 403 Forbidden")
    public void testUpdateProfile_SecurityCase_MissingCsrfReturnsForbidden() throws Exception
    {
        // GIVEN: an authenticated user request lacking CSRF token
        // WHEN: submitting profile update without CSRF
        // THEN: request is rejected with HTTP 403 Forbidden and database record remains unchanged
        mockMvc.perform(post("/profile/update")
               .with(user("john.doe@xcmailr.test").roles("USER"))
               .param("forename", "Hacker")
               .param("surname", "Attempt")
               .param("language", "de"))
               .andExpect(status().isForbidden());

        final User dbUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("John", dbUser.getForename(), "Database forename must not be altered without CSRF");
        assertEquals("en", dbUser.getLanguage(), "Database language must not be altered without CSRF");
    }

    /**
     * Verifies access control: unauthenticated POST to /profile/update is redirected to login.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Security Case (OWASP): Unauthenticated POST /profile/update redirects to login")
    public void testUpdateProfile_SecurityCase_UnauthenticatedRedirectsToLogin() throws Exception
    {
        // GIVEN: an unauthenticated request
        // WHEN: submitting profile update
        // THEN: request is redirected to login and database is unchanged
        mockMvc.perform(post("/profile/update")
               .with(csrf())
               .param("forename", "Anonymous")
               .param("surname", "User")
               .param("language", "de"))
               .andExpect(status().is3xxRedirection());

        final User dbUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("John", dbUser.getForename(), "Unauthenticated request must not alter profile");
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

        final ProfileController defensiveController = new ProfileController(userRepository, mailboxRepository, mailRepository, failingEncoder, passwordValidator);
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

    /**
     * Verifies that an authenticated standard user can successfully delete their own account.
     * <p>
     * Ensures all associated mailboxes and emails are cascade deleted, the user record is removed from the
     * database, the HTTP session is invalidated, and the user is redirected to {@code /login?accountDeleted}.
     * </p>
     *
     * @throws Exception if an error occurs during MVC request processing
     */
    @Test
    @DisplayName("POST /profile/delete happy path for standard user cascades mailboxes and mails, invalidates session, and redirects")
    public void testDeleteAccountHappyPathNormalUser() throws Exception
    {
        // Setup: Create a mailbox and an associated email for testUser
        final MBox box = new MBox("testbox", "xcmailr.test", 0L, false, testUser);
        mailboxRepository.save(box);

        final Mail mail = new Mail();
        mail.setSender("sender@example.test");
        mail.setSubject("Test Cascade Deletion");
        mail.setReceiveTime(System.currentTimeMillis());
        mail.setMessage("Sample email body".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mail.setMailbox(box);
        mailRepository.save(mail);

        assertEquals(1L, userRepository.count());
        assertEquals(1L, mailboxRepository.count());
        assertEquals(1L, mailRepository.count());

        final MockHttpSession session = new MockHttpSession();

        // Perform self-service account deletion with correct current password
        mockMvc.perform(post("/profile/delete")
               .session(session)
               .with(user(testUser.getMail()).roles("USER"))
               .with(csrf())
               .param("currentPassword", "CurrentSecret123"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?accountDeleted"));

        // Verify session invalidation
        assertTrue(session.isInvalid(), "HTTP session must be invalidated upon account deletion");

        // Verify transactional cascade deletion of all user entities
        assertTrue(userRepository.findById(testUser.getId()).isEmpty(), "User record must be deleted");
        assertTrue(mailboxRepository.findAll().isEmpty(), "User mailboxes must be cascade deleted");
        assertTrue(mailRepository.findAll().isEmpty(), "Emails belonging to deleted mailboxes must be removed");
    }

    /**
     * Verifies that an administrator can delete their account when at least one other active administrator exists.
     * <p>
     * Ensures the deleting administrator is removed, the remaining administrator is preserved,
     * the session is invalidated, and the user is redirected to {@code /login?accountDeleted}.
     * </p>
     *
     * @throws Exception if an error occurs during MVC request processing
     */
    @Test
    @DisplayName("POST /profile/delete happy path for admin when multiple active admins exist")
    public void testDeleteAccountHappyPathAdminWithMultipleAdmins() throws Exception
    {
        // Promote testUser to active admin
        testUser.setAdmin(true);
        userRepository.save(testUser);

        // Create a second active administrator
        final User secondAdmin = new User("Second", "Admin", "second.admin@xcmailr.test", "AdminSecret123", "en");
        secondAdmin.setActive(true);
        secondAdmin.setAdmin(true);
        userRepository.save(secondAdmin);

        assertEquals(2L, userRepository.countByAdminTrueAndActiveTrue(), "Should have 2 active admins initially");

        final MockHttpSession session = new MockHttpSession();

        // Perform account deletion as testUser
        mockMvc.perform(post("/profile/delete")
               .session(session)
               .with(user(testUser.getMail()).roles("ADMIN", "USER"))
               .with(csrf())
               .param("currentPassword", "CurrentSecret123"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?accountDeleted"));

        // Verify session invalidation and deletion of testUser only
        assertTrue(session.isInvalid(), "HTTP session must be invalidated upon admin account deletion");
        assertTrue(userRepository.findById(testUser.getId()).isEmpty(), "Deleting admin must be removed");
        assertTrue(userRepository.findById(secondAdmin.getId()).isPresent(), "Remaining admin must still exist");
        assertEquals(1L, userRepository.countByAdminTrueAndActiveTrue(), "Exactly one active admin should remain");
    }

    /**
     * Verifies that account deletion is rejected when an incorrect current password is supplied.
     * <p>
     * Ensures an error message is returned on the profile view and no user data is deleted.
     * </p>
     *
     * @throws Exception if an error occurs during MVC request processing
     */
    @Test
    @DisplayName("POST /profile/delete with incorrect password should reject and retain data")
    public void testDeleteAccountWrongPassword() throws Exception
    {
        final MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/profile/delete")
               .session(session)
               .with(user(testUser.getMail()).roles("USER"))
               .with(csrf())
               .param("currentPassword", "IncorrectPassword123"))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attribute("errorMessage", "Current password does not match."));

        // Verify session and user record remain intact
        assertTrue(!session.isInvalid(), "Session must not be invalidated on failed deletion");
        assertTrue(userRepository.findById(testUser.getId()).isPresent(), "User record must not be deleted");
    }

    /**
     * Verifies that account deletion is rejected when a blank or whitespace-only password is submitted.
     * <p>
     * Ensures an error message requesting the current password is shown and no data is deleted.
     * </p>
     *
     * @throws Exception if an error occurs during MVC request processing
     */
    @Test
    @DisplayName("POST /profile/delete with blank password should reject and retain data")
    public void testDeleteAccountBlankPassword() throws Exception
    {
        final MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/profile/delete")
               .session(session)
               .with(user(testUser.getMail()).roles("USER"))
               .with(csrf())
               .param("currentPassword", "   "))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attribute("errorMessage", "Please enter your current password to confirm deletion."));

        // Verify session and user record remain intact
        assertTrue(!session.isInvalid(), "Session must not be invalidated on failed deletion");
        assertTrue(userRepository.findById(testUser.getId()).isPresent(), "User record must not be deleted");
    }

    /**
     * Verifies that a sole active administrator cannot delete their account.
     * <p>
     * Ensures system integrity by requiring at least one active administrator, blocking the deletion,
     * presenting an informative error message on the profile page, and retaining all user data.
     * </p>
     *
     * @throws Exception if an error occurs during MVC request processing
     */
    @Test
    @DisplayName("POST /profile/delete for sole active admin should block deletion and display error")
    public void testDeleteAccountSoleAdminBlocked() throws Exception
    {
        // Make testUser an active administrator
        testUser.setAdmin(true);
        userRepository.save(testUser);

        // Add a mailbox and email to verify retention
        final MBox box = new MBox("adminbox", "xcmailr.test", 0L, false, testUser);
        mailboxRepository.save(box);

        final Mail mail = new Mail();
        mail.setSender("alert@system.test");
        mail.setSubject("Sole Admin Test");
        mail.setReceiveTime(System.currentTimeMillis());
        mail.setMessage("System alert".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mail.setMailbox(box);
        mailRepository.save(mail);

        assertEquals(1L, userRepository.countByAdminTrueAndActiveTrue(), "testUser must be the sole active admin");

        final MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/profile/delete")
               .session(session)
               .with(user(testUser.getMail()).roles("ADMIN", "USER"))
               .with(csrf())
               .param("currentPassword", "CurrentSecret123"))
               .andExpect(status().isOk())
               .andExpect(view().name("user/profile"))
               .andExpect(model().attribute("errorMessage",
                       "Your account cannot be deleted because it is the last administrator account. At least one active administrator account must exist."));

        // Verify session and all user data remain intact
        assertTrue(!session.isInvalid(), "Session must not be invalidated when sole admin deletion is blocked");
        assertTrue(userRepository.findById(testUser.getId()).isPresent(), "Sole admin account must be preserved");
        assertEquals(1L, mailboxRepository.count(), "Mailbox must be preserved");
        assertEquals(1L, mailRepository.count(), "Mail must be preserved");
    }

    /**
     * Verifies that unauthenticated requests to the account deletion endpoint are rejected and redirected.
     *
     * @throws Exception if an error occurs during MVC request processing
     */
    @Test
    @DisplayName("POST /profile/delete unauthenticated should redirect to login")
    public void testDeleteAccountUnauthenticated() throws Exception
    {
        mockMvc.perform(post("/profile/delete")
               .with(csrf())
               .param("currentPassword", "CurrentSecret123"))
               .andExpect(status().is3xxRedirection());

        // Verify testUser data was not altered
        assertTrue(userRepository.findById(testUser.getId()).isPresent(), "User record must remain intact");
    }

    /**
     * Verifies that account deletion requests missing a CSRF token are rejected with HTTP 403 Forbidden.
     *
     * @throws Exception if an error occurs during MVC request processing
     */
    @Test
    @DisplayName("POST /profile/delete without CSRF token should return 403 Forbidden")
    public void testDeleteAccountCsrfMissing() throws Exception
    {
        mockMvc.perform(post("/profile/delete")
               .with(user(testUser.getMail()).roles("USER"))
               .param("currentPassword", "CurrentSecret123"))
               .andExpect(status().isForbidden());

        // Verify testUser data was not altered
        assertTrue(userRepository.findById(testUser.getId()).isPresent(), "User record must remain intact");
    }

    /**
     * Verifies that account deletion requests with an invalid/forged CSRF token are rejected with HTTP 403 Forbidden.
     *
     * @throws Exception if an error occurs during MVC request processing
     */
    @Test
    @DisplayName("POST /profile/delete with invalid CSRF token should return 403 Forbidden")
    public void testDeleteAccountCsrfInvalid() throws Exception
    {
        mockMvc.perform(post("/profile/delete")
               .with(user(testUser.getMail()).roles("USER"))
               .with(csrf().useInvalidToken())
               .param("currentPassword", "CurrentSecret123"))
               .andExpect(status().isForbidden());

        // Verify testUser data was not altered
        assertTrue(userRepository.findById(testUser.getId()).isPresent(), "User record must remain intact");
    }
}

