package modules.helper.global;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Open the selected url.</p>
 */
public class FHelper_Url_openPage
{

    /**
     * <p>Open the selected url.</p>
     *
     * @param pageurl
     */
    public static void execute(String pageurl)
    {
        // resolve any placeholder in the parameters
        pageurl = resolve(pageurl);
        open(pageurl);

    }
}