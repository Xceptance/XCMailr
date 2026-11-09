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

/**
 * Data transfer object representing an individual REST API validation or processing error.
 */
public class ApiError
{
    /**
     * The name of the offending field, path parameter, or query parameter.
     */
    public final String parameter;

    /**
     * Human-readable description explaining the cause of the error.
     */
    public final String message;

    /**
     * Creates and initializes a new {@link ApiError} instance.
     *
     * @param parameter the name of the offending parameter or field
     * @param message the descriptive error message
     */
    public ApiError(final String parameter, final String message)
    {
        this.parameter = parameter;
        this.message = message;
    }
}
