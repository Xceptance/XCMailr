package filters;

import ninja.Context;
import ninja.Filter;
import ninja.FilterChain;
import ninja.Result;
import ninja.Results;

public class AdminFilter implements Filter
{
    // TODO check the authenticity of the cookie?

    @Override
    public Result filter(FilterChain chain, Context context)
    {
        // if we got no cookies we break:

        if (context.getSessionCookie() == null || context.getSessionCookie().get("adm") == null)
        {
            
            return Results.redirect("/");

        }
        else
        {
            return chain.next(context);
        }
    }
}
