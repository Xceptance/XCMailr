package xcmailrstarter;

import java.io.File;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;
import org.mortbay.xml.XmlConfiguration;

public class XCMStarter {

	/**
	 * JettyStarter no-arg constructor which start an embedded Jetty web server
	 * instance.
	 * 
	 * <p>
	 * The class must be invoked with these system properties defined:
	 * 
	 * <ul>
	 * <li><code>pineapple.jettystarter.home</code> defines home directory for
	 * Jetty. The type is <code>java.lang.String</code>.</li>
	 * <li><code>pineapple.jettystarter.host</code> defines host name which the
	 * Jetty server should listen on. The type is <code>java.lang.String</code>.
	 * </li>
	 * <li><code>pineapple.jettystarter.port</code> defines port number on which
	 * the Jetty server should listen on. The type is <code>integer</code>.</li>
	 * </ul>
	 * </p>
	 * 
	 * @param args
	 *            command line arguments. Not used by the class.
	 * 
	 * @throws Exception
	 *             If starting the Jetty server fails.
	 */
	public static void XCMStart(String[] args) throws Exception {

		try {
			// get server home system property
			String serverHome = System.getProperty("xcmailr.xcmstarter.home");

			// get server host system property
			String serverHost = System.getProperty("xcmailr.xcmstarter.host");

			// get server host system property
			String serverPort = System.getProperty("xcmailr.xcmstarter.port");
			System.out
					.println(serverHome + " " + serverHost + " " + serverPort);
			// create server
			Server server = new Server();

			// create Jetty XML file
			File jettyConfDirectory = new File(serverHome, "conf");
			File jettyXmlFile = new File(jettyConfDirectory, "jetty.xml");

			// load configuration
			XmlConfiguration configuration = new XmlConfiguration(
					jettyXmlFile.toURL());
			configuration.configure(server);

			// create WAR path
			StringBuilder warPath = new StringBuilder();
			warPath.append(serverHome);
			warPath.append("/");
			warPath.append("webapps");
			warPath.append("/");
			warPath.append("xcmailr-webapp-1.0.war");

			// add web application
			WebAppContext webapp = new WebAppContext();
			webapp.setContextPath("/");
			webapp.setWar(warPath.toString());

			// create connector
			Connector connector = new SelectChannelConnector();
			connector.setPort(Integer.parseInt(serverPort));
			connector.setHost(serverHost);

			// configure the server
			server.addConnector(connector);
			server.setHandler(webapp);
			server.setStopAtShutdown(true);

			server.start();
			server.join();

		} catch (Exception e) {
			e.printStackTrace();
			// Log.warn(message);
			Log.warn("Exiting application due to unrecoverable error.");

			System.exit(1);
		}
	}

	/**
	 * Static main method to invoke Jetty starter class.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (!(System.getProperty("xcmailr.xcmstarter.firstrun") == null)) {
			new ScriptRunner();
		}

		XCMStart(args);

	}
}
