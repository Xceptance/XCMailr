package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validate email by seleceted row.</p>
 */
public class VMail_Row_validateEmailAdress
{

    /**
     * <p>Validate email by seleceted row.</p>
     *
     * @param row
     * @param emailAdress
     */
    public static void execute(String row, String emailAdress)
    {
        // resolve any placeholder in the parameters
        row = resolve(row);
        emailAdress = resolve(emailAdress);
        assertText("css=#table-mailbox-overview tr:nth-of-type(" + row + ") .mailbox-adress", emailAdress);

    }
}