package modules.helper.external;

import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.click;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.getText;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.open;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.startAction;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.waitForText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;

/**
 * <p>
 * Goes to 10 minute email and fetches the just arrived email to retrieve the
 * activte url.
 * </p>
 */
public class FHelper_openclickAuthLinkFromExternal
{
    /**
     * <p>
     * Goes to 10 minute email and fetches the just arrived email to retrieve
     * the activte url.
     * </p>
     */
    public static void execute()
    {
        //
        // ~~~ ReturnToMailer ~~~
        //
        startAction("ReturnToMailer");
        open("${emailClientURL}");
        //
        // ~~~ WaitForMail ~~~
        //
        startAction("WaitForNewMail");
        // Sometimes it least a little bit longer to rececieve an email
        waitForText("css=html body div.container-narrow table#table_dea_messages.table.table-hover.table-bordered tbody tr#msg_1 td:nth-of-type(2)",
                    "admin@${defaultDomainPart}");
        //
        // ~~~ OpenMessage ~~~
        //
        startAction("OpenMessage");
        click("css=html body div.container-narrow table#table_dea_messages.table.table-hover.table-bordered tbody tr#msg_1");
        // There is sometimes a loading screen after opening the message.
        waitForText("css=html body div#modalMessage.modal.hide.fade.in div.modal-body", "Hi*");
        // store e-mail message
        final String messageBody = getText("css=#modalMessage .modal-body");
        final Matcher m = Pattern.compile("http:\\S+").matcher(messageBody);
        Assert.assertTrue("Did not find any properly formatted URL in message body", m.find());

        final String activationLink = m.group();

        // Open activation link
        startAction("OpenActivationLink");
        open(activationLink);

    }
}