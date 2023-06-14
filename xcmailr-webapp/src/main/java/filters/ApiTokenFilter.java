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
import models.User;
import ninja.Context;
import ninja.Filter;
import ninja.FilterChain;
import ninja.Result;

/**
 * Ensures that a valid API token is provided and attaches the corresponding user/user ID to the context.
 */
public class ApiTokenFilter implements Filter
{
    @Override
    public Result filter(FilterChain chain, Context context)
    {
        String apiToken = getBearerToken(context);
        if (apiToken != null)
        {
            final User user = User.findUserByToken(apiToken);
            if (user != null)
            {
                context.setAttribute("user", user);
                context.setAttribute("userId", user.getId());
                return chain.next(context);
            }
        }

        return ApiResults.unauthorized();
    }

    private String getBearerToken(Context context)
    {
        String s = context.getHeader("Authorization");
        if (StringUtils.isNotBlank(s))
        {
            String[] parts = StringUtils.split(s);
            if (parts.length == 2)
            {
                if (parts[0].equals("Bearer"))
                {
                    return parts[1];
                }
            }
        }

        return null;
    }
}
