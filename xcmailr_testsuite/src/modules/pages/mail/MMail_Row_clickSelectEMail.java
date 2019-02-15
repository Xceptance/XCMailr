package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click the checkbox at the selected table row.</p>
 */
public class MMail_Row_clickSelectEMail
{

    /**
     * <p>Click the checkbox at the selected table row.</p>
     *
     * @param row
     */
    public static void execute(String row)
    {
        // resolve any placeholder in the parameters
        row = resolve(row);
        startAction("SelectMailbox");
        check("css=#table-mailbox-overview tr:nth-of-type(" + row + ") input.bulkChk");
        waitForChecked("css=#table-mailbox-overview tr:nth-of-type(" + row + ") input.bulkChk");

    }
}