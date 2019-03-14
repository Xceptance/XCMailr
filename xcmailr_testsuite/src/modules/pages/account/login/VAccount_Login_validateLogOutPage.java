package modules.pages.account.login;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validate if user logout.</p>
 */
public class VAccount_Login_validateLogOutPage
{

    /**
     * <p>Validate if user logout.</p>
     *
     */
    public static void execute()
    {
        assertElementPresent("css=.alert-success");
        assertText("css=.alert-success", "You are now logged out.");

    }
}