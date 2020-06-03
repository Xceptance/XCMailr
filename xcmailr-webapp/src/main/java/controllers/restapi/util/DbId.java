/*  
 *  Copyright 2020 by the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */
package controllers.restapi.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import controllers.restapi.util.DbId.DbIdValidator;
import ninja.Context;
import ninja.validation.ConstraintViolation;
import ninja.validation.Validator;
import ninja.validation.WithValidator;

/**
 * Controller method parameters annotated with this annotation will automatically be checked to be valid database IDs.
 */
@WithValidator(DbIdValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface DbId
{
    /**
     * Returns the validation error message.
     * 
     * @return the message
     */
    public String message() default "Value is not a valid ID";

    /**
     * Validates fields/parameters that are annotated with {@link DbId}.
     */
    public static class DbIdValidator implements Validator<Long>
    {
        private final DbId dbIdAnnotation;

        public DbIdValidator(final DbId dbIdAnnotation)
        {
            this.dbIdAnnotation = dbIdAnnotation;
        }

        public void validate(Long value, String fieldName, Context context)
        {
            if (value == null)
            {
                context.getValidation()
                       .addViolation(new ConstraintViolation(null, fieldName, dbIdAnnotation.message(), value));
            }
        }

        public Class<Long> getValidatedType()
        {
            return Long.class;
        }
    }
}
