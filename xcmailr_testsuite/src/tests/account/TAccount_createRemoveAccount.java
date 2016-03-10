/*
 * NOTE: This file is generated. Do not edit! Your changes will be lost.
 */
package tests.account;
import com.xceptance.xlt.api.engine.scripting.AbstractScriptTestCase;
import com.xceptance.xlt.api.engine.scripting.ScriptName;


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
@ScriptName
("tests.account.TAccount_createRemoveAccount")
public class TAccount_createRemoveAccount extends AbstractScriptTestCase
{
}