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

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.h2.tools.RunScript;

/**
 * Executes an SQL script specified as system property.
 */
public class ScriptRunner
{
    private final static Logger LOG = Log.getLog();

    public ScriptRunner(StarterConf config)
    {
        try
        {
            LOG.info("Load database driver: " + config.XCM_DB_DRIVER);
            Class.forName(config.XCM_DB_DRIVER, false, Thread.currentThread().getContextClassLoader());
        }
        catch (Exception e)
        {
            LOG.warn("Error while loading driver: " + config.XCM_DB_DRIVER, e);
            e.printStackTrace();
            System.exit(0);
        }

        try
        {
            String customSqlScript = System.getProperty("xcmailr.xcmstart.script");
            if (customSqlScript != null)
            {
                LOG.info("Run sql script: " + customSqlScript);
                runScript(config, customSqlScript);
                System.exit(0);
            }
        }
        catch (Exception e)
        {
            LOG.warn("Error while executing: " + config.XCM_DB_DRIVER, e);
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

        if (filenames.length == 0)
        {
            return;
        }

        LOG.info(MessageFormat.format("Open database: ''{0}'' as user  ''{1}''", config.XCM_DB_URL,
                                      config.XCM_DB_USER));
        Connection connection = DriverManager.getConnection(config.XCM_DB_URL, config.XCM_DB_USER, config.XCM_DB_PASS);

        try
        {
            for (String filename : filenames)
            {
                LOG.info("Execute sql script from file: " + filename);
                RunScript.execute(connection, new BufferedReader(new FileReader(filename)));
            }
            LOG.info("Execution finished. Close database");
            connection.close();
        }
        finally
        {
            connection.close();
        }
    }
}
