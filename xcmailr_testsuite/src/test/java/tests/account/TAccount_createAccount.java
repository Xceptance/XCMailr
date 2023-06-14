package tests.account;
import org.junit.Test;
import com.xceptance.xlt.api.engine.scripting.AbstractWebDriverScriptTestCase;

import modules.global.headernav.MGlobal_Header_clickEditProfile;
import modules.global.headernav.MGlobal_Header_clickSignIn;
import modules.global.headernav.MGlobal_Header_clickSignOut;
import modules.global.headernav.VGlobal_validatePageTitle;
import modules.helper.external.FHelper_getEmailAddressFromExternal;
import modules.helper.global.FHelper_Register_createAccount;
import modules.helper.global.FHelper_Session_flowToStartSession;
import modules.pages.account.VAccount_validatePage;
import modules.pages.account.login.MAccount_Login_enterClickUserLoginData;
import modules.pages.account.login.VAccount_Login_validateLogOutPage;

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
public class TAccount_createAccount extends AbstractWebDriverScriptTestCase
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

        // # Setup
        // 
        // - Start session
        FHelper_Session_flowToStartSession.execute();

        // create test account
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
        FHelper_Register_createAccount.execute("${email_varDynamic}", "password_varDynamic");

        FHelper_Session_flowToStartSession.execute();

        MGlobal_Header_clickSignIn.execute();

        MAccount_Login_enterClickUserLoginData.execute("${email_varDynamic}", "${password_varDynamic}");

        //
        // ~~~ GoToEditProfile ~~~
        //
        MGlobal_Header_clickEditProfile.execute();

        VAccount_validatePage.execute();

        VGlobal_validatePageTitle.execute("${title_edit}");

        //
        // ~~~ Logout ~~~
        //
        MGlobal_Header_clickSignOut.execute();

        VAccount_Login_validateLogOutPage.execute();

        VGlobal_validatePageTitle.execute("${title_home}");


    }

}