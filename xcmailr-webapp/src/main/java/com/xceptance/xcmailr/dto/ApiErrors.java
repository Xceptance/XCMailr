/*
 * Copyright (c) 2013-2026 Xceptance Software Technologies GmbH
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

import java.util.List;

/**
 * Data transfer object representing a collection of API errors returned
 * when input validation fails or client requests are rejected.
 */
public class ApiErrors
{
    /**
     * The list of individual {@link ApiError} instances.
     */
    public final List<ApiError> errors;

    /**
     * Constructs a new {@code ApiErrors} wrapper containing the given list of errors.
     *
     * @param errors the list of API errors, must not be {@code null}
     */
    public ApiErrors(final List<ApiError> errors)
    {
        this.errors = errors;
    }
}
