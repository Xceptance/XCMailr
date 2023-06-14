package modules.pages.account;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click the forget password button.</p>
 */
public class MAccount_Password_clickForgetPassword
{

    /**
     * <p>Click the forget password button.</p>
     *
     */
    public static void execute()
    {
        //
        // ~~~ GoToForgetPassword ~~~
        //
        startAction("ConfirmForgotPassword");
        clickAndWait("css=#btnForgotPasswordSubmit");

    }
}