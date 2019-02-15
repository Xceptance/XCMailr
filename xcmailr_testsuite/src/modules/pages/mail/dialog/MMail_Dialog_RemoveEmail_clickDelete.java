package modules.pages.mail.dialog;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click the delete button</p>
 */
public class MMail_Dialog_RemoveEmail_clickDelete
{

    /**
     * <p>Click the delete button</p>
     *
     */
    public static void execute()
    {
        // save
        startAction("ConfirmMailboxRemoval");
        click("css=button.btn-submit");
        waitForNotElementPresent("id=deleteBoxModal");

    }
}