package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click the &quot;add email&quot; element at the menubar.</p>
 */
public class MMail_Menu_clickAddEMail
{

    /**
     * <p>Click the &quot;add email&quot; element at the menubar.</p>
     *
     */
    public static void execute()
    {
        startAction("AddNewMailbox");
        click("id=add-email");
        waitForVisible("css=.modal-dialog");

    }
}