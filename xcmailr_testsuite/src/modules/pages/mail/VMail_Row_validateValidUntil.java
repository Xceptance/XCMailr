package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validate until date by seleceted row.</p>
 */
public class VMail_Row_validateValidUntil
{

    /**
     * <p>Validate until date by seleceted row.</p>
     *
     * @param row
     * @param validUntil
     */
    public static void execute(String row, String validUntil)
    {
        // resolve any placeholder in the parameters
        row = resolve(row);
        validUntil = resolve(validUntil);
        assertText("css=#table-mailbox-overview tr:nth-of-type(" + row + ") .mailbox-date-validUntil", "");

    }
}