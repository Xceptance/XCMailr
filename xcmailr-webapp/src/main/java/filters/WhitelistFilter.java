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

import conf.XCMailrConf;
import ninja.Context;
import ninja.Filter;
import ninja.FilterChain;
import ninja.Result;
import ninja.Results;

/**
 * Filters the Handling of the whitelist<br/>
 * If the function is set to active (true) at application.whitelist in application.conf-file, then the functions will be
 * available otherwise, theres just a hint for activation displayed
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class WhitelistFilter implements Filter
{
    @Inject
    XCMailrConf xcmConf;

    @Override
    public Result filter(FilterChain chain, Context context)
    {

        if (xcmConf.APP_WHITELIST)
        { // go ahead, the whitelist-function is active
            return chain.next(context);
        }
        else
        { // show configuration hint, whitelisting is disabled
            Result result = Results.html().template("/views/AdminHandler/showDomainWhitelist.ftl.html");
            return result.render("whitelist", false);
        }
    }
}
