package controllers;

import com.avaje.ebean.Ebean;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import etc.HelperUtils;
import models.EditUsr;
import models.Login;
import ninja.Context;
import models.User;
import ninja.Result;
import ninja.Results;
import ninja.i18n.Lang;
import ninja.i18n.Messages;
import ninja.utils.NinjaProperties;
import ninja.validation.JSR303Validation;
import ninja.validation.Validation;

/**
 * Handles all general application actions like login, logout, forgot password or index page
 * 
 * @author Patrick Thum 2012 released under Apache 2.0 License
 */
@Singleton
public class Application
{

    @Inject
    Lang lang;

    @Inject
    Messages msg;

    @Inject
    NinjaProperties ninjaProp;

    public Result index(Context context)
    {

        if (context.getSessionCookie().isEmpty())
        {
            // show the default index page if there's no user
            return Results.ok().html();
        }
        else
        {
            // show the logged-in-index page if the user's logged in
            return Results.html().template("/views/Application/indexLogin.ftl.html");
        }

    }

    // -------------------- Registration -----------------------------------
    /**
     * shows the registration form
     * 
     * @return
     */

    public Result registerForm()
    {

        return Results.html();
    }

    /**
     * Creates the User (POST for register)
     * 
     * @return
     */

    public Result postRegisterForm(Context context, @JSR303Validation EditUsr frdat, Validation validation)
    {
        Result result = Results.html();
        String s;

        if (validation.hasViolations())
        {
            s = msg.get("msg_formerr", context, result, "String");
            context.getFlashCookie().error(s, (Object) null);

            return Results.html().render(frdat);
        }
        else
        { // form was filled correctly, go on!

            if (!User.mailExists(frdat.getMail()))
            {
                // a new user, check if the passwords are matching

                if (frdat.getPw().equals(frdat.getPwn1()))
                {
                    // create the user
                    User.createUser(frdat.getAsUser());

                    s = msg.get("msg_regok", context, result, "String");
                    context.getFlashCookie().success(s, (Object) null);

                    return Results.redirect("/");

                }
                else
                {

                    // password mismatch
                    s = msg.get("msg_formerr", context, result, "String");
                    context.getFlashCookie().error(s, (Object) null);
                    return Results.redirect("/register");

                }
            }
            else
            { // mailadress already exists

                // TODO should we really show this message? or rather an unspecified msg (msg_formerr)?
                // [SEC] bruteforcing this form would expose existing mailadresses
                s = msg.get("msg_mailex", context, result, "String");
                context.getFlashCookie().error(s, (Object) null);
                return Results.redirect("/register");
            }
        }

    }

    // -------------------- Login/-out Functions -----------------------------------

    /**
     * shows the login form
     * 
     * @return the rendered login form
     */
    public Result loginForm()
    {
        return Results.html();
    }

    /**
     * Handles the logout process
     * 
     * @return the index page
     */
    public Result logout(Context context)
    {
        context.getSessionCookie().clear();
        Result result = Results.html();
        String s = msg.get("msg_logout", context, result, "String");
        context.getFlashCookie().success(s, (Object) null);
        return Results.redirect("/");
    }

    /**
     * Handles the login-process POST for /register
     * 
     * @return the login form or the index page
     */
    public Result loggedInForm(Context context, @JSR303Validation Login l, Validation validation)
    {
        Result result = Results.html();
        String s;

        if (validation.hasViolations())
        {
            l.setPwd("");
            s = msg.get("msg_formerr", context, result, "String");
            context.getFlashCookie().error(s, (Object) null);
            return Results.html().template("views/Application/loginForm.ftl.html").render(l);

        }
        else
        {
            User lgr = User.auth(l.getMail().toLowerCase(), l.getPwd());
            // get the user if authentication was correct
            if (lgr != null)
            { // correct login
              // set the cookie
                context.getSessionCookie().put("id", String.valueOf(lgr.getId()));
                context.getSessionCookie().put("usrname", lgr.getMail());

                if (lgr.isAdmin())
                {
                    // also set an admin-flag if the account is an admin-account
                    context.getSessionCookie().put("adm", String.valueOf(true));
                }
                // TODO: ADM-Zugriff per DB, nicht per Cookie?

                s = msg.get("msg_login", context, result, "String");
                context.getFlashCookie().success(s, (Object) null);
                return Results.html();
            }
            else
            { // the authentication was not correct
                l.setPwd("");
                s = msg.get("msg_formerr", context, result, "String");
                context.getFlashCookie().error(s, (Object) null);
                return Results.html().template("views/Application/loginForm.ftl.html").render(l);
            }
        }
    }

