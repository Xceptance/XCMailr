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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.XcmailrApplication;
import com.xceptance.xcmailr.models.MBox;
import com.xceptance.xcmailr.models.Mail;
import com.xceptance.xcmailr.models.MailTransaction;
import com.xceptance.xcmailr.models.User;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailTransactionRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;
import com.xceptance.xcmailr.security.UserPrincipal;

/**
 * Security and defensive resilience test suite verifying that all paginated tabular endpoints
 * strictly prevent SQL injection vulnerabilities across all query parameters:
 * {@code size}, {@code page}, {@code sort}, {@code dir}, and {@code search}.
 * <p>
 * Evaluates defenses across all 5 web console tabular endpoints:
 * <ul>
 *   <li>{@code /mailboxes} (MailboxWebController)</li>
 *   <li>{@code /admin/users} (AdminWebController)</li>
 *   <li>{@code /admin/transactions} (AdminWebController)</li>
 *   <li>{@code /mailboxes/{id}/mails} (MailWebController)</li>
 *   <li>{@code /mails} (MailWebController)</li>
 * </ul>
 *
 * Adheres to the testing pyramid with tier 2 security attack simulation and verification.
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public class TableSecuritySqlInjectionTest
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
    private MailTransactionRepository transactionRepository;

    private User adminUser;
    private User regularUser;
    private MBox regularMailbox;

    @BeforeEach
    public void setup()
    {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        transactionRepository.deleteAll();
        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = new User("Admin", "Super", "admin-sec@xcmailr.test", "AdminPass123", "en");
        adminUser.setActive(true);
        adminUser.setAdmin(true);
        userRepository.save(adminUser);

        regularUser = new User("Alice", "Tester", "alice-sec@xcmailr.test", "UserPass123", "en");
        regularUser.setActive(true);
        regularUser.setAdmin(false);
        userRepository.save(regularUser);

        regularMailbox = new MBox("alice-box", "xcmailr.test", 0L, false, regularUser);
        mailboxRepository.save(regularMailbox);

        final Mail sampleMail = new Mail();
        sampleMail.setSender("friend@external.test");
        sampleMail.setSubject("Security Verification Message");
        sampleMail.setReceiveTime(System.currentTimeMillis());
        sampleMail.setMessage("Security test payload".getBytes(StandardCharsets.UTF_8));
        sampleMail.setMailbox(regularMailbox);
        mailRepository.save(sampleMail);

        final MailTransaction sampleTx = new MailTransaction(100, "friend@external.test", "alice-box@xcmailr.test", null);
        transactionRepository.save(sampleTx);
    }

    // =========================================================================
    // Security Tests: SQL Injection on 'size' Parameter
    // =========================================================================

    /**
     * Security Case: Verifies that SQL injection payloads on the 'size' parameter
     * fail type binding and are strictly rejected with HTTP 400 Bad Request across all endpoints.
     *
     * @param payload SQL injection attack vector
     * @throws Exception if mock MVC execution fails
     */
    @ParameterizedTest(name = "SQL Injection in size rejected with 400: {0}")
    @ValueSource(strings = {
        "15' OR '1'='1",
        "15; DROP TABLE mailboxes;--",
        "1 UNION SELECT 1,2,3--",
        "benchmark(10000000,MD5(1))",
        "1' AND 1=1--"
    })
    @DisplayName("Security: SQL injection payloads in 'size' parameter return HTTP 400 Bad Request")
    public void testSqlInjectionInSizeReturnsBadRequest(final String payload) throws Exception
    {
        final UserPrincipal userPrincipal = new UserPrincipal(regularUser);

        // 1. /mailboxes
        mockMvc.perform(get("/mailboxes")
               .param("size", payload)
               .with(user(userPrincipal)))
               .andExpect(status().isBadRequest());

        // 2. /admin/users
        mockMvc.perform(get("/admin/users")
               .param("size", payload)
               .with(user(adminUser.getMail()).roles("ADMIN")))
               .andExpect(status().isBadRequest());

        // 3. /admin/transactions
        mockMvc.perform(get("/admin/transactions")
               .param("size", payload)
               .with(user(adminUser.getMail()).roles("ADMIN")))
               .andExpect(status().isBadRequest());

        // 4. /mailboxes/{id}/mails
        mockMvc.perform(get("/mailboxes/" + regularMailbox.getId() + "/mails")
               .param("size", payload)
               .with(user(regularUser.getMail()).roles("USER")))
               .andExpect(status().isBadRequest());

        // 5. /mails
        mockMvc.perform(get("/mails")
               .param("size", payload)
               .with(user(regularUser.getMail()).roles("USER")))
               .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // Security Tests: SQL Injection on 'page' Parameter
    // =========================================================================

    /**
     * Security Case: Verifies that SQL injection payloads on the 'page' parameter
     * fail type binding and are strictly rejected with HTTP 400 Bad Request across all endpoints.
     *
     * @param payload SQL injection attack vector
     * @throws Exception if mock MVC execution fails
     */
    @ParameterizedTest(name = "SQL Injection in page rejected with 400: {0}")
    @ValueSource(strings = {
        "0' OR '1'='1",
        "0; DROP TABLE users;--",
        "1 UNION SELECT 1,2,3--",
        "0' AND SLEEP(5)--"
    })
    @DisplayName("Security: SQL injection payloads in 'page' parameter return HTTP 400 Bad Request")
    public void testSqlInjectionInPageReturnsBadRequest(final String payload) throws Exception
    {
        final UserPrincipal userPrincipal = new UserPrincipal(regularUser);

        // 1. /mailboxes
        mockMvc.perform(get("/mailboxes")
               .param("page", payload)
               .with(user(userPrincipal)))
               .andExpect(status().isBadRequest());

        // 2. /admin/users
        mockMvc.perform(get("/admin/users")
               .param("page", payload)
               .with(user(adminUser.getMail()).roles("ADMIN")))
               .andExpect(status().isBadRequest());

        // 3. /admin/transactions
        mockMvc.perform(get("/admin/transactions")
               .param("page", payload)
               .with(user(adminUser.getMail()).roles("ADMIN")))
               .andExpect(status().isBadRequest());

        // 4. /mailboxes/{id}/mails
        mockMvc.perform(get("/mailboxes/" + regularMailbox.getId() + "/mails")
               .param("page", payload)
               .with(user(regularUser.getMail()).roles("USER")))
               .andExpect(status().isBadRequest());

        // 5. /mails
        mockMvc.perform(get("/mails")
               .param("page", payload)
               .with(user(regularUser.getMail()).roles("USER")))
               .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // Security Tests: SQL Injection on 'sort' Parameter
    // =========================================================================

    /**
     * Security Case: Verifies that SQL injection payloads on the 'sort' parameter
     * on /mailboxes are neutralized by whitelist normalization and safely fall back
     * to the default 'id' sorting column.
     *
     * @param payload SQL injection attack vector
     * @throws Exception if mock MVC execution fails
     */
    @ParameterizedTest(name = "SQL Injection in sort neutralized: {0}")
    @ValueSource(strings = {
        "address; DROP TABLE mailboxes;--",
        "address, (SELECT CASE WHEN (1=1) THEN 1 ELSE 0 END)",
        "extractvalue(1,concat(0x7e,(select user()),0x7e))",
        "SLEEP(5)",
        "' OR 1=1 --",
        "nonexistent_column"
    })
    @DisplayName("Security: SQL injection payloads in 'sort' parameter safely fall back to default column")
    public void testSqlInjectionInSortFallsBackToDefault(final String payload) throws Exception
    {
        final UserPrincipal userPrincipal = new UserPrincipal(regularUser);

        // /mailboxes supports dynamic sort; whitelist mapping must safely neutralize injection
        mockMvc.perform(get("/mailboxes")
               .param("sort", payload)
               .with(user(userPrincipal)))
               .andExpect(status().isOk())
               .andExpect(model().attribute("sort", "id"));
    }

    /**
     * Security Case: Verifies that other endpoints safely ignore dynamic 'sort' parameters
     * and enforce compile-time static sort orders.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Security: Static sort endpoints ignore malicious sort parameter")
    public void testSqlInjectionInSortIgnoredOnStaticEndpoints() throws Exception
    {
        final String maliciousSort = "receiveTime; DROP TABLE mail;--";

        // /admin/users enforces findAllByOrderByMailAsc
        mockMvc.perform(get("/admin/users")
               .param("sort", maliciousSort)
               .with(user(adminUser.getMail()).roles("ADMIN")))
               .andExpect(status().isOk());

        // /admin/transactions enforces findAllByOrderByTsDesc
        mockMvc.perform(get("/admin/transactions")
               .param("sort", maliciousSort)
               .with(user(adminUser.getMail()).roles("ADMIN")))
               .andExpect(status().isOk());

        // /mailboxes/{id}/mails enforces receiveTime DESC
        mockMvc.perform(get("/mailboxes/" + regularMailbox.getId() + "/mails")
               .param("sort", maliciousSort)
               .with(user(regularUser.getMail()).roles("USER")))
               .andExpect(status().isOk());

        // /mails enforces receiveTime DESC
        mockMvc.perform(get("/mails")
               .param("sort", maliciousSort)
               .with(user(regularUser.getMail()).roles("USER")))
               .andExpect(status().isOk());
    }

    // =========================================================================
    // Security Tests: SQL Injection on 'dir' Parameter
    // =========================================================================

    /**
     * Security Case: Verifies that SQL injection payloads on the 'dir' parameter
     * on /mailboxes are neutralized by whitelist normalization and safely fall back
     * to 'asc' (or 'desc' for default sort).
     *
     * @param payload SQL injection attack vector
     * @throws Exception if mock MVC execution fails
     */
    @ParameterizedTest(name = "SQL Injection in dir neutralized: {0}")
    @ValueSource(strings = {
        "asc; DROP TABLE mailboxes;--",
        "desc, SLEEP(5)",
        "' OR '1'='1",
        "INVALID_DIR"
    })
    @DisplayName("Security: SQL injection payloads in 'dir' parameter safely fall back to default direction")
    public void testSqlInjectionInDirFallsBackToDefault(final String payload) throws Exception
    {
        final UserPrincipal userPrincipal = new UserPrincipal(regularUser);

        // When sort=address and dir is malicious or non-asc, resolveSortDirection safely defaults to "desc"
        mockMvc.perform(get("/mailboxes")
               .param("sort", "address")
               .param("dir", payload)
               .with(user(userPrincipal)))
               .andExpect(status().isOk())
               .andExpect(model().attribute("dir", "desc"));
    }

    // =========================================================================
    // Security Tests: SQL Injection on 'search' Parameter
    // =========================================================================

    /**
     * Security Case: Verifies that SQL injection payloads in the 'search' parameter
     * are treated as parameterized literal strings without triggering SQL syntax errors
     * or data corruption.
     *
     * @param payload SQL injection attack vector
     * @throws Exception if mock MVC execution fails
     */
    @ParameterizedTest(name = "SQL Injection in search parameterized: {0}")
    @ValueSource(strings = {
        "' OR '1'='1",
        "test'; DROP TABLE mailboxes;--",
        "admin'--",
        "%%%' OR 1=1 --",
        "UNION SELECT 1, mail, passwd FROM users--"
    })
    @DisplayName("Security: SQL injection payloads in 'search' parameter handled safely as parameterized literals")
    public void testSqlInjectionInSearchHandledSafely(final String payload) throws Exception
    {
        final UserPrincipal userPrincipal = new UserPrincipal(regularUser);

        // /mailboxes search query
        mockMvc.perform(get("/mailboxes")
               .param("search", payload)
               .with(user(userPrincipal)))
               .andExpect(status().isOk());

        // /admin/users search query
        mockMvc.perform(get("/admin/users")
               .param("search", payload)
               .with(user(adminUser.getMail()).roles("ADMIN")))
               .andExpect(status().isOk());

        // Verify database tables remain intact after injection attempts
        assertTrue(mailboxRepository.count() > 0, "Mailboxes table must remain intact");
        assertTrue(userRepository.count() > 0, "Users table must remain intact");
    }

    // =========================================================================
    // Tier 2: Boundary & Extreme Values Tests for 'size' & 'page'
    // =========================================================================

    /**
     * Special Case: Verifies that extreme or negative numeric inputs for 'size' and 'page'
     * are defensively clamped and never cause database exceptions or buffer overflows.
     *
     * @throws Exception if mock MVC execution fails
     */
    @Test
    @DisplayName("Special Case: Extreme numeric inputs for size and page are defensively clamped")
    public void testExtremeNumericInputsClampedSafely() throws Exception
    {
        final UserPrincipal userPrincipal = new UserPrincipal(regularUser);

        // Huge size clamped to 1000
        mockMvc.perform(get("/mailboxes")
               .param("size", "99999999")
               .with(user(userPrincipal)))
               .andExpect(status().isOk())
               .andExpect(model().attribute("size", 1000));

        // Negative size falls back to default size (15)
        mockMvc.perform(get("/mailboxes")
               .param("size", "-50")
               .with(user(userPrincipal)))
               .andExpect(status().isOk())
               .andExpect(model().attribute("size", 15));

        // Small positive size (1) is respected
        mockMvc.perform(get("/mailboxes")
               .param("size", "1")
               .with(user(userPrincipal)))
               .andExpect(status().isOk())
               .andExpect(model().attribute("size", 1));

        // Zero size clamped to defensive cap 1000
        mockMvc.perform(get("/mailboxes")
               .param("size", "0")
               .with(user(userPrincipal)))
               .andExpect(status().isOk())
               .andExpect(model().attribute("size", 1000));

        // Negative page clamped to 0
        mockMvc.perform(get("/mailboxes")
               .param("page", "-10")
               .with(user(userPrincipal)))
               .andExpect(status().isOk());
    }
}
