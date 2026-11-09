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
package com.xceptance.xcmailr.controllers.restapi;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.xceptance.xcmailr.dto.ApiError;
import com.xceptance.xcmailr.dto.ApiErrors;

/**
 * Global REST exception handler translating Spring MVC {@link MethodArgumentNotValidException} validation
 * failures into standard {@link ApiErrors} (HTTP 400 Bad Request) payloads.
 * <p>
 * Ensures 100% backward compatibility with API clients such as {@code xcmailr-client}, exposing field
 * names under {@code errors[i].parameter} and validation failure descriptions under {@code errors[i].message}.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@RestControllerAdvice(basePackages = "com.xceptance.xcmailr.controllers.restapi")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestValidationExceptionHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(RestValidationExceptionHandler.class);

    /**
     * Intercepts bean validation failures on inbound {@code @Valid @RequestBody} parameters and constructs
     * an HTTP 400 Bad Request response containing mapped {@link ApiError} entries.
     *
     * @param ex the intercepted validation exception
     * @return HTTP 400 Bad Request response with structured {@link ApiErrors} payload
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrors> handleValidationException(final MethodArgumentNotValidException ex)
    {
        final BindingResult bindingResult = ex.getBindingResult();
        final List<ApiError> errorList = new ArrayList<>();

        for (final FieldError fieldError : bindingResult.getFieldErrors())
        {
            final String fieldName = fieldError.getField();
            final String errorMessage = fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "Invalid value.";

            errorList.add(new ApiError(fieldName, errorMessage));
        }

        // If no field errors found (e.g. global object-level error), fall back to top-level message
        if (errorList.isEmpty())
        {
            errorList.add(new ApiError("payload", "Request validation failed."));
        }

        LOG.debug("Intercepted REST API validation failure on {}: {} field error(s)",
                  bindingResult.getObjectName(), errorList.size());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiErrors(errorList));
    }
}
