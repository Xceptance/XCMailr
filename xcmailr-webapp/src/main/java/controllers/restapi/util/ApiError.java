package controllers.restapi.util;

/**
 * The data object that represents a single API error. Multiple errors may be returned as a list of {@link ApiError}
 * objects.
 */
public class ApiError
{
    // TODO?
    // public String errorCode;
    // public String originalValue;
    // public String helpUrl;
    
    /**
     * The name of the offending path or query parameter.
     */
    public final String parameter;

    /**
     * The message explaining the cause of the error.
     */
    public final String message;

    /**
     * 
     * @param parameter
     * @param message
     */
    public ApiError(String parameter, String message)
    {
        this.parameter = parameter;
        this.message = message;
    }
}
