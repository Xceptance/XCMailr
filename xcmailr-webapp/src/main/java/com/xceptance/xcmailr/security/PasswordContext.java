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

import models.User;

/**
 * Contextual identity information of a user used during password validation to prevent
 * predictable passwords derived from personal attributes (OWASP ASVS v4.0 / NIST SP 800-63B).
 *
 * @param forename the user's first name, or {@code null} if unknown
 * @param surname the user's last name, or {@code null} if unknown
 * @param mail the user's target or login email address, or {@code null} if unknown
 *
 * @author Xceptance Software Technologies GmbH
 */
public record PasswordContext(String forename, String surname, String mail)
{
    /**
     * An empty context instance when no user personal details are available.
     */
    public static final PasswordContext EMPTY = new PasswordContext(null, null, null);

    /**
     * Factory method to create a context from explicit user identity values.
     *
     * @param forename the user's first name
     * @param surname the user's last name
     * @param mail the user's email address
     * @return populated {@link PasswordContext}
     */
    public static PasswordContext of(final String forename, final String surname, final String mail)
    {
        return new PasswordContext(forename, surname, mail);
    }

    /**
     * Factory method to extract contextual identity values from an existing {@link User} entity.
     *
     * @param user the domain user entity
     * @return populated {@link PasswordContext}, or {@link #EMPTY} if the user is {@code null}
     */
    public static PasswordContext fromUser(final User user)
    {
        if (user == null)
        {
            return EMPTY;
        }
        return new PasswordContext(user.getForename(), user.getSurname(), user.getMail());
    }
}

