package tests.homepage;
import org.junit.Test;
import com.xceptance.xlt.api.engine.scripting.AbstractWebDriverScriptTestCase;

import modules.global.headernav.VGlobal_validateFooter;
import modules.global.headernav.VGlobal_validateHeader;
import modules.global.headernav.VGlobal_validatePageTitle;
import modules.helper.global.FHelper_Session_flowToStartSession;
import modules.pages.homepage.VHomepage_validatePage;

/**
 * <p>Test browse hompage with basic functions.</p>
 * <h1 id="setup-and-preparation">Setup and preparation</h1>
 * <ul>
 * <li>Start session, open homepage and delete all visible cookies</li>
 * </ul>
 * <h1 id="scope-of-test">Scope of test</h1>
 * <ul>
 * <li>Validate global header, navigation and footer</li>
 * <li>Validate hompage content</li>
 * </ul>
 */
public class THomepage_validateLayout extends AbstractWebDriverScriptTestCase
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
        FHelper_Session_flowToStartSession.execute();

        // -----------------------------------------------
        // # Scope
        // layout test
        // -----------------------------------------------
        VGlobal_validatePageTitle.execute("${title_home}");

        VGlobal_validateHeader.execute();

        VHomepage_validatePage.execute();

        VGlobal_validateFooter.execute();


    }

}