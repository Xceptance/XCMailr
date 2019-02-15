package modules.pages.account;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Enter password.</p>
 */
public class MAccount_Password_enterclickForgetPassword
{

    /**
     * <p>Enter password.</p>
     *
     * @param password
     */
    public static void execute(String password)
    {
        // resolve any placeholder in the parameters
        password = resolve(password);

        startAction("ResetPassword");
        // enter password
        type("css=#inputResetPassword", password);
        type("css=#inputResetPasswordRepetition", password);
        click("css=#btnResetPasswordSubmit");

    }
}