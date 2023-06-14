package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

import modules.pages.mail.MMail_Row_clickRemoveSelectedEMail;
import modules.pages.mail.SMail_Row_storeEmailAdress;
import modules.pages.mail.dialog.MMail_Dialog_RemoveEmail_clickDelete;
import modules.pages.mail.dialog.VMail_Dialog_RemoveEmail_validateEMailAdress;

/**
 * <p>Remove selected email adress.</p>
 */
public class FMail_Row_removeEmail
{

    /**
     * <p>Remove selected email adress.</p>
     *
     * @param row
     */
    public static void execute(String row)
    {
        // resolve any placeholder in the parameters
        row = resolve(row);
        SMail_Row_storeEmailAdress.execute(row, "selectedEmail_varDynamic");

        MMail_Row_clickRemoveSelectedEMail.execute(row);

        // validate emailadress
        VMail_Dialog_RemoveEmail_validateEMailAdress.execute("${selectedEmail_varDynamic}");

        MMail_Dialog_RemoveEmail_clickDelete.execute();
    }
}