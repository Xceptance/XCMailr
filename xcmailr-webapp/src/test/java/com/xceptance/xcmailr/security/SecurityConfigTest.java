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
package com.xceptance.xcmailr.security;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.repositories.UserRepository;

import models.User;

/**
 * Tests verifying the Spring Security configuration:
 * <ul>
 *   <li>BCrypt password compatibility with existing legacy hashes</li>
 *   <li>REST API authentication enforcement (401 on missing/invalid token)</li>
 *   <li>REST API successful authentication with valid Bearer token</li>
 *   <li>Permitted public endpoints</li>
 * </ul>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SecurityConfigTest
{
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setUp()
    {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                                 .apply(springSecurity())
                                 .build();
    }

    @Test
    @DisplayName("Verify BCryptPasswordEncoder compatibility with existing legacy hashes")
    public void testBCryptCompatibility()
    {
        final String rawPassword = "mySecretPassword123!";

        // Generate hash using legacy org.mindrot.jbcrypt.BCrypt
        final String legacyHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));

        // Verify that Spring Security's PasswordEncoder matches hashes produced by legacy jbcrypt
        assertTrue(passwordEncoder.matches(rawPassword, legacyHash),
                   "Spring Security PasswordEncoder must match hashes produced by legacy jbcrypt");

        // Verify encoding a new password works symmetrically
        final String newHash = passwordEncoder.encode(rawPassword);
        assertTrue(passwordEncoder.matches(rawPassword, newHash));
    }

    @Test
    @DisplayName("Verify /api/** rejects requests with missing or invalid token with 401 Unauthorized")
    public void testApiUnauthorizedWithoutToken() throws Exception
    {
        // Missing Authorization header
        mockMvc.perform(get("/api/v1/mailboxes"))
               .andExpect(status().isUnauthorized());

        // Invalid Bearer token
        mockMvc.perform(get("/api/v1/mailboxes")
               .header("Authorization", "Bearer invalid-token-xyz"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Verify /api/** allows access with valid Bearer token for active user")
    public void testApiAuthorizedWithValidToken() throws Exception
    {
        final User user = new User("Valid", "User", "valid.user@xcmailr.test", "hash123", "en");
        user.setActive(true);
        user.setApiToken("secret-api-token-999");
        userRepository.save(user);

        // When valid token is provided, request passes security (returns 404 because controller isn't implemented yet, but NOT 401)
        mockMvc.perform(get("/api/v1/mailboxes")
               .header("Authorization", "Bearer secret-api-token-999"))
               .andExpect(result -> {
                   final int status = result.getResponse().getStatus();
                   assertTrue(status != 401, "Expected status other than 401 Unauthorized, got: " + status);
               });
    }

    @Test
    @DisplayName("Verify public web endpoints are accessible without authentication")
    public void testPublicEndpointsPermitted() throws Exception
    {
        mockMvc.perform(get("/login"))
               .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verify mutating REST API requests (POST, PUT, DELETE) with Bearer token succeed without CSRF token")
    public void testApiMutatingRequestsExemptFromCsrf() throws Exception
    {
        final User user = new User("ApiHappy", "UserHappy", "api.happy@xcmailr.test", "hash123", "en");
        user.setActive(true);
        user.setApiToken("api-happy-token-111");
        userRepository.save(user);

        // POST /api/v1/mailboxes without CSRF token
        mockMvc.perform(post("/api/v1/mailboxes")
               .header("Authorization", "Bearer api-happy-token-111")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{\"address\":\"api-test@xcmailr.test\"}"))
               .andExpect(result -> {
                   final int status = result.getResponse().getStatus();
                   assertNotEquals(403, status, "Mutating API request must NOT be rejected with 403 Forbidden due to CSRF");
               });

        // PUT /api/v1/mailboxes/1 without CSRF token
        mockMvc.perform(put("/api/v1/mailboxes/1")
               .header("Authorization", "Bearer api-happy-token-111")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{\"active\":true}"))
               .andExpect(result -> {
                   final int status = result.getResponse().getStatus();
                   assertNotEquals(403, status, "Mutating API request must NOT be rejected with 403 Forbidden due to CSRF");
               });

        // DELETE /api/v1/mailboxes/1 without CSRF token
        mockMvc.perform(delete("/api/v1/mailboxes/1")
               .header("Authorization", "Bearer api-happy-token-111"))
               .andExpect(result -> {
                   final int status = result.getResponse().getStatus();
                   assertNotEquals(403, status, "Mutating API request must NOT be rejected with 403 Forbidden due to CSRF");
               });
    }

    @Test
    @DisplayName("Verify web POST requests with valid CSRF token pass CSRF filter")
    public void testWebPostWithValidCsrf() throws Exception
    {
        // When submitting form with valid CSRF token, request passes CSRF check (redirects on auth failure, not 403)
        mockMvc.perform(post("/login")
               .with(csrf())
               .param("mail", "someone@xcmailr.test")
               .param("password", "invalidpassword"))
               .andExpect(result -> {
                   final int status = result.getResponse().getStatus();
                   assertNotEquals(403, status, "Web request with valid CSRF token must NOT return 403 Forbidden");
               });
    }

    @Test
    @DisplayName("Verify web POST requests missing CSRF token are rejected with 403 Forbidden")
    public void testWebPostMissingCsrfRejected() throws Exception
    {
        mockMvc.perform(post("/login")
               .param("mail", "someone@xcmailr.test")
               .param("password", "invalidpassword"))
               .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify web POST requests with invalid CSRF token are rejected with 403 Forbidden")
    public void testWebPostInvalidCsrfRejected() throws Exception
    {
        mockMvc.perform(post("/login")
               .with(csrf().useInvalidToken())
               .param("mail", "someone@xcmailr.test")
               .param("password", "invalidpassword"))
               .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify mutating API requests without Bearer token are rejected with 401 Unauthorized")
    public void testApiMutatingRequestUnauthorizedWithoutToken() throws Exception
    {
        // Missing Authorization header
        mockMvc.perform(post("/api/v1/mailboxes")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{\"address\":\"test@xcmailr.test\"}"))
               .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/mailboxes/1")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{\"active\":true}"))
               .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/mailboxes/1"))
               .andExpect(status().isUnauthorized());

        // Invalid Bearer token
        mockMvc.perform(post("/api/v1/mailboxes")
               .header("Authorization", "Bearer invalid-token-xyz")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{\"address\":\"test@xcmailr.test\"}"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Verify API endpoints ignore spurious or malformed CSRF headers when valid Bearer token is provided")
    public void testApiIgnoresMalformedCsrfHeader() throws Exception
    {
        final User user = new User("ApiSpecial", "UserSpecial", "api.special@xcmailr.test", "hash123", "en");
        user.setActive(true);
        user.setApiToken("api-special-token-888");
        userRepository.save(user);

        // Sending invalid CSRF header to /api/** alongside valid Bearer token
        mockMvc.perform(post("/api/v1/mailboxes")
               .header("Authorization", "Bearer api-special-token-888")
               .header("X-XSRF-TOKEN", "completely-bogus-csrf-token")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{\"address\":\"test-bogus-csrf@xcmailr.test\"}"))
               .andExpect(result -> {
                   final int status = result.getResponse().getStatus();
                   assertNotEquals(403, status, "API request with spurious CSRF header must NOT be rejected with 403 Forbidden");
               });
    }

    @Test
    @DisplayName("Verify non-API paths outside /api/** strictly enforce CSRF on mutating requests")
    public void testNonApiPathEnforcesCsrf() throws Exception
    {
        // Mutating request to a path outside /api/** without CSRF token must be rejected with 403 Forbidden
        mockMvc.perform(post("/register")
               .param("mail", "newuser@xcmailr.test")
               .param("password", "secret123"))
               .andExpect(status().isForbidden());
    }
}
