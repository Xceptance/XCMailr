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

import java.util.List;
import java.util.Arrays;
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
    XCMailrConf xcmConfiguration;

    @Inject
    MailrMessageSenderFactory mailrSenderFactory;

    @Inject
    MemCachedSessionHandler memCachedSessionHandler;

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
        // show the index-page
        return Results.ok().html();
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
        List<String[]> o = HelperUtils.getLanguageList(xcmConfiguration.APP_LANGS, context, msg);

        return Results.html().render("available_langs", o);
    }

    /**
     * Processes the entered Registration-Data and creates the {@link User} <br/>
     * POST /register
     * 
     * @param context
     *            the Context of this Request
     * @param userFormData
     *            the Data of the Registration-Form
     * @param validation
     *            Form validation
     * @return the Registration-Form and an error, or - if successful - the Index-Page
     */
    @FilterWith(NoLoginFilter.class)
    public Result postRegisterForm(Context context, @JSR303Validation EditUsr userFormData, Validation validation)
    {

        Result result = Results.html().template("/views/Application/registerForm.ftl.html");

        List<String[]> o = HelperUtils.getLanguageList(xcmConfiguration.APP_LANGS, context, msg);
        result.render("available_langs", o);
        if (validation.hasViolations())
        {
            userFormData.setPw("");
            userFormData.setPwn1("");
            userFormData.setPwn2("");

            context.getFlashCookie().error("msg_FormErr");

            return result.render("editUsr", userFormData);
        }
        else
        { // form was filled correctly, go on!
            if (!User.mailExists(userFormData.getMail()))
            {
                // don't let the user register with one of our domains
                // (prevent mail-loops)
                String mail = userFormData.getMail();
                String domainPart = mail.split("@")[1];
                if (Arrays.asList(xcmConfiguration.DM_LIST).contains(domainPart))
                {
                    context.getFlashCookie().error("msg_NoLoop");
                    userFormData.setMail("");
                    userFormData.setPw("");
                    userFormData.setPwn1("");
                    userFormData.setPwn2("");

                    return result.render("editUsr", userFormData);
                }

                // a new user, check whether the passwords are matching
                if (userFormData.getPw().equals(userFormData.getPwn1()))
                {
                    if (userFormData.getPwn1().length() < xcmConfiguration.PW_LEN)
                    { // password too short

                        Optional<String> opt = Optional.of(context.getAcceptLanguage());

                        String shortPw = msg.get("msg_ShortPw", opt, xcmConfiguration.PW_LEN).get();
                        context.getFlashCookie().error(shortPw);
                        userFormData.setPw("");
                        userFormData.setPwn1("");
                        userFormData.setPwn2("");
                        return result.render("editUsr", userFormData);
                    }
                    // create the user
                    User user = userFormData.getAsUser();

                    // handle the language

                    if (!Arrays.asList(xcmConfiguration.APP_LANGS).contains(user.getLanguage()))
                    { // the language stored in the user-object does not exist in the app
                        userFormData.setPw("");
                        userFormData.setPwn1("");
                        userFormData.setPwn2("");
                        context.getFlashCookie().error("msg_WrongPw");

                        return result.render("editUsr", userFormData);
                    }

                    // generate the confirmation-token
                    user.setConfirmation(HelperUtils.getRandomSecureString(20));
                    user.setTs_confirm(DateTime.now().plusHours(xcmConfiguration.CONF_PERIOD).getMillis());

                    user.save();
                    Optional<String> lng = Optional.of(context.getAcceptLanguage());
                    mailrSenderFactory.sendConfirmAddressMail(user.getMail(), user.getForename(), String.valueOf(user.getId()),
                                                user.getConfirmation(), lng);
                    context.getFlashCookie().success("msg_RegOk");

                    lang.setLanguage(user.getLanguage(), result);
                    return result.template("/views/Application/index.ftl.html").redirect("/");
                }
                else
                { // password mismatch
                    userFormData.setPw("");
                    userFormData.setPwn1("");
                    userFormData.setPwn2("");
                    context.getFlashCookie().error("msg_WrongPw");

                    return result.render("editUsr", userFormData);
                }
            }
            else
            { // mailadress already exists
                context.getFlashCookie().error("msg_MailEx");

                return result.render("editUsr", userFormData);
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
                context.getFlashCookie().success("user_Verify_Success");
                return result.template("/views/Application/index.ftl.html").redirect("/");
            }
        }
        // show no message when the process failed
        return result.template("/views/Application/index.ftl.html").redirect("/");
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
        memCachedSessionHandler.delete(sessionKey);
        context.getFlashCookie().success("msg_LogOut");
        return result.template("/views/Application/index.ftl.html").redirect("/");
    }

    /**
     * Handles the Login-Process <br/>
     * POST for /login
     * 
     * @param context
     *            the Context of this Request
     * @param loginData
     *            the Data of the Login-Form
     * @param validation
     *            Form validation
     * @return the Login-Form or the Index-Page
     */
    @FilterWith(NoLoginFilter.class)
    public Result loggedInForm(Context context, @JSR303Validation Login loginData, Validation validation)
    {
        Result result = Results.html().template("/views/Application/loginForm.ftl.html");
        if (validation.hasViolations())
        {
            loginData.setPassword("");
            context.getFlashCookie().error("msg_FormErr");
            return result.render(loginData);
        }
        else
        {
            User loginUser = User.getUsrByMail(loginData.getMail());
            if (!(loginUser == null))
            {// the user exists
                if (loginUser.checkPasswd(loginData.getPassword()))
                { // correct login
                    if (!loginUser.isActive())
                    {
                        context.getFlashCookie().error("user_Inactive");
                        return result.template("/views/Application/index.ftl.html").redirect("/");
                    }

                    // we put the username into the cookie, but use the id of the cookie for authentication
                    String sessionKey = context.getSessionCookie().getId();
                    memCachedSessionHandler.set(sessionKey, xcmConfiguration.C_EXPIRA, loginUser);
                    context.getSessionCookie().put("username", loginUser.getMail());
                    if (loginUser.isAdmin())
                    {
                        context.getSessionCookie().put("adm", "1");
                    }
                    loginUser.setBadPwCount(0);
                    loginUser.update();
                    context.getFlashCookie().success("msg_LogIn");
                    return result.template("/views/Application/index.ftl.html").redirect("/");
                }
                else
                { // the authentication was not correct
                    loginUser.setBadPwCount(loginUser.getBadPwCount() + 1);
                    loginUser.update();

                    if (loginUser.getBadPwCount() >= 6)
                    { // the password was six times wrong
                        loginUser.setActive(false);
                        loginUser.update();

                        // show the disabled message and return to the forgot-pw-page
                        context.getFlashCookie().error("user_Disabled");
                        return result.template("/views/Application/index.ftl.html").redirect("/pwresend");
                    }

                    loginData.setPassword("");
                    context.getFlashCookie().error("msg_FormErr");
                    return result.render(loginData);
                }
            }
            else
            {// the user does not exist
                loginData.setPassword("");
                context.getFlashCookie().error("msg_FormErr");
                return result.render(loginData);
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
            context.getFlashCookie().error("msg_FormErr");
            return result.redirect("/pwresend");
        }
        else
        {
            User user = User.getUsrByMail(loginDat.getMail());
            if (!(user == null))
            { // mailadress was correct (exists in the DB)
              // generate a new confirmation token and send it to the given mailadress

                // generate the confirmation-token
                user.setConfirmation(HelperUtils.getRandomSecureString(20));
                // set the new validity-time
                user.setTs_confirm(DateTime.now().plusHours(xcmConfiguration.CONF_PERIOD).getMillis());
                user.update();
                Optional<String> lang = Optional.of(context.getAcceptLanguage());
                mailrSenderFactory.sendPwForgotAddressMail(user.getMail(), user.getForename(), String.valueOf(user.getId()),
                                             user.getConfirmation(), lang);
                context.getFlashCookie().success("forgPw_Succ");
                return result.redirect("/");
            }

            // The user doesn't exist in the db, but we show him the success-msg anyway
            context.getFlashCookie().success("forgPw_Succ");
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

                // show the form for the new password
                return Results.html().render("id", id.toString()).render("token", token);
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
     * @param passwordFormData
     *            the PwData (the Form-Entrys)
     * @param validation
     *            Form validation
     * @return the "Change your Password"-Site or (on Error) the Index-Page
     */
    public Result changePw(@PathParam("id") Long id, @PathParam("token") String token, Context context,
                           @JSR303Validation PwData passwordFormData, Validation validation)
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
                    if (passwordFormData.getPw().equals(passwordFormData.getPw2()))
                    { // the entered PWs are equal

                        if (passwordFormData.getPw().length() < xcmConfiguration.PW_LEN)
                        { // check whether the password has the correct length

                            Optional<String> optionalLanguage = Optional.of(context.getAcceptLanguage());
                            String tooShortPassword = msg.get("msg_ShortPw", optionalLanguage, xcmConfiguration.PW_LEN).get();
                            context.getFlashCookie().error(tooShortPassword);
                            passwordFormData.setPw("");
                            passwordFormData.setPw2("");
                            return result.redirect("/lostpw/" + id + "/" + token);
                        }
                        user.hashPasswd(passwordFormData.getPw());
                        user.setActive(true);
                        user.setBadPwCount(0);

                        // set the confirm-period-timestamp to now to prevent the reuse of the link
                        user.setTs_confirm(DateTime.now().getMillis());
                        user.update();
                        context.getFlashCookie().success("msg_ChOk");
                        return result.redirect("/");
                    }
                    else
                    { // the passwords are not equal
                        context.getFlashCookie().error("msg_WrongPw");
                        return result.redirect("/lostpw/" + id + "/" + token);
                    }
                }
                else
                { // the form has errors
                    context.getFlashCookie().error("msg_FormErr");
                    return result.redirect("/lostpw/" + id + "/" + token);
                }
            }
        }
        // if the link was wrong -> redirect without any message
        return result.redirect("/");
    }
}
