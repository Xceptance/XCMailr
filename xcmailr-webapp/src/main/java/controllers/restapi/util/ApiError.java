package controllers.restapi.util;

/**
 * The data object that represents a single API error. Multiple errors may be returned as a list of
 * {@link ApiError} objects.
 */
public class ApiError
{
    public final String field;

    public final String message;

    // TODO
    // public String errorCode;
    // public String originalValue;
    // public String helpUrl;

    public ApiError(String field, String message)
    {
        this.field = field;
        this.message = message;
    }
}
