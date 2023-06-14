package modules.global.headernav;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validate header elements</p>
 */
public class VGlobal_validateHeader
{

    /**
     * <p>Validate header elements</p>
     *
     */
    public static void execute()
    {
        // home link (left)
        assertElementPresent("css=.navbar");
        // Validate header navigation bar
        assertText("css=.loginAccount", "Sign In");
        assertText("css=.createAccount", "Create an Account");

    }
}