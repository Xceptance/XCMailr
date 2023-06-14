package modules.helper.global;
import static com.xceptance.xlt.api.engine.scripting.StaticScriptCommands.*;

import modules.global.headernav.MGlobal_Header_clickCreateAccount;
import modules.helper.MHelper_Random_addRandomness;
import modules.helper.MHelper_Random_createPassword;
import modules.helper.external.FHelper_openclickAuthLinkFromExternal;
import modules.pages.account.register.MAccount_Register_clickRegister;
import modules.pages.account.register.MAccount_Register_enterRegisterData;

/**
 * <p>Create acount.</p>
 */
public class FHelper_Register_createAccount
{

    /**
     * <p>Create acount.</p>
     *
     * @param email
     * @param userPassword_varDynamic
     */
    public static void execute(String email, String userPassword_varDynamic)
    {
        // resolve any placeholder in the parameters
        email = resolve(email);
        userPassword_varDynamic = resolve(userPassword_varDynamic);
        // first name
        MHelper_Random_addRandomness.execute("${defaultRegisterFirstName}", "firstName_varDynamic");

        // last name
        MHelper_Random_addRandomness.execute("${defaultRegisterLastName}", "lastName_varDynamic");

        // Password
        MHelper_Random_createPassword.execute(userPassword_varDynamic);

        MGlobal_Header_clickCreateAccount.execute();

        MAccount_Register_enterRegisterData.execute("${firstName_varDynamic}", "${lastName_varDynamic}", email, "${password_varDynamic}", "${defaultRegisterLanguage}");

        MAccount_Register_clickRegister.execute();

        FHelper_openclickAuthLinkFromExternal.execute();


    }
}