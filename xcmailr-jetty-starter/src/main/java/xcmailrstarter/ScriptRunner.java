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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.h2.tools.RunScript;
import org.mortbay.log.Log;

/**
 * Executes an SQL script specified as system property.
 */
public class ScriptRunner
{
    public ScriptRunner(StarterConf config)
    {
        try
        {
            Log.info("Load database driver: " + config.XCM_DB_DRIVER);
            Class.forName(config.XCM_DB_DRIVER);
        }
        catch (Exception e)
        {
            Log.warn("Error while loading driver: " + config.XCM_DB_DRIVER, e);
            e.printStackTrace();
            System.exit(0);
        }

        try
        {
            String customSqlScript = System.getProperty("xcmailr.xcmstart.script");
            if (customSqlScript != null)
            {
                Log.info("Run sql script: " + customSqlScript);
                runScript(config, customSqlScript);
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

    private void runScript(StarterConf config, String... filenames) throws FileNotFoundException, SQLException
    {
        if (filenames == null)
        {
            throw new IllegalArgumentException("Parameter filenames mustn't be null");
        }

        if(filenames.length == 0)
        {
            return;
        }

        Log.info(MessageFormat.format("Open database: ''{0}'' as user  ''{1}''", config.XCM_DB_URL,
                                      config.XCM_DB_USER));
        Connection connection = DriverManager.getConnection(config.XCM_DB_URL, config.XCM_DB_USER, config.XCM_DB_PASS);

        for (String filename : filenames)
        {
            Log.info("Execute sql script from file: " + filename);
            RunScript.execute(connection, new BufferedReader(new FileReader(filename)));
        }
        Log.info("Execution finished. Close database");
        connection.close();
    }
}
