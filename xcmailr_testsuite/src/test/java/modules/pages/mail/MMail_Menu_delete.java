package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click the &quot;Delete&quot; element at the dropdown menu.</p>
 */
public class MMail_Menu_delete
{

    /**
     * <p>Click the &quot;Delete&quot; element at the dropdown menu.</p>
     *
     */
    public static void execute()
    {
        startAction("OpenMailboxActionDropdown");
        click("id=action-dropdown");
        waitForVisible("css=.dropdown-menu");

        startAction("DeleteMailboxViaDropdown");
        // .disable-selected
        click("css=.delete-selected");
        waitForNotVisible("css=.dropdown-menu");

    }
}