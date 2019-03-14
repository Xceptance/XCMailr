package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validate last email at the table.</p>
 */
public class VMail_Table_validateLastElement
{

    /**
     * <p>Validate last email at the table.</p>
     *
     * @param nominalValue
     */
    public static void execute(String nominalValue)
    {
        // resolve any placeholder in the parameters
        nominalValue = resolve(nominalValue);
        assertText("css=#table-mailbox-overview tr:last-of-type .mailbox-adress", nominalValue);

    }
}