    /**
     * shows the forgot pw page
     * 
     * @return forgot-pw-form
     */
    public Result forgotPwForm()
    {
        return Results.html();
    }

    /**
     * generates a new password and sends it to the user
     * 
     * @return index page
     */
    public Result pwResend(Context context, @JSR303Validation Login l, Validation validation)
    {
        Result result = Results.html();
        String s;

        if (validation.hasViolations())
        {
            // some fields weren't filled
            s = msg.get("msg_formerr", context, result, "String");
            context.getFlashCookie().error(s, (Object) null);
            return Results.redirect("/pwresend");
        }
        else
        {

            User usr = User.getUsrByMail(l.getMail());
            if (usr != null)
            { // mailadress was correct (exists in the DB)
              // generate a new pw and send it to the given mailadress
                String newPw = sendMail(usr.getMail(), usr.getForename(), context.getAcceptLanguage());
                if (newPw.equals(null))
                {
                    s = msg.get("forgpw_succ", context, result, "String");
                    context.getFlashCookie().success(s, (Object) null);
                    return Results.redirect("/");

                }
                else
                {
                    // set the new pw in the db
                    usr.hashPasswd(newPw);
                    Ebean.update(usr);

                    s = msg.get("forgpw_succ", context, result, "String");
                    context.getFlashCookie().success(s, (Object) null);
                    return Results.redirect("/");

                }
            }

            /*
             * TODO what shall we do here?
             * 
             * ->Situation: we get here, when the mailaddress does not exist in the db
             * 
             * Ideas:
             * 
             * 1. as its now: show the "msg_formerr" and return to the pwresend-page -> its now possible to
             * automatically send POST-Requests until a valid address has been found
             * 
             * 2. redirect to the index-page and show the error there (do this also when the form has errors) (or
             * conversly, redirect the success-case to the pwresend-page and show the message there-> spammers now have
             * to go deeper into the page, because they have then not just to check the redirect-target, as well as the
             * returned error/success message in the html-body
             * 
             * 3. ???
             * 
             * TODO implement captchas!
             */

            s = msg.get("msg_formerr", context, result, "String");
            context.getFlashCookie().error(s, (Object) null);
            return Results.redirect("/pwresend");
        }

    }

    /**
     * sends the forgot-password mail to a user
     * 
     * @param mail
     *            recipient address of a user
     * @param forename
     *            name of a user for the text
     * @return the password to set it in the db
     */

    private String sendMail(String mail, String forename, String lang)
    {
        //
        String from = ninjaProp.get("mbox.adminaddr");
        String subject = msg.get("forgpw_title", lang, (Object) null);

        // TODO use the ninjaProp.getOrDie method to prevent a NullPtEx here or alternatively sorround the
        // integer.valueof with a try-catch and catch the numberformatEx to set a default length
        String rueck = HelperUtils.getRndString(Integer.valueOf(ninjaProp.get("pw.length")));
        Object[] param = new Object[]
            {
                forename, rueck
            };

        String content = msg.get("forgpw_msg", lang, param);
        boolean wasSent = HelperUtils.sendMail(from, mail, content, subject);
        if (wasSent)
        { // if the message was successfully sent, return the new pwd
            return rueck;
        }
        else
        {
            return null;
        }
    }
}
