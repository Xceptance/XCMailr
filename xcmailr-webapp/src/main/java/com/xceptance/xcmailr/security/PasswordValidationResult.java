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

/**
 * Immutable outcome of a password evaluation performed by {@link PasswordValidator}.
 * <p>
 * Carries a validity status flag alongside a human-readable, user-actionable error message
 * and an optional i18n message key when validation criteria (such as length or user context restrictions)
 * are violated.
 * </p>
 *
 * @param isValid true if the password meets all security policy requirements, false otherwise
 * @param errorMessage a user-facing explanatory error message if invalid, or {@code null} if valid
 * @param messageKey an optional message bundle key for localized display, or {@code null} if unset
 *
 * @author Xceptance Software Technologies GmbH
 */
public record PasswordValidationResult(boolean isValid, String errorMessage, String messageKey)
{
    /**
     * Singleton instance representing a fully compliant and accepted password.
     */
    private static final PasswordValidationResult VALID_INSTANCE = new PasswordValidationResult(true, null, null);

    /**
     * Backward-compatible secondary constructor defaulting {@code messageKey} to {@code null}.
     *
     * @param isValid validity status flag
     * @param errorMessage user-facing error message
     */
    public PasswordValidationResult(final boolean isValid, final String errorMessage)
    {
        this(isValid, errorMessage, null);
    }

    /**
     * Returns a successful validation result indicating that all password policy requirements were met.
     *
     * @return successful validation result
     */
    public static PasswordValidationResult valid()
    {
        return VALID_INSTANCE;
    }

    /**
     * Constructs a failure validation result with a specific user-actionable error message.
     *
     * @param message the explanation of the policy violation to display to the user
     * @return failure validation result containing the specified error message
     */
    public static PasswordValidationResult invalid(final String message)
    {
        return new PasswordValidationResult(false, message, null);
    }

    /**
     * Constructs a failure validation result with a user-actionable error message and corresponding i18n message key.
     *
     * @param message the default English explanation of the policy violation
     * @param messageKey the message bundle key for localized translation resolution
     * @return failure validation result containing the error message and message key
     */
    public static PasswordValidationResult invalid(final String message, final String messageKey)
    {
        return new PasswordValidationResult(false, message, messageKey);
    }
}
