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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Year;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.XcmailrApplication;
import com.xceptance.xcmailr.repositories.DomainRepository;
import com.xceptance.xcmailr.repositories.MailRepository;
import com.xceptance.xcmailr.repositories.MailboxRepository;
import com.xceptance.xcmailr.repositories.UserRepository;

import jakarta.servlet.RequestDispatcher;
import com.xceptance.xcmailr.models.Domain;
import com.xceptance.xcmailr.models.User;

/**
 * Contract tests for layout rendering across the testing pyramid, verifying:
 * <ul>
 *   <li>Happy path: Public (/login) and authenticated (/mailboxes) views render the corporate copyright notice with inception-to-current year range and application version, without internal technology stack metadata.</li>
 *   <li>Error case: Styled error views (e.g. 404) render the base layout with the copyright notice, year range, and application version without technology stack metadata.</li>
 *   <li>Special case: Base template markup contains the resilient closeModal event listener binding to #actionModal.</li>
 * </ul>
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public class BaseLayoutFooterContractTest
{
    private static final String TEST_USER_EMAIL = "testuser@xcmailr.test";
    private static final String TEST_DOMAIN_NAME = "xcmailr.test";
    private static final String EXPECTED_YEAR_RANGE = String.format("2013-%d", Year.now().getValue());
    private static final String COMPANY_NAME = "Xceptance Software Technologies GmbH";
    private static final String ALL_RIGHTS_RESERVED = "All rights reserved.";
    private static final String VERSION_MARKER_CLASS = "xcmailr-version";
    private static final String EXPECTED_VERSION = "4.0.0";
    private static final String TECH_STACK_SNIPPET = "XCMailr Modernized &bull; Java 25 &bull; Spring Boot 4.x &bull; HTMX";
    private static final String TECH_STACK_MODERNIZED = "XCMailr Modernized";
    private static final String TECH_STACK_JAVA = "Java 25";
    private static final String TECH_STACK_SPRING_BOOT = "Spring Boot 4.x";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private MailboxRepository mailboxRepository;

    @Autowired
    private MailRepository mailRepository;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp()
    {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();

        final Domain domain = new Domain(TEST_DOMAIN_NAME);
        domainRepository.save(domain);

        final User user = new User("Jane", "Doe", TEST_USER_EMAIL, "Password123!", "en");
        user.setActive(true);
        userRepository.save(user);
    }

    @AfterEach
    public void tearDown()
    {
        mailRepository.deleteAll();
        mailboxRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();
    }

    // =========================================================================
    // HAPPY PATH CONTRACT TESTS
    // =========================================================================

    @Test
    @DisplayName("Happy path: Public view (/login) renders base layout with copyright year range, company notice, and version")
    public void testPublicLoginViewRendersYearlessCopyright() throws Exception
    {
        mockMvc.perform(get("/login")
               .accept(MediaType.TEXT_HTML))
               .andExpect(status().isOk())
               .andExpect(view().name("auth/login"))
               .andExpect(content().string(containsString(EXPECTED_YEAR_RANGE)))
               .andExpect(content().string(containsString(COMPANY_NAME)))
               .andExpect(content().string(containsString(ALL_RIGHTS_RESERVED)))
               .andExpect(content().string(containsString("Version:")))
               .andExpect(content().string(containsString(VERSION_MARKER_CLASS)))
               .andExpect(content().string(containsString(EXPECTED_VERSION)))
               .andExpect(content().string(not(containsString(TECH_STACK_SNIPPET))))
               .andExpect(content().string(not(containsString(TECH_STACK_MODERNIZED))))
               .andExpect(content().string(not(containsString(TECH_STACK_JAVA))))
               .andExpect(content().string(not(containsString(TECH_STACK_SPRING_BOOT))));
    }

    @Test
    @DisplayName("Happy path: Authenticated view (/mailboxes) renders base layout with copyright year range, company notice, and version")
    public void testAuthenticatedMailboxesViewRendersYearlessCopyright() throws Exception
    {
        mockMvc.perform(get("/mailboxes")
               .with(user(TEST_USER_EMAIL).roles("USER"))
               .accept(MediaType.TEXT_HTML))
               .andExpect(status().isOk())
               .andExpect(view().name("mailboxes/index"))
               .andExpect(content().string(containsString(EXPECTED_YEAR_RANGE)))
               .andExpect(content().string(containsString(COMPANY_NAME)))
               .andExpect(content().string(containsString(ALL_RIGHTS_RESERVED)))
               .andExpect(content().string(containsString("Version:")))
               .andExpect(content().string(containsString(VERSION_MARKER_CLASS)))
               .andExpect(content().string(containsString(EXPECTED_VERSION)))
               .andExpect(content().string(not(containsString(TECH_STACK_SNIPPET))))
               .andExpect(content().string(not(containsString(TECH_STACK_MODERNIZED))))
               .andExpect(content().string(not(containsString(TECH_STACK_JAVA))))
               .andExpect(content().string(not(containsString(TECH_STACK_SPRING_BOOT))));
    }

    // =========================================================================
    // ERROR CASE CONTRACT TESTS
    // =========================================================================

    @Test
    @DisplayName("Error case: Styled 404 error view renders base layout with copyright year range, company notice, and version")
    public void testStyled404ErrorViewRendersYearlessCopyright() throws Exception
    {
        mockMvc.perform(get("/error")
               .accept(MediaType.TEXT_HTML)
               .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
               .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/non-existent-page-404"))
               .andExpect(status().isNotFound())
               .andExpect(view().name("error/404"))
               .andExpect(content().string(containsString("Page Not Found")))
               .andExpect(content().string(containsString("HTTP 404")))
               .andExpect(content().string(containsString(EXPECTED_YEAR_RANGE)))
               .andExpect(content().string(containsString(COMPANY_NAME)))
               .andExpect(content().string(containsString(ALL_RIGHTS_RESERVED)))
               .andExpect(content().string(containsString("Version:")))
               .andExpect(content().string(containsString(VERSION_MARKER_CLASS)))
               .andExpect(content().string(containsString(EXPECTED_VERSION)))
               .andExpect(content().string(not(containsString(TECH_STACK_SNIPPET))))
               .andExpect(content().string(not(containsString(TECH_STACK_MODERNIZED))))
               .andExpect(content().string(not(containsString(TECH_STACK_JAVA))))
               .andExpect(content().string(not(containsString(TECH_STACK_SPRING_BOOT))));
    }

    // =========================================================================
    // SPECIAL CASE: CLIENT MODAL DISMISSAL SCRIPT VERIFICATION
    // =========================================================================

    @Test
    @DisplayName("Special case: Base template contains resilient closeModal listener binding to #actionModal")
    public void testBaseLayoutContainsResilientCloseModalListener() throws Exception
    {
        mockMvc.perform(get("/login")
               .accept(MediaType.TEXT_HTML))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString("document.body.addEventListener('closeModal', function () {")))
               .andExpect(content().string(containsString("const modalEl = document.getElementById('actionModal');")))
               .andExpect(content().string(containsString("const modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);")))
               .andExpect(content().string(containsString("modalInstance.hide();")))
               .andExpect(content().string(containsString("id=\"actionModal\"")))
               .andExpect(content().string(containsString("id=\"modal-container\"")));
    }
}

