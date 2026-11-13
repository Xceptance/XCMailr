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
package com.xceptance.xcmailr.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Year;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

/**
 * Unit tests for {@link AppInfoAdvice} verifying copyright year calculation and version resolution logic.
 *
 * @author Xceptance Software Technologies GmbH
 */
class AppInfoAdviceTest
{
    @Test
    @DisplayName("Copyright year reflects inception year 2013 and current calendar year")
    void testCopyrightYearCalculatedDynamically()
    {
        final AppInfoAdvice advice = new AppInfoAdvice(null, "4.0.0");
        final String expected = String.format("2013-%d", Year.now().getValue());
        assertEquals(expected, advice.getCopyrightYear());
    }

    @Test
    @DisplayName("App version prioritizes BuildProperties when present and non-blank")
    void testAppVersionPrioritizesBuildProperties()
    {
        final Properties properties = new Properties();
        properties.setProperty("version", "4.1.2-TEST");
        final BuildProperties buildProperties = new BuildProperties(properties);

        final AppInfoAdvice advice = new AppInfoAdvice(buildProperties, "4.0.0");
        assertEquals("4.1.2-TEST", advice.getAppVersion());
    }

    @Test
    @DisplayName("App version falls back to configured default version when BuildProperties is null")
    void testAppVersionFallsBackToConfiguredDefaultWhenBuildPropertiesNull()
    {
        final AppInfoAdvice advice = new AppInfoAdvice(null, "4.0.0-CONFIGURED");
        assertEquals("4.0.0-CONFIGURED", advice.getAppVersion());
    }

    @Test
    @DisplayName("App version falls back to DEFAULT_VERSION when BuildProperties and configured version are blank")
    void testAppVersionFallsBackToDefaultWhenAllInputsBlank()
    {
        final Properties properties = new Properties();
        properties.setProperty("version", "  ");
        final BuildProperties buildProperties = new BuildProperties(properties);

        final AppInfoAdvice advice = new AppInfoAdvice(buildProperties, "   ");
        assertEquals(AppInfoAdvice.DEFAULT_VERSION, advice.getAppVersion());
    }
}
