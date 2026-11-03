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

import java.util.Locale;
import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import com.xceptance.xcmailr.repositories.UserRepository;
import com.xceptance.xcmailr.security.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import models.User;

/**
 * Resolves the request {@link Locale} based on the authenticated user's profile language preference,
 * defaulting to English and falling back to HTTP request accept-language headers for unauthenticated requests.
 *
 * @author Xceptance Software Technologies GmbH
 */
public class UserPreferenceLocaleResolver implements LocaleResolver
{
    private final AcceptHeaderLocaleResolver fallbackResolver;
    private final UserRepository userRepository;

    /**
     * Constructs a new {@link UserPreferenceLocaleResolver} configuring a fallback resolver with default English locale
     * and without an external repository reference (delegates strictly to {@link UserPrincipal}).
     */
    public UserPreferenceLocaleResolver()
    {
        this(null);
    }

    /**
     * Constructs a new {@link UserPreferenceLocaleResolver} with an optional {@link UserRepository} fallback
     * to resolve language preferences when the principal is not directly an instance of {@link UserPrincipal}.
     *
     * @param userRepository the user repository for principal lookups, may be null
     */
    public UserPreferenceLocaleResolver(final UserRepository userRepository)
    {
        final AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        this.fallbackResolver = resolver;
        this.userRepository = userRepository;
    }

    /**
     * Resolves the current locale.
     * <p>
     * If the current request is authenticated, the user's persisted language preference is queried
     * directly from the {@link UserRepository} as the authoritative source of truth. If the repository
     * is unavailable or does not return a user record (e.g. in standalone unit tests), resolution
     * falls back to the in-session {@link UserPrincipal}.
     * </p>
     * <p>
     * If the language is "de" (case-insensitive), {@link Locale#GERMAN} is returned; otherwise,
     * {@link Locale#ENGLISH} is returned.
     * </p>
     * <p>
     * For unauthenticated or anonymous requests, resolution delegates to the {@link AcceptHeaderLocaleResolver}.
     * </p>
     *
     * @param request the HTTP servlet request
     * @return the resolved {@link Locale}, never null
     */
    @Override
    public Locale resolveLocale(final HttpServletRequest request)
    {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken))
        {
            String language = null;
            boolean resolvedFromDb = false;

            // Defense-in-depth Layer 1: Query database first as authoritative source of truth
            if (userRepository != null)
            {
                final Optional<User> userOpt = userRepository.findByMailIgnoreCase(authentication.getName());
                if (userOpt.isPresent())
                {
                    language = userOpt.get().getLanguage();
                    resolvedFromDb = true;
                }
            }

            // Fallback to in-session UserPrincipal if repository was null or user was not found
            if (!resolvedFromDb && authentication.getPrincipal() instanceof final UserPrincipal principal)
            {
                language = principal.getUser() != null ? principal.getUser().getLanguage() : null;
            }

            if (language != null && "de".equalsIgnoreCase(language.trim()))
            {
                return Locale.GERMAN;
            }
            return Locale.ENGLISH;
        }

        // Delegate to fallback accept-header resolver for anonymous or unauthenticated requests
        return fallbackResolver.resolveLocale(request);
    }

    /**
     * Unsupported operation as user language is managed via persistent user profile settings.
     *
     * @param request the HTTP servlet request
     * @param response the HTTP servlet response
     * @param locale the new locale, or null to clear
     */
    @Override
    public void setLocale(final HttpServletRequest request, final HttpServletResponse response, final Locale locale)
    {
        // No-op: Locale preference is driven by persistent user profile data rather than mutable session/cookies
    }
}
