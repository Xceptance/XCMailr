package modules.pages.mail.dialog;

import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Enter data for the new email.
 * </p>
 */
public class MMail_Dialog_CUEmail_enterData
{

    /**
     * <p>
     * Enter data for the new email.
     * </p>
     *
     * @param localPart
     * @param domainPart
     * @param validUntilPast
     */
    public static void execute(String localPart, String domainPart, String validUntilPast)
    {
        // resolve any placeholder in the parameters
        localPart = resolve(localPart);
        domainPart = resolve(domainPart);
        validUntilPast = resolve(validUntilPast);

        type("id=editBoxDialogAddress", localPart);
        waitForText("id=editBoxDialogAddress", localPart);
        select("name=domain", "value=" + domainPart);

        if (StringUtils.isNotBlank(validUntilPast))
        {
            type("css=.input-datetime", validUntilPast);
            waitForText("css=.input-datetime", validUntilPast);
        }
        else if(!isChecked("css=.modal-body input[id^=chkUnlimited]"))
        {
            check("css=.modal-body input[id^=chkUnlimited]");
            waitForChecked("css=.modal-body input[id^=chkUnlimited]");
        }
    }
}