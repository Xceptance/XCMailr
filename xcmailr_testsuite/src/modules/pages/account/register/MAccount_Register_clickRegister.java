package modules.pages.account.register;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Click create account button.</p>
 */
public class MAccount_Register_clickRegister
{

    /**
     * <p>Click create account button.</p>
     *
     */
    public static void execute()
    {
        //
        // ~~~ confirmRegistration ~~~
        //
        startAction("ConfirmRegistration");
        clickAndWait("css=.btn-submit");

    }
}