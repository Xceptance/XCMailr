package modules.helper.global;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

import modules.global.headernav.MGlobal_Header_clickEditProfile;
import modules.pages.account.MAccount_clickDeleteAccount;
import modules.pages.account.dialog.MAccount_Dialog_clickYesDeleteAccount;
import modules.pages.account.dialog.MAccount_Dialog_enterPassword;

/**
 * <p>Remove account, by pressing the remove button.</p>
 */
public class FHelper_Register_removeAccount
{

    /**
     * <p>Remove account, by pressing the remove button.</p>
     *
     * @param userPassword
     */
    public static void execute(String userPassword)
    {
        // resolve any placeholder in the parameters
        userPassword = resolve(userPassword);
        MGlobal_Header_clickEditProfile.execute();

        // Delete account
        MAccount_clickDeleteAccount.execute();

        // enter account password
        MAccount_Dialog_enterPassword.execute(userPassword);

        MAccount_Dialog_clickYesDeleteAccount.execute();


    }
}