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

import org.h2.tools.RunScript;
import org.mortbay.log.Log;

/**
 * Prepares the Database and checks whether all necessary tables exist
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class ScriptRunner
{

    public ScriptRunner(StarterConf config)
    {
        // Handle the connection
        Connection connection;
        try
        {
            Class.forName(config.XCM_DB_DRIVER);
            Log.info("Driver Loaded.");
        }
        catch (Exception e)
        {
            Log.warn("Error while loading driver: " + config.XCM_DB_DRIVER, e);
            e.printStackTrace();
            System.exit(0);
        }

        String fileName = "";
        try
        {
            if (System.getProperty("xcmailr.xcmstart.droptables") != null)
            {
                connection = DriverManager.getConnection(config.XCM_DB_URL, config.XCM_DB_USER, config.XCM_DB_PASS);
                Log.info("Got Connection.");

                // drop all XCMailr structures
                Log.info("Execute database structure drop script");
                fileName = "default-drop.sql";
                RunScript.execute(connection, new FileReader(fileName));

                // create XCMailr structures
                Log.info("Execute database structure creation script");
                fileName = "default-create.sql";
                RunScript.execute(connection, new FileReader(fileName));

                connection.close();
                Log.info("Finished executing DB initialization scripts. Remove parameter \"xcmailr.xcmstart.droptables\" then start XCMailr again.");
                System.exit(0);
            }

            if (System.getProperty("xcmailr.xcmstart.upgrade") != null)
            {
                connection = DriverManager.getConnection(config.XCM_DB_URL, config.XCM_DB_USER, config.XCM_DB_PASS);
                Log.info("Got Connection.");

                Log.info("Upgrade DB using upgrade_db.sql");
                fileName = "upgrade_db.sql";
                RunScript.execute(connection, new FileReader(fileName));

                connection.close();
                Log.info("Finished executing upgrade DB script. Remove parameter \"xcmailr.xcmstart.upgrade\" then start XCMailr again.");
                System.exit(0);
            }
        }
        catch (Exception e)
        {
            Log.warn("Error while executing: " + config.XCM_DB_DRIVER, e);
            e.printStackTrace();
            System.exit(0);
        }
    }
}
