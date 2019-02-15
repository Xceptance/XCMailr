package modules.pages.mail.dialog;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click the save button.</p>
 */
public class MMail_Dialog_CUEmail_clickSave
{

    /**
     * <p>Click the save button.</p>
     *
     */
    public static void execute()
    {
        // save
        click("css=button.btn-submit");
        waitForNotElementPresent("css=.modal-content");

    }
}