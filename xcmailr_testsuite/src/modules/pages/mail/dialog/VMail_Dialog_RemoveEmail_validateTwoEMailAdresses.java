package modules.pages.mail.dialog;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Valiadate the two emails.</p>
 */
public class VMail_Dialog_RemoveEmail_validateTwoEMailAdresses
{

    /**
     * <p>Valiadate the two emails.</p>
     *
     * @param email_01
     * @param email_02
     */
    public static void execute(String email_01, String email_02)
    {
        // resolve any placeholder in the parameters
        email_01 = resolve(email_01);
        email_02 = resolve(email_02);
        assertText("css=.ng-scope li:nth-of-type(1) .email-adress", email_01);
        assertText("css=.ng-scope li:nth-of-type(2) .email-adress", email_02);

    }
}