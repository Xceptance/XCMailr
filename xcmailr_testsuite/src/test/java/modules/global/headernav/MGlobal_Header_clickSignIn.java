package modules.global.headernav;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Navigates to the &quot;Sign In&quot; page</p>
 */
public class MGlobal_Header_clickSignIn
{

    /**
     * <p>Navigates to the &quot;Sign In&quot; page</p>
     *
     */
    public static void execute()
    {
        // Navigate to the "Sign In" page
        //
        // ~~~ GoToSignIn ~~~
        //
        startAction("GoToSignIn");
        // css=html body div.navbar div.container div.navbar-collapse ul.nav li:nth-of-type(1)  a
        clickAndWait("css=.loginAccount");

    }
}