package modules.pages.account.dialog;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click  the delete account button in the dialog.</p>
 */
public class MAccount_Dialog_clickYesDeleteAccount
{

    /**
     * <p>Click  the delete account button in the dialog.</p>
     *
     */
    public static void execute()
    {
        //
        // ~~~ confirmDeleteAccount ~~~
        //
        startAction("ConfirmDeleteAccount");
        clickAndWait("css=#deleteUserSubmit");

    }
}