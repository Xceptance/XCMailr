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
import etc.HelperUtils;
import models.EditUsr;
import models.Login;
import models.PwData;
import ninja.Context;
import models.User;
import ninja.Result;
import ninja.Results;
import ninja.i18n.Messages;
import ninja.params.PathParam;
import ninja.utils.NinjaProperties;
import ninja.validation.JSR303Validation;
import ninja.validation.Validation;

/**
 * Handles all general application actions like login, logout, forgot password or index page
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Singleton
public class Application
{
    @Inject
    Messages msg;

    @Inject
    NinjaProperties ninjaProp;

    @Inject
    MailrMessageHandlerFactory mmhf;

    @Inject
    MemCachedSessionHandler mcsh;

    /**
     * Shows the Index-Page <br/>
     * GET /
     * 
     * @param context
     * @return the Index-Page
     */
    public Result index(Context context)
    {
        String uuid = context.getSessionCookie().getId();
        User usr = (User) mcsh.get(uuid);
        if (usr == null)
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
     * Shows the Registration-Form <br/>
     * GET /register
     * 
     * @return the Registration-Form
     */

    public Result registerForm()
    {
        return Results.html();
    }

    /**
     * Processes the entered Data and creates the {@link User} <br/>
     * POST /register
     * 
     * @return the Registration-Form and an error, or - if successful - the Index-Page
     */

    public Result postRegisterForm(Context context, @JSR303Validation EditUsr frdat, Validation validation)
    {
        Optional<Result> opt = Optional.of(Results.html());
        String s;
        if (validation.hasViolations())
        {
            frdat.setPw("");
            frdat.setPwn1("");
            frdat.setPwn2("");

            s = msg.get("i18nmsg_formerr", context, opt, (Object) null).get();
            context.getFlashCookie().error(s, (Object) null);
            return Results.html().template("views/Application/registerForm.ftl.html").render(frdat);
        }
        else
        { // form was filled correctly, go on!
            if (!User.mailExists(frdat.getMail()))
            {
                // a new user, check if the passwords are matching
                if (frdat.getPw().equals(frdat.getPwn1()))
                {
                    // create the user
                    User user = frdat.getAsUser();

                    // generate the confirmation-token
                    user.setConfirmation(HelperUtils.getRndSecureString(20));
                    user.setTs_confirm(DateTime.now().plusHours(ninjaProp.getIntegerWithDefault("confirm.period", 1))
                                               .getMillis());

                    user.save();
                    Optional<String> lang = Optional.of(context.getAcceptLanguage());
                    mmhf.sendConfirmAddressMail(user.getMail(), user.getForename(), String.valueOf(user.getId()),
                                                user.getConfirmation(), lang);

                    s = msg.get("i18nmsg_regok", context, opt, (Object) null).get();
                    context.getFlashCookie().success(s, (Object) null);

                    return Results.redirect("/");
                }
                else
                { // password mismatch
                    frdat.setPw("");
                    frdat.setPwn1("");
                    frdat.setPwn2("");
                    s = msg.get("i18nmsg_wrongpw", context, opt, (Object) null).get();
                    context.getFlashCookie().error(s, (Object) null);
                    return Results.html().template("views/Application/registerForm.ftl.html").render(frdat);
                }
            }
            else
            { // mailadress already exists
                s = msg.get("i18nmsg_mailex", context, opt, (Object) null).get();
                context.getFlashCookie().error(s, (Object) null);
                return Results.html().template("views/Application/registerForm.ftl.html").render(frdat);
            }
        }
    }

    /**
     * Handles the Verification for the Activation-Process <br/>
     * GET /verify/{id}/{token}
     * 
     * @param id
     *            - the {@link User}-ID
     * @param token
     *            - the Verification-Token
     * @param context
     * @return to the Index-Page
     */
    public Result verifyActivation(@PathParam("id") Long id, @PathParam("token") String token, Context context)
    {
        User user = User.getById(id);
        if (!(user == null))
        { // the user exists
            if ((user.getConfirmation().equals(token)) && (user.getTs_confirm() >= DateTime.now().getMillis()))
            { // the passed token is the right one -> activate the user
                user.setActive(true);
                user.update();

                Optional<Result> opt = Optional.of(Results.html());
                String s = msg.get("i18nuser_verify_success", context, opt, (Object) null).get();
                context.getFlashCookie().success(s, (Object) null);
                return Results.redirect("/");
            }
        }
        // show no message when the process failed
        return Results.redirect("/");
    }

    // -------------------- Login/-out Functions -----------------------------------

    /**
     * Shows the Login-Form<br/>
     * GET /login
     * 
     * @return the rendered Login-Form
     */
    public Result loginForm(Context context)
    {
        return Results.html();
    }

    /**
     * Handles the Logout-Process<br/>
     * GET /logout
     * 
     * @return the Index-Page
     */
    public Result logout(Context context)
    {
        String sessionKey = context.getSessionCookie().getId();
        context.getSessionCookie().clear();
        mcsh.delete(sessionKey);

        Optional<Result> opt = Optional.of(Results.html());
        String s = msg.get("i18nmsg_logout", context, opt, (Object) null).get();
        context.getFlashCookie().success(s, (Object) null);
        return Results.redirect("/");
    }

    /**
     * Handles the Login-Process <br/>
     * POST for /login
     * 
     * @return the Login-Form or the Index-Page
     */
    public Result loggedInForm(Context context, @JSR303Validation Login loginDat, Validation validation)
    {

        Result result = Results.html();
        Optional<Result> opt = Optional.of(result);
        String s;

        if (validation.hasViolations())
        {
            loginDat.setPwd("");
            s = msg.get("i18nmsg_formerr", context, opt, (Object) null).get();
            context.getFlashCookie().error(s, (Object) null);
            return Results.html().template("views/Application/loginForm.ftl.html").render(loginDat);
        }
        else
        {
            User lgr = User.getUsrByMail(loginDat.getMail());
            if (!(lgr == null))
            {// the user exists
                if (lgr.checkPasswd(loginDat.getPwd()))
                { // correct login
                    if (!lgr.isActive())
                    {
                        s = msg.get("i18nuser_inactive", context, opt, (Object) null).get();
                        context.getFlashCookie().error(s, (Object) null);
                        return Results.html().template("views/Application/index.ftl.html");
                    }

                    // we put the username into the cookie, but use the id of the cookie for authentication
                    context.setAttribute("uname", lgr.getMail());
                    String sessionKey = context.getSessionCookie().getId();
                    mcsh.set(sessionKey, 3600, lgr);
                    context.getSessionCookie().put("username", lgr.getMail());
                    lgr.setBadPwCount(0);
                    lgr.update();

                    s = msg.get("i18nmsg_login", context, opt, (Object) null).get();
                    context.getFlashCookie().success(s, (Object) null);
                    return Results.html().template("views/Application/indexLogin.ftl.html");
                }
                else
                { // the authentication was not correct
                    lgr.setBadPwCount(lgr.getBadPwCount() + 1);
                    lgr.update();

                    if (lgr.getBadPwCount() >= 6)
                    { // the password was six times wrong
                        lgr.setActive(false);
                        lgr.update();

                        // show the disabled message and return to the forgot-pw-page
                        s = msg.get("i18nuser_disabled", context, opt, (Object) null).get();
                        context.getFlashCookie().error(s, (Object) null);
                        return Results.redirect("/pwresend");
                    }

                    loginDat.setPwd("");
                    s = msg.get("i18nmsg_formerr", context, opt, (Object) null).get();
                    context.getFlashCookie().error(s, (Object) null);
                    return Results.html().template("views/Application/loginForm.ftl.html").render(loginDat);
                }
            }
            else
            {// the user does not exist
                loginDat.setPwd("");
                s = msg.get("i18nmsg_formerr", context, opt, (Object) null).get();
                context.getFlashCookie().error(s, (Object) null);
                return Results.html().template("views/Application/loginForm.ftl.html").render(loginDat);
            }
        }
    }

    /**
     * Shows the "Forgot Password"-Page <br/>
     * GET /resendpw
     * 
     * @return Forgot-Password-Form
     */
    public Result forgotPwForm()
    {
        return Results.html();
    }

    /**
     * Generates a new Token and sends it to the user<br/>
     * POST /resendpw
     * 
     * @return Index-Page
     */
    public Result pwResend(Context context, @JSR303Validation Login loginDat, Validation validation)
    {
        Result result = Results.html();
        Optional<Result> opt = Optional.of(result);
        String s;

        if (validation.hasViolations())
        {
            // some fields weren't filled
            s = msg.get("i18nmsg_formerr", context, opt, (Object) null).get();
            context.getFlashCookie().error(s, (Object) null);
            return Results.redirect("/pwresend");
        }
        else
        {
            User usr = User.getUsrByMail(loginDat.getMail());
            if (!(usr == null))
            { // mailadress was correct (exists in the DB)
              // generate a new pw and send it to the given mailadress

                // generate the confirmation-token
                usr.setConfirmation(HelperUtils.getRndSecureString(20));
                // set the new validity-time
                usr.setTs_confirm(DateTime.now().plusHours(ninjaProp.getIntegerWithDefault("confirm.period", 1))
                                          .getMillis());
                usr.update();
                Optional<String> lang = Optional.of(context.getAcceptLanguage());
                mmhf.sendPwForgotAddressMail(usr.getMail(), usr.getForename(), String.valueOf(usr.getId()),
                                             usr.getConfirmation(), lang);
                s = msg.get("i18nforgpw_succ", context, opt, (Object) null).get();
                context.getFlashCookie().success(s, (Object) null);
                return Results.redirect("/");
            }

            // The user doesn't exist in the db, but we show him the success-msg anyway
            s = msg.get("i18nforgpw_succ", context, opt, (Object) null).get();
            context.getFlashCookie().success(s, (Object) null);
            return Results.redirect("/");
        }

    }

    /**
     * This Method handles the Confirmation-Mail-Link<br/>
     * GET /lostpw/{id}/{token}
     * 
     * @param id
     *            - the {@link User}-ID
     * @param token
     *            - the Token for the {@link User}
     * @param context
     * @return the Reset-Password-Form or (on error) the Index-Page
     */
    public Result lostPw(@PathParam("id") Long id, @PathParam("token") String token, Context context)
    {

        User user = User.getById(id);
        if (!(user == null))
        { // the user exists
            if ((user.getConfirmation().equals(token)) && (user.getTs_confirm() >= DateTime.now().getMillis()))
            { // the token is right and the request is in time
                Map<String, String> map = new HashMap<String, String>();
                map.put("id", id.toString());
                map.put("token", token);
                // show the form for the new password
                return Results.html().render(map);
            }
        }
        // something was wrong, so redirect without any comment to the index-page
        return Results.redirect("/");

    }

    /**
     * Sets a new Password for the {@link User}<br/>
     * POST /lostpw/{id}/{token}
     * 
     * @param id
     *            - the {@link User}-ID
     * @param token
     *            - the Token of the {@link User}
     * @param context
     * @param pwd
     *            - the PwData (the Form-Entrys)
     * @param validation
     * @return the "Change your Password"-Site or (on Error) the Index-Page
     */
    public Result changePw(@PathParam("id") Long id, @PathParam("token") String token, Context context,
                           @JSR303Validation PwData pwd, Validation validation)
    {
        Result result = Results.html();
        Optional<Result> opt = Optional.of(result);
        String s;
        // check the PathParams again
        User user = User.getById(id);
        if (!(user == null))
        { // the user exists
            if ((user.getConfirmation().equals(token)) && (user.getTs_confirm() >= DateTime.now().getMillis()))
            { // the passed token is the right one
                if (!validation.hasViolations())
                { // the form was filled correctly
                    if (pwd.getPw().equals(pwd.getPw2()))
                    { // the entered PWs are equal -> set the new pw
                        user.hashPasswd(pwd.getPw());
                        user.setActive(true);
                        user.setBadPwCount(0);

                        // set the confirm-period-timestamp to now to prevent the reuse of the link
                        user.setTs_confirm(DateTime.now().getMillis());
                        user.update();
                        s = msg.get("i18nmsg_chok", context, opt, (Object) null).get();
                        context.getFlashCookie().success(s, (Object) null);
                        return Results.redirect("/");
                    }
                    else
                    { // the passwords are not equal
                        s = msg.get("i18nmsg_wrongpw", context, opt, (Object) null).get();
                        context.getFlashCookie().error(s, (Object) null);
                        return Results.redirect("/lostpw/" + id + "/" + token);
                    }
                }
                else
                { // the form has errors
                    s = msg.get("i18nmsg_formerr", context, opt, (Object) null).get();
                    context.getFlashCookie().error(s, (Object) null);
                    return Results.redirect("/lostpw/" + id + "/" + token);
                }
            }
        }
        // if the link was wrong -> redirect without any message
        return Results.redirect("/");
    }
}
