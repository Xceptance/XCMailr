/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
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
package filters;

import org.apache.commons.lang3.StringUtils;

import controllers.restapi.util.ApiResults;
import ninja.Context;
import ninja.Filter;
import ninja.FilterChain;
import ninja.Result;

/**
 * Checks that the client sends JSON bodies only and accepts either JSON or any responses.
 */
public class AcceptJsonFilter implements Filter
{
    @Override
    public Result filter(FilterChain chain, Context context)
    {
        String contentType = context.getHeader("Content-Type");
        if (contentType != null && !StringUtils.startsWith(contentType, "application/json"))
        {
            return ApiResults.unsupportedMediaType();
        }

        String accept = context.getHeader("Accept");
        if (accept != null && !StringUtils.startsWithAny(accept, "application/json", "*/*"))
        {
            return ApiResults.notAcceptable();
        }

        return chain.next(context);
    }
}
