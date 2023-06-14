package modules.pages.account.login;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Valdiate error message after login with false profile data.</p>
 */
public class VAccount_Login_validateErrorMsgNoMatch
{

    /**
     * <p>Valdiate error message after login with false profile data.</p>
     *
     */
    public static void execute()
    {
        waitForElementPresent("css=p.alert-danger");
        assertText("css=p.alert-danger", "Some values are wrong or missing.");

    }
}