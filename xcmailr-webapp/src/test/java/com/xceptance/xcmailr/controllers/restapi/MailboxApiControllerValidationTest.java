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
package com.xceptance.xcmailr.controllers.restapi;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xceptance.xcmailr.XcmailrApplication;
import com.xceptance.xcmailr.dto.ApiError;
import com.xceptance.xcmailr.dto.ApiErrors;
import com.xceptance.xcmailr.dto.MailboxData;
import com.xceptance.xcmailr.models.User;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * Integration and unit test suite following the Test Pyramid strategy to verify server-side validation
 * for the REST Mailbox API in {@link MailboxApiController}.
 * <p>
 * Written test-driven (TDD) to prove that unvalidated inbound requests are caught at the controller boundary
 * via declarative Jakarta Bean Validation (`@Valid`) and translated to standard {@link ApiErrors}
 * (HTTP 400 Bad Request).
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
@Transactional
public class MailboxApiControllerValidationTest
{
    private static final String API_PREFIX = "/api/v1/mailboxes";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private MockMvc mockMvc;
    private User testUser;
    private String token;

    /**
     * Sets up mock MVC and an authenticated test user with an API token.
     */
    @BeforeEach
    public void setUp()
    {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                                 .apply(springSecurity())
                                 .build();

        testUser = new User("Api", "Tester", "api.tester@xcmailr.test", "$2a$10$abcdefghijklmnopqrstuv", "en");
        testUser.setActive(true);
        testUser.setApiToken("test-api-token-validation-" + System.nanoTime());
        testUser = userRepository.save(testUser);
        token = testUser.getApiToken();
    }

    // =========================================================================
    // BEAN VALIDATION UNIT TESTS ON MailboxData
    // =========================================================================

    /**
     * Verifies that {@link MailboxData} enforces a maximum length of 255 characters on the comment field
     * via Jakarta Bean Validation constraints.
     */
    @Test
    @DisplayName("Bean validation: MailboxData comment exceeding 255 characters should violate @Size constraint")
    public void testMailboxData_CommentExceeds255Chars_HasViolation()
    {
        final MailboxData data = new MailboxData();
        data.address = "valid@xcmailr.test";
        data.comment = "C".repeat(256);

        final Set<ConstraintViolation<MailboxData>> violations = validator.validate(data);
        final boolean hasCommentViolation = violations.stream()
            .anyMatch(v -> "comment".equals(v.getPropertyPath().toString()));

        assertFalse(violations.isEmpty(), "Violations set must not be empty for oversized comment");
        org.junit.jupiter.api.Assertions.assertTrue(hasCommentViolation,
            "MailboxData.comment must have a Bean Validation @Size violation when exceeding 255 characters");
    }

    // =========================================================================
    // ENDPOINT BOUNDARY VALIDATION: INBOUND FAILS BEFORE RESOURCE RESOLUTION
    // =========================================================================

    /**
     * Verifies that PUT requests with invalid body payload fail validation immediately at the HTTP boundary
     * (returning HTTP 400 Bad Request), rather than leaking internal database state with HTTP 404 Not Found.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Boundary case: PUT with invalid email in payload should fail with 400 Bad Request before database lookup")
    public void testUpdateMailbox_NonExistentMailboxWithInvalidPayload_FailsValidationFirst() throws Exception
    {
        final MailboxData data = new MailboxData();
        data.address = "invalid-email-format";
        data.comment = "Valid comment";

        mockMvc.perform(put(API_PREFIX + "/nonexistent@xcmailr.test")
               .header("Authorization", "Bearer " + token)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(data)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors[0].parameter", is("address")));
    }

    /**
     * Verifies that PUT requests with comment exceeding 255 characters fail validation immediately
     * with HTTP 400 Bad Request before database lookup.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Boundary case: PUT with comment > 255 chars should fail with 400 Bad Request before database lookup")
    public void testUpdateMailbox_NonExistentMailboxWithCommentExceeding255_FailsValidationFirst() throws Exception
    {
        final MailboxData data = new MailboxData();
        data.address = "valid@xcmailr.test";
        data.comment = "C".repeat(256);

        mockMvc.perform(put(API_PREFIX + "/nonexistent@xcmailr.test")
               .header("Authorization", "Bearer " + token)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(data)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors[0].parameter", is("comment")));
    }

    // =========================================================================
    // POST /api/v1/mailboxes VALIDATION ERROR CASES
    // =========================================================================

    /**
     * Verifies that creating a mailbox with missing/null email returns HTTP 400 Bad Request with field error.
     *
     * @throws Exception if mock MVC request execution fails
     */
    @Test
    @DisplayName("Error case: POST mailbox with missing email should return HTTP 400 Bad Request")
    public void testCreateMailbox_MissingEmail_ReturnsBadRequest() throws Exception
    {
        final String emptyPayload = "{}";

        mockMvc.perform(post(API_PREFIX)
               .header("Authorization", "Bearer " + token)
               .contentType(MediaType.APPLICATION_JSON)
               .content(emptyPayload))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors[*].parameter", hasItem("address")));
    }

