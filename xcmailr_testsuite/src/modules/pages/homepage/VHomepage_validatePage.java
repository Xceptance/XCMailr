package modules.pages.homepage;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validate elements present.</p>
 */
public class VHomepage_validatePage
{

    /**
     * <p>Validate elements present.</p>
     *
     */
    public static void execute()
    {
        // content header title
        assertElementPresent("css=body .container .container-fluid div:nth-of-type(2) h1.legendary");
        // content content-info
        assertElementPresent("css=body .container .container-fluid div:nth-of-type(2) p:nth-of-type(1)");
        // content content- "available languages"
        assertText("css=body .container .container-fluid div:nth-of-type(2) p:nth-of-type(2)", "glob:Available Languages:");

    }
}