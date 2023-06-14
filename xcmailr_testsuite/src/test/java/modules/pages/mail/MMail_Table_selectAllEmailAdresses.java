package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click the checkbox which select all emails</p>
 */
public class MMail_Table_selectAllEmailAdresses
{

    /**
     * <p>Click the checkbox which select all emails</p>
     *
     */
    public static void execute()
    {
        startAction("SelectAllMailboxes");
        check("id=chk_all");
        waitForChecked("id=chk_all");

    }
}