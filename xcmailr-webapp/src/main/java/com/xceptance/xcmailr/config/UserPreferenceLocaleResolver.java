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
     * If the current request is authenticated with a {@link UserPrincipal}, the user's persisted language
     * preference is checked. If authenticated with an alternate principal type and a {@link UserRepository}
     * is available, the user record is looked up by username.
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
            if (authentication.getPrincipal() instanceof final UserPrincipal principal)
            {
                language = principal.getUser() != null ? principal.getUser().getLanguage() : null;
            }
            else if (userRepository != null)
            {
                language = userRepository.findByMailIgnoreCase(authentication.getName())
                                         .map(User::getLanguage)
                                         .orElse(null);
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
