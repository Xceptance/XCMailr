package filters;

import models.User;
import ninja.Context;
import ninja.Filter;
import ninja.FilterChain;
import ninja.Result;
import ninja.Results;

public class SecureFilter implements Filter
{
    // TODO check the authencity of the cookie?

    @Override
    public Result filter(FilterChain chain, Context context)
    {
        // if we got no cookies we break:

        if (context.getSessionCookie() == null || context.getSessionCookie().get("usrname") == null)
        {

            return Results.redirect("/login");

        }
        else
        {
            Long id = Long.parseLong(context.getSessionCookie().get("id"));
            User u = User.getById(id);

            // TODO possible error-point: the if-condition will only work while java uses short-circuiting
            if (!(u == null) && u.isActive())
            { // the user exists
                return chain.next(context);
            }
            else
            {
                // the user does not exist
                // this check is necessary, when a user was deleted and the client has nevertheless a cookie which is
                // not expired until "now"

                // we redirect to the login-page, because redirecting to the register-page would expose which accounts
                // exists (assuming that theres a possibility to fake the cookie)
                return Results.redirect("/login");

            }

        }
    }
}
