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

/**
 * The data object that represents a single API error.
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
     * Creates and initializes a new {@link ApiError} instance.
     * 
     * @param parameter
     *            the parameter name
     * @param message
     *            the error message
     */
    public ApiError(String parameter, String message)
    {
        this.parameter = parameter;
        this.message = message;
    }
}
