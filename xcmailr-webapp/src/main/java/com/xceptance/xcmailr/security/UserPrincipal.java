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

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import models.User;

/**
 * Spring Security {@link UserDetails} implementation wrapping an XCMailr {@link User} entity.
 *
 * @author Xceptance Software Technologies GmbH
 */
public class UserPrincipal implements UserDetails
{
    private static final long serialVersionUID = 1L;

    private final User user;
    private final List<GrantedAuthority> authorities;

    /**
     * Constructs a UserPrincipal from the given domain user entity.
     *
     * @param user the persistent domain user
     */
    public UserPrincipal(final User user)
    {
        this.user = user;
        if (user.isAdmin())
        {
            this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        else
        {
            this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }

    /**
     * @return the underlying domain user entity
     */
    public User getUser()
    {
        return user;
    }

    /**
     * @return the primary key ID of the domain user
     */
    public Long getId()
    {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        return authorities;
    }

    @Override
    public String getPassword()
    {
        return user.getPasswd();
    }

    @Override
    public String getUsername()
    {
        return user.getMail();
    }

    @Override
    public boolean isAccountNonExpired()
    {
        return true;
    }

    @Override
    public boolean isAccountNonLocked()
    {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired()
    {
        return true;
    }

    @Override
    public boolean isEnabled()
    {
        return user.isActive();
    }
}
