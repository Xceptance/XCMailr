/**  
 *  Copyright 2013 the original author or authors.
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
 *
 */
package filters;

import com.google.inject.Inject;

import controllers.MemCachedSessionHandler;
import models.User;
import ninja.Context;
import ninja.Filter;
import ninja.FilterChain;
import ninja.Result;
import ninja.Results;

/**
 * Ensures that an user is logged in, otherwise it will redirect to the login-page
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class SecureFilter implements Filter
{
    @Inject
    MemCachedSessionHandler mcsh;

    @Override
    public Result filter(FilterChain chain, Context context)
    {
        // get the user-object from memcached-server
        User usr = (User) mcsh.get(context.getSessionCookie().getId());

        if ((usr != null) && usr.isActive())
        {
            // add the user-object to the context to reduce the no. of connections to the memcached server
            context.setAttribute("user", usr);

            if (context.getSessionCookie().get("adm") != null)
            { // user has admin-token at the cookie
                if (!usr.isAdmin())
                { // but is no admin -> remove it
                    context.getSessionCookie().remove("adm");
                }
            }
            else
            { // user has no admin-token
                if (usr.isAdmin())
                { // but he's admin
                  // set a admin-flag at the cookie if the user is admin
                  // we use this only to change the header-menu-view, but not for "real admin-actions"
                    context.getSessionCookie().put("adm", "1");
                }
            }
            // go to the next filter (or controller-method)
            return chain.next(context);
        }
        else
        {
            if (!context.getSessionCookie().isEmpty())
            { // delete the cookie if there's no user object but a session-cookie
                context.getSessionCookie().clear();
            }
            Result result = Results.forbidden().template("/views/system/noContent.ftl.html");
            return result.redirect(context.getContextPath() + "/login");
        }
    }
}
