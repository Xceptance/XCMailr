package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click the &quot;Enable&quot; element at the dropdown menu.</p>
 */
public class MMail_Menu_setStatusActive
{

    /**
     * <p>Click the &quot;Enable&quot; element at the dropdown menu.</p>
     *
     */
    public static void execute()
    {
        startAction("OpenMailboxActionDropdown");
        click("id=action-dropdown");
        waitForVisible("css=.dropdown-menu");

        startAction("EnableMailboxViaDropdown");
        // .disable-selected
        click("css=.enable-selected");
        waitForNotVisible("css=.dropdown-menu");

    }
}