package modules.global.headernav;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Navigates to the &quot;Edit Profile&quot; page</p>
 */
public class MGlobal_Header_clickEditProfile
{

    /**
     * <p>Navigates to the &quot;Edit Profile&quot; page</p>
     *
     */
    public static void execute()
    {
        // Navigate to the "My Account" page
        //
        // ~~~ GoToEditAccount ~~~
        //
        startAction("GoToEditAccount");
        // css=.navbar .container div:nth-of-type(2) ul:nth-of-type(2) li:nth-of-type(2) a
        clickAndWait("css=.editAccount");

    }
}