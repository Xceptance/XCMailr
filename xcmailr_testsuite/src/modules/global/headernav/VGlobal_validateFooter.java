package modules.global.headernav;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validate footer elements</p>
 */
public class VGlobal_validateFooter
{

    /**
     * <p>Validate footer elements</p>
     *
     */
    public static void execute()
    {
        // validate footer
        assertElementPresent("css=footer");
        assertElementPresent("css=footer .XCMailr_year");

    }
}