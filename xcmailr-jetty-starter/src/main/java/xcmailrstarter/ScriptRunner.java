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
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.h2.tools.RunScript;
import org.mortbay.log.Log;

/**
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class ScriptRunner
{

    public ScriptRunner(StarterConf config)
    {
        // Handle the connection
        Connection conn;
        try
        {
            Class.forName(config.XCM_DB_DRIVER);
            Log.info("Driver Loaded.");

            conn = DriverManager.getConnection(config.XCM_DB_URL, config.XCM_DB_USER, config.XCM_DB_PASS);
            Log.info("Got Connection.");

            boolean usrTable = false;
            boolean mbxTable = false;
            boolean mtxTable = false;
            // if xcmstarter has been started with the parameter "-Dxcmailr.xcmstart.droptables=true"
            // drop all tables
            if (!(System.getProperty("xcmailr.xcmstart.droptables") == null))
            {
                RunScript.execute(conn, new FileReader("conf/default-drop.sql"));
                Log.info("Executed Drop Table.");
            }
            DatabaseMetaData md = conn.getMetaData();

            ResultSet rs = md.getTables(null, null, "%", null);
            // check if the tables exist
            while (rs.next())
            {
                if (rs.getString(3).equals("USERS"))
                {
                    usrTable = true;
                }
                if (rs.getString(3).equals("MAILBOXES"))
                {
                    mbxTable = true;
                }
                if (rs.getString(3).equals("MAILTRANSACTIONS"))
                {
                    mtxTable = true;
                }
            }

            // create the tables if they're not existing
            if (!(usrTable && mbxTable && mtxTable))
            {
                RunScript.execute(conn, new FileReader("conf/default-create.sql"));
                Log.info("Executed Create Table.");
            }
            alterTable("USERS", "LANGUAGE", conn, md);
            alterTable("MAILTRANSACTIONS", "RELAYADDR", conn, md);

            conn.close();

        }
        catch (Exception e)
        {
            System.err.println("Got an exception! Connection error or Script execution failed.");
            e.printStackTrace();
            System.exit(0);
        }

    }

    public void alterTable(String tableName, String columnName, Connection conn, DatabaseMetaData md)
        throws SQLException
    {
        // get the user-table
        ResultSet rs = md.getColumns(null, "PUBLIC", tableName, null);

        // get all column-names of the usertable
        String columns = "";
        while (rs.next())
        {
            columns += rs.getString("COLUMN_NAME");
            columns += "; ";
        }
        // alter the user-table if it has no language-attribute
        if (!columns.contains(columnName))
        {
            Statement stmnt = conn.createStatement();
            stmnt.execute("ALTER TABLE " + tableName + " ADD " + columnName + " VARCHAR");
            Log.info("Altered table " + tableName + ", added column " + columnName);
        }
    }
}
