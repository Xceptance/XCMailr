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
package com.xceptance.xcmailr.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Validates candidate passwords against modern OWASP ASVS v4.0 and NIST SP 800-63B guidelines
 * aligned with Spring Security's BCrypt cryptographic key schedule.
 * <p>
 * This validator enforces policy checks solely upon new credential creation and updates
 * (registration, password reset, and profile password changes) while leaving authentication
 * of existing legacy database credentials unhindered.
 * </p>
 * <p>
 * Enforced security controls:
 * <ul>
 *   <li><b>Minimum Length:</b> At least 10 characters to provide sufficient entropy.</li>
 *   <li><b>Maximum Length:</b> At most 72 characters aligned with BCrypt Blowfish key schedule.</li>
 *   <li><b>Maximum Byte Count:</b> At most 72 bytes in UTF-8 encoding to prevent runtime exceptions in BCrypt.</li>
 *   <li><b>Context Exclusion:</b> Rejection of passwords containing personal identity values
 *       (forename, surname, or email username/local part) case-insensitively.</li>
 *   <li><b>Repetition Defense:</b> Rejection of trivial passwords consisting of a single repeated character.</li>
 *   <li><b>Character Freedom:</b> No artificial composition rules (e.g. requiring symbols or digits),
 *       permitting spaces, punctuation, numbers, and Unicode/emojis without restriction.</li>
 * </ul>
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@Component
public class PasswordValidator
{
    /**
     * Minimum allowed password length (OWASP / NIST SP 800-63B recommendation).
     */
    public static final int MIN_LENGTH = 10;

    /**
     * Maximum allowed password length aligned with BCrypt's 72-byte key schedule limit.
     */
    public static final int MAX_LENGTH = 72;

    /**
     * Maximum allowed UTF-8 byte count supported by the BCrypt Blowfish key derivation algorithm.
     */
    public static final int MAX_BYTES = 72;

    /**
     * Minimum character length of contextual identity terms (forename, surname, email local-part)
     * required before evaluating substring containment, avoiding false-positive rejections on short initials.
     */
    public static final int MIN_CONTEXT_TERM_LENGTH = 3;

    /**
     * Validates a candidate password with no contextual user identity information.
     *
     * @param password the candidate raw password string
     * @return validation outcome result
     */
    public PasswordValidationResult validate(final String password)
    {
        return validate(password, PasswordContext.EMPTY);
    }

    /**
     * Validates a candidate password against OWASP security guidelines and user identity context.
     *
     * @param password the candidate raw password string
     * @param context user personal identity context (e.g. forename, surname, email)
     * @return validation outcome result with actionable error message upon failure
     */
    public PasswordValidationResult validate(final String password, final PasswordContext context)
    {
        // 1. Guard against null or empty input
        if (password == null || password.isEmpty())
        {
            return PasswordValidationResult.invalid("Password cannot be empty.", "auth.password.error.empty");
        }

        // 2. Minimum length check: 10 characters ensures adequate entropy
        if (password.length() < MIN_LENGTH)
        {
            return PasswordValidationResult.invalid("Password must be at least " + MIN_LENGTH + " characters long.", "auth.password.error.minLength");
        }

        // 3. Maximum character length check: 72 characters aligned with BCrypt key schedule
        if (password.length() > MAX_LENGTH)
        {
            return PasswordValidationResult.invalid("Password cannot exceed " + MAX_LENGTH + " characters.", "auth.password.error.maxLength");
        }

        // 4. Maximum UTF-8 byte length check: BCrypt strictly rejects inputs exceeding 72 bytes
        final byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        if (passwordBytes.length > MAX_BYTES)
        {
            return PasswordValidationResult.invalid("Password cannot exceed " + MAX_BYTES + " bytes (special or non-Latin characters use multiple bytes).", "auth.password.error.maxBytes");
        }

        // 5. Repetition defense: reject trivial passwords consisting of only one repeating character
        if (isSingleRepeatedCharacter(password))
        {
            return PasswordValidationResult.invalid("Password cannot consist of a single repeated character.", "auth.password.error.repeatedChar");
        }

        // 5. User context check: reject passwords containing personal identity values
        if (context != null)
        {
            final String passwordLower = password.toLowerCase(Locale.ROOT);
            final List<String> identityTerms = extractIdentityTerms(context);

            for (final String term : identityTerms)
            {
                if (term.length() >= MIN_CONTEXT_TERM_LENGTH && passwordLower.contains(term))
                {
                    return PasswordValidationResult.invalid("Password must not contain your name or email address.", "auth.password.error.containsIdentity");
                }
            }
        }

        // All OWASP rules satisfied
        return PasswordValidationResult.valid();
    }

    /**
     * Detects whether the candidate password consists solely of identical repeated characters.
     *
     * @param password candidate password to check
     * @return {@code true} if every character in the password is identical, {@code false} otherwise
     */
    private boolean isSingleRepeatedCharacter(final String password)
    {
        final char firstChar = password.charAt(0);
        for (int i = 1; i < password.length(); i++)
        {
            if (password.charAt(i) != firstChar)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Extracts normalized, lower-cased identity tokens from the supplied user context.
     *
     * @param context the user identity context
     * @return list of normalized identity terms for substring matching
     */
    private List<String> extractIdentityTerms(final PasswordContext context)
    {
        final List<String> terms = new ArrayList<>();

        if (context.forename() != null && !context.forename().isBlank())
        {
            terms.add(context.forename().trim().toLowerCase(Locale.ROOT));
        }

        if (context.surname() != null && !context.surname().isBlank())
        {
            terms.add(context.surname().trim().toLowerCase(Locale.ROOT));
        }

        if (context.mail() != null && !context.mail().isBlank())
        {
            final String cleanMail = context.mail().trim().toLowerCase(Locale.ROOT);
            final int atIndex = cleanMail.indexOf('@');
            final String localPart = (atIndex > 0) ? cleanMail.substring(0, atIndex) : cleanMail;

            if (!localPart.isBlank())
            {
                terms.add(localPart);

                // Split local part by common separators (e.g. john.doe -> john, doe)
                final String[] splitTokens = localPart.split("[.+_-]");
                for (final String token : splitTokens)
                {
                    if (!token.isBlank() && !terms.contains(token))
                    {
                        terms.add(token);
                    }
                }
            }
        }

        return terms;
    }
}

