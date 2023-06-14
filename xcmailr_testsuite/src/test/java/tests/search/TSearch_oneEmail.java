package tests.search;
import org.junit.Test;
import com.xceptance.xlt.api.engine.scripting.AbstractWebDriverScriptTestCase;

import modules.global.headernav.MGlobal_Header_clickMyEmailAdresses;
import modules.global.headernav.MGlobal_Header_clickSignIn;
import modules.helper.MHelper_Random_addRandomness;
import modules.helper.external.FHelper_getEmailAddressFromExternal;
import modules.helper.global.FHelper_Register_createAccount;
import modules.helper.global.FHelper_Session_flowToStartSession;
import modules.pages.account.login.MAccount_Login_enterClickUserLoginData;
import modules.pages.mail.FMail_Menu_addEmail;
import modules.pages.mail.VMail_Table_validateLastElement;
import modules.pages.search.MSearch_enterTextAndSubmit;

/**
 * <p>Test search for one email.</p>
 * <h1 id="1-setup-and-preparation">1. Setup and preparation</h1>
 * <ul>
 * <li>Start session, open homepage and delete all visible cookies</li>
 * </ul>
 * <h1 id="2-scope-of-test">2. Scope of test</h1>
 * <ul>
 * <li>Enter exist email </li>
 * <li>Validate  results page</li>
 * </ul>
 */
public class TSearch_oneEmail extends AbstractWebDriverScriptTestCase
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

        // add e-mail
        FMail_Menu_addEmail.execute("${domainLocalPart01_varDynamic}", "${defaultDomainPart}", "");

        FMail_Menu_addEmail.execute("${domainLocalPart02_varDynamic}", "${defaultDomainPart}", "");

        FMail_Menu_addEmail.execute("${domainLocalPart03_varDynamic}", "${defaultDomainPart}", "");

        // -----------------------------------------------
        // # Scope
        // -----------------------------------------------
        // ## Result
        // 
        // - Search false string
        // - Validate not found page with suggestion

        startAction("SearchMailboxes");
        MSearch_enterTextAndSubmit.execute("${domainLocalPart01_varDynamic}@${defaultDomainPart}");

        // validate
        VMail_Table_validateLastElement.execute("${domainLocalPart01_varDynamic}@${defaultDomainPart}");


    }

}