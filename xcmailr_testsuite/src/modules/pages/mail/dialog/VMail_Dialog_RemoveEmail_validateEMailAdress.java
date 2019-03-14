package modules.pages.mail.dialog;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validate selected email.</p>
 */
public class VMail_Dialog_RemoveEmail_validateEMailAdress
{

    /**
     * <p>Validate selected email.</p>
     *
     * @param email
     */
    public static void execute(String email)
    {
        // resolve any placeholder in the parameters
        email = resolve(email);
        assertText("css=.email-adress", email);

    }
}