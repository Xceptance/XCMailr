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
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.XcmailrApplication;

/**
 * Tier 3 integration test verifying static asset delivery and HTTP security for UI theme stylesheets.
 * <p>
 * Verifies that:
 * <ul>
 *   <li>The default corporate stylesheet ({@code /css/xceptance-theme.css}) is served with HTTP 200 OK,
 *       {@code text/css} Content-Type, and valid token declarations.</li>
 *   <li>The alternative developer stylesheet ({@code /css/emerald-theme.css}) is served with HTTP 200 OK,
 *       {@code text/css} Content-Type, and valid emerald token declarations.</li>
 *   <li>Directory traversal attempts via static asset paths return client error status codes.</li>
 * </ul>
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public class ThemeIntegrationTest
{
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    @DisplayName("HTTP GET /css/xceptance-theme.css returns 200 OK with text/css Content-Type")
    void testXceptanceThemeDelivery() throws Exception
    {
        mockMvc.perform(get("/css/xceptance-theme.css"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/css"))
            .andExpect(content().string(containsString("--xc-primary")))
            .andExpect(content().string(containsString(".navbar-xceptance")));
    }

    @Test
    @DisplayName("HTTP GET /css/emerald-theme.css returns 200 OK with text/css Content-Type")
    void testEmeraldThemeDelivery() throws Exception
    {
        mockMvc.perform(get("/css/emerald-theme.css"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/css"))
            .andExpect(content().string(containsString("--xc-primary")))
            .andExpect(content().string(containsString("#059669")));
    }

    @Test
    @DisplayName("HTTP GET with directory traversal is rejected by web layer")
    void testDirectoryTraversalRejected() throws Exception
    {
        mockMvc.perform(get("/css/../../etc/passwd"))
            .andExpect(status().is4xxClientError());
    }
}
