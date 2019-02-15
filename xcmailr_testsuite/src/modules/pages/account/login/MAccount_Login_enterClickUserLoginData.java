package modules.pages.account.login;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

/**
 * <p>Enter the User - Login Datas</p>
 */
public class MAccount_Login_enterClickUserLoginData
{

    /**
     * <p>Enter the User - Login Datas</p>
     *
     * @param username
     * @param password
     */
    public static void execute(String username, String password)
    {
        // resolve any placeholder in the parameters
        username = resolve(username);
        password = resolve(password);
        startAction("FillLoginForm");
        // enter email and password
        type("css=#inputLoginMail", username);
        waitForText("css=#inputLoginMail", username);
        type("css=#inputLoginPassword", password);
        //
        // ~~~ clickSignIn ~~~
        //
        startAction("Login");
        clickAndWait("css=#btnLoginSubmit");

    }
}