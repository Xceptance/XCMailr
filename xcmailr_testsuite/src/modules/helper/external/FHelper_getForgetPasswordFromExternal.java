package modules.helper.external;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;

/**
 * <p>Goes to 10 minute email and fetches the just arrived email to retrieve the activate url.</p>
 */
public class FHelper_getForgetPasswordFromExternal
{

    /**
     * <p>Goes to 10 minute email and fetches the just arrived email to retrieve the activate url.</p>
     *
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
        waitForText("css=#msg_2 td:nth-of-type(2)", "admin@${defaultDomainPart}");
        //
        // ~~~ OpenMessage ~~~
        //
        startAction("OpenMessage");
        click("css=#msg_2");
        // There is sometimes a loading screen after opening the message.
        waitForText("css=#modalMessage .modal-body", "Hi*");

        final Matcher m = Pattern.compile("http:\\S+").matcher(getText("css=#modalMessage .modal-body"));
        Assert.assertTrue("Did not find any properly formatted URL in message body", m.find());

        startAction("OpenForgotPasswordLink");
        open(m.group());

    }
}