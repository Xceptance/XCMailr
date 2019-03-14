package modules.pages.account.dialog;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Enter password.</p>
 */
public class MAccount_Dialog_enterPassword
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
        // enter password
        // target/id  "password" exist more then one ->  "css=.controls #password"
        type("css=#deleteUserModal #password", password);
        waitForText("css=#deleteUserModal #password", password);
    }
}