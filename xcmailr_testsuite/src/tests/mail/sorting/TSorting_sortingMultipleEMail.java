package tests.mail.sorting;
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
import modules.pages.mail.MMail_Menu_setStatusInactive;
import modules.pages.mail.MMail_Row_clickSelectEMail;
import modules.pages.mail.VMail_Table_validateFirstElement;
import modules.pages.mail.VMail_Table_validateLastElement;
import modules.pages.mail.sorting.MMail_Sorting_clickEMail;
import modules.pages.mail.sorting.MMail_Sorting_clickStatus;

/**
 * <p>validate sorting</p>
 * <h1 id="setup-and-preparation">Setup and preparation</h1>
 * <ul>
 * <li>Start session, open homepage and delete all visible cookies</li>
 * <li>create user</li>
 * </ul>
 * <h1 id="scope-of-test">Scope of test</h1>
 */
public class TSorting_sortingMultipleEMail extends AbstractWebDriverScriptTestCase
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

        // 6x Domain local Part
        MHelper_Random_addRandomness.execute("${defaultdomainLocalPart}", "domainLocalPart01_varDynamic");

        MHelper_Random_addRandomness.execute("${defaultdomainLocalPart}", "domainLocalPart02_varDynamic");

        MHelper_Random_addRandomness.execute("${defaultdomainLocalPart}", "domainLocalPart03_varDynamic");

        MHelper_Random_addRandomness.execute("${defaultdomainLocalPart}", "domainLocalPart04_varDynamic");

        MHelper_Random_addRandomness.execute("${defaultdomainLocalPart}", "domainLocalPart05_varDynamic");

        MHelper_Random_addRandomness.execute("${defaultdomainLocalPart}", "domainLocalPart06_varDynamic");

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
        FMail_Menu_addEmail.execute("e-${domainLocalPart05_varDynamic}", "${defaultDomainPart}", "");

        FMail_Menu_addEmail.execute("a-${domainLocalPart01_varDynamic}", "${defaultDomainPart}", "");

        FMail_Menu_addEmail.execute("c-${domainLocalPart03_varDynamic}", "${defaultDomainPart}", "");

        FMail_Menu_addEmail.execute("b-${domainLocalPart02_varDynamic}", "${defaultDomainPart}", "");

        FMail_Menu_addEmail.execute("f-${domainLocalPart04_varDynamic}", "${defaultDomainPart}", "");

        FMail_Menu_addEmail.execute("d-${domainLocalPart06_varDynamic}", "${defaultDomainPart}", "");

        // -----------------------------------------------
        // # Scope
        // -----------------------------------------------
        // sorting - e-mail
        //
        // ~~~ changeSortingEMail ~~~
        //
        MMail_Sorting_clickEMail.execute();

        // validate
        VMail_Table_validateFirstElement.execute("f-${domainLocalPart04_varDynamic}@${defaultDomainPart}");

        VMail_Table_validateLastElement.execute("a-${domainLocalPart01_varDynamic}@${defaultDomainPart}");

        // sorting - status
        // set status of the second element to disable
        //
        // ~~~ checkBoxEmail ~~~
        //
        MMail_Row_clickSelectEMail.execute("3");

        MMail_Menu_setStatusInactive.execute();

        MMail_Sorting_clickStatus.execute();

        // validate
        VMail_Table_validateFirstElement.execute("e-${domainLocalPart05_varDynamic}@${defaultDomainPart}");

        VMail_Table_validateLastElement.execute("d-${domainLocalPart06_varDynamic}@${defaultDomainPart}");


    }

}