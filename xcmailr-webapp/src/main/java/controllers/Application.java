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
import org.slf4j.Logger;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import conf.XCMailrConf;
import etc.HelperUtils;
import filters.NoLoginFilter;
import models.UserFormData;
import models.LoginFormData;
import models.PasswordFormData;
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
    Messages messages;

    @Inject
    Lang lang;

    @Inject
    Logger log;

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
        List<String[]> languageList = HelperUtils.getLanguageList(xcmConfiguration.APP_LANGS, context, messages);

        return Results.html().render("available_langs", languageList);
    }

    /**
     * Processes the entered Registration-Data and creates the {@link User} <br/>
     * POST /register
     * 
     * @param context
     *            the Context of this Request
     * @param registerFormData
     *            the Data of the Registration-Form
     * @param validation
     *            Form validation
     * @return the Registration-Form and an error, or - if successful - the Index-Page
     */
    @FilterWith(NoLoginFilter.class)
    public Result registrationProcess(Context context, @JSR303Validation UserFormData registerFormData,
                                      Validation validation)
    {

        Result result = Results.html().template("/views/Application/registerForm.ftl.html");

        List<String[]> o = HelperUtils.getLanguageList(xcmConfiguration.APP_LANGS, context, messages);
        result.render("available_langs", o);
        if (validation.hasViolations())
        {

            registerFormData.setPassword("");
            registerFormData.setPasswordNew1("");
            registerFormData.setPasswordNew2("");
            
            context.getFlashCookie().error("flash_FormError");

            return result.render("editUsr", registerFormData);
        }
        else
        { // form was filled correctly, go on!
            if (!User.mailExists(registerFormData.getMail()))
            {
                // don't let the user register with one of our domains
                // (prevent mail-loops)
                String mail = registerFormData.getMail();
                String domainPart = mail.split("@")[1];
                if (Arrays.asList(xcmConfiguration.DOMAIN_LIST).contains(domainPart))
                {
                    context.getFlashCookie().error("flash_NoLoop");
                    registerFormData.setMail("");
                    registerFormData.setPassword("");
                    registerFormData.setPasswordNew1("");
                    registerFormData.setPasswordNew2("");

                    return result.render("editUsr", registerFormData);
                }

                // a new user, check whether the passwords are matching
                if (registerFormData.getPassword().equals(registerFormData.getPasswordNew1()))
                {
                    if (registerFormData.getPasswordNew1().length() < xcmConfiguration.PW_LENGTH)
                    { // password too short

                        Optional<String> opt = Optional.of(context.getAcceptLanguage());

                        String shortPw = messages.get("flash_PasswordTooShort", opt, xcmConfiguration.PW_LENGTH).get();
                        context.getFlashCookie().error(shortPw);
                        registerFormData.setPassword("");
                        registerFormData.setPasswordNew1("");
                        registerFormData.setPasswordNew2("");
                        return result.render("editUsr", registerFormData);
                    }
                    // create the user
                    User user = registerFormData.getAsUser();

                    // handle the language

                    if (!Arrays.asList(xcmConfiguration.APP_LANGS).contains(user.getLanguage()))
                    { // the language stored in the user-object does not exist in the app
                        registerFormData.setPassword("");
                        registerFormData.setPasswordNew1("");
                        registerFormData.setPasswordNew2("");
                        context.getFlashCookie().error("flash_PasswordsUnequal");

                        return result.render("editUsr", registerFormData);
                    }

                    // generate the confirmation-token
                    user.setConfirmation(HelperUtils.getRandomSecureString(20));
                    user.setTs_confirm(DateTime.now().plusHours(xcmConfiguration.CONFIRMATION_PERIOD).getMillis());

                    user.save();
                    Optional<String> language = Optional.of(context.getAcceptLanguage());
                    mailrSenderFactory.sendConfirmAddressMail(user.getMail(), user.getForename(),
                                                              String.valueOf(user.getId()), user.getConfirmation(),
                                                              language);
                    context.getFlashCookie().success("flash_RegistrationSuccessful");

                    lang.setLanguage(user.getLanguage(), result);
                    return result.template("/views/Application/index.ftl.html")
                                 .redirect(context.getContextPath() + "/");
                }
                else
                { // password mismatch
                    registerFormData.setPassword("");
                    registerFormData.setPasswordNew1("");
                    registerFormData.setPasswordNew2("");
                    context.getFlashCookie().error("flash_PasswordsUnequal");

                    return result.render("editUsr", registerFormData);
                }
            }
            else
            { // mailadress already exists
                context.getFlashCookie().error("flash_MailExists");

                return result.render("editUsr", registerFormData);
            }
        }
    }

    /**
     * Handles the Verification for the Activation-Process <br/>
     * GET /verify/{id}/{token}
     * 
     * @param userId
     *            the {@link User}-ID
     * @param token
     *            the Verification-Token
     * @param context
     *            the Context of this Request
     * @return to the Index-Page
     */
    public Result verifyActivation(@PathParam("id") Long userId, @PathParam("token") String token, Context context)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        User user = User.getById(userId);
        if (!(user == null))
        { // the user exists
            if ((user.getConfirmation().equals(token)) && (user.getTs_confirm() >= DateTime.now().getMillis()))
            { // the passed token is the right one -> activate the user
                user.setActive(true);
                user.update();
                context.getFlashCookie().success("user_Verify_Success");
                return result.template("/views/Application/index.ftl.html").redirect(context.getContextPath() + "/");
            }
        }
        // show no message when the process failed
        return result.template("/views/Application/index.ftl.html").redirect(context.getContextPath() + "/");
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
    public Result logoutProcess(Context context)
    {

        // remove the session (memcachedServer and cookie)
        String sessionKey = context.getSessionCookie().getId();
        context.getSessionCookie().clear();
        memCachedSessionHandler.delete(sessionKey);

        // show the index-page
        Result result = Results.html().template("/views/Application/index.ftl.html");
        context.getFlashCookie().success("flash_LogOut");
        return result.redirect(context.getContextPath() + "/");
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
    public Result logInProcess(Context context, @JSR303Validation LoginFormData loginData, Validation validation)
    {
        Result result = Results.html().template("/views/Application/loginForm.ftl.html");
        if (validation.hasViolations())
        {
            loginData.setPassword("");
            context.getFlashCookie().error("flash_FormError");
            return result.render(loginData);
        }
        else
        {
            User loginUser = User.getUsrByMail(loginData.getMail());
            if (loginUser != null)
            {// the user exists
                if (loginUser.checkPasswd(loginData.getPassword()))
                { // correct login
                    if (!loginUser.isActive())
                    {
                        context.getFlashCookie().error("user_Inactive");
                        return result.template("/views/Application/index.ftl.html").redirect(context.getContextPath()
                                                                                                 + "/");
                    }

                    // we put the username into the cookie, but use the id of the cookie for authentication
                    String sessionKey = context.getSessionCookie().getId();
                    memCachedSessionHandler.set(sessionKey, xcmConfiguration.COOKIE_EXPIRETIME, loginUser);
                    context.getSessionCookie().put("username", loginUser.getMail());
                    if (loginUser.isAdmin())
                    {
                        context.getSessionCookie().put("adm", "1");
                    }
                    loginUser.setBadPwCount(0);
                    loginUser.update();
                    context.getFlashCookie().success("flash_LogIn");

                    // set the language the user wants to have
                    lang.setLanguage(loginUser.getLanguage(), result);

                    return result.template("/views/Application/index.ftl.html")
                                 .redirect(context.getContextPath() + "/");
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
                        return result.template("/views/Application/index.ftl.html").redirect(context.getContextPath()
                                                                                                 + "/pwresend");
                    }

                    loginData.setPassword("");
                    context.getFlashCookie().error("flash_FormError");
                    return result.render(loginData);
                }
            }
            else
            {// the user does not exist
                loginData.setPassword("");
                context.getFlashCookie().error("flash_FormError");
                return result.render(loginData);
            }
        }
    }

    /**
     * Shows the "Forgot Password"-Page <br/>
     * GET /pwresend
     * 
     * @return Forgot-Password-Form
     */
    public Result forgotPasswordForm()
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
    public Result forgotPasswordProcess(Context context, @JSR303Validation LoginFormData loginDat, Validation validation)
    {
        Result result = Results.html().template("/views/Application/forgotPwForm.ftl.html");
        if (validation.hasViolations())
        {
            // some fields weren't filled
            context.getFlashCookie().error("flash_FormError");
            return result.redirect(context.getContextPath() + "/pwresend");
        }
        else
        {
            User user = User.getUsrByMail(loginDat.getMail());

            if (user != null)
            { // mailadress was correct (exists in the DB)
              // generate a new confirmation token and send it to the given mailadress

                // generate the confirmation-token
                user.setConfirmation(HelperUtils.getRandomSecureString(20));
                // set the new validity-time
                user.setTs_confirm(DateTime.now().plusHours(xcmConfiguration.CONFIRMATION_PERIOD).getMillis());
                user.update();
                Optional<String> lang = Optional.of(context.getAcceptLanguage());
                mailrSenderFactory.sendPwForgotAddressMail(user.getMail(), user.getForename(),
                                                           String.valueOf(user.getId()), user.getConfirmation(), lang);
                context.getFlashCookie().success("flash_forgotPassword_Success");
                return result.redirect(context.getContextPath() + "/");
            }

            // The user doesn't exist in the db, but we show him the success-msg anyway
            context.getFlashCookie().success("flash_forgotPassword_Success");
            return result.redirect(context.getContextPath() + "/");
        }

    }

    /**
     * This Method handles the Forgot-Password-Mail-Link<br/>
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
    public Result resetPasswordForm(@PathParam("id") Long id, @PathParam("token") String token, Context context)
    {
        User user = User.getById(id);
        if (user != null)
        { // the user exists
            if ((user.getConfirmation().equals(token)) && (user.getTs_confirm() >= DateTime.now().getMillis()))
            { // the token is right and the request is in time

                // show the form for the new password
                return Results.html().render("id", id.toString()).render("token", token);
            }
        }
        // something was wrong, so redirect without any comment to the index-page
        return Results.redirect(context.getContextPath() + "/");

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
    public Result resetPasswordProcess(@PathParam("id") Long id, @PathParam("token") String token, Context context,
                                       @JSR303Validation PasswordFormData passwordFormData, Validation validation)
    {
        Result result = Results.html().template("/views/Application/resetPasswordForm.ftl.html");
        // check the PathParams again
        User user = User.getById(id);
        if (!(user == null))
        { // the user exists
            if ((user.getConfirmation().equals(token)) && (user.getTs_confirm() >= DateTime.now().getMillis()))
            { // the passed token is the right one
                if (!validation.hasViolations())
                { // the form was filled correctly
                    if (passwordFormData.getPassword().equals(passwordFormData.getPassword2()))
                    { // the entered PWs are equal

                        if (passwordFormData.getPassword().length() < xcmConfiguration.PW_LENGTH)
                        { // check whether the password has the correct length

                            Optional<String> optionalLanguage = Optional.of(context.getAcceptLanguage());
                            String tooShortPassword = messages.get("flash_PasswordTooShort", optionalLanguage,
                                                                   xcmConfiguration.PW_LENGTH).get();
                            context.getFlashCookie().error(tooShortPassword);
                            passwordFormData.setPassword("");
                            passwordFormData.setPassword2("");
                            return result.redirect(context.getContextPath() + "/lostpw/" + id + "/" + token);
                        }
                        user.hashPasswd(passwordFormData.getPassword());
                        user.setActive(true);
                        user.setBadPwCount(0);

                        // set the confirm-period-timestamp to now to prevent the reuse of the link
                        user.setTs_confirm(DateTime.now().getMillis());
                        user.update();
                        context.getFlashCookie().success("flash_DataChangeSuccess");
                        return result.redirect(context.getContextPath() + "/");
                    }
                    else
                    { // the passwords are not equal
                        context.getFlashCookie().error("flash_PasswordsUnequal");
                        return result.redirect(context.getContextPath() + "/lostpw/" + id + "/" + token);
                    }
                }
                else
                { // the form has errors
                    context.getFlashCookie().error("flash_FormError");
                    return result.redirect(context.getContextPath() + "/lostpw/" + id + "/" + token);
                }
            }
        }
        // if the link was wrong -> redirect without any message
        return result.redirect(context.getContextPath() + "/");
    }
}
