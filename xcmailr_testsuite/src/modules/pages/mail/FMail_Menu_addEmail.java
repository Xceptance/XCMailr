package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

import modules.pages.mail.MMail_Menu_clickAddEMail;
import modules.pages.mail.dialog.MMail_Dialog_CUEmail_clickSave;
import modules.pages.mail.dialog.MMail_Dialog_CUEmail_enterData;

/**
 * <p>Add E-Mail adress.</p>
 */
public class FMail_Menu_addEmail
{

    /**
     * <p>Add E-Mail adress.</p>
     *
     * @param domainLocalPart
     * @param domainHostPart
     * @param validUntilDate
     */
    public static void execute(String domainLocalPart, String domainHostPart, String validUntilDate)
    {
        // resolve any placeholder in the parameters
        domainLocalPart = resolve(domainLocalPart);
        domainHostPart = resolve(domainHostPart);
        validUntilDate = resolve(validUntilDate);

        MMail_Menu_clickAddEMail.execute();

        MMail_Dialog_CUEmail_enterData.execute(domainLocalPart, domainHostPart, validUntilDate);

        startAction("ConfirmMailboxNew");
        MMail_Dialog_CUEmail_clickSave.execute();


    }
}