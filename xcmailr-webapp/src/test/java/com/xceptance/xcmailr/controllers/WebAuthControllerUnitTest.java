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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import com.xceptance.xcmailr.config.XcmailrProperties;
import com.xceptance.xcmailr.repositories.UserRepository;
import com.xceptance.xcmailr.security.PasswordValidationResult;
import com.xceptance.xcmailr.security.PasswordValidator;
import com.xceptance.xcmailr.services.OutboundMailService;

import models.User;

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

        final String view = controller.registerSubmit("Jane", "Doe", "jane@example.test", "Secret12345!", "Different!", "de", model);

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

        final String view = controller.registerSubmit("Jane", "Doe", "jane@example.test", "short", "short", "de", model);

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

        final String view = controller.registerSubmit("Jane", "Doe", "jane@example.test", "ValidSecret123!", "ValidSecret123!", "de", model);

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
}

