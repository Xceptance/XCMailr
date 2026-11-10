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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests verifying theme stylesheet pagination and display count styling definitions across the testing pyramid (Tier 1).
 * <p>
 * Tests ensure that corporate and developer theme stylesheets ({@code xceptance-theme.css} and {@code emerald-theme.css})
 * properly override Bootstrap 5 {@code .pagination} custom properties with modern, neutral monochrome tokens,
 * eliminating saturated electric blue defaults and supporting dark theme overrides.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
public final class PaginationThemeStylingTest
{
    /**
     * Helper method to load CSS stylesheet content from the classpath.
     *
     * @param cssPath relative path to the stylesheet
     * @return stylesheet content as string
     * @throws IOException if the stylesheet cannot be read
     */
    private String loadStylesheet(final String cssPath) throws IOException
    {
        try (final InputStream inputStream = getClass().getResourceAsStream(cssPath))
        {
            assertNotNull(inputStream, "Stylesheet resource must exist on classpath: " + cssPath);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "/static/css/xceptance-theme.css", "/static/css/emerald-theme.css" })
    @DisplayName("Happy Path: Theme stylesheets define modern monochrome pagination variables")
    public void testThemeDefinesMonochromePaginationVariables(final String stylesheetPath) throws IOException
    {
        final String content = loadStylesheet(stylesheetPath);

        // Verify .pagination rule block is present
        assertTrue(content.contains(".pagination {"),
                   "Stylesheet " + stylesheetPath + " must declare .pagination styling block");

        // Verify inactive items use neutral body text
        assertTrue(content.contains("--bs-pagination-color: var(--bs-body-color);"),
                   "Stylesheet " + stylesheetPath + " must configure neutral body text color for pagination");

        // Verify subtle border color
        assertTrue(content.contains("--bs-pagination-border-color: var(--xc-border-color);"),
                   "Stylesheet " + stylesheetPath + " must configure subtle border color");

        // Verify subtle hover wash
        assertTrue(content.contains("--bs-pagination-hover-bg: var(--bs-tertiary-bg);"),
                   "Stylesheet " + stylesheetPath + " must configure soft hover background");

        // Verify dark charcoal active state
        assertTrue(content.contains("--bs-pagination-active-bg: #212529;"),
                   "Stylesheet " + stylesheetPath + " must configure dark charcoal active background (#212529)");
        assertTrue(content.contains("--bs-pagination-active-border-color: #212529;"),
                   "Stylesheet " + stylesheetPath + " must configure dark charcoal active border (#212529)");
        assertTrue(content.contains("--bs-pagination-active-color: #ffffff;"),
                   "Stylesheet " + stylesheetPath + " must configure crisp white text for active page (#ffffff)");

        // Verify disabled muted styling
        assertTrue(content.contains("--bs-pagination-disabled-color: var(--xc-text-muted);"),
                   "Stylesheet " + stylesheetPath + " must configure muted color for disabled pagination items");
    }

    @ParameterizedTest
    @ValueSource(strings = { "/static/css/xceptance-theme.css", "/static/css/emerald-theme.css" })
    @DisplayName("Happy Path: Theme stylesheets define dark theme pagination overrides")
    public void testThemeDefinesDarkThemePaginationOverrides(final String stylesheetPath) throws IOException
    {
        final String content = loadStylesheet(stylesheetPath);

        // Verify dark mode rule block is present
        assertTrue(content.contains("[data-bs-theme=\"dark\"] .pagination {"),
                   "Stylesheet " + stylesheetPath + " must declare dark theme .pagination override block");

        // Verify dark mode active charcoal slate styling
        assertTrue(content.contains("--bs-pagination-active-bg: #334155;"),
                   "Stylesheet " + stylesheetPath + " must configure slate active background (#334155) in dark mode");
        assertTrue(content.contains("--bs-pagination-active-border-color: #475569;"),
                   "Stylesheet " + stylesheetPath + " must configure slate active border (#475569) in dark mode");
    }

    @ParameterizedTest
    @ValueSource(strings = { "/static/css/xceptance-theme.css", "/static/css/emerald-theme.css" })
    @DisplayName("Negative / Absence Test: Theme stylesheets avoid saturated electric blue in pagination block")
    public void testThemeAvoidsElectricBlueInPagination(final String stylesheetPath) throws IOException
    {
        final String content = loadStylesheet(stylesheetPath);

        // Extract the .pagination block
        final int startIndex = content.indexOf(".pagination {");
        assertTrue(startIndex >= 0, "Pagination block must exist in " + stylesheetPath);
        final int endIndex = content.indexOf("}", startIndex);
        assertTrue(endIndex > startIndex, "Pagination block closing brace must exist in " + stylesheetPath);

        final String paginationBlock = content.substring(startIndex, endIndex);

        // Saturated electric blue (#0d6efd) must not be configured for pagination
        assertFalse(paginationBlock.contains("#0d6efd"),
                    "Pagination block in " + stylesheetPath + " must not reference electric blue #0d6efd");
    }
}
