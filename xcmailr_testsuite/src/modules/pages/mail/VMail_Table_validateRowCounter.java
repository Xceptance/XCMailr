package modules.pages.mail;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Validate nominal value with row table counter.</p>
 */
public class VMail_Table_validateRowCounter
{

    /**
     * <p>Validate nominal value with row table counter.</p>
     *
     * @param nominalValue
     */
    public static void execute(String nominalValue)
    {
        // resolve any placeholder in the parameters
        nominalValue = resolve(nominalValue);
        // counterRow
        assertXpathCount("/html/body/div[2]/div/div/table/tbody/tr", nominalValue);

    }
}