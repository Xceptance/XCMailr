package modules.pages.account;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Enter usere data.</p>
 */
public class MAccount_enterData
{

    /**
     * <p>Enter usere data.</p>
     *
     * @param firstName
     * @param lastName
     * @param language
     * @param oldPassword
     */
    public static void execute(String firstName, String lastName, String language, String oldPassword)
    {
        // resolve any placeholder in the parameters
        firstName = resolve(firstName);
        lastName = resolve(lastName);
        language = resolve(language);
        oldPassword = resolve(oldPassword);
        type("css=#password", oldPassword);
        type("css=#firstName", firstName);
        type("css=#surName", lastName);
        select("css=#language", "value=" + language);

    }
}