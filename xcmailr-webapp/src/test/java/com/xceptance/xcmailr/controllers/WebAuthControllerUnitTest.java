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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import com.xceptance.xcmailr.config.XcmailrProperties;
import com.xceptance.xcmailr.dto.RegistrationForm;
import com.xceptance.xcmailr.repositories.UserRepository;
import com.xceptance.xcmailr.security.PasswordValidationResult;
import com.xceptance.xcmailr.security.PasswordValidator;
import com.xceptance.xcmailr.services.OutboundMailService;

import com.xceptance.xcmailr.models.User;

/**
 * Isolated unit tests for {@link WebAuthController} verifying template message key setting,
 * language retention, and absence of Locale parameters in controller method signatures.
 */
public class WebAuthControllerUnitTest
{
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private OutboundMailService outboundMailService;
    private XcmailrProperties properties;
    private PasswordValidator passwordValidator;
    private WebAuthController controller;

    @BeforeEach
    public void setUp()
    {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        outboundMailService = mock(OutboundMailService.class);
        properties = new XcmailrProperties();
        passwordValidator = mock(PasswordValidator.class);

        controller = new WebAuthController(userRepository, passwordEncoder, outboundMailService, properties, passwordValidator);
    }

    @Test
    @DisplayName("registerSubmit with mismatched passwords sets errorMessageKey and retains language in model")
    public void testRegisterSubmitPasswordMismatchSetsErrorMessageKeyAndLanguage()
    {
        final Model model = new ConcurrentModel();
        final RegistrationForm form = new RegistrationForm();
        form.setForename("Jane");
        form.setSurname("Doe");
        form.setMail("jane@example.test");
        form.setPassword("Secret12345!");
        form.setConfirmPassword("Different!");
        form.setLanguage("de");
        final BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        final String view = controller.registerSubmit(form, bindingResult, model);

        assertEquals("auth/register", view);
        assertEquals("auth.error.register.passwordMismatch", model.getAttribute("errorMessageKey"));
        assertEquals("de", model.getAttribute("language"));
        assertEquals("Jane", model.getAttribute("forename"));
        assertEquals("Doe", model.getAttribute("surname"));
        assertEquals("jane@example.test", model.getAttribute("mail"));
    }

    @Test
    @DisplayName("registerSubmit with validation failure sets errorMessageKey from validation result and retains language")
    public void testRegisterSubmitValidationFailureSetsErrorMessageKeyAndLanguage()
    {
        final Model model = new ConcurrentModel();
        when(passwordValidator.validate(anyString(), any())).thenReturn(PasswordValidationResult.invalid("Too short", "auth.password.error.minLength"));

        final RegistrationForm form = new RegistrationForm();
        form.setForename("Jane");
        form.setSurname("Doe");
        form.setMail("jane@example.test");
        form.setPassword("short");
        form.setConfirmPassword("short");
        form.setLanguage("de");
        final BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        final String view = controller.registerSubmit(form, bindingResult, model);

        assertEquals("auth/register", view);
        assertEquals("auth.password.error.minLength", model.getAttribute("errorMessageKey"));
        assertEquals("de", model.getAttribute("language"));
    }

    @Test
    @DisplayName("registerSubmit with existing email sets errorMessageKey and retains language")
    public void testRegisterSubmitExistingEmailSetsErrorMessageKeyAndLanguage()
    {
        final Model model = new ConcurrentModel();
        when(passwordValidator.validate(anyString(), any())).thenReturn(PasswordValidationResult.valid());
        when(userRepository.existsByMailIgnoreCase("jane@example.test")).thenReturn(true);

        final RegistrationForm form = new RegistrationForm();
        form.setForename("Jane");
        form.setSurname("Doe");
        form.setMail("jane@example.test");
        form.setPassword("ValidSecret123!");
        form.setConfirmPassword("ValidSecret123!");
        form.setLanguage("de");
        final BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        final String view = controller.registerSubmit(form, bindingResult, model);

        assertEquals("auth/register", view);
        assertEquals("auth.error.register.emailExists", model.getAttribute("errorMessageKey"));
        assertEquals("de", model.getAttribute("language"));
    }

    @Test
    @DisplayName("resetPasswordSubmit with mismatched passwords sets errorMessageKey and token in model")
    public void testResetPasswordSubmitPasswordMismatchSetsErrorMessageKey()
    {
        final Model model = new ConcurrentModel();
        final User user = new User("Jane", "Doe", "jane@example.test", "oldPass", "en");
        when(userRepository.findByConfirmation("valid-token")).thenReturn(Optional.of(user));

        final String view = controller.resetPasswordSubmit("valid-token", "Secret12345!", "Mismatch!", model);

        assertEquals("auth/reset-password", view);
        assertEquals("auth.error.resetPassword.passwordMismatch", model.getAttribute("errorMessageKey"));
        assertEquals("valid-token", model.getAttribute("token"));
    }

