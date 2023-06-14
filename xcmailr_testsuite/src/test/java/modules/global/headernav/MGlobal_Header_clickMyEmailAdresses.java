package modules.global.headernav;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Navigates to the &quot;My EmailAdresses&quot; page</p>
 */
public class MGlobal_Header_clickMyEmailAdresses
{

    /**
     * <p>Navigates to the &quot;My EmailAdresses&quot; page</p>
     *
     */
    public static void execute()
    {
        // Navigate to the "Sign Out" page
        //
        // ~~~ GoToMailOverview ~~~
        //
        startAction("GoToMailOverview");
        // li:nth-of-type(8) a
        clickAndWait("css=.mailOverview");

    }
}