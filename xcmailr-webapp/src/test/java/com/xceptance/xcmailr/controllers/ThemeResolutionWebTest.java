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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.xceptance.xcmailr.XcmailrApplication;

/**
 * Web-tier slice tests verifying dynamic theme stylesheet resolution in Thymeleaf layout rendering
 * across configuration permutations (Tier 2).
 * <p>
 * Tests verify:
 * <ul>
 *   <li>Default configuration renders {@code /css/xceptance-theme.css}.</li>
 *   <li>Configured {@code emerald} theme renders {@code /css/emerald-theme.css}.</li>
 *   <li>Missing theme stylesheet safely falls back to {@code /css/xceptance-theme.css}.</li>
 *   <li>Path traversal injection vectors safely fall back to {@code /css/xceptance-theme.css} without path leakage.</li>
 * </ul>
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
public class ThemeResolutionWebTest
{
    @Nested
    @SpringBootTest(classes = XcmailrApplication.class)
    @ActiveProfiles("test")
    @TestPropertySource(properties = "xcmailr.ui.theme=xceptance")
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
    @DisplayName("Default Theme Configuration")
    class DefaultThemeWebTest
    {
        @Autowired
        private WebApplicationContext context;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp()
        {
            mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        }

        @Test
        @DisplayName("Default configuration renders /css/xceptance-theme.css link tag")
        void testDefaultThemeRendered() throws Exception
        {
            mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/xceptance-theme.css")))
                .andExpect(content().string(not(containsString("/css/emerald-theme.css"))));
        }
    }

    @Nested
    @SpringBootTest(classes = XcmailrApplication.class)
    @ActiveProfiles("test")
    @TestPropertySource(properties = "xcmailr.ui.theme=")
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
    @DisplayName("Blank/Unset Theme Fallback Configuration")
    class FallbackThemeWebTest
    {
        @Autowired
        private WebApplicationContext context;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp()
        {
            mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        }

        @Test
        @DisplayName("Blank theme configuration safely falls back to /css/xceptance-theme.css")
        void testBlankThemeFallbackRendered() throws Exception
        {
            mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/xceptance-theme.css")))
                .andExpect(content().string(not(containsString("/css/emerald-theme.css"))));
        }
    }

    @Nested
    @SpringBootTest(classes = XcmailrApplication.class)
    @ActiveProfiles("test")
    @TestPropertySource(properties = "xcmailr.ui.theme=emerald")
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
    @DisplayName("Configured Emerald Theme")
    class EmeraldThemeWebTest
    {
        @Autowired
        private WebApplicationContext context;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp()
        {
            mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        }

        @Test
        @DisplayName("Configured 'emerald' theme renders /css/emerald-theme.css link tag")
        void testEmeraldThemeRendered() throws Exception
        {
            mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/emerald-theme.css")))
                .andExpect(content().string(not(containsString("/css/xceptance-theme.css"))));
        }
    }

    @Nested
    @SpringBootTest(classes = XcmailrApplication.class)
    @ActiveProfiles("test")
    @TestPropertySource(properties = "xcmailr.ui.theme=missing-theme")
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
    @DisplayName("Missing Theme Fallback")
    class MissingThemeFallbackWebTest
    {
        @Autowired
        private WebApplicationContext context;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp()
        {
            mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        }

        @Test
        @DisplayName("Missing configured theme falls back to /css/xceptance-theme.css link tag")
        void testMissingThemeFallsBack() throws Exception
        {
            mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/xceptance-theme.css")))
                .andExpect(content().string(not(containsString("missing-theme"))));
        }
    }

    @Nested
    @SpringBootTest(classes = XcmailrApplication.class)
    @ActiveProfiles("test")
    @TestPropertySource(properties = "xcmailr.ui.theme=../../etc/passwd")
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
    @DisplayName("Path Traversal Injection Security")
    class PathTraversalSecurityWebTest
    {
        @Autowired
        private WebApplicationContext context;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp()
        {
            mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        }

        @Test
        @DisplayName("Path traversal input safely falls back to /css/xceptance-theme.css without path leakage")
        void testPathTraversalFallsBackWithoutLeakage() throws Exception
        {
            mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/xceptance-theme.css")))
                .andExpect(content().string(not(containsString("etc/passwd"))));
        }
    }
}