    @Test
    @DisplayName("resetPasswordSubmit with validation failure sets errorMessageKey from validation result")
    public void testResetPasswordSubmitValidationFailureSetsErrorMessageKey()
    {
        final Model model = new ConcurrentModel();
        final User user = new User("Jane", "Doe", "jane@example.test", "oldPass", "en");
        when(userRepository.findByConfirmation("valid-token")).thenReturn(Optional.of(user));
        when(passwordValidator.validate(anyString(), any())).thenReturn(PasswordValidationResult.invalid("Too short", "auth.password.error.minLength"));

        final String view = controller.resetPasswordSubmit("valid-token", "short", "short", model);

        assertEquals("auth/reset-password", view);
        assertEquals("auth.password.error.minLength", model.getAttribute("errorMessageKey"));
        assertEquals("valid-token", model.getAttribute("token"));
    }

    /**
     * Verifies the happy path for {@link WebAuthController#forgotPasswordSubmit(String)}:
     * an active user with uppercase/whitespace-padded email input receives a newly generated
     * confirmation token, a future expiration timestamp, repository persistence, and an
     * outbound password-reset email.
     */
    @Test
    @DisplayName("Happy Path: forgotPasswordSubmit for active user generates token, saves user, and sends email")
    public void testForgotPasswordSubmitActiveUserGeneratesTokenSavesAndSendsEmail()
    {
        final User activeUser = new User("Active", "User", "active@example.test", "encodedPass", "en");
        activeUser.setActive(true);
        when(userRepository.findByMailIgnoreCase("active@example.test")).thenReturn(Optional.of(activeUser));

        final long before = System.currentTimeMillis();
        final String view = controller.forgotPasswordSubmit("  ACTIVE@Example.test  ");

        assertEquals("redirect:/login?resetRequested", view);
        assertNotNull(activeUser.getConfirmation(), "Reset confirmation token must be generated for active user");
        assertNotNull(activeUser.getTs_confirm(), "Reset confirmation timestamp must be set for active user");
        assertTrue(activeUser.getTs_confirm() > before, "Reset expiration timestamp must be in the future");
        verify(userRepository).save(activeUser);
        verify(outboundMailService).sendAsync(eq("active@example.test"), anyString(), anyString());
    }

    /**
     * Verifies the security and data-integrity safeguard for {@link WebAuthController#forgotPasswordSubmit(String)}:
     * submitting a password-reset request for an inactive or administrator-deactivated user must NOT
     * generate or overwrite {@code confirmation} or {@code ts_confirm}, must NOT persist the user,
     * and must NOT dispatch an email, while still returning the identical redirect to prevent enumeration.
     */
    @Test
    @DisplayName("Special Case: forgotPasswordSubmit for inactive user does not generate token or send email")
    public void testForgotPasswordSubmitInactiveUserDoesNotGenerateTokenOrSendEmail()
    {
        final User deactivatedUser = new User("Deactivated", "User", "deactivated@example.test", "encodedPass", "en");
        deactivatedUser.setActive(false);
        deactivatedUser.setConfirmation(null);
        deactivatedUser.setTs_confirm(null);
        when(userRepository.findByMailIgnoreCase("deactivated@example.test")).thenReturn(Optional.of(deactivatedUser));

        final String view = controller.forgotPasswordSubmit("deactivated@example.test");

        assertEquals("redirect:/login?resetRequested", view,
                     "Must return identical redirect for CWE-204 anti-enumeration");
        assertNull(deactivatedUser.getConfirmation(),
                   "Inactive user confirmation token must remain null so account cannot be marked for purge or self-reactivated");
        assertNull(deactivatedUser.getTs_confirm(),
                   "Inactive user confirmation timestamp must remain null");
        verify(userRepository, never()).save(any(User.class));
        verify(outboundMailService, never()).sendAsync(anyString(), anyString(), anyString());
    }

    /**
     * Verifies the error/unknown-account case for {@link WebAuthController#forgotPasswordSubmit(String)}:
     * submitting a non-existent email address returns the standard redirect without side effects.
     */
    @Test
    @DisplayName("Error Case: forgotPasswordSubmit for non-existent email returns redirect without saving or sending email")
    public void testForgotPasswordSubmitUnknownEmailReturnsRedirectWithoutSideEffects()
    {
        when(userRepository.findByMailIgnoreCase("unknown@example.test")).thenReturn(Optional.empty());

        final String view = controller.forgotPasswordSubmit("unknown@example.test");

        assertEquals("redirect:/login?resetRequested", view);
        verify(userRepository, never()).save(any(User.class));
        verify(outboundMailService, never()).sendAsync(anyString(), anyString(), anyString());
    }
}

