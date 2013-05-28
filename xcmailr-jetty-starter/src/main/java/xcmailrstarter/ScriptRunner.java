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

import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.h2.tools.RunScript;
import org.mortbay.log.Log;

/**
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class ScriptRunner {

	public ScriptRunner(String[] args) {
		// get the server file-location
		String serverHome = System.getProperty("xcmailr.xcmstarter.home");
		// get the config file-location
		String confFile = System.getProperty("ninja.external.configuration");
		PropertiesConfiguration c = new PropertiesConfiguration();
		c.setEncoding("utf-8");
		c.setDelimiterParsingDisabled(true);
		String confPath = serverHome + "/" + confFile;
		try {

			c.load(confPath);

		} catch (ConfigurationException e) {

			Log.info("Could not load file " + confPath
					+ " (not a bad thing necessarily, but I am returing null)");
			System.exit(0);
		}

		Configuration conf = (Configuration) c;
		String dbUrl = conf.getString("ebean.datasource.databaseUrl");
		String dbUser = conf.getString("ebean.datasource.username");
		String dbPass = conf.getString("ebean.datasource.password");
		String dbDriver = conf.getString("ebean.datasource.databaseDriver");

		// Handle the connection
		Connection conn;
		try {

			Class.forName(dbDriver);
			Log.info("Driver Loaded.");

			conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
			Log.info("Got Connection.");
            if (!(System.getProperty("xcmailr.xcmstart.droptables") == null)) {
                RunScript
                        .execute(conn, new FileReader("conf/default-drop.sql"));
            }
			RunScript.execute(conn, new FileReader("conf/default-create.sql"));

			conn.close();
			Log.info("Executed Scripts.");
		} catch (Exception e) {
			System.err
					.println("Got an exception! Connection error or Script execution failed.");
			e.printStackTrace();
			System.exit(0);
		}

	}
}
