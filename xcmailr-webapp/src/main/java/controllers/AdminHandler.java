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
import java.util.Map;

import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import conf.XCMailrConf;
import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.i18n.Messages;
import ninja.params.PathParam;
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
    XCMailrConf xcmConf;

    @Inject
    Messages msg;

    @Inject
    MailrMessageSenderFactory mmhf;

    /**
     * Shows the Administration-Index-Page<br/>
     * GET site/admin
     * 
     * @param context
     *            the Context of this Request
     * @return the Admin-Index-Page
     */
    public Result showAdmin(Context context)
    {
        return Results.html();
    }

    /**
     * Shows a List of all {@link models.User Users} in the DB <br/>
     * GET site/admin/users
     * 
     * @param context
     *            the Context of this Request
     * @return a List of all Users
     */
    public Result showUsers(Context context)
    {
        User usr = context.getAttribute("user", User.class);

        Map<String, Object> map = new HashMap<String, Object>();

        // set a default number or the number which the user had chosen
        HelperUtils.parseEntryValue(context, xcmConf.APP_DEFAULT_ENTRYNO);

        // get the default number of entries per page
        int entries = Integer.parseInt(context.getSessionCookie().get("no"));

        // generate the paged-list to get pagination in the pattern
        PageList<User> plist = new PageList<User>(User.all(), entries);
        map.put("users", plist);

        // put in the users-ID to identify the row where no buttons will be shown
        map.put("uid", usr.getId());

        return Results.html().render(map);
    }

    /**
     * Shows a List of all {@link models.Status Status} in the DB <br/>
     * GET site/admin/summedtx
     * 
     * @param context
     *            the Context of this Request
     * @return a List of all Status
     */
    public Result showSumTx(Context context)
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("stats", MailTransaction.getStatusList());
        return Results.html().render(map);
    }

    /**
     * Shows a paginated List of all {@link models.MailTransaction Mailtransactions} in the DB <br/>
     * GET site/admin/mtxs
     * 
     * @param context
     *            the Context of this Request
     * @return the Page to show paginated MailTransactions
     */
    public Result pagedMTX(Context context)
    {
        Map<String, Object> map = new HashMap<String, Object>();

        // set a default number or the number which the user had chosen
        HelperUtils.parseEntryValue(context, xcmConf.APP_DEFAULT_ENTRYNO);
        // get the default number of entries per page
        int entries = Integer.parseInt(context.getSessionCookie().get("no"));

        // generate the paged-list to get pagination on the page
        PageList<MailTransaction> pl = new PageList<MailTransaction>(MailTransaction.all("ts desc"), entries);

        map.put("plist", pl);
        return Results.html().render(map);
    }

    /**
     * Delete a time-specified number of MailTransactions
     * 
     * @param time
     *            the time in days (all before will be deleted)
     * @return to the MailTransaction-Page
     */

    public Result deleteMTX(@PathParam("time") Integer time)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        if (time == -1)
        { // all entries will be deleted
            MailTransaction.deleteTxInPeriod(null);
        }
        else
        {
            // calculate the time and delete all entries before
            DateTime dt = DateTime.now().minusDays(time);
            MailTransaction.deleteTxInPeriod(dt.getMillis());
        }

        return result.redirect("/admin/mtxs");
    }

    /**
     * Activates or Deactivates the User with the given ID <br/>
     * POST /admin/activate/{id}
     * 
     * @param id
     *            ID of a User
     * @param context
     *            the Context of this Request
     * @return the User-Overview-Page (/admin/users)
     */
    public Result activate(@PathParam("id") Long id, Context context)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        User usr = context.getAttribute("user", User.class);
        if (!(usr.getId() == id))
        { // the user to (de-)activate is not the user who performs this action

            // activate or deactivate the user
            boolean active = User.activate(id);

            // generate the (de-)activation-information mail and send it to the user
            User actusr = User.getById(id);
            String from = xcmConf.ADMIN_ADD;
            String host = xcmConf.MB_HOST;
            Optional<String> opt = Optional.fromNullable(context.getAcceptLanguage());

            if (active)
            { // the account is now active
              // generate the message title

                String subject = msg.get("i18nUser_Activate_Title", opt, host).get();
                // generate the message body

                String content = msg.get("i18nUser_Activate_Message", opt, actusr.getForename()).get();
                // send the mail
                mmhf.sendMail(from, actusr.getMail(), content, subject);
            }
            else
            {// the account is now inactive
             // generate the message title
                String subject = msg.get("i18nUser_Deactivate_Title", opt, host).get();
                // generate the message body
                String content = msg.get("i18nUser_Deactivate_Message", opt, actusr.getForename()).get();
                // send the mail
                mmhf.sendMail(from, actusr.getMail(), content, subject);
            }
            return result.redirect("/admin/users");
        }
        else
        { // the admin wants to disable his own account, this is not allowed
            return result.redirect("/admin/users");
        }
    }

    /**
     * Pro- or Demotes the {@link models.User User} with the given ID <br/>
     * POST /admin/promote/{id}
     * 
     * @param id
     *            ID of a {@link models.User User}
     * @param context
     *            the Context of this Request
     * @return the User-Overview-Page (/admin/users)
     */
    public Result promote(@PathParam("id") Long id, Context context)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        User usr = context.getAttribute("user", User.class);
        if (!(usr.getId() == id))
        { // the user to pro-/demote is not the user who performs this action
            User.promote(id);
        }
        return result.redirect("/admin/users");
    }

    /**
     * Handles the {@link models.User User}-Delete-Function <br/>
     * POST /admin/delete/{id}
     * 
     * @param id
     *            the ID of a {@link models.User User}
     * @param context
     *            the Context of this Request
     * @return the User-Overview-Page (/admin/users)
     */
    public Result deleteUser(@PathParam("id") Long id, Context context)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        User usr = context.getAttribute("user", User.class);
        if (!(usr.getId() == id))
        { // the user to delete is not the user who performs this action
            User.delete(id);
        }

        return result.redirect("/admin/users");
    }

}
