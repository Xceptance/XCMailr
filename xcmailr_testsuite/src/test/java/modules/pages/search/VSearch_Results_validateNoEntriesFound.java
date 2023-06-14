package modules.pages.search;

import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>
 * Validate no entry message.
 * </p>
 */
public class VSearch_Results_validateNoEntriesFound
{

    /**
     * <p>
     * Validate no entry message.
     * </p>
     */
    public static void execute()
    {
        waitForVisible("css=.info");
        assertText("css=tr.info td:nth-of-type(2)", "No entries to display *");
    }
}