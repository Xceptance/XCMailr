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
        { // the filled form has errors
          // delete all pw-entries

            edt.setPw("");
            edt.setPwn1("");
            edt.setPwn2("");
            s = msg.get("msg_formerr", context, result, "String");
            context.getFlashCookie().error(s, (Object) null);
            return Results.html().template("/views/UserHandler/editUserForm.ftl.html").render(edt);

        }
        else
        { // the form is filled correctly

            String pw1 = edt.getPwn1();
            String pw2 = edt.getPwn2();
            User updU = User.authById(uId, edt.getPw());

            if (!(updU == null))
            { // the user authorized himself

                if (!User.mailChanged(edt.getMail(), uId))
                { // the mailaddress changed
                    updU.setMail(edt.getMail());
                }
                // update the fore- and surname
                updU.setForename(edt.getForename());
                updU.setSurname(edt.getSurName());
                if (!(pw1 == null) && !(pw2 == null))
                {
                    if (!(pw2.isEmpty()) && !(pw1.isEmpty()) && pw1.equals(pw2))
                    { // new password was entered and the repetition is equally
                        updU.hashPasswd(pw2);

                    }
                }
                User.updateUser(updU); // update the user
                s = msg.get("msg_chok", context, result, "String");
                context.getFlashCookie().success(s, (Object) null);
                return Results.redirect("/user/edit");
            }
            else
            { // the authorization-prozess failed
              // reset all pw-entries
                edt.setPw("");
                edt.setPwn1("");
                edt.setPwn2("");
                s = msg.get("msg_formerr", context, result, "String");
                context.getFlashCookie().error(s, (Object) null);
                return Results.html().template("/views/UserHandler/editUserForm.ftl.html").render(edt);
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
