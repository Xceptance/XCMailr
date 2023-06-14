package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validate first email at the table.</p>
 */
public class VMail_Table_validateFirstElement
{

    /**
     * <p>Validate first email at the table.</p>
     *
     * @param nominalValue
     */
    public static void execute(String nominalValue)
    {
        // resolve any placeholder in the parameters
        nominalValue = resolve(nominalValue);
        assertText("css=#table-mailbox-overview tr:nth-of-type(2) .mailbox-adress", nominalValue);

    }
}