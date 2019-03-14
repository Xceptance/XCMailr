package modules.helper.global;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Flow to start session.</p>
 * <ul>
 * <li>Start with open browser tab</li>
 * <li>Open homepage</li>
 * <li>Delete cookies</li>
 * <li>Open homepage</li>
 * <li>End on homepage</li>
 * </ul>
 */
public class FHelper_Session_flowToStartSession
{

    /**
     * <p>Flow to start session.</p>
     * <ul>
     * <li>Start with open browser tab</li>
     * <li>Open homepage</li>
     * <li>Delete cookies</li>
     * <li>Open homepage</li>
     * <li>End on homepage</li>
     * </ul>
     *
     */
    public static void execute()
    {
        //
        // ~~~ Homepage ~~~
        //
        startAction("Homepage");
        // # Setup - Start Session
        open("${storefront_url}");
        // Delete cookies in the current context
        deleteAllVisibleCookies();
        // reopen page to get new cookies
        //
        // ~~~ HomepageAgain ~~~
        //
        startAction("HomepageAgain");
        open("${storefront_url}?lang=${defaultRegisterLanguage}");

    }
}