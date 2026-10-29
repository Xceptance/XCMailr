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
package com.xceptance.xcmailr.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import com.xceptance.xcmailr.security.UserPrincipal;

import models.User;

/**
 * Unit tests verifying {@link UserPreferenceLocaleResolver} across the testing pyramid (Tier 1).
 * <p>
 * Tests cover happy paths (German and English authenticated users), error/fallback cases (null,
 * empty, and unsupported language codes), and special cases (unauthenticated requests and no-op mutation).
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
class UserPreferenceLocaleResolverTest
{
    private UserPreferenceLocaleResolver resolver;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    /**
     * Initializes test fixtures before each test execution.
     */
    @BeforeEach
    void setUp()
    {
        this.resolver = new UserPreferenceLocaleResolver();
        this.request = new MockHttpServletRequest();
        this.response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    /**
     * Cleans up the security context after each test.
     */
    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    /**
     * Verifies happy path: an authenticated user with language "de" resolves to German locale.
     */
    @Test
    @DisplayName("Happy Path: Authenticated user with 'de' language resolves Locale.GERMAN")
    void resolveLocale_withGermanUser_returnsGermanLocale()
    {
        final User user = new User();
        user.setLanguage("de");
        final UserPrincipal principal = new UserPrincipal(user);
        final Authentication auth = new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        final Locale actualLocale = resolver.resolveLocale(request);

        assertEquals(Locale.GERMAN, actualLocale, "Expected German locale for user with language 'de'");
    }

    /**
     * Verifies happy path: case-insensitivity and whitespace trimming for German language setting.
     */
    @Test
    @DisplayName("Happy Path: Authenticated user with uppercase or whitespace ' DE ' resolves Locale.GERMAN")
    void resolveLocale_withUntrimmedUppercaseGermanUser_returnsGermanLocale()
    {
        final User user = new User();
        user.setLanguage("  DE  ");
        final UserPrincipal principal = new UserPrincipal(user);
        final Authentication auth = new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        final Locale actualLocale = resolver.resolveLocale(request);

        assertEquals(Locale.GERMAN, actualLocale, "Expected German locale for user with language '  DE  '");
    }

    /**
     * Verifies happy path: an authenticated user with language "en" resolves to English locale.
     */
    @Test
    @DisplayName("Happy Path: Authenticated user with 'en' language resolves Locale.ENGLISH")
    void resolveLocale_withEnglishUser_returnsEnglishLocale()
    {
        final User user = new User();
        user.setLanguage("en");
        final UserPrincipal principal = new UserPrincipal(user);
        final Authentication auth = new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        final Locale actualLocale = resolver.resolveLocale(request);

        assertEquals(Locale.ENGLISH, actualLocale, "Expected English locale for user with language 'en'");
    }

    /**
     * Verifies error/fallback case: an authenticated user with null language safely falls back to English.
     */
    @Test
    @DisplayName("Error/Fallback Case: Authenticated user with null language defaults to Locale.ENGLISH")
    void resolveLocale_withNullLanguage_fallsBackToEnglishLocale()
    {
        final User user = new User();
        user.setLanguage(null);
        final UserPrincipal principal = new UserPrincipal(user);
        final Authentication auth = new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        final Locale actualLocale = resolver.resolveLocale(request);

        assertEquals(Locale.ENGLISH, actualLocale, "Expected fallback English locale for user with null language");
    }

    /**
     * Verifies error/fallback case: an authenticated user with blank language safely falls back to English.
     */
    @Test
    @DisplayName("Error/Fallback Case: Authenticated user with empty/blank language defaults to Locale.ENGLISH")
    void resolveLocale_withBlankLanguage_fallsBackToEnglishLocale()
    {
        final User user = new User();
        user.setLanguage("   ");
        final UserPrincipal principal = new UserPrincipal(user);
        final Authentication auth = new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        final Locale actualLocale = resolver.resolveLocale(request);

        assertEquals(Locale.ENGLISH, actualLocale, "Expected fallback English locale for user with blank language");
    }

    /**
     * Verifies error/fallback case: an authenticated user with unsupported language code (e.g. "fr") falls back to English.
     */
    @Test
    @DisplayName("Error/Fallback Case: Authenticated user with unsupported language 'fr' defaults to Locale.ENGLISH")
    void resolveLocale_withUnsupportedLanguage_fallsBackToEnglishLocale()
    {
        final User user = new User();
        user.setLanguage("fr");
        final UserPrincipal principal = new UserPrincipal(user);
        final Authentication auth = new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        final Locale actualLocale = resolver.resolveLocale(request);

        assertEquals(Locale.ENGLISH, actualLocale, "Expected fallback English locale for unsupported language 'fr'");
    }

    /**
     * Verifies special case: an unauthenticated request with an Accept-Language header delegates to the header resolver.
     */
    @Test
    @DisplayName("Special Case: Unauthenticated request with Accept-Language header delegates to header resolver")
    void resolveLocale_unauthenticatedWithGermanHeader_resolvesGermanHeaderLocale()
    {
        // No authentication in security context
        request.addHeader("Accept-Language", "de-DE,de;q=0.9");

        final Locale actualLocale = resolver.resolveLocale(request);

        assertEquals(Locale.GERMAN.getLanguage(), actualLocale.getLanguage(), "Expected German language from request header");
    }

    /**
     * Verifies special case: an unauthenticated request without Accept-Language header falls back to default English.
     */
    @Test
    @DisplayName("Special Case: Unauthenticated request without header defaults to English locale")
    void resolveLocale_unauthenticatedWithoutHeader_resolvesDefaultEnglishLocale()
    {
        // No authentication and empty headers
        final Locale actualLocale = resolver.resolveLocale(request);

        assertEquals(Locale.ENGLISH, actualLocale, "Expected default English locale for unauthenticated headerless request");
    }

    /**
     * Verifies special case: anonymous authentication token is treated as unauthenticated.
     */
    @Test
    @DisplayName("Special Case: AnonymousAuthenticationToken delegates to header fallback resolver")
    void resolveLocale_withAnonymousAuthentication_delegatesToHeaderFallback()
    {
        final Authentication anonymous = new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        request.addHeader("Accept-Language", "de-DE,de;q=0.9");
        final Locale actualLocale = resolver.resolveLocale(request);

        assertEquals(Locale.GERMAN.getLanguage(), actualLocale.getLanguage(), "Anonymous token must delegate to header fallback");
    }

    /**
     * Verifies special case: setLocale is safely a no-op and does not throw an exception.
     */
    @Test
    @DisplayName("Special Case: setLocale is safe no-op and does not throw UnsupportedOperationException")
    void setLocale_isSafeNoOp()
    {
        assertDoesNotThrow(() -> resolver.setLocale(request, response, Locale.FRENCH),
                "Invoking setLocale must execute safely without raising exceptions");
    }
}

