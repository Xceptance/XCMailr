package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click the play element at the selected table row.</p>
 */
public class MMail_Row_clickPlaySelectedEMail
{

    /**
     * <p>Click the play element at the selected table row.</p>
     *
     * @param row
     */
    public static void execute(String row)
    {
        // resolve any placeholder in the parameters
        row = resolve(row);

        startAction("ActivateSelectedMailbox");
        click("css=#table-mailbox-overview tr:nth-of-type(" + row + ") a.btn-link-play");
        waitForElementPresent("css=#table-mailbox-overview tr:nth-of-type(" + row + ") i.glyphicon-pause");

    }
}