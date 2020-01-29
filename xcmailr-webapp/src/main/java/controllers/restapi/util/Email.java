package controllers.restapi.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.commons.lang3.StringUtils;

import controllers.restapi.util.Email.EmailValidator;
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
            String[] parts = StringUtils.split(StringUtils.defaultString(value), '@');

            if (parts.length != 2)
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
