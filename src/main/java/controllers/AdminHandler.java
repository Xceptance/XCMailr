package controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.i18n.Messages;
import ninja.params.PathParam;
import ninja.utils.NinjaProperties;
import filters.AdminFilter;
import filters.SecureFilter;

import models.MailTransaction;
import models.User;

/**
 * Handles all Actions for the Admin Section
 * 
 * @author Patrick Thum 2012 released under Apache 2.0 License
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
    MailHandler mailhndlr;

    // ---------------------Functions for the Admin-Section ---------------------
    /**
     * Shows a list of all Users in the DB site/admin
     * 
     * @param context
     * @return a list of all Users
     */
    public Result showAdmin(Context context)
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("users", User.all());
        map.put("stats", MailTransaction.getStatusList());
        List<?> mtxs = MailTransaction.all();
        map.put("mtxs", mtxs);
        map.put("uid", Long.parseLong(context.getSessionCookie().get("id")));
        return Results.html().render(map);
    }

    /**
     * activates or deactivates the User with the given id <br/>
     * POST /admin/activate/{id}
     * 
     * @param id
     *            - id of a user
     * @return the admin-page
     */
    public Result activate(@PathParam("id") Long id, Context context)
    {
        if ((User.getActiveAdminCount() == 1) ^ (User.isUserAdmin(id)))
        {

            // activate or deactivate the user
            boolean active = User.activate(id);

            // generate the (de-)activation-information mail and send it to the user
            User usr = User.getById(id);
            String from = ninjaProp.get("mbox.adminaddr");
            String host = ninjaProp.get("mbox.host");

            if (active)
            { // the account is now active
              // generate the message title
                Object[] param = new Object[]
                    {
                        host
                    };
                String subject = msg.get("i18nuser_activate_title", context.getAcceptLanguage(), param);
                // generate the message body
                param = new Object[]
                    {
                        usr.getForename()
                    };
                String content = msg.get("i18nuser_activate_message", context.getAcceptLanguage(), param);
                // send the mail
                mailhndlr.sendMail(from, usr.getMail(), content, subject);
            }
            else
            {// the account is now inactive
             // generate the message title
                Object[] param = new Object[]
                    {
                        host
                    };
                String subject = msg.get("i18nuser_deactivate_title", context.getAcceptLanguage(), param);
                // generate the message body
                param = new Object[]
                    {
                        usr.getForename()
                    };
                String content = msg.get("i18nuser_deactivate_message", context.getAcceptLanguage(), param);
                // send the mail
                mailhndlr.sendMail(from, usr.getMail(), content, subject);
            }

            return Results.redirect("/admin");
        }
        else
        {
            return Results.redirect("/admin");
        }
    }

    /**
     * Pro- or demotes the User with the given id <br/>
     * POST /admin/promote/{id}
     * 
     * @param id
     *            - id of a user
     * @return the admin-page
     */
    public Result promote(@PathParam("id") Long id)
    {
        if ((User.getActiveAdminCount() == 1) ^ (User.isUserAdmin(id)))
        { // don't demote the account when there is just one adminaccount and the given userid belongs to a user
            User.promote(id);
        }
        return Results.redirect("/admin");

    }

    /**
     * Handles the user delete function <br/>
     * POST /admin/delete/{id}
     * 
     * @param id
     * @return
     */
    public Result deleteUser(@PathParam("id") Long id)
    {
        if ((User.getActiveAdminCount() == 1) ^ (User.isUserAdmin(id)))
        { // don't remove the account when there is just one adminaccount and the given userid belongs to a user
            User.delete(id);
        }

        return Results.redirect("/admin");
    }

}
