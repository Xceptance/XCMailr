package modules.pages.account.login;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validate elements present.</p>
 */
public class VAccount_Login_validatePage
{

    /**
     * <p>Validate elements present.</p>
     *
     */
    public static void execute()
    {
        // input
        assertElementPresent("css=#inputLoginMail");
        assertElementPresent("css=#inputLoginPassword");
        // button
        assertElementPresent("css=#btnLoginReset");
        assertElementPresent("css=#btnLoginSubmit");
        // links
        assertElementPresent("css=.createAccount");
        assertElementPresent("css=.forgotPassword");
        // error message
        assertNotElementPresent("css=#loginform div.card-content div.formInput p.inputError");

    }
}