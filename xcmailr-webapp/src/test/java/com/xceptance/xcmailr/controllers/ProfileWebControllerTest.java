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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import com.xceptance.xcmailr.security.UserPrincipal;

import models.User;

/**
 * Controller-tier integration tests verifying localized HTML rendering for user profile views,
 * personal info updates, API token management, danger zone prompts, account deletion confirmation,
 * and OWASP XSS prevention.
 *
 * Testing pyramid coverage:
 * - Happy path: English and German rendering for profile, token states, and modal dialogs.
 * - Error cases: Localized status banners and feedback messages.
 * - Special cases: Runtime language switching and OWASP escaping of user data.
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public class ProfileWebControllerTest
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
    @DisplayName("GET /profile for English user renders English labels, token info, and delete modal")
    public void testProfilePageEnglish() throws Exception
    {
        final User userEn = new User("Alice", "Tester", "alice@example.com", this.passwordEncoder.encode("secretPassword123!"), "en");
        userEn.setActive(true);
        userEn.setApiToken("test-token-12345");
        final User savedUser = this.userRepository.save(userEn);
        final UserPrincipal principal = new UserPrincipal(savedUser);

        this.mockMvc.perform(get("/profile").with(user(principal)))
            .andExpect(status().isOk())
            .andExpect(view().name("user/profile"))
            // Header
            .andExpect(content().string(containsString("Account Profile &amp; API Tokens")))
            // Personal Information
            .andExpect(content().string(containsString("Personal Information")))
            .andExpect(content().string(containsString("Target Forwarding Email")))
            .andExpect(content().string(containsString("Forwarding email address cannot be changed.")))
            .andExpect(content().string(containsString("First Name")))
            .andExpect(content().string(containsString("Last Name")))
            .andExpect(content().string(containsString("Language")))
            .andExpect(content().string(containsString("Save Changes")))
            // Change Password
            .andExpect(content().string(containsString("Change Password")))
            .andExpect(content().string(containsString("Current Password")))
            .andExpect(content().string(containsString("New Password")))
            .andExpect(content().string(containsString("Confirm New Password")))
            .andExpect(content().string(containsString("Update Password")))
            // REST API Token with active token
            .andExpect(content().string(containsString("REST API Token")))
            .andExpect(content().string(containsString("Active Token")))
            .andExpect(content().string(containsString("Copy")))
            .andExpect(content().string(containsString("Regenerate Token")))
            .andExpect(content().string(containsString("Revoke Token")))
            // Danger Zone & Modal
            .andExpect(content().string(containsString("Danger Zone")))
            .andExpect(content().string(containsString("Delete Account")))
            .andExpect(content().string(containsString("Delete Account Confirmation")))
            .andExpect(content().string(containsString("Permanently Delete Account")));
    }

    @Test
    @DisplayName("GET /profile for German user renders German labels, token info, and delete modal")
    public void testProfilePageGerman() throws Exception
    {
        final User userDe = new User("Hans", "Muster", "hans@beispiel.de", this.passwordEncoder.encode("secretPassword123!"), "de");
        userDe.setActive(true);
        userDe.setApiToken("test-token-de-67890");
        final User savedUser = this.userRepository.save(userDe);
        final UserPrincipal principal = new UserPrincipal(savedUser);

        this.mockMvc.perform(get("/profile").with(user(principal)))
            .andExpect(status().isOk())
            .andExpect(view().name("user/profile"))
            // Header
            .andExpect(content().string(containsString("Kontoprofil &amp; API-Token")))
            // Personal Information
            .andExpect(content().string(containsString("Persönliche Daten")))
            .andExpect(content().string(containsString("Weiterleitungs-Zieladresse")))
            .andExpect(content().string(containsString("Die Weiterleitungs-E-Mail-Adresse kann nicht geändert werden.")))
            .andExpect(content().string(containsString("Vorname")))
            .andExpect(content().string(containsString("Nachname")))
            .andExpect(content().string(containsString("Sprache")))
            .andExpect(content().string(containsString("Änderungen speichern")))
            // Change Password
            .andExpect(content().string(containsString("Passwort ändern")))
            .andExpect(content().string(containsString("Aktuelles Passwort")))
            .andExpect(content().string(containsString("Neues Passwort")))
            .andExpect(content().string(containsString("Neues Passwort bestätigen")))
            .andExpect(content().string(containsString("Passwort aktualisieren")))
            // REST API Token with active token
            .andExpect(content().string(containsString("REST-API-Token")))
            .andExpect(content().string(containsString("Aktiver Token")))
            .andExpect(content().string(containsString("Kopieren")))
            .andExpect(content().string(containsString("Token neu generieren")))
            .andExpect(content().string(containsString("Token widerrufen")))
            // Danger Zone & Modal
            .andExpect(content().string(containsString("Gefahrenbereich")))
            .andExpect(content().string(containsString("Konto löschen")))
            .andExpect(content().string(containsString("Bestätigung der Kontolöschung")))
            .andExpect(content().string(containsString("Konto dauerhaft löschen")));
    }

    @Test
    @DisplayName("GET /profile without active API token renders localized empty-state CTA")
    public void testProfileWithoutToken() throws Exception
    {
        final User userNoToken = new User("Bob", "Builder", "bob@example.com", this.passwordEncoder.encode("secretPassword123!"), "de");
        userNoToken.setActive(true);
        userNoToken.setApiToken(null);
        final User savedUser = this.userRepository.save(userNoToken);
        final UserPrincipal principal = new UserPrincipal(savedUser);

        this.mockMvc.perform(get("/profile").with(user(principal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Für Ihr Konto existiert kein aktiver API-Token.")))
            .andExpect(content().string(containsString("API-Token generieren")));
    }

    // =========================================================================
    // 2. Status Banners & Feedback Alerts
    // =========================================================================

    @Test
    @DisplayName("GET /profile with status banner query parameters renders German alert messages")
    public void testProfileAlertBannersGerman() throws Exception
    {
        final User userDe = new User("Anna", "Schmidt", "anna@beispiel.de", this.passwordEncoder.encode("secretPassword123!"), "de");
        userDe.setActive(true);
        final User savedUser = this.userRepository.save(userDe);
        final UserPrincipal principal = new UserPrincipal(savedUser);

        this.mockMvc.perform(get("/profile").param("updated", "").with(user(principal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Profildetails erfolgreich aktualisiert.")));

        this.mockMvc.perform(get("/profile").param("passwordChanged", "").with(user(principal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Ihr Passwort wurde erfolgreich geändert.")));

        this.mockMvc.perform(get("/profile").param("tokenCreated", "").with(user(principal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Neuer API-Token erfolgreich generiert.")));

        this.mockMvc.perform(get("/profile").param("tokenRevoked", "").with(user(principal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("API-Token wurde widerrufen.")));
    }

    // =========================================================================
    // 3. Special Cases: Language Switch & OWASP XSS Prevention
    // =========================================================================

    @Test
    @DisplayName("POST /profile/update switches user language preference from English to German")
    public void testProfileLanguageSwitch() throws Exception
    {
        final User userSwitch = new User("Clara", "Switch", "clara@example.com", this.passwordEncoder.encode("secretPassword123!"), "en");
        userSwitch.setActive(true);
        final User savedUser = this.userRepository.save(userSwitch);
        final UserPrincipal principal = new UserPrincipal(savedUser);

        // Update language from en to de
        this.mockMvc.perform(post("/profile/update")
            .with(csrf())
            .with(user(principal))
            .param("forename", "Clara")
            .param("surname", "Switch")
            .param("language", "de"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/profile?updated"));

        // Subsequent GET with updated principal should resolve German
        final User updatedUser = this.userRepository.findByMailIgnoreCase("clara@example.com").orElseThrow();
        final UserPrincipal updatedPrincipal = new UserPrincipal(updatedUser);

        this.mockMvc.perform(get("/profile").with(user(updatedPrincipal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Persönliche Daten")))
            .andExpect(content().string(containsString("Kontoprofil &amp; API-Token")));
    }

    @Test
    @DisplayName("OWASP check: Special HTML characters in user profile name are escaped safely")
    public void testProfileNameXssEscaping() throws Exception
    {
        final String maliciousName = "<script>alert('xss')</script>";
        final User xssUser = new User(maliciousName, "SafeSurname", "xss@example.com", this.passwordEncoder.encode("secretPassword123!"), "en");
        xssUser.setActive(true);
        final User savedUser = this.userRepository.save(xssUser);
        final UserPrincipal principal = new UserPrincipal(savedUser);

        this.mockMvc.perform(get("/profile").with(user(principal)))
            .andExpect(status().isOk())
            // Malicious script payload must never appear unescaped
            .andExpect(content().string(not(containsString(maliciousName))))
            .andExpect(content().string(containsString("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;")));
    }
}

