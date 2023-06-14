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

/**
 * Ensures that the user is an admin, otherwise it will redirect to the index-page
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class AdminFilter implements Filter
{

    @Override
    public Result filter(FilterChain chain, Context context)
    {
        // get the user-object from context (if we get to this point, the SecureFilter should have added the object)
        User user = context.getAttribute("user", User.class);

        if (!(user == null) && user.isActive() && user.isAdmin())
            return chain.next(context);

        Result result = Results.html().template("/views/system/noContent.ftl.html");
        return result.redirect(context.getContextPath() + "/");
    }
}
