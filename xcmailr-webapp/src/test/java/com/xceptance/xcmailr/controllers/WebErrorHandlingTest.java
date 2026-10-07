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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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

import jakarta.servlet.RequestDispatcher;

@SpringBootTest(classes = XcmailrApplication.class)
@ActiveProfiles("test")
public class WebErrorHandlingTest
{
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp()
    {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Authenticated access to non-existent route should return 404 status")
    public void testNonExistentRouteReturns404() throws Exception
    {
        mockMvc.perform(get("/this-route-definitely-does-not-exist-12345")
               .with(user("user@example.com").roles("USER"))
               .accept(MediaType.TEXT_HTML))
               .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Error endpoint with 404 status attribute should resolve error/404 view")
    public void testErrorEndpointResolves404View() throws Exception
    {
        mockMvc.perform(get("/error")
               .accept(MediaType.TEXT_HTML)
               .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
               .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/unknown-page"))
               .andExpect(status().isNotFound())
               .andExpect(view().name("error/404"))
               .andExpect(content().string(containsString("Page Not Found")))
               .andExpect(content().string(containsString("HTTP 404")))
               .andExpect(content().string(containsString("Return to Dashboard")));
    }

    @Test
    @DisplayName("Error endpoint with 500 status attribute should resolve error/500 view")
    public void testErrorEndpointResolves500View() throws Exception
    {
        mockMvc.perform(get("/error")
               .accept(MediaType.TEXT_HTML)
               .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
               .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/broken-action"))
               .andExpect(status().isInternalServerError())
               .andExpect(view().name("error/500"))
               .andExpect(content().string(containsString("Internal Server Error")))
               .andExpect(content().string(containsString("HTTP 500")))
               .andExpect(content().string(containsString("Return to Dashboard")));
    }

    @Test
    @DisplayName("Error endpoint with other status attribute should resolve generic error view")
    public void testErrorEndpointResolvesGenericErrorView() throws Exception
    {
        mockMvc.perform(get("/error")
               .accept(MediaType.TEXT_HTML)
               .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)
               .requestAttr(RequestDispatcher.ERROR_MESSAGE, "Bad Request")
               .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/malformed"))
               .andExpect(status().isBadRequest())
               .andExpect(view().name("error"))
               .andExpect(content().string(containsString("HTTP 400")))
               .andExpect(content().string(containsString("Return to Dashboard")));
    }
}
