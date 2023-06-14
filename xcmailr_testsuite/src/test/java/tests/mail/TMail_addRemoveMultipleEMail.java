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
import modules.pages.mail.FMail_Menu_removeLastTwoEMailAdresses;
import modules.pages.mail.FMail_Row_removeEmail;
import modules.pages.mail.VMail_Table_validateLastElement;
import modules.pages.mail.VMail_Table_validateRowCounter;
import modules.pages.search.VSearch_Results_validateNoEntriesFound;

/**
 * <p>add and remove e-mail adress</p>
 * <h1 id="setup-and-preparation">Setup and preparation</h1>
 * <ul>
 * <li>Start session, open homepage and delete all visible cookies</li>
 * <li>create user</li>
 * </ul>
 * <h1 id="scope-of-test">Scope of test</h1>
 */
public class TMail_addRemoveMultipleEMail extends AbstractWebDriverScriptTestCase
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

        // 3x Domain local Part
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

        // -----------------------------------------------
        // # Scope
        // -----------------------------------------------
        // add e-mail
        FMail_Menu_addEmail.execute("${domainLocalPart01_varDynamic}", "${defaultDomainPart}", "");

        FMail_Menu_addEmail.execute("${domainLocalPart02_varDynamic}", "${defaultDomainPart}", "");

        FMail_Menu_addEmail.execute("${domainLocalPart03_varDynamic}", "${defaultDomainPart}", "");

        // validate
        VMail_Table_validateRowCounter.execute("4");

        VMail_Table_validateLastElement.execute("${domainLocalPart03_varDynamic}@${defaultDomainPart}");

        // remove 2. e-mail
        FMail_Row_removeEmail.execute("3");

        // validate last element
        VMail_Table_validateLastElement.execute("${domainLocalPart03_varDynamic}@${defaultDomainPart}");

        VMail_Table_validateRowCounter.execute("3");

        // remove all emails
        FMail_Menu_removeLastTwoEMailAdresses.execute();

        // validate empty status
        VSearch_Results_validateNoEntriesFound.execute();

        MGlobal_Header_clickSignOut.execute();


    }

}