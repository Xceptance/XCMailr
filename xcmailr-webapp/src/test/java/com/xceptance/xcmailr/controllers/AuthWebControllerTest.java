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
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import com.xceptance.xcmailr.repositories.UserRepository;

import models.User;

/**
 * Integration and web-tier tests verifying comprehensive internationalization (English and German)
 * across authentication views (login, registration, forgot-password, and reset-password) as well
 * as flash banners, status alerts, locale fallback, and OWASP XSS prevention.
 *
 * Following the testing pyramid:
 * - Happy path: English and German rendering for all auth views.
 * - Error cases: Localized error banners, token expiration, and invalid credentials.
 * - Special cases: Unsupported locale fallback and OWASP XSS sanitization.
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public class AuthWebControllerTest
{
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup()
    {
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(this.context)
            .apply(springSecurity())
            .build();

        this.userRepository.deleteAll();
    }

    // =========================================================================
    // 1. Happy Path Localization Tests (English & German)
    // =========================================================================

    @Test
    @DisplayName("GET /login with English locale renders English strings, navbar, and footer")
    public void testLoginPageEnglish() throws Exception
    {
        final Locale targetLocale = Locale.ENGLISH;

        this.mockMvc.perform(get("/login").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/login"))
            // Page header and subtitle
            .andExpect(content().string(containsString("Sign In")))
            .andExpect(content().string(containsString("Access your temporary and disposable mailboxes")))
            // Form labels and placeholders
            .andExpect(content().string(containsString("Email address")))
            .andExpect(content().string(containsString("name@example.com")))
            .andExpect(content().string(containsString("Password")))
            .andExpect(content().string(containsString("Forgot password?")))
            .andExpect(content().string(containsString("Log In")))
            // Registration callout
            .andExpect(content().string(containsString("have an account?")))
            .andExpect(content().string(containsString("Register here")))
            // Common layout elements
            .andExpect(content().string(containsString("All rights reserved.")))
            .andExpect(content().string(containsString("XCMailr Modernized")));
    }

    @Test
    @DisplayName("GET /login with German locale renders German strings, navbar, and footer")
    public void testLoginPageGerman() throws Exception
    {
        final Locale targetLocale = Locale.GERMAN;

        this.mockMvc.perform(get("/login").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/login"))
            // Page header and subtitle
            .andExpect(content().string(containsString("Anmelden")))
            .andExpect(content().string(containsString("Greifen Sie auf Ihre temporären und Einweg-Postfächer zu")))
            // Form labels and placeholders
            .andExpect(content().string(containsString("E-Mail-Adresse")))
            .andExpect(content().string(containsString("name@beispiel.de")))
            .andExpect(content().string(containsString("Passwort")))
            .andExpect(content().string(containsString("Passwort vergessen?")))
            // Registration callout
            .andExpect(content().string(containsString("Noch kein Konto?")))
            .andExpect(content().string(containsString("Hier registrieren")))
            // Common layout elements
            .andExpect(content().string(containsString("Alle Rechte vorbehalten.")))
            .andExpect(content().string(containsString("XCMailr Modernisiert")));
    }

    @Test
    @DisplayName("GET /register with English locale renders English labels and requirements")
    public void testRegisterPageEnglish() throws Exception
    {
        final Locale targetLocale = Locale.ENGLISH;

        this.mockMvc.perform(get("/register").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/register"))
            .andExpect(content().string(containsString("Create Account")))
            .andExpect(content().string(containsString("Register for free to create and manage test mailboxes")))
            .andExpect(content().string(containsString("First Name")))
            .andExpect(content().string(containsString("Last Name")))
            .andExpect(content().string(containsString("Target Email Address")))
            .andExpect(content().string(containsString("Password requirements:")))
            .andExpect(content().string(containsString("At least 10 characters (up to 72)")))
            .andExpect(content().string(containsString("Confirm Password")))
            .andExpect(content().string(containsString("Language")))
            .andExpect(content().string(containsString("Register Account")))
            .andExpect(content().string(containsString("Already have an account?")))
            .andExpect(content().string(containsString("Log in here")));
    }

    @Test
    @DisplayName("GET /register with German locale renders German labels and requirements")
    public void testRegisterPageGerman() throws Exception
    {
        final Locale targetLocale = Locale.GERMAN;

        this.mockMvc.perform(get("/register").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/register"))
            .andExpect(content().string(containsString("Konto erstellen")))
            .andExpect(content().string(containsString("Kostenlos registrieren, um Test-Postfächer zu erstellen und zu verwalten")))
            .andExpect(content().string(containsString("Vorname")))
            .andExpect(content().string(containsString("Nachname")))
            .andExpect(content().string(containsString("Ziel-E-Mail-Adresse")))
            .andExpect(content().string(containsString("Passwort-Anforderungen:")))
            .andExpect(content().string(containsString("Mindestens 10 Zeichen (bis zu 72)")))
            .andExpect(content().string(containsString("Passwort bestätigen")))
            .andExpect(content().string(containsString("Sprache")))
            .andExpect(content().string(containsString("Konto registrieren")))
            .andExpect(content().string(containsString("Bereits ein Konto?")))
            .andExpect(content().string(containsString("Hier anmelden")));
    }

    @Test
    @DisplayName("GET /forgot-password with English locale renders English recovery text")
    public void testForgotPasswordPageEnglish() throws Exception
    {
        final Locale targetLocale = Locale.ENGLISH;

        this.mockMvc.perform(get("/forgot-password").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/forgot-password"))
            .andExpect(content().string(containsString("Password Recovery")))
            .andExpect(content().string(containsString("Enter your account email to receive a password reset link")))
            .andExpect(content().string(containsString("Send Reset Link")))
            .andExpect(content().string(containsString("Back to login")));
    }

    @Test
    @DisplayName("GET /forgot-password with German locale renders German recovery text")
    public void testForgotPasswordPageGerman() throws Exception
    {
        final Locale targetLocale = Locale.GERMAN;

        this.mockMvc.perform(get("/forgot-password").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/forgot-password"))
            .andExpect(content().string(containsString("Passwort-Wiederherstellung")))
            .andExpect(content().string(containsString("Geben Sie Ihre Konto-E-Mail ein, um einen Link zum Zurücksetzen des Passworts zu erhalten")))
            .andExpect(content().string(containsString("Link zum Zurücksetzen senden")))
            .andExpect(content().string(containsString("Zurück zur Anmeldung")));
    }

    @Test
    @DisplayName("GET /pwreset/{token} with valid token in English renders English password reset form")
    public void testResetPasswordPageEnglish() throws Exception
    {
        final String rawToken = "reset-token-en-test";
        final User user = new User("John", "Doe", "john.doe.reset@xcmailr.test", this.passwordEncoder.encode("oldSecret123"), "en");
        user.setActive(true);
        user.setConfirmation(rawToken);
        user.setTs_confirm(System.currentTimeMillis() + 86_400_000L);
        this.userRepository.save(user);

        final Locale targetLocale = Locale.ENGLISH;

        this.mockMvc.perform(get("/pwreset/" + rawToken).locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/reset-password"))
            .andExpect(content().string(containsString("Set New Password")))
            .andExpect(content().string(containsString("Enter a new secure password for your account")))
            .andExpect(content().string(containsString("New Password")))
            .andExpect(content().string(containsString("Confirm New Password")))
            .andExpect(content().string(containsString("Update Password")));
    }

    @Test
    @DisplayName("GET /pwreset/{token} with valid token in German renders German password reset form")
    public void testResetPasswordPageGerman() throws Exception
    {
        final String rawToken = "reset-token-de-test";
        final User user = new User("Hans", "Muster", "hans.muster.reset@xcmailr.test", this.passwordEncoder.encode("oldSecret123"), "de");
        user.setActive(true);
        user.setConfirmation(rawToken);
        user.setTs_confirm(System.currentTimeMillis() + 86_400_000L);
        this.userRepository.save(user);

        final Locale targetLocale = Locale.GERMAN;

        this.mockMvc.perform(get("/pwreset/" + rawToken).locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/reset-password"))
            .andExpect(content().string(containsString("Neues Passwort festlegen")))
            .andExpect(content().string(containsString("Geben Sie ein neues sicheres Passwort für Ihr Konto ein")))
            .andExpect(content().string(containsString("Neues Passwort")))
            .andExpect(content().string(containsString("Neues Passwort bestätigen")))
            .andExpect(content().string(containsString("Passwort aktualisieren")));
    }

    // =========================================================================
    // 2. Error Cases & Status Feedback Banners
    // =========================================================================

    @Test
    @DisplayName("GET /login with status banner parameters renders localized German alerts")
    public void testLoginStatusBannersGerman() throws Exception
    {
        final Locale targetLocale = Locale.GERMAN;

        // Login failed error banner
        this.mockMvc.perform(get("/login").param("error", "").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Ungültige E-Mail-Adresse oder Passwort. Bitte versuchen Sie es erneut.")));

        // Logout banner
        this.mockMvc.perform(get("/login").param("logout", "").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Sie wurden erfolgreich abgemeldet.")));

        // Account deleted banner
        this.mockMvc.perform(get("/login").param("accountDeleted", "").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Ihr Konto und alle zugehörigen Postfächer und E-Mails wurden erfolgreich gelöscht.")));

        // Registered activation required banner
        this.mockMvc.perform(get("/login").param("registered", "").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Registrierung erfolgreich! Bitte überprüfen Sie Ihre E-Mails")));

        // Ready (direct login permitted) banner
        this.mockMvc.perform(get("/login").param("ready", "").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Registrierung erfolgreich! Sie können sich jetzt anmelden.")));

        // Invalid token banner
        this.mockMvc.perform(get("/login").param("invalidToken", "").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Der Bestätigungs- oder Rücksetzlink ist ungültig oder wurde bereits verwendet.")));

        // Expired token banner
        this.mockMvc.perform(get("/login").param("expiredToken", "").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Der Bestätigungslink ist abgelaufen. Bitte registrieren Sie sich erneut")));
    }

    @Test
    @DisplayName("GET /login with status banner parameters renders localized English alerts")
    public void testLoginStatusBannersEnglish() throws Exception
    {
        final Locale targetLocale = Locale.ENGLISH;

        // Login error banner
        this.mockMvc.perform(get("/login").param("error", "").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Invalid email address or password. Please try again.")));

        // Logout banner
        this.mockMvc.perform(get("/login").param("logout", "").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("You have been logged out successfully.")));

        // Account deleted banner
        this.mockMvc.perform(get("/login").param("accountDeleted", "").locale(targetLocale))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Your account and all associated mailboxes and emails have been successfully deleted.")));
    }

    // =========================================================================
    // 3. Special Cases: Unsupported Locale Fallback & OWASP Security
    // =========================================================================

    @Test
    @DisplayName("GET /login with unsupported locale (e.g. French) gracefully falls back to default English")
    public void testLoginPageUnsupportedLocaleFallback() throws Exception
    {
        final Locale unsupportedLocale = Locale.FRENCH;

        this.mockMvc.perform(get("/login").locale(unsupportedLocale))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/login"))
            // Must fall back to default English message bundle
            .andExpect(content().string(containsString("Sign In")))
            .andExpect(content().string(containsString("Access your temporary and disposable mailboxes")))
            .andExpect(content().string(containsString("Email address")))
            .andExpect(content().string(containsString("Log In")));
    }

    @Test
    @DisplayName("OWASP check: Injected XSS script payload in parameters is safely escaped in rendered HTML")
    public void testSecurityXssInFeedbackBanners() throws Exception
    {
        final String maliciousPayload = "<script>alert('xss')</script>";

        this.mockMvc.perform(get("/login").param("error", maliciousPayload).locale(Locale.ENGLISH))
            .andExpect(status().isOk())
            // Ensure raw executable script tags are never present in the response body
            .andExpect(content().string(not(containsString(maliciousPayload))))
            .andExpect(content().string(containsString("Invalid email address or password.")));
    }
}