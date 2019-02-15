package modules.pages.search;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Enter search string.</p>
 */
public class MSearch_enterTextAndSubmit
{

    /**
     * <p>Enter search string.</p>
     *
     * @param searchText
     */
    public static void execute(String searchText)
    {
        // resolve any placeholder in the parameters
        searchText = resolve(searchText);
        type("id=search-field", searchText);
        waitForText("id=search-field", searchText);
        waitForVisible("css=.info");

    }
}