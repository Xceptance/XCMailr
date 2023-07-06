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

import models.User;
import ninja.Context;
import ninja.Filter;
import ninja.FilterChain;
import ninja.Result;
import ninja.Results;

import com.google.inject.Inject;

import controllers.CachingSessionHandler;

/**
 * Ensures that the user is NOT logged in, otherwise it will redirect to the index-page
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class NoLoginFilter implements Filter
{
    @Inject
    CachingSessionHandler csh;

    @Override
    public Result filter(FilterChain chain, Context context)
    {
        User usr = (User) csh.get(context.getSession().getId());
        if (usr == null)
        {
            return chain.next(context);

        }
        else
        {
            return Results.redirect(context.getContextPath() + "/");
        }
    }
}
