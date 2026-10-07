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
package com.xceptance.xcmailr;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import com.xceptance.xcmailr.config.XcmailrProperties;

/**
 * Verifies that the Spring Boot application context bootstraps successfully
 * with virtual threads enabled, Flyway migrations executed, and data sources active.
 *
 * @author Xceptance Software Technologies GmbH
 */
@SpringBootTest
@ActiveProfiles("test")
public class XcmailrApplicationTests
{
    @Autowired
    private Environment environment;

    @Autowired
    private XcmailrProperties properties;

    @Test
    @DisplayName("Verify Spring application context and configuration properties load cleanly")
    public void testContextAndConfigurationLoads()
    {
        // Assert context environment is healthy
        assertNotNull(environment, "Spring environment must be non-null");

        // Assert virtual threads property is enabled
        final String virtualThreads = environment.getProperty("spring.threads.virtual.enabled");
        assertTrue("true".equalsIgnoreCase(virtualThreads), "Virtual threads must be enabled");

        // Assert typed configuration properties are properly populated
        assertNotNull(properties, "XcmailrProperties must be bound");
        assertNotNull(properties.getApp(), "App properties must be configured");
        assertNotNull(properties.getApp().getName(), "Application name must be configured");
    }
}
