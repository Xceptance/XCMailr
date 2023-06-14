package modules.pages.mail.sorting;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>click &quot;Email Adress&quot; at menubar</p>
 */
public class MMail_Sorting_clickEMail
{

    /**
     * <p>click &quot;Email Adress&quot; at menubar</p>
     *
     */
    public static void execute()
    {
        startAction("SortByAddress");
        click("css=.head-sort-adress");

    }
}