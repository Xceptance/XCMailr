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
package com.xceptance.xcmailr.security;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.xceptance.xcmailr.repositories.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that intercepts incoming HTTP requests with an {@code Authorization: Bearer <token>} header
 * and populates the {@link SecurityContextHolder} if the API token corresponds to an active user.
 *
 * @author Xceptance Software Technologies GmbH
 */
public class ApiTokenAuthenticationFilter extends OncePerRequestFilter
{
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;

    /**
     * Constructs the filter with the user repository.
     *
     * @param userRepository repository used to resolve API tokens
     */
    public ApiTokenAuthenticationFilter(final UserRepository userRepository)
    {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain)
        throws ServletException, IOException
    {
        final String authHeader = request.getHeader("Authorization");

        if (StringUtils.isNotBlank(authHeader) && authHeader.startsWith(BEARER_PREFIX))
        {
            final String token = authHeader.substring(BEARER_PREFIX.length()).trim();

            if (!token.isEmpty())
            {
                userRepository.findByApiToken(token)
                              .filter(models.User::isActive)
                              .ifPresent(user -> {
                                  final UserPrincipal principal = new UserPrincipal(user);
                                  final UsernamePasswordAuthenticationToken authentication =
                                      new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
                                  authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                  SecurityContextHolder.getContext().setAuthentication(authentication);
                              });
            }
        }

        filterChain.doFilter(request, response);
    }
}
