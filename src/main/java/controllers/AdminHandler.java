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
import etc.HelperUtils;
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
    public Result showUsers(Context context)
    {
        Map<String, List<User>> map = new HashMap<String, List<User>>();
        map.put("users", User.all());

        return Results.html().render(map);
    }

    /**
     * activates or deactivates the User with the given id site/admin/activate/id
     * 
     * @param id
     *            - id of a user
     * @return the admin-page
     */
    public Result activate(@PathParam("id") Long id, Context context)
    {
        // activate or deactivate the user
        boolean active = User.activate(id);

        // generate the (de-)activation-information mail and send it to the user
        User usr = User.getById(id);
        String from = ninjaProp.get("mbox.adminaddr");
        String host = ninjaProp.get("mbox.host");
        System.out.println(host);

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

    /**
     * pro- or demotes the User with the given id site/admin/promote/id
     * 
     * @param id
     *            - id of a user
     * @return the admin-page
     */
    public Result promote(@PathParam("id") Long id)
    {
        User.promote(id);
        return Results.redirect("/admin");

    }

    /**
     * Handles the user delete function site/admin/delete/id
     * 
     * @param id
     * @return
     */
    public Result deleteUser(@PathParam("id") Long id)
    {
        // TODO check whether the user is authorized to do this!
        User.delete(id);
        return Results.redirect("/admin");
    }

    public Result showStats()
    {
        // TODO implement a nice view (e.g. with pagination and/or time-filtered table)
        Map<String, List<?>> map = new HashMap<String, List<?>>();
        map.put("stats", MailTransaction.getStatusList());
        List<?> mtxs = MailTransaction.all();
        map.put("mtxs", mtxs);
        return Results.html().render(map);
    }

}
