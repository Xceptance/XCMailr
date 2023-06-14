package modules.pages.account;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click  the save button</p>
 */
public class MAccount_clickSaveAccount
{

    /**
     * <p>Click  the save button</p>
     *
     */
    public static void execute()
    {
        // click - "save"
        //
        // ~~~ confirm ~~~
        //
        startAction("SaveChanges");
        clickAndWait("css=#editUserSubmit");

    }
}