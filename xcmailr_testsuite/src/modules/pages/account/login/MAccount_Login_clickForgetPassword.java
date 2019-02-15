package modules.pages.account.login;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click the forget password link.</p>
 */
public class MAccount_Login_clickForgetPassword
{

    /**
     * <p>Click the forget password link.</p>
     *
     */
    public static void execute()
    {
        //
        // ~~~ GoToForgetPassword ~~~
        //
        startAction("GoToForgetPassword");
        clickAndWait("css=#formLogin .forgotPassword");

    }
}