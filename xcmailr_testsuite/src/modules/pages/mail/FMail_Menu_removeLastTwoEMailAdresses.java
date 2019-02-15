package modules.pages.mail;
import modules.pages.mail.dialog.MMail_Dialog_RemoveEmail_clickDelete;
import modules.pages.mail.dialog.VMail_Dialog_RemoveEmail_validateTwoEMailAdresses;

/**
 * <p>Remove last two email adresses.</p>
 */
public class FMail_Menu_removeLastTwoEMailAdresses
{

    /**
     * <p>Remove last two email adresses.</p>
     *
     */
    public static void execute()
    {
        SMail_Row_storeEmailAdress.execute("2", "selectedEmail_01_varDynamic");

        SMail_Row_storeEmailAdress.execute("3", "selectedEmail_02_varDynamic");

        MMail_Table_selectAllEmailAdresses.execute();

        MMail_Menu_delete.execute();

        VMail_Dialog_RemoveEmail_validateTwoEMailAdresses.execute("${selectedEmail_01_varDynamic}", "${selectedEmail_02_varDynamic}");

        MMail_Dialog_RemoveEmail_clickDelete.execute();
    }
}