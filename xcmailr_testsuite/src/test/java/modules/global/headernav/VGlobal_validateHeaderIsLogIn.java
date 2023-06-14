package modules.global.headernav;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validate header - is user login</p>
 */
public class VGlobal_validateHeaderIsLogIn
{

    /**
     * <p>Validate header - is user login</p>
     *
     */
    public static void execute()
    {
        // # assert
        assertText("css=.editAccount", "Account");
        assertText("css=.logoutAccount", "Sign Out");

    }
}