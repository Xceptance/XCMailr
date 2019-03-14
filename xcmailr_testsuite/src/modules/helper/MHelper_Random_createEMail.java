package modules.helper;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Create random email.</p>
 */
public class MHelper_Random_createEMail
{

    /**
     * <p>Create random email.</p>
     *
     * @param email_varDynamic
     */
    public static void execute(String email_varDynamic)
    {
        // resolve any placeholder in the parameters
        email_varDynamic = resolve(email_varDynamic);
        store("${RANDOM.String(16)}@varmail.net", email_varDynamic);

    }
}