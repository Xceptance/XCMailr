package modules.pages.account;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click the delete account button.</p>
 */
public class MAccount_clickDeleteAccount
{

    /**
     * <p>Click the delete account button.</p>
     *
     */
    public static void execute()
    {
        // click - delete account button
        //
        // ~~~ clickDeleteAccount ~~~
        //
        startAction("ClickDeleteAccount");
        click("css=.remove-user");
        waitForVisible("css=.modal-dialog");

    }
}