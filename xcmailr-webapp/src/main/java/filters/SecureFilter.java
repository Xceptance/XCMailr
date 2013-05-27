package filters;

import com.google.inject.Inject;

import controllers.MemCachedSessionHandler;
import models.User;
import ninja.Context;
import ninja.Filter;
import ninja.FilterChain;
import ninja.Result;
import ninja.Results;

public class SecureFilter implements Filter
{
    @Inject
    MemCachedSessionHandler mcsh;

    @Override
    public Result filter(FilterChain chain, Context context)
    {
        User usr = (User) mcsh.get(context.getSessionCookie().getId());
        if (!(usr == null) && usr.isActive())
        {
            return chain.next(context);

        }
        else
        {
            return Results.redirect("/login");
        }
    }
}
