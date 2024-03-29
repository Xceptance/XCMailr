/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
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
package controllers.restapi.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import controllers.restapi.util.Email.EmailValidator;
import etc.HelperUtils;
import ninja.Context;
import ninja.validation.ConstraintViolation;
import ninja.validation.Validator;
import ninja.validation.WithValidator;

/**
 * Controller method parameters annotated with this annotation will automatically be checked to be valid email addresses.
 */
@WithValidator(EmailValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Email
{
    /**
     * Returns the validation error message.
     * 
     * @return the message
     */
    public String message() default "Value is not a valid email address";

    /**
     * Validates fields/parameters that are annotated with {@link Email}.
     */
    public static class EmailValidator implements Validator<String>
    {
        private final Email emailAnnotation;

        public EmailValidator(final Email dbIdAnnotation)
        {
            this.emailAnnotation = dbIdAnnotation;
        }

        public void validate(String value, String fieldName, Context context)
        {
            final String[] parts = HelperUtils.splitMailAddress(value);

            if (parts == null || parts.length != 2)
            {
                context.getValidation()
                       .addViolation(new ConstraintViolation(null, fieldName, emailAnnotation.message(), value));
            }
        }

        public Class<String> getValidatedType()
        {
            return String.class;
        }
    }
}
