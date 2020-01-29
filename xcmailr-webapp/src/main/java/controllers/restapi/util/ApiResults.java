package controllers.restapi.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ninja.Result;
import ninja.Results;
import ninja.validation.ConstraintViolation;

/**
 * Factory for {@link Result} objects returned by the REST API. Results that will carry a JSON body have the appropriate
 * content type already set.
 */
public class ApiResults
{
    /**
     * Creates a result with status code 200 (OK). The JSON body still needs to be set.
     * 
     * @return the result
     */
    public static Result ok()
    {
        return Results.ok().json();
    }

    /**
     * Creates a result with status code 201 (Created). The JSON body still needs to be set.
     * 
     * @return the result
     */
    public static Result created()
    {
        return Results.created(Optional.empty()).json();
    }

    /**
     * Creates a result with status code 204 (No Content) and no body.
     * 
     * @return the result
     */
    public static Result noContent()
    {
        return Results.noContent();
    }

    /**
     * Creates a result with status code 400 (Bad Request) and JSON body with a list of {@link ApiError} instances that
     * is created from the passed constraint violations.
     * 
     * @return the result
     */
    public static Result badRequest(List<ConstraintViolation> constraintViolations)
    {
        // convert constraint violations to API errors
        List<ApiError> errors = constraintViolations.stream()
                                                    .map(cv -> new ApiError(cv.getFieldKey(), cv.getDefaultMessage()))
                                                    .collect(Collectors.toList());

        return Results.badRequest().json().render(new ApiErrors(errors));
    }

    /**
     * Creates a result with status code 401 (Unauthorized) and no body.
     * 
     * @return the result
     */
    public static Result unauthorized()
    {
        return Results.unauthorized().render(Result.NO_HTTP_BODY);
    }

    /**
     * Creates a result with status code 403 (Forbidden) and JSON body with a list of a single {@link ApiError} instance
     * that is created from the passed field name and message.
     * 
     * @return the result
     */
    public static Result forbidden(String fieldName, String message)
    {
        List<ApiError> errors = new ArrayList<ApiError>();
        errors.add(new ApiError(fieldName, message));

        return Results.forbidden().json().render(new ApiErrors(errors));
    }

    /**
     * Creates a result with status code 404 (Not Found) and no body.
     * 
     * @return the result
     */
    public static Result notFound()
    {
        return Results.notFound().render(Result.NO_HTTP_BODY);
    }

    /**
     * Creates a result with status code 406 (Not Acceptable) and no body.
     * 
     * @return the result
     */
    public static Result notAcceptable()
    {
        return Results.status(406).render(Result.NO_HTTP_BODY);
    }

    /**
     * Creates a result with status code 415 (Unsupported Media Type) and no body.
     * 
     * @return the result
     */
    public static Result unsupportedMediaType()
    {
        return Results.status(415).render(Result.NO_HTTP_BODY);
    }

    /**
     * Creates a result with status code 500 (Internal Server Error) and no body.
     * 
     * @return the result
     */
    public static Result internalServerError()
    {
        return Results.internalServerError().render(Result.NO_HTTP_BODY);
    }
}
