package tests.account.login;
import org.junit.Test;
import com.xceptance.xlt.api.engine.scripting.AbstractWebDriverScriptTestCase;

import modules.global.headernav.MGlobal_Header_clickSignIn;
import modules.global.headernav.VGlobal_validatePageTitle;
import modules.helper.MHelper_Random_createEMail;
import modules.helper.MHelper_Random_createPassword;
import modules.helper.global.FHelper_Session_flowToStartSession;
import modules.pages.account.login.MAccount_Login_enterClickUserLoginData;
import modules.pages.account.login.VAccount_Login_validateErrorMsgNoMatch;

/**
 * <p>Test login with  invalid username and invalid password</p>
 * <h1 id="setup-and-preparation">Setup and preparation</h1>
 * <ul>
 * <li>Start session, open homepage and delete all visible cookies</li>
 * <li>Click login link in the header and go to the login page</li>
 * </ul>
 * <h1 id="scope">Scope</h1>
 * <ul>
 * <li>Try to login with a invalid username and invalid password</li>
 * <li>Validate page title </li>
 * <li>Validate present error message element</li>
 * </ul>
 */
public class TAccount_Login_invalidUsernameInvalidPassword extends AbstractWebDriverScriptTestCase
{

    /**
     * Executes the test.
     *
     * @throws Throwable if anything went wrong
     */
    @Test
    public void test() throws Throwable
    {
        // -----------------------------------------------
        // # Setup
        // 
        // - Start session
        // -----------------------------------------------
        MHelper_Random_createEMail.execute("email_varDynamic");

        MHelper_Random_createPassword.execute("password_varDynamic");

        FHelper_Session_flowToStartSession.execute();

        // -----------------------------------------------
        // # Scope
        // enter invalid username and invalid password
        // -----------------------------------------------
        MGlobal_Header_clickSignIn.execute();

        // validate - Layout
        VGlobal_validatePageTitle.execute("${title_login}");

        MAccount_Login_enterClickUserLoginData.execute("${email_varDynamic}", "${password_varDynamic}");

        // validate - error msg. box
        VAccount_Login_validateErrorMsgNoMatch.execute();


    }

}