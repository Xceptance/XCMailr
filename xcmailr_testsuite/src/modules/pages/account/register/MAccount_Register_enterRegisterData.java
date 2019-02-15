package modules.pages.account.register;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Enter first and last name, email, password, language.</p>
 */
public class MAccount_Register_enterRegisterData
{

    /**
     * <p>Enter first and last name, email, password, language.</p>
     *
     * @param firstName
     * @param lastName
     * @param email
     * @param password
     * @param language
     */
    public static void execute(String firstName, String lastName, String email, String password, String language)
    {
        // resolve any placeholder in the parameters
        firstName = resolve(firstName);
        lastName = resolve(lastName);
        email = resolve(email);
        password = resolve(password);
        language = resolve(language);
        // first name
        type("css=#inputRegisterFirstName", firstName);
        // last name
        type("css=#inputRegisterSurName", lastName);
        // E-Mail
        type("css=#inputRegisterMail", email);
        // Password
        type("css=#inputRegisterPassword", password);
        type("css=#inputRegisterPasswordRepetition", password);
        // language
        select("css=#selectRegisterLanguage", "value=" + language);

    }
}