package tests.account;
import org.junit.Test;
import com.xceptance.xlt.api.engine.scripting.AbstractWebDriverScriptTestCase;

import modules.global.headernav.MGlobal_Header_clickSignIn;
import modules.helper.MHelper_Random_createPassword;
import modules.helper.external.FHelper_getEmailAddressFromExternal;
import modules.helper.external.FHelper_getForgetPasswordFromExternal;
import modules.helper.global.FHelper_Register_createAccount;
import modules.helper.global.FHelper_Session_flowToStartSession;
import modules.pages.account.MAccount_Password_clickForgetPassword;
import modules.pages.account.MAccount_Password_enterEmail;
import modules.pages.account.MAccount_Password_enterclickForgetPassword;
import modules.pages.account.login.MAccount_Login_clickForgetPassword;
import modules.pages.account.login.MAccount_Login_enterClickUserLoginData;
import modules.pages.account.login.VAccount_Login_validateIsLogin;

/**
 * <p>Test register profile with create account via link in header.</p>
 * <p>Note: this test case requires testmode on (../xltportal/conf/xltportal.properties -&gt; testMode=On) !!!</p>
 * <h1 id="setup-and-preparation">Setup and preparation</h1>
 * <ul>
 * <li>Start session, open homepage and delete all visible cookies</li>
 * </ul>
 * <h1 id="scope-of-test">Scope of test</h1>
 * <ul>
 * <li>Click on login link in the header and go to the create account page</li>
 * <li>Enter profile data and submit the form</li>
 * <li>Logout using the link in the header menu</li>
 * <li>Login again to validate login data and goto the account page</li>
 * <li>Validate personal profile data</li>
 * <li>Logout using the global link in the header</li>
 * </ul>
 */
public class TAccount_forgotPassword extends AbstractWebDriverScriptTestCase
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
        // - Generate email
        // - Generate password
        // - Start session
        // -----------------------------------------------
        // e-mail (extern)
        FHelper_getEmailAddressFromExternal.execute("email_varDynamic");

        // Password
        MHelper_Random_createPassword.execute("password_ForgetPassword_varDynamic");

        // # Setup
        // 
        // - Start session
        FHelper_Session_flowToStartSession.execute();

        // create test account
        FHelper_Register_createAccount.execute("${email_varDynamic}", "password_varDynamic");

        FHelper_Session_flowToStartSession.execute();

        // -----------------------------------------------
        // # Scope
        // -----------------------------------------------
        // Account
        // 
        // - Register account via header link
        // - Enter profile data
        // - Submit profile data
        // - Validate account page and nav
        // - Validate customer name
        // - Logout
        //

        MGlobal_Header_clickSignIn.execute();

        MAccount_Login_clickForgetPassword.execute();

        MAccount_Password_enterEmail.execute("${email_varDynamic}");

        MAccount_Password_clickForgetPassword.execute();

        FHelper_getForgetPasswordFromExternal.execute();

        MAccount_Password_enterclickForgetPassword.execute("${password_ForgetPassword_varDynamic}");

        FHelper_Session_flowToStartSession.execute();

        MGlobal_Header_clickSignIn.execute();

        MAccount_Login_enterClickUserLoginData.execute("${email_varDynamic}", "${password_ForgetPassword_varDynamic}");

        VAccount_Login_validateIsLogin.execute();


    }

}