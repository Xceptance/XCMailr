package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validate status by seleceted row.</p>
 */
public class VMail_Row_validateEmailStatus
{

    /**
     * <p>Validate status by seleceted row.</p>
     *
     * @param row
     * @param emailStatus
     */
    public static void execute(String row, String emailStatus)
    {
        // resolve any placeholder in the parameters
        row = resolve(row);
        emailStatus = resolve(emailStatus);
        assertText("css=#table-mailbox-overview tr:nth-of-type(" + row + ") .mailbox-status", emailStatus);

    }
}