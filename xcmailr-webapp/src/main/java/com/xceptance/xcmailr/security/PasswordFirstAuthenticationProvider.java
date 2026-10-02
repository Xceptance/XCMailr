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

import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Authentication provider that validates user password credentials BEFORE verifying account active status.
 * <p>
 * In standard Spring Security configurations, {@link DaoAuthenticationProvider} verifies {@link UserDetails#isEnabled()}
 * during pre-authentication checks before verifying the submitted password against the stored password hash.
 * This default ordering introduces an Account Enumeration vulnerability (CWE-204), as an unauthenticated attacker
 * submitting incorrect passwords could determine whether an account exists and is unconfirmed.
 * </p>
 * <p>
 * This provider reorders the checks:
 * <ol>
 *   <li>Pre-authentication: verifies account non-locked and non-expired.</li>
 *   <li>Credential validation: verifies password match using {@link PasswordEncoder}. Invalid passwords or non-existent
 *       users always throw {@link BadCredentialsException}.</li>
 *   <li>Post-authentication: verifies credentials non-expired and account enabled ({@link UserDetails#isEnabled()}).
 *       Only when valid credentials have been supplied for an unconfirmed account is a {@link DisabledException} thrown.</li>
 * </ol>
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
public class PasswordFirstAuthenticationProvider extends DaoAuthenticationProvider
{
    /**
     * Constructs a new {@link PasswordFirstAuthenticationProvider} with the given service and encoder.
     *
     * @param userDetailsService service used to retrieve user principal records
     * @param passwordEncoder encoder used to verify submitted passwords against stored hashes
     */
    public PasswordFirstAuthenticationProvider(final UserDetailsService userDetailsService,
                                               final PasswordEncoder passwordEncoder)
    {
        super(userDetailsService);
        setPasswordEncoder(passwordEncoder);
        setHideUserNotFoundExceptions(true);

        // Security design: Do NOT check isEnabled() in pre-authentication to prevent CWE-204 account enumeration.
        setPreAuthenticationChecks(this::preCheck);
        // Defer isEnabled() check to post-authentication so credentials are validated first.
        setPostAuthenticationChecks(this::postCheck);
    }

    /**
     * Checks account status attributes prior to password verification, excluding account enabled status.
     *
     * @param user user details to inspect
     * @throws LockedException if the account is locked
     * @throws AccountExpiredException if the account has expired
     */
    private void preCheck(final UserDetails user)
    {
        if (!user.isAccountNonLocked())
        {
            throw new LockedException("User account is locked");
        }
        if (!user.isAccountNonExpired())
        {
            throw new AccountExpiredException("User account has expired");
        }
    }

    /**
     * Checks credentials status and account enabled status after password verification succeeds.
     *
     * @param user user details to inspect
     * @throws CredentialsExpiredException if the user credentials have expired
     * @throws DisabledException if the user account is inactive or pending email confirmation
     */
    private void postCheck(final UserDetails user)
    {
        if (!user.isCredentialsNonExpired())
        {
            throw new CredentialsExpiredException("User credentials have expired");
        }
        if (!user.isEnabled())
        {
            // Valid password was provided, but the account has not been activated yet.
            throw new DisabledException("User account is not active");
        }
    }
}
