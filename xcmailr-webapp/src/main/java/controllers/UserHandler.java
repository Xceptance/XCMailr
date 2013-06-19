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

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import conf.XCMailrConf;
import models.UserFormData;
import models.User;
import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.i18n.Lang;
import ninja.i18n.Messages;
import ninja.validation.JSR303Validation;
import ninja.validation.Validation;
import etc.HelperUtils;
import filters.SecureFilter;

/**
 * Handles the actions of the User-Object
 * 
 * @see User
 * @author Patrick Thum 2012 released under Apache 2.0 License
 */
@FilterWith(SecureFilter.class)
@Singleton
public class UserHandler
{
    @Inject
    MemCachedSessionHandler memCachedSessionHandler;

    @Inject
    XCMailrConf xcmConfiguration;

    @Inject
    Messages msg;

    @Inject
    Lang lang;

    /**
     * Edits the {@link User}-Data <br/>
     * POST /user/edit
     * 
     * @param context
     *            the Context of this Request
     * @param userFormData
     *            the Data of the User-Edit-Form
     * @param validation
     *            Form validation
     * @return the Edit-Page again
     */
    public Result editUserProcess(Context context, @JSR303Validation UserFormData userFormData, Validation validation)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");

        // set the available languages again. in most cases this may not be necessary,
        // but if you send the post-request directly and have form violations or wrong passwords or sth.
        // then you would likely get a NullPointerException
        List<String[]> o = HelperUtils.getLanguageList(xcmConfiguration.APP_LANGS, context, msg);
        result.render("available_langs", o);

        User user = context.getAttribute("user", User.class);

        if (validation.hasViolations())
        { // the filled form has errors
            context.getFlashCookie().error("flash_FormError");
            return result.template("/views/UserHandler/editUserForm.ftl.html").redirect(context.getContextPath()
                                                                                            + "/user/edit");
        }
        else
        { // the form is filled correctly

            // don't let the user register with one of our domains
            // (prevent mail-loops)
            String mail = userFormData.getMail();
            String domainPart = mail.split("@")[1];
            if (Arrays.asList(xcmConfiguration.DOMAIN_LIST).contains(domainPart))
            {
                context.getFlashCookie().error("flash_NoLoop");
                userFormData.setMail(user.getMail());
                userFormData.setPassword("");
                userFormData.setPasswordNew1("");
                userFormData.setPasswordNew2("");
                return result.template("/views/UserHandler/editUserForm.ftl.html").render(userFormData);
            }

            String password1 = userFormData.getPasswordNew1();
            String password2 = userFormData.getPasswordNew2();

            if (user.checkPasswd(userFormData.getPassword()))
            { // the user authorized himself

                if (!userFormData.getMail().equals(user.getMail()))
                { // the user's mail-address changed
                    if (User.mailExists(userFormData.getMail()))
                    {// throw mailex-error
                        context.getFlashCookie().error("flash_MailExists");
                        userFormData.setPassword("");
                        userFormData.setPasswordNew1("");
                        userFormData.setPasswordNew2("");

                        return result.template("/views/UserHandler/editUserForm.ftl.html").render(userFormData);
                    }
                    else
                    { // the address does not exist
                        user.setMail(userFormData.getMail());
                    }
                }
                // update the fore- and surname
                user.setForename(userFormData.getFirstName());
                user.setSurname(userFormData.getSurName());
                if ((password1 != null) && (password2 != null))
                {
                    if (!(password2.isEmpty()) && !(password1.isEmpty()))
                    { // new password has been entered
                        if (password1.equals(password2))
                        { // the repetition is equal to the new pw
                            if (password1.length() < xcmConfiguration.PW_LENGTH)
                            {
                                Optional<String> opt = Optional.of(context.getAcceptLanguage());
                                String tooShortPassword = msg.get("flash_PasswordTooShort", opt,
                                                                  xcmConfiguration.PW_LENGTH.toString()).get();
                                context.getFlashCookie().error(tooShortPassword);
                                userFormData.setPassword("");
                                userFormData.setPasswordNew1("");
                                userFormData.setPasswordNew2("");

                                return result.template("/views/UserHandler/editUserForm.ftl.html").render(userFormData);
                            }

                            user.hashPasswd(password2);
                        }
                        else
                        { // the passwords are not equal
                            context.getFlashCookie().error("flash_PasswordsUnequal");
                            userFormData.setPassword("");
                            userFormData.setPasswordNew1("");
                            userFormData.setPasswordNew2("");
                            return result.template("/views/UserHandler/editUserForm.ftl.html").render(userFormData);
                        }
                    }
                }
                if (Arrays.asList(xcmConfiguration.APP_LANGS).contains(userFormData.getLanguage()))
                { // set the selected language in the user-object and also in the application
                    user.setLanguage(userFormData.getLanguage());
                    lang.setLanguage(userFormData.getLanguage(), result);
                }
                // update the user
                user.update();
                context.getSessionCookie().put("username", userFormData.getMail());
                memCachedSessionHandler.set(context.getSessionCookie().getId(), xcmConfiguration.COOKIE_EXPIRETIME,
                                            user);
                // user-edit was successful
                context.getFlashCookie().success("flash_DataChangeSuccess");
                return result.template("/views/UserHandler/editUserForm.ftl.html").redirect(context.getContextPath()
                                                                                                + "/user/edit");
            }
            else
            { // the authorization-process failed
                userFormData.setPassword("");
                userFormData.setPasswordNew1("");
                userFormData.setPasswordNew2("");
                context.getFlashCookie().error("flash_FormError");
                return result.template("/views/UserHandler/editUserForm.ftl.html").redirect(context.getContextPath()
                                                                                                + "/user/edit");
            }
        }
    }

    /**
     * Prepopulates the EditForm and show it <br/>
     * GET /user/edit
     * 
     * @param context
     *            the Context of this Request
     * @return the {@link User}-Edit-Form
     */

    public Result editUserForm(Context context)
    {
        List<String[]> availableLanguageList = HelperUtils.getLanguageList(xcmConfiguration.APP_LANGS, context, msg);
        Result result = Results.html();
        result.render("available_langs", availableLanguageList);
        User user = context.getAttribute("user", User.class);
        if (user.getLanguage() == null || user.getLanguage() == "")
        {
            Optional<Result> opt = Optional.of(result);
            user.setLanguage(lang.getLanguage(context, opt).get());
            user.update();
            memCachedSessionHandler.set(context.getSessionCookie().getId(), xcmConfiguration.COOKIE_EXPIRETIME, user);
        }
        UserFormData userFormData = UserFormData.prepopulate(user);
        return result.render(userFormData);
    }

    /**
     * Handles the {@link models.User User}-Delete-Function <br/>
     * POST /user/delete
     * 
     * @param userId
     *            the ID of a {@link models.User User}
     * @param context
     *            the Context of this Request
     * @return the User-Overview-Page (/admin/users)
     */
    public Result deleteUserProcess(Context context)
    {
        Result result = Results.html().template("/views/Application/index.ftl.html");
        User user = context.getAttribute("user", User.class);
        if (!user.isLastAdmin())
        {
            // delete the session
            context.getSessionCookie().clear();
            memCachedSessionHandler.delete(String.valueOf(user.getId()));
            // delete the user-account
            User.delete(user.getId());
            context.getFlashCookie().success("flash_UserDeletionSuccess");
            return result.redirect(context.getContextPath() + "/");
        }

        context.getFlashCookie().error("flash_UserDeletionFailed");
        return result.redirect(context.getContextPath() + "/");
    }

}
