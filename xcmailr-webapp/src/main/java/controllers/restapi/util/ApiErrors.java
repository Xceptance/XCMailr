package controllers.restapi.util;

import java.util.List;

/**
 * The data object that represents a single API error. Multiple errors may be returned as a list of {@link ApiErrors}
 * objects.
 */
public class ApiErrors
{
    /**
     *     
     */
    public final List<ApiError> errors;

    /**
     * @param parameter
     * @param message
     */
    public ApiErrors(List<ApiError> errors)
    {
        this.errors = errors;
    }
}
