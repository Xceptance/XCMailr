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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;

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

import com.xceptance.xcmailr.repositories.UserRepository;
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
    private UserRepository mockUserRepository;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    /**
     * Initializes test fixtures before each test execution.
     */
    @BeforeEach
    void setUp()
    {
        this.mockUserRepository = mock(UserRepository.class);
        this.resolver = new UserPreferenceLocaleResolver(mockUserRepository);
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
     * Verifies happy path: database language setting takes precedence over stale in-session UserPrincipal.
     */
    @Test
    @DisplayName("Happy Path: Database language preference takes precedence over stale session UserPrincipal")
    void resolveLocale_withDatabasePrecedenceOverStalePrincipal_resolvesDatabaseLocale()
    {
        // GIVEN: in-session UserPrincipal holds stale "de", but database has updated to "en"
        final User sessionUser = new User();
        sessionUser.setMail("john@xcmailr.test");
        sessionUser.setLanguage("de");
        final UserPrincipal principal = new UserPrincipal(sessionUser);
        final Authentication auth = new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        final User dbUser = new User();
        dbUser.setMail("john@xcmailr.test");
        dbUser.setLanguage("en");
        when(mockUserRepository.findByMailIgnoreCase("john@xcmailr.test")).thenReturn(Optional.of(dbUser));

        // WHEN: locale is resolved
        final Locale actualLocale = resolver.resolveLocale(request);

        // THEN: database preference "en" is authoritative
        assertEquals(Locale.ENGLISH, actualLocale, "Expected database language 'en' to override stale session language 'de'");
    }

    /**
     * Verifies happy path: database German preference takes precedence over stale session English.
     */
    @Test
    @DisplayName("Happy Path: Database German preference takes precedence over stale session English")
    void resolveLocale_withDatabaseGermanPrecedenceOverStaleEnglishPrincipal_resolvesGermanLocale()
    {
        // GIVEN: in-session UserPrincipal holds stale "en", but database has updated to "de"
        final User sessionUser = new User();
        sessionUser.setMail("john@xcmailr.test");
        sessionUser.setLanguage("en");
        final UserPrincipal principal = new UserPrincipal(sessionUser);
        final Authentication auth = new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        final User dbUser = new User();
        dbUser.setMail("john@xcmailr.test");
        dbUser.setLanguage("de");
        when(mockUserRepository.findByMailIgnoreCase("john@xcmailr.test")).thenReturn(Optional.of(dbUser));

        // WHEN: locale is resolved
        final Locale actualLocale = resolver.resolveLocale(request);

        // THEN: database preference "de" is authoritative
        assertEquals(Locale.GERMAN, actualLocale, "Expected database language 'de' to override stale session language 'en'");
    }

    /**
     * Verifies happy path: case-insensitive email lookup in database.
     */
    @Test
    @DisplayName("Happy Path: Case-insensitive email lookup successfully resolves database locale")
    void resolveLocale_withCaseInsensitiveEmailLookup_resolvesDatabaseLocale()
    {
        // GIVEN: authentication principal username has mixed/uppercase casing
        final User sessionUser = new User();
        sessionUser.setMail("John.Doe@XCMailr.Test");
        sessionUser.setLanguage("en");
        final UserPrincipal principal = new UserPrincipal(sessionUser);
        final Authentication auth = new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        final User dbUser = new User();
        dbUser.setMail("john.doe@xcmailr.test");
        dbUser.setLanguage("de");
        when(mockUserRepository.findByMailIgnoreCase("John.Doe@XCMailr.Test")).thenReturn(Optional.of(dbUser));

        // WHEN: locale is resolved
        final Locale actualLocale = resolver.resolveLocale(request);

        // THEN: database user with German language is found and resolved
        assertEquals(Locale.GERMAN, actualLocale, "Expected German locale from case-insensitive database lookup");
    }

    /**
     * Verifies special / fallback case: when database does not find the user, falls back to in-session UserPrincipal.
     */
    @Test
    @DisplayName("Special Case: User not found in database falls back to in-session UserPrincipal language")
    void resolveLocale_withUserNotFoundInDatabase_fallsBackToPrincipalLanguage()
    {
        // GIVEN: in-session UserPrincipal holds "de", but database lookup returns empty Optional
        final User sessionUser = new User();
        sessionUser.setMail("unknown@xcmailr.test");
        sessionUser.setLanguage("de");
        final UserPrincipal principal = new UserPrincipal(sessionUser);
        final Authentication auth = new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(mockUserRepository.findByMailIgnoreCase("unknown@xcmailr.test")).thenReturn(Optional.empty());

        // WHEN: locale is resolved
        final Locale actualLocale = resolver.resolveLocale(request);

        // THEN: falls back to session principal's language "de"
        assertEquals(Locale.GERMAN, actualLocale, "Expected fallback to in-session UserPrincipal language when database returns empty");
    }

    /**
     * Verifies special / fallback case: resolver constructed with null repository gracefully falls back to UserPrincipal.
     */
    @Test
    @DisplayName("Special Case: Resolver with null repository gracefully falls back to UserPrincipal")
    void resolveLocale_withNullRepository_fallsBackToPrincipalLanguage()
    {
        // GIVEN: resolver constructed without repository reference
        final UserPreferenceLocaleResolver nullRepoResolver = new UserPreferenceLocaleResolver(null);

        final User sessionUser = new User();
        sessionUser.setMail("standalone@xcmailr.test");
        sessionUser.setLanguage("de");
        final UserPrincipal principal = new UserPrincipal(sessionUser);
        final Authentication auth = new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // WHEN: locale is resolved
        final Locale actualLocale = nullRepoResolver.resolveLocale(request);

        // THEN: falls back cleanly to UserPrincipal
        assertEquals(Locale.GERMAN, actualLocale, "Expected fallback to UserPrincipal when repository is null");
    }

    /**
     * Verifies error / injection case: hostile, malformed, or injection language tags in database fall back to English safely.
     */
    @Test
    @DisplayName("Error/OWASP Case: Hostile or malformed language tag in database safely falls back to English without error")
    void resolveLocale_withHostileLanguageTagInDatabase_safelyFallsBackToEnglish()
    {
        // GIVEN: database record contains hostile injection-like string
        final User sessionUser = new User();
        sessionUser.setMail("hacker@xcmailr.test");
        sessionUser.setLanguage("de");
        final UserPrincipal principal = new UserPrincipal(sessionUser);
        final Authentication auth = new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        final User dbUser = new User();
        dbUser.setMail("hacker@xcmailr.test");
        dbUser.setLanguage("de\r\nSet-Cookie: malicious=true");
        when(mockUserRepository.findByMailIgnoreCase("hacker@xcmailr.test")).thenReturn(Optional.of(dbUser));

        // WHEN: locale is resolved
        final Locale actualLocale = resolver.resolveLocale(request);

        // THEN: does not match "de", safely falls back to default English
        assertEquals(Locale.ENGLISH, actualLocale, "Hostile language string must safely fall back to English");
    }

    /**
     * Verifies error case: database record contains null language, falling back to default English.
     */
    @Test
    @DisplayName("Error Case: Database record with null language falls back to default English")
    void resolveLocale_withDatabaseUserNullLanguage_fallsBackToEnglish()
    {
        // GIVEN: database record has null language
        final User sessionUser = new User();
        sessionUser.setMail("john@xcmailr.test");
        sessionUser.setLanguage("de");
        final UserPrincipal principal = new UserPrincipal(sessionUser);
        final Authentication auth = new UsernamePasswordAuthenticationToken(principal, "credentials", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        final User dbUser = new User();
        dbUser.setMail("john@xcmailr.test");
        dbUser.setLanguage(null);
        when(mockUserRepository.findByMailIgnoreCase("john@xcmailr.test")).thenReturn(Optional.of(dbUser));

        // WHEN: locale is resolved
        final Locale actualLocale = resolver.resolveLocale(request);

        // THEN: defaults to English without falling back to stale session
        assertEquals(Locale.ENGLISH, actualLocale, "Null database language must fall back to default English");
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

