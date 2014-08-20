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
import java.util.List;

import models.Domain;
import models.LoginFormData;
import models.PasswordFormData;
import models.User;
import models.UserFormData;
import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.i18n.Lang;
import ninja.i18n.Messages;
import ninja.params.Param;
import ninja.params.PathParam;
import ninja.validation.JSR303Validation;
import ninja.validation.Validation;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import conf.XCMailrConf;
import etc.HelperUtils;
import filters.NoLoginFilter;

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
    CachingSessionHandler cachingSessionHandler;

    @Inject
    Messages messages;

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
    public Result index(Context context, @Param("lang") String languageParam)
    {
        Result result = Results.ok().html();
        // set the wanted language
        if (!StringUtils.isBlank(languageParam))
        {
            lang.setLanguage(languageParam, result);
        }

        // show the index-page
        List<String[]> languageList = HelperUtils.getLanguageList(xcmConfiguration.APP_LANGS, context, result, messages);
        return result.render("available_langs", languageList);
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
        Result result = Results.html();
        // render the available languages
        List<String[]> languageList = HelperUtils.getLanguageList(xcmConfiguration.APP_LANGS, context, result, messages);
        return result.render("available_langs", languageList).render("registerUserData", new UserFormData());
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

        List<String[]> availableLanguageList = HelperUtils.getLanguageList(xcmConfiguration.APP_LANGS, context, result,
                                                                           messages);
        result.render("available_langs", availableLanguageList);
        if (validation.hasViolations())
        { // the form contains errors

            registerFormData.clearPasswordFields();
            context.getFlashScope().error("flash_FormError");

            return result.render("registerUserData", registerFormData);
        }
        else
        { // form was filled correctly, go on!
            if (!User.mailExists(registerFormData.getMail()))
            {// the user's mailaddress does not exist already in the database

                String mail = registerFormData.getMail();
                String domainPart = mail.split("@")[1];
                // don't let the user register with one of our domains
                // (prevent mail-loops)

                if (Arrays.asList(xcmConfiguration.DOMAIN_LIST).contains(domainPart))
                {
                    context.getFlashScope().error("flash_NoLoop");
                    registerFormData.setMail("");
                    registerFormData.clearPasswordFields();

                    return result.render("registerUserData", registerFormData);
                }
                // block the registration, if the domain is not on the whitelist (and the whitelisting is active)
                if (xcmConfiguration.APP_WHITELIST)
                { // whitelisting is active
                    if (!Domain.getAll().isEmpty() && !Domain.exists(domainPart))
                    { // the domain is not in the whitelist and the whitelist is not empty
                        context.getFlashScope().error("registerUser_Flash_NotWhitelisted");
                        registerFormData.clearPasswordFields();

                        return result.render("registerUserData", registerFormData);
                    }
                }

                // a new user, check whether the passwords are matching
                if (registerFormData.getPassword().equals(registerFormData.getPasswordNew1()))
                {
                    if (registerFormData.getPasswordNew1().length() < xcmConfiguration.PW_LENGTH)
                    { // password is too short

                        setShortPwInCtx(registerFormData.getLanguage(), context);
                        registerFormData.clearPasswordFields();
                        return result.render("registerUserData", registerFormData);
                    }
                    // create the user
                    User user = registerFormData.getAsUser();

                    // handle the language
                    if (!Arrays.asList(xcmConfiguration.APP_LANGS).contains(user.getLanguage()))
                    { // the language stored in the user-object does not exist in the app
                        registerFormData.clearPasswordFields();
                        context.getFlashScope().error("flash_PasswordsUnequal");

                        return result.render("registerUserData", registerFormData);
                    }

                    // generate the confirmation-token
                    user.setConfirmation(HelperUtils.getRandomSecureString(20));
                    user.setTs_confirm(DateTime.now().plusHours(xcmConfiguration.CONFIRMATION_PERIOD).getMillis());

                    user.save();
                    Optional<String> language = Optional.of(user.getLanguage());
                    mailrSenderFactory.sendConfirmAddressMail(user.getMail(), user.getForename(),
                                                              String.valueOf(user.getId()), user.getConfirmation(),
                                                              language);
                    context.getFlashScope().success("registerUser_Flash_Successful");

                    lang.setLanguage(user.getLanguage(), result);
                    return Results.redirect(context.getContextPath() + "/");
                }
                else
                { // password mismatch
                    registerFormData.clearPasswordFields();
                    context.getFlashScope().error("flash_PasswordsUnequal");

                    return result.render("registerUserData", registerFormData);
                }
            }
            else
            { // mailadress already exists
                context.getFlashScope().error("flash_MailExists");
                return result.render("registerUserData", registerFormData);
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
        Result result = Results.html();
        User user = User.getById(userId);
        if (user != null)
        { // the user exists
            if (user.getConfirmation() != null && (user.getConfirmation().equals(token))
                && (user.getTs_confirm() >= DateTime.now().getMillis()))
            { // the passed token is the right one -> activate the user
                user.setActive(true);
                user.update();
                context.getFlashScope().success("user_Verify_Success");
                lang.setLanguage(user.getLanguage(), result);
                return result.redirect(context.getContextPath() + "/login");
            }
        }
        // show no message when the process failed
        return result.redirect(context.getContextPath() + "/login");
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
        return Results.html().render(new LoginFormData());
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
            context.getFlashScope().error("flash_FormError");
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
                        context.getFlashScope().error("user_Inactive");
                        return result.redirect(context.getContextPath() + "/");
                    }

                    // we put the username into the cookie, but use the id of the cookie for authentication
                    String sessionKey = context.getSession().getId();
                    cachingSessionHandler.set(sessionKey, xcmConfiguration.COOKIE_EXPIRETIME, loginUser);
                    // set a reverse mapped user-mail -> sessionId-list in the memcached server to handle
                    // session-expiration for admin-actions (e.g. if an admin deletes a user that is currently
                    // logged-in)
                    cachingSessionHandler.setSessionUser(loginUser, sessionKey, xcmConfiguration.COOKIE_EXPIRETIME);

                    context.getSession().put("username", loginUser.getMail());

                    if (loginUser.isAdmin())
                    { // set a admin-flag at the cookie if the user is admin
                      // we use this only to change the header-menu-view, but not for "real admin-actions"
                        context.getSession().put("adm", "1");
                    }
                    loginUser.setBadPwCount(0);
                    loginUser.update();
                    context.getFlashScope().success("login_Flash_LogIn");

                    // set the language the user wants to have
                    lang.setLanguage(loginUser.getLanguage(), result);

                    return result.redirect(context.getContextPath() + "/");
                }
                else
                { // the authentication was not correct
                    loginUser.setBadPwCount(loginUser.getBadPwCount() + 1);
                    if (loginUser.getBadPwCount() >= 6)
                    { // the password was six times wrong
                        loginUser.setActive(false);
                        loginUser.update();

                        // show the disabled message and return to the forgot-pw-page
                        context.getFlashScope().error("login_Flash_UserDisabled");
                        return result.redirect(context.getContextPath() + "/pwresend");
                    }
                    // update the user here
                    loginUser.update();
                    loginData.setPassword("");
                    context.getFlashScope().error("flash_FormError");
                    return result.render(loginData);
                }
            }
            else
            {// the user does not exist
                loginData.setPassword("");
                context.getFlashScope().error("flash_FormError");
                return result.render(loginData);
            }
        }
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
        // remove the session (cachingServer and cookie)
        String sessionKey = context.getSession().getId();
        context.getSession().clear();
        cachingSessionHandler.delete(sessionKey);

        // show the index-page
        context.getFlashScope().success("logout_Flash_LogOut");
        return Results.redirect(context.getContextPath() + "/");
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
     * @param loginData
     *            the Data of the Resend-Password-Form (just one Field for the Mail-Address)
     * @param validation
     *            Form validation
     * @return the Index-Page
     */
    public Result forgotPasswordProcess(Context context, @JSR303Validation LoginFormData loginData,
                                        Validation validation)
    {
        if (validation.hasViolations())
        { // some fields weren't filled
            context.getFlashScope().error("flash_FormError");
            return Results.redirect(context.getContextPath() + "/pwresend");
        }
        else
        {
            User user = User.getUsrByMail(loginData.getMail());

            if (user != null)
            { // mailadress was correct (exists in the DB)
              // generate a new confirmation token and send it to the given mailadress

                // generate the confirmation-token and set it in the user object
                user.setConfirmation(HelperUtils.getRandomSecureString(20));
                // set the new validity-time
                user.setTs_confirm(DateTime.now().plusHours(xcmConfiguration.CONFIRMATION_PERIOD).getMillis());
                user.update();
                Optional<String> lang = Optional.of(user.getLanguage());
                mailrSenderFactory.sendPwForgotAddressMail(user.getMail(), user.getForename(),
                                                           String.valueOf(user.getId()), user.getConfirmation(), lang);
            }

            // We always show the success-message, even when the user does not exist
            context.getFlashScope().success("forgotPassword_Flash_Success");
            return Results.redirect(context.getContextPath() + "/");
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
                Result result = Results.html();
                lang.setLanguage(user.getLanguage(), result);
                // show the form for the new password
                return result.render("id", id.toString()).render("token", token);
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
        // check the PathParams again
        User user = User.getById(id);
        if (user != null)
        { // the user exists
            if ((user.getConfirmation().equals(token)) && (user.getTs_confirm() >= DateTime.now().getMillis()))
            { // the passed token is the right one
                if (!validation.hasViolations())
                { // the form was filled correctly
                    if (passwordFormData.getPassword().equals(passwordFormData.getPassword2()))
                    { // the entered PWs are equal
                        if (passwordFormData.getPassword().length() < xcmConfiguration.PW_LENGTH)
                        { // check whether the password has the correct length
                            setShortPwInCtx(user.getLanguage(), context);
                            passwordFormData.setPassword("");
                            passwordFormData.setPassword2("");
                            return Results.redirect(context.getContextPath() + "/lostpw/" + id + "/" + token);
                        }
                        user.hashPasswd(passwordFormData.getPassword());
                        user.setActive(true);
                        user.setBadPwCount(0);

                        // set the confirm-period-timestamp to now to prevent the reuse of the link
                        user.setTs_confirm(DateTime.now().getMillis());
                        user.update();
                        context.getFlashScope().success("flash_DataChangeSuccess");
                        return Results.redirect(context.getContextPath() + "/");
                    }
                    else
                    { // the passwords are not equal
                        context.getFlashScope().error("flash_PasswordsUnequal");
                        return Results.redirect(context.getContextPath() + "/lostpw/" + id + "/" + token);
                    }
                }
                else
                { // the form has errors
                    context.getFlashScope().error("flash_FormError");
                    return Results.redirect(context.getContextPath() + "/lostpw/" + id + "/" + token);
                }
            }
        }
        // if the link was wrong -> redirect without any message
        return Results.redirect(context.getContextPath() + "/");
    }

    /**
     * Sets the "password is too short"-message to the context
     * 
     * @param language
     *            the language to use
     * @param context
     *            the context object
     */
    private void setShortPwInCtx(String language, Context context)
    {
        Optional<String> optionalLanguage = Optional.of(language);
        String tooShortPassword = messages.get("flash_PasswordTooShort", optionalLanguage, xcmConfiguration.PW_LENGTH)
                                          .get();
        context.getFlashScope().error(tooShortPassword);
    }

    public Result getStatusMessage(Context context, String messageKey)
    {
        Result result = Results.json();
        String errorMessage = messages.get(messageKey, context, Optional.of(result)).get();
        return result.render("message", errorMessage);
    }
}
