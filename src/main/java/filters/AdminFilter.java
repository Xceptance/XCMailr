package filters;

import com.google.inject.Inject;

import controllers.MemCachedSessionHandler;
import models.User;
import ninja.Context;
import ninja.Filter;
import ninja.FilterChain;
import ninja.Result;
import ninja.Results;

public class AdminFilter implements Filter
{
    @Inject
    MemCachedSessionHandler mcsh;

    @Override
    public Result filter(FilterChain chain, Context context)
    {
        User user = (User) mcsh.get(context.getSessionCookie().getId());
        if (!(user == null) && user.isActive() && user.isAdmin())
        {
            return chain.next(context);

        }
        else
        {
            return Results.redirect("/");
        }
    }
}
