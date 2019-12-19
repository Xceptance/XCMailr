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
