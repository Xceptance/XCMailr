/**  
 *  Copyright 2013 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */

package xcmailrstarter;

import java.io.File;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;
import org.mortbay.xml.XmlConfiguration;

/**
 * Drops and creates the SQL-Tables and runs the Jetty-Server
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class XCMStarter {

	/**
	 * @param args
	 * @throws Exception
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
			XmlConfiguration configuration = new XmlConfiguration(jettyXmlFile
					.toURI().toURL());
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
			Log.warn("Exiting application due to unrecoverable error.");
			System.exit(1);
		}
	}

	/**
	 * Static main method to invoke Jetty starter class and run the SQL-Scripts.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		new ScriptRunner(args);

		XCMStart(args);

	}
}
