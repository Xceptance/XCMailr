package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click the reset counter element at the selected table row.</p>
 */
public class MMail_Row_clickResetCounterSelectedEMail
{

    /**
     * <p>Click the reset counter element at the selected table row.</p>
     *
     * @param row
     */
    public static void execute(String row)
    {
        // resolve any placeholder in the parameters
        row = resolve(row);
        //
        // ~~~ resetCounterSelectedEMail ~~~
        //
        startAction("ResetCounterForSelectedMailbox");
        click("css=#table-mailbox-overview tr:nth-of-type(" + row + ") a.btn-link-repeat");
        waitForText("css=#table-mailbox-overview tr:nth-of-type(" + row + ") .counter-forwards", "0");
        waitForText("css=#table-mailbox-overview tr:nth-of-type(" + row + ") .counter-suppressions", "0");

    }
}