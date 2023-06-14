package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Store the email from the selected row.</p>
 */
public class SMail_Row_storeEmailAdress
{

    /**
     * <p>Store the email from the selected row.</p>
     *
     * @param row
     * @param selectedEmailAdress_varDynamic
     */
    public static void execute(String row, String selectedEmailAdress_varDynamic)
    {
        // resolve any placeholder in the parameters
        row = resolve(row);
        selectedEmailAdress_varDynamic = resolve(selectedEmailAdress_varDynamic);
        storeText("css=#table-mailbox-overview tr:nth-of-type(" + row + ") .mailbox-adress", selectedEmailAdress_varDynamic);

    }
}