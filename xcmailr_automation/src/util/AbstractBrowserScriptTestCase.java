package util;

import org.junit.After;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.xceptance.xlt.api.engine.scripting.AbstractScriptTestCase;
import com.xceptance.xlt.api.util.XltLogger;
import com.xceptance.xlt.api.util.XltProperties;

public class AbstractBrowserScriptTestCase extends AbstractScriptTestCase 
{
	/**
	 * The browser driver
	 */
	private final WebDriver webDriver;

	/**
	 * Constructor.
	 */
	public AbstractBrowserScriptTestCase(int width, int height) 
	{
		// get the wanted drivers from the properties
		final String driverName = XltProperties.getInstance().getProperty("webdriver");
		
		if (driverName.equalsIgnoreCase("chrome"))
		{
			System.setProperty("webdriver.chrome.driver", XltProperties.getInstance().getProperty("webdriver.chrome.binary.location"));

			final DesiredCapabilities capabilities = DesiredCapabilities.chrome();

			webDriver = new ChromeDriver(capabilities);
			webDriver.manage().window().setSize(new Dimension(width, height));
		}
		else if (driverName.equalsIgnoreCase("ie"))
		{
			System.setProperty("webdriver.ie.driver", XltProperties.getInstance().getProperty("webdriver.ie.binary.location"));
			
			final DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();
			
			webDriver = new InternetExplorerDriver(capabilities);
			webDriver.manage().window().setSize(new Dimension(width, height));
		}
		else if ((driverName.equalsIgnoreCase("firefox")))
		{
			webDriver = new FirefoxDriver();
			webDriver.manage().window().setSize(new Dimension(width, height));
		}
		else
		{
			XltLogger.runTimeLogger.error("No webdriver could be determined. Not setting one, so this will now fail.");
			webDriver = null;
		}

		// enabled shutdown on quit
		if (webDriver != null && XltProperties.getInstance().getProperty("webdriver.shutdownAfterTest", true))
		{
			// clean up the driver when the JVM quits
			Runtime.getRuntime().addShutdownHook(new Thread()
			{
				@Override
				public void run()
				{
					webDriver.quit();
				}
			});
		}

		// sets the web driver to be used.
		setWebDriver(webDriver);
	}

	/**
	 * 
	 */
	public AbstractBrowserScriptTestCase()
	{
		this(XltProperties.getInstance().getProperty("webdriver.screensize.width", 1200), 
			 XltProperties.getInstance().getProperty("webdriver.screensize.height", 900));
	}
	
	@After
	public void stopWebDriver() 
	{
		// do we want to close it after the test?
		if (XltProperties.getInstance().getProperty("webdriver.shutdownAfterTest", true))
		{
			webDriver.quit();
		}
	}
}
