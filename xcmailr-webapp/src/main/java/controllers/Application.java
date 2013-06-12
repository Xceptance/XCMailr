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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTime;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import conf.XCMailrConf;
import etc.HelperUtils;
import filters.NoLoginFilter;
import models.EditUsr;
import models.Login;
import models.PwData;
import ninja.Context;
import models.User;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.i18n.Lang;
import ninja.i18n.Messages;
import ninja.params.PathParam;
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
    XCMailrConf xcmConf;

    @Inject
    MailrMessageSenderFactory mmhf;

    @Inject
    MemCachedSessionHandler mcsh;

    @Inject
    Messages msg;

    @Inject
    Lang lang;

    /**
     * Shows the general or logged-in Index-Page <br/>
     * GET /
     * 
     * @param context
     *            the Context of this Request
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
     * @param context
     *            the Context of this Request
     * @return the Registration-Form
     */
    @FilterWith(NoLoginFilter.class)
    public Result registerForm(Context context)
    {
        Map<String, Object> map = HelperUtils.geti18nPrefixedLangMap(xcmConf.APP_LANGS, context, msg);

        return Results.html().render(map);
    }

    /**
     * Processes the entered Registration-Data and creates the {@link User} <br/>
     * POST /register
     * 
     * @param context
     *            the Context of this Request
     * @param frdat
     *            the Data of the Registration-Form
     * @param validation
     *            Form validation
     * @return the Registration-Form and an error, or - if successful - the Index-Page
     */
    @FilterWith(NoLoginFilter.class)
    public Result postRegisterForm(Context context, @JSR303Validation EditUsr frdat, Validation validation)
    {
        
        Result result = Results.html().template("/views/Application/registerForm.ftl.html");
        Map<String, Object> map = HelperUtils.geti18nPrefixedLangMap(xcmConf.APP_LANGS, context, msg);
        if (validation.hasViolations())
        {
            frdat.setPw("");
            frdat.setPwn1("");
            frdat.setPwn2("");
            map.put("editUsr", frdat);
            context.getFlashCookie().error("i18nMsg_FormErr", (Object) null);

            return result.render(map);
        }
        else
        { // form was filled correctly, go on!
            if (!User.mailExists(frdat.getMail()))
            {
                // don't let the user register with one of our domains
                // (prevent mail-loops)
                String mail = frdat.getMail();
                String domPart = mail.split("@")[1];
                if (Arrays.asList(xcmConf.DM_LIST).contains(domPart))
                {
                    context.getFlashCookie().error("i18nMsg_NoLoop", (Object) null);
                    frdat.setMail("");
                    frdat.setPw("");
                    frdat.setPwn1("");
                    frdat.setPwn2("");
                    map.put("editUsr", frdat);

                    return result.render(map);
                }

                // a new user, check whether the passwords are matching
                if (frdat.getPw().equals(frdat.getPwn1()))
                {
                    if (frdat.getPwn1().length() < xcmConf.PW_LEN)
                    { // password too short

                        Optional<String> opt = Optional.of(context.getAcceptLanguage());

                        String shortPw = msg.get("i18nMsg_ShortPw", opt, xcmConf.PW_LEN).get();
                        context.getFlashCookie().error(shortPw, (Object) null);
                        frdat.setPw("");
                        frdat.setPwn1("");
                        frdat.setPwn2("");
                        map.put("editUsr", frdat);

                        return result.render(map);
                    }
                    // create the user
                    User user = frdat.getAsUser();

                    // handle the language

                    if (!Arrays.asList(xcmConf.APP_LANGS).contains(user.getLanguage()))
                    { // the language stored in the user-object does not exist in the app
                        frdat.setPw("");
                        frdat.setPwn1("");
                        frdat.setPwn2("");
                        map.put("editUsr", frdat);
                        context.getFlashCookie().error("i18nMsg_WrongPw", (Object) null);

                        return result.render(map);
                    }

                    // generate the confirmation-token
                    user.setConfirmation(HelperUtils.getRndSecureString(20));
                    user.setTs_confirm(DateTime.now().plusHours(xcmConf.CONF_PERIOD).getMillis());

                    user.save();
                    Optional<String> lng = Optional.of(context.getAcceptLanguage());
                    mmhf.sendConfirmAddressMail(user.getMail(), user.getForename(), String.valueOf(user.getId()),
                                                user.getConfirmation(), lng);
                    context.getFlashCookie().success("i18nMsg_RegOk", (Object) null);
                    
                    lang.setLanguage(user.getLanguage(), result);
                    return result.redirect("/");
                }
                else
                { // password mismatch
                    frdat.setPw("");
                    frdat.setPwn1("");
                    frdat.setPwn2("");
                    map.put("editUsr", frdat);
                    context.getFlashCookie().error("i18nMsg_WrongPw", (Object) null);

                    return result.render(map);
                }
            }
            else
            { // mailadress already exists
                map.put("editUsr", frdat);
                context.getFlashCookie().error("i18nMsg_MailEx", (Object) null);

                return result.render(map);
            }
        }
    }

    /**
     * Handles the Verification for the Activation-Process <br/>
     * GET /verify/{id}/{token}
     * 
     * @param id
     *            the {@link User}-ID
     * @param token
     *            the Verification-Token
     * @param context
     *            the Context of this Request
     * @return to the Index-Page
     */
    public Result verifyActivation(@PathParam("id") Long id, @PathParam("token") String token, Context context)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        User user = User.getById(id);
        if (!(user == null))
        { // the user exists
            if ((user.getConfirmation().equals(token)) && (user.getTs_confirm() >= DateTime.now().getMillis()))
            { // the passed token is the right one -> activate the user
                user.setActive(true);
                user.update();
                context.getFlashCookie().success("i18nUser_Verify_Success", (Object) null);
                return result.redirect("/");
            }
        }
        // show no message when the process failed
        return result.redirect("/");
    }

    // -------------------- Login/-out Functions -----------------------------------

    /**
     * Shows the Login-Form<br/>
     * GET /login
     * 
     * @param context
     *            the Context of this Request
     * @return the rendered Login-Form (or index-page if already logged in)
     */
    @FilterWith(NoLoginFilter.class)
    public Result loginForm(Context context)
    {
        return Results.html();
    }

    /**
     * Handles the Logout-Process<br/>
     * GET /logout
     * 
     * @param context
     *            the Context of this Request
     * @return the Index-Page
     */
    public Result logout(Context context)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        String sessionKey = context.getSessionCookie().getId();
        context.getSessionCookie().clear();
        mcsh.delete(sessionKey);
        context.getFlashCookie().success("i18nMsg_LogOut", (Object) null);
        return result.redirect("/");
    }

    /**
     * Handles the Login-Process <br/>
     * POST for /login
     * 
     * @param context
     *            the Context of this Request
     * @param loginDat
     *            the Data of the Login-Form
     * @param validation
     *            Form validation
     * @return the Login-Form or the Index-Page
     */
    @FilterWith(NoLoginFilter.class)
    public Result loggedInForm(Context context, @JSR303Validation Login loginDat, Validation validation)
    {
        Result result = Results.html().template("/views/Application/loginForm.ftl.html");
        if (validation.hasViolations())
        {
            loginDat.setPwd("");
            context.getFlashCookie().error("i18nMsg_FormErr", (Object) null);
            return result.render(loginDat);
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
                        context.getFlashCookie().error("i18nUser_Inactive", (Object) null);
                        return result.redirect("/");
                    }

                    // we put the username into the cookie, but use the id of the cookie for authentication
                    String sessionKey = context.getSessionCookie().getId();
                    mcsh.set(sessionKey, xcmConf.C_EXPIRA, lgr);
                    context.getSessionCookie().put("username", lgr.getMail());
                    if (lgr.isAdmin())
                    {
                        context.getSessionCookie().put("adm", "1");
                    }
                    lgr.setBadPwCount(0);
                    lgr.update();
                    context.getFlashCookie().success("i18nMsg_LogIn", (Object) null);
                    return result.redirect("/");
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
                        context.getFlashCookie().error("i18nUser_Disabled", (Object) null);
                        return result.redirect("/pwresend");
                    }

                    loginDat.setPwd("");
                    context.getFlashCookie().error("i18nMsg_FormErr", (Object) null);
                    return result.render(loginDat);
                }
            }
            else
            {// the user does not exist
                loginDat.setPwd("");
                context.getFlashCookie().error("i18nMsg_FormErr", (Object) null);
                return result.render(loginDat);
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
     * POST /pwresend
     * 
     * @param context
     *            the Context of this Request
     * @param loginDat
     *            the Data of the Resend-Password-Form (just one Field for the Mail-Address)
     * @param validation
     *            Form validation
     * @return the Index-Page
     */
    public Result pwResend(Context context, @JSR303Validation Login loginDat, Validation validation)
    {
        Result result = Results.html().template("/views/Application/forgotPwForm.ftl.html");
        if (validation.hasViolations())
        {
            // some fields weren't filled
            context.getFlashCookie().error("i18nMsg_FormErr", (Object) null);
            return result.redirect("/pwresend");
        }
        else
        {
            User usr = User.getUsrByMail(loginDat.getMail());
            if (!(usr == null))
            { // mailadress was correct (exists in the DB)
              // generate a new confirmation token and send it to the given mailadress

                // generate the confirmation-token
                usr.setConfirmation(HelperUtils.getRndSecureString(20));
                // set the new validity-time
                usr.setTs_confirm(DateTime.now().plusHours(xcmConf.CONF_PERIOD).getMillis());
                usr.update();
                Optional<String> lang = Optional.of(context.getAcceptLanguage());
                mmhf.sendPwForgotAddressMail(usr.getMail(), usr.getForename(), String.valueOf(usr.getId()),
                                             usr.getConfirmation(), lang);
                context.getFlashCookie().success("i18nForgPw_Succ", (Object) null);
                return result.redirect("/");
            }

            // The user doesn't exist in the db, but we show him the success-msg anyway
            context.getFlashCookie().success("i18nForgPw_Succ", (Object) null);
            return result.redirect("/");
        }

    }

    /**
     * This Method handles the Confirmation-Mail-Link<br/>
     * GET /lostpw/{id}/{token}
     * 
     * @param id
     *            the {@link User}-ID
     * @param token
     *            the Token for the {@link User}
     * @param context
     *            the Context of this Request
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
     *            the {@link User}-ID
     * @param token
     *            the Token of the {@link User}
     * @param context
     *            the Context of this Request
     * @param pwd
     *            the PwData (the Form-Entrys)
     * @param validation
     *            Form validation
     * @return the "Change your Password"-Site or (on Error) the Index-Page
     */
    public Result changePw(@PathParam("id") Long id, @PathParam("token") String token, Context context,
                           @JSR303Validation PwData pwd, Validation validation)
    {
        Result result = Results.html().template("/views/Application/lostPw.ftl.html");
        // check the PathParams again
        User user = User.getById(id);
        if (!(user == null))
        { // the user exists
            if ((user.getConfirmation().equals(token)) && (user.getTs_confirm() >= DateTime.now().getMillis()))
            { // the passed token is the right one
                if (!validation.hasViolations())
                { // the form was filled correctly
                    if (pwd.getPw().equals(pwd.getPw2()))
                    { // the entered PWs are equal

                        if (pwd.getPw().length() < xcmConf.PW_LEN)
                        { // check whether the password has the correct length

                            Optional<String> opt = Optional.of(context.getAcceptLanguage());
                            String shortPw = msg.get("i18nMsg_ShortPw", opt, xcmConf.PW_LEN).get();
                            context.getFlashCookie().error(shortPw, (Object) null);
                            pwd.setPw("");
                            pwd.setPw2("");
                            return result.redirect("/lostpw/" + id + "/" + token);
                        }
                        user.hashPasswd(pwd.getPw());
                        user.setActive(true);
                        user.setBadPwCount(0);

                        // set the confirm-period-timestamp to now to prevent the reuse of the link
                        user.setTs_confirm(DateTime.now().getMillis());
                        user.update();
                        context.getFlashCookie().success("i18nMsg_ChOk", (Object) null);
                        return result.redirect("/");
                    }
                    else
                    { // the passwords are not equal
                        context.getFlashCookie().error("i18nMsg_WrongPw", (Object) null);
                        return result.redirect("/lostpw/" + id + "/" + token);
                    }
                }
                else
                { // the form has errors
                    context.getFlashCookie().error("i18nMsg_FormErr", (Object) null);
                    return result.redirect("/lostpw/" + id + "/" + token);
                }
            }
        }
        // if the link was wrong -> redirect without any message
        return result.redirect("/");
    }
}
