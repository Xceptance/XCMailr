package modules.pages.account;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validates the display of the error message</p>
 */
public class VAccount_validatePage
{

    /**
     * <p>Validates the display of the error message</p>
     *
     */
    public static void execute()
    {
        // delete button
        assertElementPresent("css=#editUserForm .remove-user");
        // input field
        assertElementPresent("css=#password");
        assertElementPresent("css=#firstName");
        assertElementPresent("css=#surName");
        assertElementPresent("css=#mail");
        assertElementPresent("css=#passwordNew1");
        assertElementPresent("css=#passwordNew2");
        // select (language)
        assertElementPresent("css=#language");
        // buttons (down)
        assertElementPresent("css=#editUserReset");
        assertElementPresent("css=#editUserSubmit");

    }
}