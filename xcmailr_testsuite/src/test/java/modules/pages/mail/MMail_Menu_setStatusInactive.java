package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click the &quot;Disable&quot; element at the dropdown menu.</p>
 */
public class MMail_Menu_setStatusInactive
{

    /**
     * <p>Click the &quot;Disable&quot; element at the dropdown menu.</p>
     *
     */
    public static void execute()
    {
        startAction("OpenMailboxActionDropdown");
        click("id=action-dropdown");
        waitForVisible("css=.dropdown-menu");

        startAction("DisableMailboxViaDropdown");
        click("css=.disable-selected");
        waitForNotVisible("css=.dropdown-menu");

    }
}