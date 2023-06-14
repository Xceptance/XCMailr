package modules.helper;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Create a random password.</p>
 */
public class MHelper_Random_createPassword
{

    /**
     * <p>Create a random password.</p>
     *
     * @param pwd_varDynamic
     */
    public static void execute(String pwd_varDynamic)
    {
        // resolve any placeholder in the parameters
        pwd_varDynamic = resolve(pwd_varDynamic);
        store("${RANDOM.String(16)}", pwd_varDynamic);

    }
}