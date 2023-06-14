package modules.pages.account.register;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validate elements present.</p>
 */
public class VAccount_Register_validatePage
{

    /**
     * <p>Validate elements present.</p>
     *
     */
    public static void execute()
    {
        assertElementPresent("css=#name");
        assertElementPresent("css=#contact");
        assertElementPresent("css=#street_number");
        assertElementPresent("css=#city");
        assertElementPresent("css=#zip");
        assertElementPresent("css=#country");
        assertElementPresent("css=#email");
        assertElementPresent("css=#password");
        assertElementPresent("css=#passwordCheck");
        assertElementPresent("css=#code");
        // captcha code as image
        assertElementPresent("css=#captcha");
        // links
        assertElementPresent("css=#submitFormValues");
        assertElementPresent("css=#back_link");
        // title
        assertText("css=#register h2", "Registration");

    }
}