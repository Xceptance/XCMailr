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
package controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.Period;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.i18n.Messages;
import ninja.params.PathParam;
import ninja.utils.NinjaProperties;
import etc.HelperUtils;
import filters.AdminFilter;
import filters.SecureFilter;
import models.MailTransaction;
import models.PageList;
import models.User;

/**
 * Handles all Actions for the Administration-Section
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany.
 */

@FilterWith(
    {
        SecureFilter.class, AdminFilter.class
    })
@Singleton
public class AdminHandler
{
    @Inject
    NinjaProperties ninjaProp;

    @Inject
    Messages msg;

    @Inject
    MailrMessageHandlerFactory mmhf;

    @Inject
    MemCachedSessionHandler mcsh;

    // ---------------------Functions for the Admin-Section ---------------------
    /**
     * Shows a the Administration-Index-Page<br/>
     * GET site/admin
     * 
     * @param context
     * @return the Admin-Index-Page
     */
    public Result showAdmin(Context context, String no)
    {
        return Results.html();
    }

    /**
     * Shows a List of all {@link User}s in the DB <br/>
     * site/admin/users
     * 
     * @param context
     * @param no
     *            - the Number of Users per Page
     * @return a List of all Users
     */
    public Result showUsers(Context context)
    {
        User usr = (User) mcsh.get(context.getSessionCookie().getId());
        Map<String, Object> map = new HashMap<String, Object>();
        int entrys = HelperUtils.parseEntryValue(context);
        PageList<User> plist = new PageList<User>(User.all(), entrys);
        map.put("users", plist);
        map.put("uid", usr.getId());

        return Results.html().render(map);
    }

    /**
     * Shows a List of all {@link Status} in the DB <br/>
     * GET site/admin/summedtx
     * 
     * @param context
     * @param no
     *            - the Number of Transactions per Page
     * @return a List of all Status
     */
    public Result showSumTx(Context context, String no)
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("stats", MailTransaction.getStatusList());
        return Results.html().render(map);
    }

    /**
     * Shows a paginated List of all {@link MailTransaction}s in the DB <br/>
     * GET site/admin/mtxs
     * 
     * @param context
     * @param no
     *            - the Number of {@link MailTransaction}s per Page
     * @return the Page to show paginated MailTransactions
     */
    public Result pagedMTX(Context context)
    {
        Map<String, Object> map = new HashMap<String, Object>();
        PageList<MailTransaction> pl;
        int entrys = HelperUtils.parseEntryValue(context);
        pl = new PageList<MailTransaction>(MailTransaction.all(), entrys);
        
        map.put("plist", pl);
        return Results.html().render(map);
    }

    /**
     * Activates or Deactivates the User with the given ID <br/>
     * POST /admin/activate/{id}
     * 
     * @param id
     *            - ID of a User
     * @return the User-Overview-Page (/admin/users)
     */
    public Result activate(@PathParam("id") Long id, Context context)
    {
        User usr = (User) mcsh.get(context.getSessionCookie().getId());
        if (!(usr.getId() == id))
        { // the user to (de-)activate is not the user who performs this action

            // activate or deactivate the user
            boolean active = User.activate(id);

            // generate the (de-)activation-information mail and send it to the user
            User actusr = User.getById(id);
            String from = ninjaProp.get("mbox.adminaddr");
            String host = ninjaProp.get("mbox.host");
            Optional<String> opt = Optional.fromNullable(context.getAcceptLanguage());

            if (active)
            { // the account is now active
              // generate the message title
                Object[] param = new Object[]
                    {
                        host
                    };
                String subject = msg.get("i18nuser_activate_title", opt, param).get();
                // generate the message body
                param = new Object[]
                    {
                        actusr.getForename()
                    };
                String content = msg.get("i18nuser_activate_message", opt, param).get();
                // send the mail
                mmhf.sendMail(from, actusr.getMail(), content, subject);
            }
            else
            {// the account is now inactive
             // generate the message title
                Object[] param = new Object[]
                    {
                        host
                    };
                String subject = msg.get("i18nuser_deactivate_title", opt, param).get();
                // generate the message body
                param = new Object[]
                    {
                        actusr.getForename()
                    };
                String content = msg.get("i18nuser_deactivate_message", opt, param).get();
                // send the mail
                mmhf.sendMail(from, actusr.getMail(), content, subject);
            }
            return Results.redirect("/admin/users");
        }
        else
        {
            return Results.redirect("/admin/users");
        }
    }

    /**
     * Pro- or Demotes the {@link User} with the given ID <br/>
     * POST /admin/promote/{id}
     * 
     * @param id
     *            - ID of a {@link User}
     * @return the User-Overview-Page (/admin/users)
     */
    public Result promote(@PathParam("id") Long id, Context context)
    {
        User usr = (User) mcsh.get(context.getSessionCookie().getId());
        if (!(usr.getId() == id))
        { // the user to pro-/demote is not the user who performs this action
            User.promote(id);
        }
        return Results.redirect("/admin/users");
    }

    /**
     * Handles the {@link User}-Delete-Function <br/>
     * POST /admin/delete/{id}
     * 
     * @param id
     *            - the ID of a {@link User}
     * @return the User-Overview-Page (/admin/users)
     */
    public Result deleteUser(@PathParam("id") Long id, Context context)
    {
        User usr = (User) mcsh.get(context.getSessionCookie().getId());
        if (!(usr.getId() == id))
        { // the user to delete is not the user who performs this action
            User.delete(id);
        }

        return Results.redirect("/admin/users");
    }

}
