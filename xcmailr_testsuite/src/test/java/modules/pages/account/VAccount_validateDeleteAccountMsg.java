package modules.pages.account;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Valdiate error message after login with false profile data.</p>
 */
public class VAccount_validateDeleteAccountMsg
{

    /**
     * <p>Valdiate error message after login with false profile data.</p>
     *
     */
    public static void execute()
    {
        waitForElementPresent("css=p.alert-success");
        assertText("css=p.alert-success", " Your account has been deleted.");

    }
}