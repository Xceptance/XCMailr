package controllers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import models.EditUsr;
import models.User;
import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.i18n.Lang;
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
    Lang lang;

    @Inject
    Messages msg;

    /**
     * edits the user-data
     * 
     * @return the result-page
     */
    public Result editUser(Context context, @JSR303Validation EditUsr edt, Validation validation)
    {

        Long uId = new Long(context.getSessionCookie().get("id"));
        Result result = Results.html();
        String s;

        if (validation.hasViolations())
        {
            // the filled form has errors
            s = msg.get("msg_formerr", context, result, "String");
            context.getFlashCookie().error(s, (Object) null);
            return Results.redirect("/user/edit").render(edt);
        }
        else
        {
            // the form is filled correctly
            if (!User.mailExists(edt.getMail(), uId))
            {
                /*
                 * TODO check if this works properly actually it should go here if there's no user or uid and mailadr.
                 * belong together
                 */

                String pw1 = edt.getPwn1();
                String pw2 = edt.getPwn2();
                User updU = User.authById(uId, edt.getPw());
                // TODO check if the pw was set...
                if (!updU.equals(null))
                {
                    // the user authorized himself
                    // update the fore- and surname
                    updU.setForename(edt.getForename());
                    updU.setSurname(edt.getSurName());
                }// TODO i think this brace should be set after the return
                if (!(pw1.isEmpty() && pw2.isEmpty()) && pw1.equals(pw2))
                {
                    // new password was entered and the repetition is equally
                    updU.setPasswd(pw2);
                }
                updU.setId(uId); // TODO check if this is unneccessary
                User.updateUser(updU); // update the user
                s = msg.get("msg_editok", context, result, "String");
                context.getFlashCookie().error(s, (Object) null);
                return Results.redirect("/user/edit");

            }
            else
            {
                s = msg.get("msg_mailex", context, result, "String");
                context.getFlashCookie().error(s, (Object) null);
                return Results.redirect("/user/edit");

            }
        }

    }

    /**
     * prepopulate the editform and show it
     * 
     * @return the user-edit-form
     */
    public Result editUserForm(Context context)
    {

        Long id = new Long(context.getSessionCookie().get("id"));

        User usr = User.getById(id);

        return Results.html().render(EditUsr.prepopulate(usr));

    }

}
