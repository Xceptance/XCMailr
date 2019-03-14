package modules.pages.account;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Enter email.</p>
 */
public class MAccount_Password_enterEmail
{

    /**
     * <p>Enter email.</p>
     *
     * @param email
     */
    public static void execute(String email)
    {
        // resolve any placeholder in the parameters
        email = resolve(email);
        // enter email
        type("css=#inputForgotPasswordMail", email);

    }
}