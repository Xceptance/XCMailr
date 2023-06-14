package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click the edit element at the selected table row.</p>
 */
public class MMail_Row_clickEditSelectedEMail
{

    /**
     * <p>Click the edit element at the selected table row.</p>
     *
     * @param row
     */
    public static void execute(String row)
    {
        // resolve any placeholder in the parameters
        row = resolve(row);
        //
        // ~~~ openEditDialog ~~~
        //
        startAction("OpenMailboxSettingsEditDialog");
        // select first element
        click("css=#table-mailbox-overview tr:nth-of-type(" + row + ") a.btn-link-edit");
        waitForElementPresent("css=.modal-dialog");

    }
}