    /**
     * Verifies that creating a mailbox with malformed email addresses returns HTTP 400 Bad Request.
     *
     * @param malformedAddress candidate malformed email
     * @throws Exception if mock MVC request execution fails
     */
    @ParameterizedTest(name = "Validation failure: malformed address \"{0}\" returns 400 Bad Request")
    @ValueSource(strings = {"not-an-email", "@missinguser.com", "user@.com", "plainstring", "user@domain..com"})
    @DisplayName("Error case: POST mailbox with malformed email should return HTTP 400 Bad Request")
    public void testCreateMailbox_MalformedEmail_ReturnsBadRequest(final String malformedAddress) throws Exception
    {
        final MailboxData data = new MailboxData();
        data.address = malformedAddress;

        mockMvc.perform(post(API_PREFIX)
               .header("Authorization", "Bearer " + token)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(data)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors[*].parameter", hasItem("address")));
    }

    // =========================================================================
    // SPECIAL CASES: EXACT BOUNDARIES, UTF-8, XSS, SQL INJECTION STRINGS
    // =========================================================================

    /**
     * Verifies that a mailbox comment of exactly 255 characters passes bean validation.
     */
    @Test
    @DisplayName("Special case: MailboxData comment of exactly 255 characters should pass bean validation")
    public void testMailboxData_CommentExact255Chars_PassesValidation()
    {
        final MailboxData data = new MailboxData();
        data.address = "valid.exact255@xcmailr.test";
        data.comment = "C".repeat(255);

        final Set<ConstraintViolation<MailboxData>> violations = validator.validate(data);
        org.junit.jupiter.api.Assertions.assertTrue(violations.isEmpty(),
            "MailboxData with 255-character comment must not produce validation violations");
    }

    /**
     * Verifies that a mailbox comment containing UTF-8 international characters passes bean validation.
     */
    @Test
    @DisplayName("Special case: MailboxData comment with UTF-8 international characters should pass bean validation")
    public void testMailboxData_CommentUtf8Multibyte_PassesValidation()
    {
        final MailboxData data = new MailboxData();
        data.address = "valid.utf8@xcmailr.test";
        data.comment = "Postfach für automatisierte Tests mit Umlauten: äöüß und Éléphant";

        final Set<ConstraintViolation<MailboxData>> violations = validator.validate(data);
        org.junit.jupiter.api.Assertions.assertTrue(violations.isEmpty(),
            "MailboxData with UTF-8 comment must not produce validation violations");
    }

    /**
     * Verifies that a mailbox comment containing XSS and SQL injection payloads passes bean validation.
     */
    @Test
    @DisplayName("Special case: MailboxData comment with XSS and SQL injection strings should pass bean validation")
    public void testMailboxData_CommentXssAndSqlInjection_PassesValidation()
    {
        final MailboxData data = new MailboxData();
        data.address = "valid.xss@xcmailr.test";
        data.comment = "<script>alert('xss')</script> ' OR 1=1;--";

        final Set<ConstraintViolation<MailboxData>> violations = validator.validate(data);
        org.junit.jupiter.api.Assertions.assertTrue(violations.isEmpty(),
            "MailboxData with injection payload strings within length limits must be handled safely without constraint failure");
    }
}
