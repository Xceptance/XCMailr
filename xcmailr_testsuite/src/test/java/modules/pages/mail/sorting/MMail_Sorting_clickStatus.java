package modules.pages.mail.sorting;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>click &quot;Status&quot; at menubar</p>
 */
public class MMail_Sorting_clickStatus
{

    /**
     * <p>click &quot;Status&quot; at menubar</p>
     *
     */
    public static void execute()
    {
        startAction("SortByExpiration");
        click("css=.head-sort-expired");
        // waitForElementPresent("css=.modal-dialog");
    }
}