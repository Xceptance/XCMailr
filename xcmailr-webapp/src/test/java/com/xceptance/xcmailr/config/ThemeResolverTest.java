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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Unit tests verifying {@link ThemeResolver} logic across the testing pyramid (Tier 1).
 * <p>
 * Tests cover happy path theme resolution (default, out-of-the-box, custom existing),
 * fallback behavior for missing or blank property values, boundary length constraints,
 * and security sanitization against directory traversal, injection, and invalid characters.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
class ThemeResolverTest
{
    private ResourceLoader mockResourceLoader;
    private Resource mockExistingResource;
    private Resource mockMissingResource;

    @BeforeEach
    void setUp()
    {
        mockResourceLoader = mock(ResourceLoader.class);
        mockExistingResource = mock(Resource.class);
        mockMissingResource = mock(Resource.class);

        when(mockExistingResource.exists()).thenReturn(true);
        when(mockMissingResource.exists()).thenReturn(false);
    }

    @Test
    @DisplayName("Constructor throws NullPointerException when ResourceLoader is null")
    void testConstructorThrowsOnNullResourceLoader()
    {
        assertThrows(NullPointerException.class, () -> new ThemeResolver("xceptance", null));
    }

    @Test
    @DisplayName("Default theme 'xceptance' resolves successfully with DefaultResourceLoader")
    void testDefaultThemeWithDefaultResourceLoader()
    {
        final ResourceLoader realResourceLoader = new DefaultResourceLoader();
        final ThemeResolver resolver = new ThemeResolver("xceptance", realResourceLoader);

        assertEquals(ThemeResolver.DEFAULT_THEME, resolver.getActiveTheme());
        assertEquals("xceptance", resolver.getConfiguredTheme());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Unset, empty, or whitespace theme configuration falls back to 'xceptance'")
    void testFallbackOnBlankOrUnsetConfig(final String candidate)
    {
        final ThemeResolver resolver = new ThemeResolver(candidate, mockResourceLoader);

        assertEquals(ThemeResolver.DEFAULT_THEME, resolver.getActiveTheme());
        assertEquals(candidate, resolver.getConfiguredTheme());
    }

    @Test
    @DisplayName("Explicit out-of-the-box theme 'emerald' resolves when resource exists")
    void testEmeraldThemeResolvesWhenResourceExists()
    {
        when(mockResourceLoader.getResource("classpath:/static/css/emerald-theme.css")).thenReturn(mockExistingResource);

        final ThemeResolver resolver = new ThemeResolver("emerald", mockResourceLoader);

        assertEquals("emerald", resolver.getActiveTheme());
        assertEquals("emerald", resolver.getConfiguredTheme());
    }

    @Test
    @DisplayName("Custom valid theme slug resolves when stylesheet is present on classpath")
    void testCustomValidThemeResolvesWhenPresent()
    {
        when(mockResourceLoader.getResource("classpath:/static/css/acme-corp-theme.css")).thenReturn(mockExistingResource);

        final ThemeResolver resolver = new ThemeResolver("acme-corp", mockResourceLoader);

        assertEquals("acme-corp", resolver.getActiveTheme());
        assertEquals("acme-corp", resolver.getConfiguredTheme());
    }

    @Test
    @DisplayName("Missing stylesheet falls back to 'xceptance' with warning")
    void testFallbackWhenStylesheetMissingOnClasspath()
    {
        when(mockResourceLoader.getResource("classpath:/static/css/nonexistent-theme.css")).thenReturn(mockMissingResource);

        final ThemeResolver resolver = new ThemeResolver("nonexistent", mockResourceLoader);

        assertEquals(ThemeResolver.DEFAULT_THEME, resolver.getActiveTheme());
    }

    @Test
    @DisplayName("Null Resource returned by ResourceLoader falls back to 'xceptance'")
    void testFallbackWhenResourceLoaderReturnsNull()
    {
        when(mockResourceLoader.getResource(anyString())).thenReturn(null);

        final ThemeResolver resolver = new ThemeResolver("custom", mockResourceLoader);

        assertEquals(ThemeResolver.DEFAULT_THEME, resolver.getActiveTheme());
    }

    @ParameterizedTest(name = "Malicious vector [{0}] must be rejected and fall back to default theme")
    @ValueSource(strings = {
        "../foo",
        "../../etc/passwd",
        "..\\..\\windows",
        "themes/../secret",
        "/etc/passwd",
        "C:\\boot.ini",
        "..%2f..%2f",
        "%2e%2e%2f",
        "<script>",
        "</style>",
        "theme;rm -rf",
        "theme|cat",
        "theme&whoami",
        "theme$VAR",
        "theme'OR'1'='1",
        "theme\"--",
        "theme\0.css",
        "theme with spaces",
        " theme",
        "theme ",
        "theme!#*()",
        "theme.css",
        "theme/sub"
    })
    @DisplayName("Path traversal, injection characters, and punctuation are rejected without resource lookup")
    void testSecurityPathTraversalAndInjectionRejected(final String maliciousInput)
    {
        final ThemeResolver resolver = new ThemeResolver(maliciousInput, mockResourceLoader);

        assertEquals(ThemeResolver.DEFAULT_THEME, resolver.getActiveTheme());
        // Verify resource loader is never queried for malformed slugs
        verify(mockResourceLoader, never()).getResource(anyString());
    }

    @Test
    @DisplayName("Theme slug exceeding 32 characters is rejected without resource lookup")
    void testSlugExceedingMaxLengthRejected()
    {
        final String longSlug = "a".repeat(ThemeResolver.MAX_THEME_SLUG_LENGTH + 1);
        final ThemeResolver resolver = new ThemeResolver(longSlug, mockResourceLoader);

        assertEquals(ThemeResolver.DEFAULT_THEME, resolver.getActiveTheme());
        verify(mockResourceLoader, never()).getResource(anyString());
    }

    @Test
    @DisplayName("Theme slug of exactly 32 characters is accepted when resource exists")
    void testSlugExactly32CharsAcceptedWhenResourceExists()
    {
        final String exact32Slug = "abcdefghijklmnopqrstuvwxyz123456";
        when(mockResourceLoader.getResource("classpath:/static/css/" + exact32Slug + "-theme.css"))
            .thenReturn(mockExistingResource);

        final ThemeResolver resolver = new ThemeResolver(exact32Slug, mockResourceLoader);

        assertEquals(exact32Slug, resolver.getActiveTheme());
    }

    @Test
    @DisplayName("Theme slug of exactly 32 characters falls back to default when resource is missing")
    void testSlugExactly32CharsFallsBackWhenResourceMissing()
    {
        final String exact32Slug = "abcdefghijklmnopqrstuvwxyz123456";
        when(mockResourceLoader.getResource("classpath:/static/css/" + exact32Slug + "-theme.css"))
            .thenReturn(mockMissingResource);

        final ThemeResolver resolver = new ThemeResolver(exact32Slug, mockResourceLoader);

        assertEquals(ThemeResolver.DEFAULT_THEME, resolver.getActiveTheme());
    }

    @Test
    @DisplayName("resolveActiveColorMode returns 'light' when request is null")
    void testResolveActiveColorModeNullRequest()
    {
        final ThemeResolver resolver = new ThemeResolver("xceptance", mockResourceLoader);
        assertEquals(ThemeResolver.COLOR_MODE_LIGHT, resolver.resolveActiveColorMode(null));
    }

    @Test
    @DisplayName("resolveActiveColorMode returns 'light' when cookies array is null")
    void testResolveActiveColorModeNullCookies()
    {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        final ThemeResolver resolver = new ThemeResolver("xceptance", mockResourceLoader);
        assertEquals(ThemeResolver.COLOR_MODE_LIGHT, resolver.resolveActiveColorMode(request));
    }

    @Test
    @DisplayName("resolveActiveColorMode returns 'light' when cookies array is empty")
    void testResolveActiveColorModeEmptyCookies()
    {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[0]);

        final ThemeResolver resolver = new ThemeResolver("xceptance", mockResourceLoader);
        assertEquals(ThemeResolver.COLOR_MODE_LIGHT, resolver.resolveActiveColorMode(request));
    }

