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
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class ScriptRunner
{

    public ScriptRunner()
    {
        Connection conn;
        try
        {

            Class.forName("org.h2.Driver");
            System.out.println("Driver Loaded.");
            String url = "jdbc:h2:~/xcmailrDB;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";

            conn = DriverManager.getConnection(url, "sa", "");
            Log.info("Got Connection.");
            
            RunScript.execute(conn, new FileReader("conf/default-drop.sql"));
            RunScript.execute(conn, new FileReader("conf/default-create.sql"));
            conn.close();
            Log.info("Executed Scripts.");
        }
        catch (Exception e)
        {
            System.err.println("Got an exception! Connection error or Script execution failed.");
            e.printStackTrace();
            System.exit(0);
        }

    }
}
