package modules.global.headernav;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Navigates to the &quot;Sign Out&quot; page</p>
 */
public class MGlobal_Header_clickSignOut
{

    /**
     * <p>Navigates to the &quot;Sign Out&quot; page</p>
     *
     */
    public static void execute()
    {
        // Navigate to the "Sign Out" page
        //
        // ~~~ GoToSignOut ~~~
        //
        startAction("Logout");
        // li:nth-of-type(8) a
        clickAndWait("css=.logoutAccount");

    }
}