package filters;

import ninja.Context;
import ninja.Filter;
import ninja.FilterChain;
import ninja.Result;
import ninja.Results;

public class SecureFilter implements Filter{
	
	
	//public final String usrname = "usrname";
	@Override
	public Result filter(FilterChain chain, Context context) {
        // if we got no cookies we break:
		
        if (context.getSessionCookie() == null
                || context.getSessionCookie().get("usrname") == null) {
        
        
            return Results.forbidden().html().template("/views/forbidden403.ftl.html");

        } else {
            return chain.next(context);
        }
	}
}
