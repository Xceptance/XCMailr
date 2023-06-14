package modules.helper.global;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Create random number from &quot;runtime_min&quot; to &quot;runtime_max&quot;.</p>
 */
public class MHelper_Random_createRuntime
{

    /**
     * <p>Create random number from &quot;runtime_min&quot; to &quot;runtime_max&quot;.</p>
     *
     * @param runtime_min
     * @param runtime_max
     * @param runtime_varName
     */
    public static void execute(String runtime_min, String runtime_max, String runtime_varName)
    {
        // resolve any placeholder in the parameters
        runtime_min = resolve(runtime_min);
        runtime_max = resolve(runtime_max);
        runtime_varName = resolve(runtime_varName);
        store("${RANDOM.Number(" + runtime_min + "," + runtime_max + ")}", runtime_varName);

    }
}