package modules.global.headernav;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validates the page title</p>
 */
public class VGlobal_validatePageTitle
{

    /**
     * <p>Validates the page title</p>
     *
     * @param page_title
     */
    public static void execute(String page_title)
    {
        // resolve any placeholder in the parameters
        page_title = resolve(page_title);
        assertText("css=h1.legendary",page_title);
    }
}