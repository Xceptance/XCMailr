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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import models.User;

/**
 * Unit tests verifying role mapping, account status, and immutability invariants
 * of {@link UserPrincipal}.
 *
 * @author Xceptance Software Technologies GmbH
 */
public class UserPrincipalUnitTest
{
    /**
     * Verifies that standard non-admin users receive only {@code ROLE_USER}.
     */
    @Test
    @DisplayName("Standard active user has ROLE_USER only")
    public void testStandardUserAuthorities()
    {
        final User user = new User("Jane", "Doe", "jane@xcmailr.test", "Secret123", "en");
        user.setActive(true);
        user.setAdmin(false);

        final UserPrincipal principal = new UserPrincipal(user);
        final Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();

        assertNotNull(authorities, "Authorities collection should not be null");
        assertEquals(1, authorities.size(), "Standard user should have exactly one authority");
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")), "Should have ROLE_USER");
        assertFalse(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")), "Should not have ROLE_ADMIN");
    }

    /**
     * Verifies that administrator users receive both {@code ROLE_USER} and {@code ROLE_ADMIN}.
     */
    @Test
    @DisplayName("Admin user has both ROLE_USER and ROLE_ADMIN")
    public void testAdminUserAuthorities()
    {
        final User admin = new User("Admin", "Owner", "admin@xcmailr.test", "AdminPass123", "en");
        admin.setActive(true);
        admin.setAdmin(true);

        final UserPrincipal principal = new UserPrincipal(admin);
        final Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();

        assertNotNull(authorities, "Authorities collection should not be null");
        assertEquals(2, authorities.size(), "Admin should have exactly two authorities");
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")), "Admin should have ROLE_USER");
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")), "Admin should have ROLE_ADMIN");
    }

    /**
     * Verifies that inactive users have {@code isEnabled() == false}.
     */
    @Test
    @DisplayName("Inactive user maps isEnabled to false")
    public void testInactiveUserDisabled()
    {
        final User inactiveUser = new User("Pending", "User", "pending@xcmailr.test", "PendingPass123", "en");
        inactiveUser.setActive(false);
        inactiveUser.setAdmin(false);

        final UserPrincipal principal = new UserPrincipal(inactiveUser);
        assertFalse(principal.isEnabled(), "Inactive user should not be enabled in Spring Security");
    }

    /**
     * Verifies that active users have {@code isEnabled() == true}.
     */
    @Test
    @DisplayName("Active user maps isEnabled to true")
    public void testActiveUserEnabled()
    {
        final User activeUser = new User("Active", "User", "active@xcmailr.test", "ActivePass123", "en");
        activeUser.setActive(true);
        activeUser.setAdmin(false);

        final UserPrincipal principal = new UserPrincipal(activeUser);
        assertTrue(principal.isEnabled(), "Active user should be enabled in Spring Security");
    }

    /**
     * Verifies account lifecycle booleans (non-expired, non-locked).
     */
    @Test
    @DisplayName("Account non-expired and non-locked defaults to true")
    public void testAccountLifecycleFlags()
    {
        final User user = new User("John", "Smith", "john@xcmailr.test", "Pass123", "en");
        user.setActive(true);

        final UserPrincipal principal = new UserPrincipal(user);
        assertTrue(principal.isAccountNonExpired(), "Account should not be expired");
        assertTrue(principal.isAccountNonLocked(), "Account should not be locked");
        assertTrue(principal.isCredentialsNonExpired(), "Credentials should not be expired");
    }

    /**
     * Verifies password, username, and domain model passthrough.
     */
    @Test
    @DisplayName("Username and password match user entity")
    public void testAttributePassthrough()
    {
        final User user = new User("Passthrough", "Test", "pt@xcmailr.test", "Pass999", "de");
        user.setActive(true);

        final UserPrincipal principal = new UserPrincipal(user);
        assertEquals("pt@xcmailr.test", principal.getUsername(), "Username should match email");
        assertEquals(user.getPasswd(), principal.getPassword(), "Password should match user hash");
        assertSame(user, principal.getUser(), "Domain user entity should be accessible");
    }
}

