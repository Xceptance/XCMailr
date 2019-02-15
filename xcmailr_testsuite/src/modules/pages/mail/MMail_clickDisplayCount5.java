package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click the play element at the selected table row.</p>
 */
public class MMail_clickDisplayCount5
{

    /**
     * <p>Click the play element at the selected table row.</p>
     *
     */
    public static void execute()
    {
        //
        // ~~~ clickPlaySelectedEMail ~~~
        //
        startAction("ChangeItemsPerPage");
        click("css=.show-itemsPerPage-5");

    }
}