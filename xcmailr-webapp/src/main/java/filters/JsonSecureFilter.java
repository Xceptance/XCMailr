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
 * Ensures that an user is logged in, otherwise it will redirect to the login-page
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class JsonSecureFilter implements Filter
{
    @Inject
    CachingSessionHandler csh;

    @Override
    public Result filter(FilterChain chain, Context context)
    {
        // get the user-object from memcached-server
        User usr = (User) csh.get(context.getSession().getId());

        if ((usr != null) && usr.isActive())
        {
            // add the user-object to the context to reduce the no. of connections to the caching server
            context.setAttribute("user", usr);

            if (context.getSession().get("adm") != null)
            { // user has admin-token at the cookie
                if (!usr.isAdmin())
                { // but is no admin -> remove it
                    context.getSession().remove("adm");
                }
            }
            else
            { // user has no admin-token
                if (usr.isAdmin())
                { // but he's admin
                  // set a admin-flag at the cookie if the user is admin
                  // we use this only to change the header-menu-view, but not for "real admin-actions"
                    context.getSession().put("adm", "1");
                }
            }
            if (!usr.getMail().equals(context.getSession().get("user")))
            {
                context.getSession().put("username", usr.getMail());
            }
            // go to the next filter (or controller-method)
            return chain.next(context);
        }
        else
        {
            if (!context.getSession().isEmpty())
            { // delete the cookie if there's no user object but a session-cookie
                context.getSession().clear();
            }
            Result result = Results.json();
            return result.render("error", "nologin");
        }
    }
}