    @Test
    @DisplayName("resolveActiveColorMode returns 'light' when color mode cookie is absent")
    void testResolveActiveColorModeUnrelatedCookies()
    {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Cookie sessionCookie = new Cookie("JSESSIONID", "XYZ123");
        when(request.getCookies()).thenReturn(new Cookie[] { sessionCookie });

        final ThemeResolver resolver = new ThemeResolver("xceptance", mockResourceLoader);
        assertEquals(ThemeResolver.COLOR_MODE_LIGHT, resolver.resolveActiveColorMode(request));
    }

    @Test
    @DisplayName("resolveActiveColorMode returns 'dark' when color mode cookie equals 'dark'")
    void testResolveActiveColorModeDarkCookie()
    {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Cookie colorCookie = new Cookie(ThemeResolver.COLOR_MODE_COOKIE_NAME, "dark");
        when(request.getCookies()).thenReturn(new Cookie[] { colorCookie });

        final ThemeResolver resolver = new ThemeResolver("xceptance", mockResourceLoader);
        assertEquals(ThemeResolver.COLOR_MODE_DARK, resolver.resolveActiveColorMode(request));
    }

    @Test
    @DisplayName("resolveActiveColorMode is case-insensitive and returns 'dark' for 'DARK'")
    void testResolveActiveColorModeCaseInsensitiveDarkCookie()
    {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Cookie colorCookie = new Cookie(ThemeResolver.COLOR_MODE_COOKIE_NAME, "DARK");
        when(request.getCookies()).thenReturn(new Cookie[] { colorCookie });

        final ThemeResolver resolver = new ThemeResolver("xceptance", mockResourceLoader);
        assertEquals(ThemeResolver.COLOR_MODE_DARK, resolver.resolveActiveColorMode(request));
    }

