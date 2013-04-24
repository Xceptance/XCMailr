package controllers;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import etc.HelperUtils;
import models.EditUsr;
import models.Login;
import models.PwData;
import ninja.Context;
import models.User;
import ninja.Result;
import ninja.Results;
import ninja.i18n.Lang;
import ninja.i18n.Messages;
import ninja.params.PathParam;
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

    @Inject
    MailHandler mailhndlr;

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
            s = msg.get("msg_formerr", context, result, (Object) null);
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
                    User u = frdat.getAsUser();

                    // generate the confirmation-token
                    u.setConfirmation(HelperUtils.getRndSecureString(20));
                    u.setTs_confirm(DateTime.now().plusHours(ninjaProp.getIntegerWithDefault("confirm.period", 1))
                                            .getMillis());

                    User.createUser(u);
                    mailhndlr.sendConfirmAddressMail(u.getMail(), u.getForename(), String.valueOf(u.getId()),
                                                     u.getConfirmation(), context.getAcceptLanguage().toString());

                    s = msg.get("msg_regok", context, result, (Object) null);
                    context.getFlashCookie().success(s, (Object) null);

                    return Results.redirect("/");

                }
                else
                {

                    // password mismatch
                    s = msg.get("msg_formerr", context, result, (Object) null);
                    context.getFlashCookie().error(s, (Object) null);
                    return Results.redirect("/register");

                }
            }
            else
            { // mailadress already exists
                s = msg.get("msg_mailex", context, result, (Object) null);
                context.getFlashCookie().error(s, (Object) null);
                return Results.redirect("/register");
            }
        }
    }

    public Result verifyActivation(@PathParam("id") Long id, @PathParam("token") String token, Context context)
    {
        User u = User.getById(id);
        if (!(u == null))
        { // the user exists
            if ((u.getConfirmation().equals(token)) && (u.getTs_confirm() >= DateTime.now().getMillis()))
            { // the passed token is the right one
              // so activate the user
                u.setActive(true);
                User.updateUser(u);
                context.getFlashCookie().success("Erfolgreich aktiviert!", (Object) null);
                return Results.redirect("/");
                // TODO create the i18n messages
            }
        }
        context.getFlashCookie().error("FEHLER BEI DER AKTIVIERUNG!", (Object) null);
        return Results.redirect("/");
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
        String s = msg.get("msg_logout", context, result, (Object) null);
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
            s = msg.get("msg_formerr", context, result, (Object) null);
            context.getFlashCookie().error(s, (Object) null);
            return Results.html().template("views/Application/loginForm.ftl.html").render(l);

        }
        else
        {
            User lgr = User.getUsrByMail(l.getMail());
            if (!(lgr == null))
            {// the user exists
                if (lgr.checkPasswd(l.getPwd()))
                { // correct login
                    if (!lgr.isActive())
                    {
                        s = msg.get("i18nuser_inactive", context, result, (Object) null);
                        context.getFlashCookie().error(s, (Object) null);
                        return Results.redirect("/");
                    }

                    // set the cookie
                    context.getSessionCookie().put("id", String.valueOf(lgr.getId()));
                    context.getSessionCookie().put("usrname", lgr.getMail());

                    if (lgr.isAdmin())
                    {
                        // also set an admin-flag if the account is an admin-account
                        context.getSessionCookie().put("adm", String.valueOf(true));
                    }
                    // TODO: ADM-Zugriff per DB, nicht per Cookie?

                    lgr.setBadPwCount(0);
                    User.updateUser(lgr);
                    s = msg.get("msg_login", context, result, (Object) null);
                    context.getFlashCookie().success(s, (Object) null);
                    return Results.html();
                }
                else
                { // the authentication was not correct
                    lgr.setBadPwCount(lgr.getBadPwCount() + 1);

                    User.updateUser(lgr);

                    if (lgr.getBadPwCount() >= 6)
                    { // the password was six times wrong
                        lgr.setActive(false);
                        User.updateUser(lgr);

                        // show the disabled message and return to the forgot-pw-page
                        s = msg.get("i18nuser_disabled", context, result, (Object) null);
                        context.getFlashCookie().error(s, (Object) null);
                        return Results.redirect("/pwresend");
                    }

                    l.setPwd("");
                    s = msg.get("msg_formerr", context, result, (Object) null);
                    context.getFlashCookie().error(s, (Object) null);
                    return Results.html().template("views/Application/loginForm.ftl.html").render(l);
                }
            }
            else
            {// the user does not exist
                l.setPwd("");
                s = msg.get("msg_formerr", context, result, (Object) null);
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
            s = msg.get("msg_formerr", context, result, (Object) null);
            context.getFlashCookie().error(s, (Object) null);
            return Results.redirect("/pwresend");
        }
        else
        {
            User usr = User.getUsrByMail(l.getMail());
            if (!(usr == null))
            { // mailadress was correct (exists in the DB)
              // generate a new pw and send it to the given mailadress

                // generate the confirmation-token
                usr.setConfirmation(HelperUtils.getRndSecureString(20));
                usr.setTs_confirm(DateTime.now().plusHours(ninjaProp.getIntegerWithDefault("confirm.period", 1))
                                          .getMillis());

                User.updateUser(usr);
                mailhndlr.sendPwForgotAddressMail(usr.getMail(), usr.getForename(), String.valueOf(usr.getId()),
                                                  usr.getConfirmation(), context.getAcceptLanguage().toString());
                s = msg.get("i18nforgpw_succ", context, result, (Object) null);
                context.getFlashCookie().error(s, (Object) null);
                return Results.redirect("/");
            }

            /*
             * The user doesn't exist in the db, but we show him the success-msg anyway
             */

            s = msg.get("i18nforgpw_succ", context, result, (Object) null);
            context.getFlashCookie().error(s, (Object) null);
            return Results.redirect("/");
        }

    }

    /**
     * this method will handle the confirmation-mail-link
     * 
     * @param id
     * @param token
     * @param context
     * @return
     */
    public Result lostPw(@PathParam("id") Long id, @PathParam("token") String token, Context context)
    {

        User u = User.getById(id);
        if (!(u == null))
        { // the user exists
            if ((u.getConfirmation().equals(token)) && (u.getTs_confirm() >= DateTime.now().getMillis()))
            {
                Map<String, String> map = new HashMap<String, String>();
                map.put("id", id.toString());
                map.put("token", token);

                // show the form for the new password
                return Results.html().render(map);
            }
        }

        return Results.redirect("/");

    }

    public Result changePw(@PathParam("id") Long id, @PathParam("token") String token, Context context,
                           @JSR303Validation PwData pwd, Validation validation)
    {
        Result result = Results.html();
        String s;
        // check the PathParams again
        User u = User.getById(id);
        if (!(u == null))
        { // the user exists

            if ((u.getConfirmation().equals(token)) && (u.getTs_confirm() >= DateTime.now().getMillis()))
            { // the passed token is the right one

                if (!validation.hasViolations())
                {
                    if (pwd.getPw().equals(pwd.getPw2()))
                    {

                        // both pws match -> set the new pw
                        u.hashPasswd(pwd.getPw());
                        u.setActive(true);
                        u.setBadPwCount(0);
                        User.updateUser(u);
                        // TODO maybe use another message
                        s = msg.get("msg_chok", context, result, (Object) null);
                        context.getFlashCookie().error(s, (Object) null);
                        return Results.redirect("/");
                    }
                    else
                    {

                        s = msg.get("msg_formerr", context, result, (Object) null);
                        context.getFlashCookie().error(s, (Object) null);
                        return Results.redirect("/lostpw" + id + "/" + token);
                    }
                }
                else
                {

                    s = msg.get("msg_formerr", context, result, (Object) null);
                    context.getFlashCookie().error(s, (Object) null);
                    return Results.redirect("/lostpw" + id + "/" + token);
                }

            }
        }
        // if the link was wrong -> redirect without any message
        return Results.redirect("/");
    }
}
