package tests.mail;
import org.junit.Test;
import com.xceptance.xlt.api.engine.scripting.AbstractWebDriverScriptTestCase;

import modules.global.headernav.MGlobal_Header_clickMyEmailAdresses;
import modules.global.headernav.MGlobal_Header_clickSignIn;
import modules.global.headernav.MGlobal_Header_clickSignOut;
import modules.helper.MHelper_Random_addRandomness;
import modules.helper.external.FHelper_getEmailAddressFromExternal;
import modules.helper.global.FHelper_Register_createAccount;
import modules.helper.global.FHelper_Session_flowToStartSession;
import modules.pages.account.login.MAccount_Login_enterClickUserLoginData;
import modules.pages.mail.FMail_Menu_addEmail;
import modules.pages.mail.MMail_Menu_setStatusActive;
import modules.pages.mail.MMail_Menu_setStatusInactive;
import modules.pages.mail.MMail_Row_clickSelectEMail;
import modules.pages.mail.VMail_Row_validateEmailStatus;

/**
 * <p>add and remove e-mail adress</p>
 * <h1 id="setup-and-preparation">Setup and preparation</h1>
 * <ul>
 * <li>Start session, open homepage and delete all visible cookies</li>
 * <li>create user</li>
 * </ul>
 * <h1 id="scope-of-test">Scope of test</h1>
 */
public class TMail_enableDisableMultipleEMail extends AbstractWebDriverScriptTestCase
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

        // 2x Domain local Part
        MHelper_Random_addRandomness.execute("${defaultdomainLocalPart}", "domainLocalPart01_varDynamic");

        MHelper_Random_addRandomness.execute("${defaultdomainLocalPart}", "domainLocalPart02_varDynamic");

        MHelper_Random_addRandomness.execute("${defaultdomainLocalPart}", "domainLocalPart03_varDynamic");

        // # Setup
        // 
        // - Start session
        FHelper_Session_flowToStartSession.execute();

        // create test account
        FHelper_Register_createAccount.execute("${email_varDynamic}", "password_varDynamic");

        FHelper_Session_flowToStartSession.execute();

        MGlobal_Header_clickSignIn.execute();

        MAccount_Login_enterClickUserLoginData.execute("${email_varDynamic}", "${password_varDynamic}");

        MGlobal_Header_clickMyEmailAdresses.execute();

        // add e-mail
        FMail_Menu_addEmail.execute("${domainLocalPart01_varDynamic}", "${defaultDomainPart}", "");

        FMail_Menu_addEmail.execute("${domainLocalPart02_varDynamic}", "${defaultDomainPart}", "");

        // -----------------------------------------------
        // # Scope
        // -----------------------------------------------
        // select element as inactive

        MMail_Row_clickSelectEMail.execute("2");

        MMail_Menu_setStatusInactive.execute();

        VMail_Row_validateEmailStatus.execute("2", "Expired");

        // select element as active
        MMail_Row_clickSelectEMail.execute("2");

        MMail_Menu_setStatusActive.execute();

        VMail_Row_validateEmailStatus.execute("2", "Active");

        // validate element
        VMail_Row_validateEmailStatus.execute("3", "Active");

        // validate last element
        MGlobal_Header_clickSignOut.execute();


    }

}