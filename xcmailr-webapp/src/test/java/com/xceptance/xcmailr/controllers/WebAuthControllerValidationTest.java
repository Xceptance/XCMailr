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

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

/**
 * Integration test suite following the Test Pyramid strategy to verify server-side validation
 * for user registration in {@link WebAuthController}.
 * <p>
 * Written test-driven (TDD) to prove that unvalidated inbound requests (bypassing client UI attributes)
 * are rejected at the controller boundary with field-level binding errors, returning HTTP 200 with the
 * registration form view instead of unhandled HTTP 500 server crashes.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public class WebAuthControllerValidationTest
{
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Initializes MockMvc and cleans up test database state before each test execution.
     */
    @BeforeEach
    public void setup()
    {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
        userRepository.deleteAll();
    }

    // =========================================================================
    // HAPPY PATH INTEGRATION TESTS
    // =========================================================================

    /**
     * Verifies that submitting valid registration details successfully creates the user
     * and redirects to the registration confirmation landing page.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Happy path: valid registration input redirects to login with registration confirmation")
    public void testRegister_ValidInput_SuccessRedirect() throws Exception
    {
        final String forename = "Alice";
        final String surname = "Wonderland";
        final String mail = "alice.wonderland@xcmailr.test";
        final String password = "SecureSecretPassword123!";

        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", forename)
               .param("surname", surname)
               .param("mail", mail)
               .param("password", password)
               .param("confirmPassword", password)
               .param("language", "en"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?registered"));

        final User createdUser = userRepository.findByMailIgnoreCase(mail).orElse(null);
        assertNotNull(createdUser, "User should be persisted in database");
        assertFalse(createdUser.isActive(), "User must be inactive pending confirmation");
        assertTrue(passwordEncoder.matches(password, createdUser.getPasswd()), "Password must be encoded");
    }

    // =========================================================================
    // VALIDATION ERROR CASES: MANDATORY & BLANK FIELDS
    // =========================================================================

    /**
     * Verifies that submitting a blank or whitespace-only forename fails server-side validation
     * and re-renders the registration form with field-specific binding error on 'forename'.
     *
     * @param blankForename invalid candidate forename string
     * @throws Exception if mock MVC request execution fails
     */
    @ParameterizedTest(name = "Validation failure: blank forename \"{0}\" is rejected")
    @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
    @DisplayName("Error case: blank or whitespace-only forename should trigger field validation error")
    public void testRegister_BlankForename_ValidationFailure(final String blankForename) throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", blankForename)
               .param("surname", "Wonderland")
               .param("mail", "alice.blankforename@xcmailr.test")
               .param("password", "SecureSecretPassword123!")
               .param("confirmPassword", "SecureSecretPassword123!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attributeHasFieldErrors("form", "forename"));

        assertTrue(userRepository.findByMailIgnoreCase("alice.blankforename@xcmailr.test").isEmpty(),
            "Database must not contain unvalidated user record");
    }

    /**
     * Verifies that submitting a blank or whitespace-only surname fails server-side validation
     * and re-renders the registration form with field-specific binding error on 'surname'.
     *
     * @param blankSurname invalid candidate surname string
     * @throws Exception if mock MVC request execution fails
     */
    @ParameterizedTest(name = "Validation failure: blank surname \"{0}\" is rejected")
    @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
    @DisplayName("Error case: blank or whitespace-only surname should trigger field validation error")
    public void testRegister_BlankSurname_ValidationFailure(final String blankSurname) throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Alice")
               .param("surname", blankSurname)
               .param("mail", "alice.blanksurname@xcmailr.test")
               .param("password", "SecureSecretPassword123!")
               .param("confirmPassword", "SecureSecretPassword123!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attributeHasFieldErrors("form", "surname"));

        assertTrue(userRepository.findByMailIgnoreCase("alice.blanksurname@xcmailr.test").isEmpty(),
            "Database must not contain unvalidated user record");
    }

    /**
     * Verifies that submitting a malformed email address fails server-side validation
     * and re-renders the registration form with field-specific binding error on 'mail'.
     *
     * @param malformedEmail invalid candidate email string
     * @throws Exception if mock MVC request execution fails
     */
    @ParameterizedTest(name = "Validation failure: malformed email \"{0}\" is rejected")
    @ValueSource(strings = {"not-an-email", "missing-at.com", "@domain.com", "user@.com", "plainaddress", "user@domain..com"})
    @DisplayName("Error case: malformed email address should trigger field validation error")
    public void testRegister_MalformedEmail_ValidationFailure(final String malformedEmail) throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Alice")
               .param("surname", "Wonderland")
               .param("mail", malformedEmail)
               .param("password", "SecureSecretPassword123!")
               .param("confirmPassword", "SecureSecretPassword123!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attributeHasFieldErrors("form", "mail"));

        assertTrue(userRepository.findByMailIgnoreCase(malformedEmail).isEmpty(),
            "Database must not contain unvalidated user record");
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
    public void testRegister_ForenameExceeds255Chars_ValidationFailure() throws Exception
    {
        final String oversizedForename = "F".repeat(256);

        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", oversizedForename)
               .param("surname", "Wonderland")
               .param("mail", "oversized.forename@xcmailr.test")
               .param("password", "SecureSecretPassword123!")
               .param("confirmPassword", "SecureSecretPassword123!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attributeHasFieldErrors("form", "forename"));
    }

    /**
     * Verifies that surname exceeding 255 characters is rejected with a validation error.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Boundary case: surname exceeding 255 characters should trigger field validation error")
    public void testRegister_SurnameExceeds255Chars_ValidationFailure() throws Exception
    {
        final String oversizedSurname = "S".repeat(256);

        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Alice")
               .param("surname", oversizedSurname)
               .param("mail", "oversized.surname@xcmailr.test")
               .param("password", "SecureSecretPassword123!")
               .param("confirmPassword", "SecureSecretPassword123!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attributeHasFieldErrors("form", "surname"));
    }

    /**
     * Verifies that email address exceeding 255 characters is rejected with a validation error.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Boundary case: email address exceeding 255 characters should trigger field validation error")
    public void testRegister_EmailExceeds255Chars_ValidationFailure() throws Exception
    {
        // 245 characters local part + "@xcmailr.test" (13 chars) = 258 chars total (> 255)
        final String oversizedEmail = "a".repeat(245) + "@xcmailr.test";

        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Alice")
               .param("surname", "Wonderland")
               .param("mail", oversizedEmail)
               .param("password", "SecureSecretPassword123!")
               .param("confirmPassword", "SecureSecretPassword123!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attributeHasFieldErrors("form", "mail"));
    }

    // =========================================================================
    // PASSWORD VALIDATION & OWASP RULES
    // =========================================================================

    /**
     * Verifies that password confirmation mismatch is caught and re-renders form with error.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Password case: mismatched confirmation password should trigger validation error")
    public void testRegister_PasswordMismatch_ValidationFailure() throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Alice")
               .param("surname", "Wonderland")
               .param("mail", "alice.mismatch@xcmailr.test")
               .param("password", "SecureSecretPassword123!")
               .param("confirmPassword", "DifferentPassword456!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attributeHasFieldErrors("form", "confirmPassword"));
    }

    /**
     * Verifies that password shorter than 10 characters is rejected by bean validation.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Password boundary: password shorter than 10 characters should trigger field validation error")
    public void testRegister_PasswordTooShort_ValidationFailure() throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Alice")
               .param("surname", "Wonderland")
               .param("mail", "alice.shortpw@xcmailr.test")
               .param("password", "Short9!")
               .param("confirmPassword", "Short9!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attributeHasFieldErrors("form", "password"));
    }

    /**
     * Verifies that password exceeding 72 characters is rejected by bean validation.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Password boundary: password exceeding 72 characters should trigger field validation error")
    public void testRegister_PasswordTooLong_ValidationFailure() throws Exception
    {
        final String longPassword = "P".repeat(73);

        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Alice")
               .param("surname", "Wonderland")
               .param("mail", "alice.longpw@xcmailr.test")
               .param("password", longPassword)
               .param("confirmPassword", longPassword))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attributeHasFieldErrors("form", "password"));
    }

    /**
     * Verifies that a password containing the user's forename is rejected according to OWASP guidelines.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("OWASP case: password containing forename should trigger password policy rejection")
    public void testRegister_PasswordContainsForename_ValidationFailure() throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Alexander")
               .param("surname", "Hamilton")
               .param("mail", "alex.hamilton@xcmailr.test")
               .param("password", "MyAlexander2026!")
               .param("confirmPassword", "MyAlexander2026!"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attributeHasFieldErrors("form", "password"));
    }

    /**
     * Verifies that a password consisting of a single repeated character is rejected by OWASP repetition rule.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("OWASP case: password of single repeated character should trigger password policy rejection")
    public void testRegister_PasswordRepeatedCharacters_ValidationFailure() throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Alice")
               .param("surname", "Wonderland")
               .param("mail", "alice.repeatpw@xcmailr.test")
               .param("password", "aaaaaaaaaaaa")
               .param("confirmPassword", "aaaaaaaaaaaa"))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/register"))
               .andExpect(model().attributeHasFieldErrors("form", "password"));
    }

    // =========================================================================
    // SPECIAL CASES: EXACT BOUNDARIES, UTF-8, XSS, SQL INJECTION STRINGS
    // =========================================================================

    /**
     * Verifies that fields with exact boundary length of 255 characters pass validation successfully.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Special case: forename and surname of exactly 255 characters should pass validation")
    public void testRegister_Exact255Characters_Success() throws Exception
    {
        final String exact255 = "A".repeat(255);

        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", exact255)
               .param("surname", exact255)
               .param("mail", "exact255@xcmailr.test")
               .param("password", "SecureSecretPassword123!")
               .param("confirmPassword", "SecureSecretPassword123!"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?registered"));

        final User user = userRepository.findByMailIgnoreCase("exact255@xcmailr.test").orElseThrow();
        assertEquals(255, user.getForename().length());
        assertEquals(255, user.getSurname().length());
    }

    /**
     * Verifies that multi-byte UTF-8 international characters (umlauts, accents, ligatures) are accepted cleanly.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Special case: multi-byte UTF-8 international names should pass validation")
    public void testRegister_Utf8MultibyteCharacters_Success() throws Exception
    {
        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", "Jörg-René")
               .param("surname", "Müller-Groß")
               .param("mail", "joerg.mueller@xcmailr.test")
               .param("password", "SecureSecretPassword123!")
               .param("confirmPassword", "SecureSecretPassword123!"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?registered"));

        final User user = userRepository.findByMailIgnoreCase("joerg.mueller@xcmailr.test").orElseThrow();
        assertEquals("Jörg-René", user.getForename());
        assertEquals("Müller-Groß", user.getSurname());
    }

    /**
     * Verifies that script/HTML injection payloads in input fields are bounded and stored cleanly as plain text.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Special case: XSS/HTML payload in name fields should be treated as text without crashing")
    public void testRegister_XssPayloadInName_StoredSafelyAsPlainText() throws Exception
    {
        final String xssPayload = "<script>alert('xss')</script>";

        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", xssPayload)
               .param("surname", "Tester")
               .param("mail", "xss.tester@xcmailr.test")
               .param("password", "SecureSecretPassword123!")
               .param("confirmPassword", "SecureSecretPassword123!"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?registered"));

        final User user = userRepository.findByMailIgnoreCase("xss.tester@xcmailr.test").orElseThrow();
        assertEquals(xssPayload, user.getForename());
    }

    /**
     * Verifies that SQL control characters in input fields are handled safely by prepared statements.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Special case: SQL control characters in name fields should be treated as text without crashing")
    public void testRegister_SqlInjectionPayloadInName_StoredSafelyAsPlainText() throws Exception
    {
        final String sqlPayload = "O'Connor' OR 1=1;--";

        mockMvc.perform(post("/register")
               .with(csrf())
               .param("forename", sqlPayload)
               .param("surname", "Tester")
               .param("mail", "sql.tester@xcmailr.test")
               .param("password", "SecureSecretPassword123!")
               .param("confirmPassword", "SecureSecretPassword123!"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?registered"));

        final User user = userRepository.findByMailIgnoreCase("sql.tester@xcmailr.test").orElseThrow();
        assertEquals(sqlPayload, user.getForename());
    }
}
