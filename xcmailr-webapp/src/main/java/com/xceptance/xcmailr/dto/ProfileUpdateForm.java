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

import java.io.Serializable;
import java.util.Objects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form backing bean for user profile modification requests.
 * <p>
 * Enforces declarative server-side validation at the Spring MVC controller boundary, preventing
 * blank, whitespace-only, or oversized names from reaching persistence layers and causing
 * unhandled transaction rollbacks or database constraint violations.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
public class ProfileUpdateForm implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** First name / forename of the user. Mandatory, max 255 characters. */
    @NotBlank(message = "{profile.error.update.forenameRequired}")
    @Size(max = 255, message = "{profile.error.update.forenameSize}")
    private String forename;

    /** Last name / surname of the user. Mandatory, max 255 characters. */
    @NotBlank(message = "{profile.error.update.surnameRequired}")
    @Size(max = 255, message = "{profile.error.update.surnameSize}")
    private String surname;

    /** Preferred UI locale language tag (e.g. "en" or "de"). Defaults to "en". */
    @Size(max = 10)
    private String language = "en";

    /**
     * Default no-argument constructor for Spring MVC form binding and serialization.
     */
    public ProfileUpdateForm()
    {
    }

    /**
     * Convenience constructor initializing profile update form fields.
     *
     * @param forename first name
     * @param surname last name
     * @param language preferred UI language
     */
    public ProfileUpdateForm(final String forename,
                             final String surname,
                             final String language)
    {
        this.forename = forename;
        this.surname = surname;
        this.language = language != null ? language : "en";
    }

    /**
     * Returns the user's first name.
     *
     * @return candidate first name
     */
    public String getForename()
    {
        return forename;
    }

    /**
     * Sets the user's first name.
     *
     * @param forename candidate first name
     */
    public void setForename(final String forename)
    {
        this.forename = forename;
    }

    /**
     * Returns the user's last name.
     *
     * @return candidate last name
     */
    public String getSurname()
    {
        return surname;
    }

    /**
     * Sets the user's last name.
     *
     * @param surname candidate last name
     */
    public void setSurname(final String surname)
    {
        this.surname = surname;
    }

    /**
     * Returns the preferred language tag.
     *
     * @return language code (e.g. "en", "de")
     */
    public String getLanguage()
    {
        return language;
    }

    /**
     * Sets the preferred language tag.
     *
     * @param language language code
     */
    public void setLanguage(final String language)
    {
        this.language = language;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof final ProfileUpdateForm that))
        {
            return false;
        }
        return Objects.equals(forename, that.forename) &&
               Objects.equals(surname, that.surname) &&
               Objects.equals(language, that.language);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(forename, surname, language);
    }

    @Override
    public String toString()
    {
        return "ProfileUpdateForm{" +
               "forename='" + forename + '\'' +
               ", surname='" + surname + '\'' +
               ", language='" + language + '\'' +
               '}';
    }
}
