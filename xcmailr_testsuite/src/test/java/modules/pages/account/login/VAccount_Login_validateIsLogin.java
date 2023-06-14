package modules.pages.account.login;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validate if user login.</p>
 */
public class VAccount_Login_validateIsLogin
{

    /**
     * <p>Validate if user login.</p>
     *
     */
    public static void execute()
    {
        assertText("css=.editAccount", "Edit Profile");

    }
}