package controllers;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import models.EditUsr;
import models.User;
import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.i18n.Messages;
import ninja.validation.JSR303Validation;
import ninja.validation.Validation;
import filters.SecureFilter;

/**
 * Handles the actions of the user-object
 * 
 * @author Patrick Thum 2012 released under Apache 2.0 License
 */
@FilterWith(SecureFilter.class)
@Singleton
public class UserHandler
{
    @Inject
    MemCachedSessionHandler mcsh;

    @Inject
    Messages msg;

    /**
     * Edits the user-data <br/>
     * POST /user/edit
     * 
     * @return the edit-page again
     */
    public Result editUser(Context context, @JSR303Validation EditUsr edt, Validation validation)
    {

        User usr = (User) mcsh.get(context.getSessionCookie().getId());
        Result result = Results.html();
        Optional<Result> opt = Optional.of(result);
        String s;

        if (validation.hasViolations())
        { // the filled form has errors
            s = msg.get("i18nmsg_formerr", context, opt, (Object) null).get();
            context.getFlashCookie().error(s, (Object) null);
            return Results.redirect("/user/edit");
        }
        else
        { // the form is filled correctly
            String pw1 = edt.getPwn1();
            String pw2 = edt.getPwn2();

            if (usr.checkPasswd(edt.getPw()))
            { // the user authorized himself
                if (!User.mailChanged(edt.getMail(), usr.getId()))
                { // the mailaddress changed
                    usr.setMail(edt.getMail());
                }
                // update the fore- and surname
                usr.setForename(edt.getForename());
                usr.setSurname(edt.getSurName());
                if (!(pw1 == null) && !(pw2 == null))
                {
                    if (!(pw2.isEmpty()) && !(pw1.isEmpty()))
                    { // new password has been entered
                        if (pw1.equals(pw2))
                        { // the repetition is equal to the new pw
                            usr.hashPasswd(pw2);
                        }
                        else
                        { // the passwords are not equal
                            s = msg.get("i18nmsg_wrongpw", context, opt, (Object) null).get();
                            context.getFlashCookie().error(s, (Object) null);
                            return Results.html().template("views/UserHandler/editUserForm.ftl.html").render(edt);
                        }
                    }
                }
                // update the user
                usr.update();
                mcsh.set(context.getSessionCookie().getId(), 3600, usr);
                s = msg.get("i18nmsg_chok", context, opt, (Object) null).get();
                context.getFlashCookie().success(s, (Object) null);
                return Results.redirect("/user/edit");
            }
            else
            { // the authorization-prozess failed
                s = msg.get("i18nmsg_formerr", context, opt, (Object) null).get();
                context.getFlashCookie().error(s, (Object) null);
                return Results.redirect("/user/edit");
            }
        }
    }

    /**
     * Prepopulates the EditForm and Show it <br/>
     * GET /user/edit
     * 
     * @return the user-edit-form
     */
    public Result editUserForm(Context context)
    {
        User usr = (User) mcsh.get(context.getSessionCookie().getId());
        return Results.html().render(EditUsr.prepopulate(usr));
    }

}
