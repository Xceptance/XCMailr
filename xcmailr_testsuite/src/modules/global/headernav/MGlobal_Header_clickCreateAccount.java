package modules.global.headernav;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Navigates to the &quot;Create an Account&quot; page</p>
 */
public class MGlobal_Header_clickCreateAccount
{

    /**
     * <p>Navigates to the &quot;Create an Account&quot; page</p>
     *
     */
    public static void execute()
    {
        // Navigate to the "Create an Account" page
        //
        // ~~~ GoToCreateAccount ~~~
        //
        startAction("GoToCreateAccount");
        clickAndWait("css=.createAccount");

    }
}