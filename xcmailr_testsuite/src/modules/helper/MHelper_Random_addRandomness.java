package modules.helper;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Create a random string.</p>
 */
public class MHelper_Random_addRandomness
{

    /**
     * <p>Create a random string.</p>
     *
     * @param inputString
     * @param returnValue_varDynamic
     */
    public static void execute(String inputString, String returnValue_varDynamic)
    {
        // resolve any placeholder in the parameters
        inputString = resolve(inputString);
        returnValue_varDynamic = resolve(returnValue_varDynamic);
        // Just a random value for future use.
        store("${RANDOM.Number(1,5)}", "rLength");
        // Add some random characters to the input string.
        store(inputString + "${RANDOM.String(${rLength})}", returnValue_varDynamic);

    }
}