    @Test
    @DisplayName("resolveActiveColorMode returns 'light' when color mode cookie equals 'light'")
    void testResolveActiveColorModeLightCookie()
    {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Cookie colorCookie = new Cookie(ThemeResolver.COLOR_MODE_COOKIE_NAME, "light");
        when(request.getCookies()).thenReturn(new Cookie[] { colorCookie });

        final ThemeResolver resolver = new ThemeResolver("xceptance", mockResourceLoader);
        assertEquals(ThemeResolver.COLOR_MODE_LIGHT, resolver.resolveActiveColorMode(request));
    }

    @ParameterizedTest(name = "Malformed cookie value [{0}] defaults safely to 'light'")
    @ValueSource(strings = {
        "dark\"><script>alert(1)</script>",
        "dark' OR '1'='1",
        "../../etc/passwd",
        "blue",
        "system",
        "auto",
        "",
        "   "
    })
    @DisplayName("resolveActiveColorMode rejects invalid or injection values and defaults to 'light'")
    void testResolveActiveColorModeMalformedValuesDefaultToLight(final String invalidValue)
    {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Cookie colorCookie = new Cookie(ThemeResolver.COLOR_MODE_COOKIE_NAME, invalidValue);
        when(request.getCookies()).thenReturn(new Cookie[] { colorCookie });

        final ThemeResolver resolver = new ThemeResolver("xceptance", mockResourceLoader);
        assertEquals(ThemeResolver.COLOR_MODE_LIGHT, resolver.resolveActiveColorMode(request));
    }

    @Test
    @DisplayName("resolveActiveColorMode finds cookie when multiple cookies are present")
    void testResolveActiveColorModeMultipleCookies()
    {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Cookie sessionCookie = new Cookie("JSESSIONID", "abc");
        final Cookie colorCookie = new Cookie(ThemeResolver.COLOR_MODE_COOKIE_NAME, "dark");
        final Cookie otherCookie = new Cookie("other", "123");
        when(request.getCookies()).thenReturn(new Cookie[] { sessionCookie, colorCookie, otherCookie });

        final ThemeResolver resolver = new ThemeResolver("xceptance", mockResourceLoader);
        assertEquals(ThemeResolver.COLOR_MODE_DARK, resolver.resolveActiveColorMode(request));
    }
}
