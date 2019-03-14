package modules.helper.external;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Goes to 10 minute Mail and fetches a fresh temporary email address for testing.</p>
 */
public class FHelper_getEmailAddressFromExternal
{

    /**
     * <p>Goes to 10 minute Mail and fetches a fresh temporary email address for testing.</p>
     *
     * @param email_varName
     */
    public static void execute(String email_varName)
    {
        // resolve any placeholder in the parameters
        email_varName = resolve(email_varName);
        //
        // ~~~ OpenExternalMailer ~~~
        //
        startAction("OpenExternalMailer");
        open("${emailClientURL}");
        // Delete cookies to get a new email
        deleteAllVisibleCookies();
        //
        // ~~~ OpenExternalMailerAgain ~~~
        //
        startAction("OpenExternalMailerAgain");
        open("${emailClientURL}");
        // Get the email address from the page
        storeText("css=html body div.container-narrow div.jumbotron p.lead", email_varName);

    }
}