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
package com.xceptance.xcmailr.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form backing data transfer object for user account registration.
 * <p>
 * Provides declarative Jakarta Bean Validation constraints to reject missing, blank,
 * or out-of-bounds inputs at the Spring MVC controller boundary before database persistence.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
public class RegistrationForm
{
    /** Regex matching standard valid email addresses with proper domain labels and TLD. */
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$";

    /** First name of the registering user. Mandatory, max 255 characters. */
    @NotBlank(message = "{auth.error.register.forenameRequired}")
    @Size(max = 255, message = "{auth.error.register.forenameSize}")
    private String forename;

    /** Last name / surname of the registering user. Mandatory, max 255 characters. */
    @NotBlank(message = "{auth.error.register.surnameRequired}")
    @Size(max = 255, message = "{auth.error.register.surnameSize}")
    private String surname;

    /** Target email address where test emails are forwarded. Mandatory, valid email syntax, max 255 characters. */
    @NotBlank(message = "{auth.error.register.mailRequired}")
    @Email(regexp = EMAIL_PATTERN, message = "{auth.error.register.mailInvalid}")
    @Size(max = 255, message = "{auth.error.register.mailSize}")
    private String mail;

    /** Plaintext password candidate. Mandatory, max 255 characters. Password policy (10-72 chars, UTF-8 bytes, repetition, context) is validated by PasswordValidator. */
    @NotBlank(message = "{auth.error.register.passwordRequired}")
    @Size(max = 255, message = "{auth.error.register.passwordSize}")
    private String password;

    /** Password confirmation repetition to prevent typos during registration. Mandatory. */
    @NotBlank(message = "{auth.error.register.confirmPasswordRequired}")
    @Size(max = 255, message = "{auth.error.register.passwordSize}")
    private String confirmPassword;

    /** Preferred UI locale language tag (e.g. "en" or "de"). Defaults to "en". */
    private String language = "en";

    /**
     * Default no-argument constructor for Spring MVC form binding and serialization.
     */
    public RegistrationForm()
    {
    }

    /**
     * Convenience constructor initializing all registration form fields.
     *
     * @param forename first name
     * @param surname last name
     * @param mail target email address
     * @param password candidate password
     * @param confirmPassword repeated confirmation password
     * @param language preferred UI language
     */
    public RegistrationForm(final String forename,
                            final String surname,
                            final String mail,
                            final String password,
                            final String confirmPassword,
                            final String language)
    {
        this.forename = forename;
        this.surname = surname;
        this.mail = mail;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.language = language != null ? language : "en";
    }

    /**
     * Returns the user's first name.
     *
     * @return first name string
     */
    public String getForename()
    {
        return forename;
    }

    /**
     * Sets the user's first name.
     *
     * @param forename first name string
     */
    public void setForename(final String forename)
    {
        this.forename = forename;
    }

    /**
     * Returns the user's last name.
     *
     * @return last name string
     */
    public String getSurname()
    {
        return surname;
    }

    /**
     * Sets the user's last name.
     *
     * @param surname last name string
     */
    public void setSurname(final String surname)
    {
        this.surname = surname;
    }

    /**
     * Returns the user's target email address.
     *
     * @return email address string
     */
    public String getMail()
    {
        return mail;
    }

    /**
     * Sets the user's target email address.
     *
     * @param mail email address string
     */
    public void setMail(final String mail)
    {
        this.mail = mail;
    }

    /**
     * Returns the candidate plaintext password.
     *
     * @return password string
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the candidate plaintext password.
     *
     * @param password password string
     */
    public void setPassword(final String password)
    {
        this.password = password;
    }

    /**
     * Returns the confirmation password string.
     *
     * @return confirmation password string
     */
    public String getConfirmPassword()
    {
        return confirmPassword;
    }

    /**
     * Sets the confirmation password string.
     *
     * @param confirmPassword confirmation password string
     */
    public void setConfirmPassword(final String confirmPassword)
    {
        this.confirmPassword = confirmPassword;
    }

    /**
     * Returns the preferred UI language tag.
     *
     * @return language tag string (e.g. "en")
     */
    public String getLanguage()
    {
        return language;
    }

    /**
     * Sets the preferred UI language tag.
     *
     * @param language language tag string
     */
    public void setLanguage(final String language)
    {
        this.language = language;
    }
}
