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
import org.mortbay.xml.XmlConfiguration;
import xcmailrstarter.ScriptRunner;

/**
 * Drops and creates the SQL-Tables and runs the Jetty-Server
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class XCMStarter
{

    /**
     * @param args
     */
    public static void XCMStart(StarterConf config)
    {
        try
        {
            // create server
            Server server = new Server();

            // create Jetty XML file
            File jettyConfDirectory = new File(config.XCM_HOME, "conf");
            File jettyXmlFile = new File(jettyConfDirectory, "jetty.xml");

            // load configuration
            XmlConfiguration configuration = new XmlConfiguration(jettyXmlFile.toURI().toURL());
            configuration.configure(server);

            // create WAR path
            StringBuilder warPath = new StringBuilder();
            warPath.append(config.XCM_HOME);
            warPath.append("/");
            warPath.append("webapps");
            warPath.append("/");

            warPath.append("xcmailr-webapp.war");

            // add web application
            WebAppContext webapp = new WebAppContext();
            webapp.setContextPath("/"+config.XCM_CONTEXT_PATH); //TODO add +config.XCM_CONTEXT_PATH here after update to ninja 1.4.4
            webapp.setWar(warPath.toString());

            // create connector
            Connector connector = new SelectChannelConnector();
            connector.setPort(config.XCM_PORT);
            connector.setHost("localhost");

            // configure the server
            server.addConnector(connector);
            server.setHandler(webapp);
            server.setStopAtShutdown(true);

            server.start();
            server.join();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Static main method to invoke Jetty starter class and run the SQL-Scripts.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception
    {
        StarterConf config = new StarterConf(args);
        new ScriptRunner(config);
        XCMStart(config);

    }
}
