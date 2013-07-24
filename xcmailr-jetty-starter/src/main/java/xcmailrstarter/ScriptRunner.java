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

            connection = DriverManager.getConnection(config.XCM_DB_URL, config.XCM_DB_USER, config.XCM_DB_PASS);
            Log.info("Got Connection.");

            boolean usrTable = false;
            boolean mbxTable = false;
            boolean mtxTable = false;
            boolean fk_mtxTable_usr_id = false;
            boolean domainTable = false;

            // if xcmstarter has been started with the parameter "-Dxcmailr.xcmstart.droptables=true"
            // drop all tables
            if (System.getProperty("xcmailr.xcmstart.droptables") != null)
            {
                RunScript.execute(connection, new FileReader("conf/default-drop.sql"));
                Log.info("Executed Drop Table.");
            }

            // get the DB-metadata
            DatabaseMetaData md = connection.getMetaData();
            ResultSet rs = md.getTables(null, null, "%", null);
            // check if all tables exist
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
                if (rs.getString(3).equals("REGISTER_DOMAINS"))
                {
                    domainTable = true;
                }
            }

            // create the tables if they not exist
            if (!(usrTable && mbxTable && mtxTable && domainTable))
            { // simply execute the create-script
                RunScript.execute(connection, new FileReader("conf/default-create.sql"));
                Log.info("Executed Create Table.");
            }

            // check whether the foreign-key of the mailbox-table (usr_id->USER) exists
            rs = md.getImportedKeys(connection.getCatalog(), null, "MAILBOXES");
            while (rs.next())
            {
                String name = rs.getString("FK_NAME");
                if (name.equals("FK_MAILBOXES_USR_1"))
                {
                    fk_mtxTable_usr_id = true;
                }
            }

            if (!fk_mtxTable_usr_id)
            { // add the foreign-key if not exist
                Statement statement = connection.createStatement();
                statement.execute("alter table mailboxes add constraint fk_mailboxes_usr_1 foreign key (usr_id) references users (id) on delete restrict on update restrict;");
            }

            //
            alterTable("USERS", "LANGUAGE", connection, md);
            alterTable("MAILTRANSACTIONS", "RELAYADDR", connection, md);
            Statement statement = connection.createStatement();
            statement.execute("CREATE INDEX IF NOT EXISTS ix_mailtransactions_ts_1 ON mailtransactions (ts);");

            connection.close();

        }
        catch (Exception e)
        {
            Log.warn("Got an exception! Connection error or Script execution failed.");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Adds a column with the given name to the given table if its not already existing
     * 
     * @param tableName
     *            the name of the table to alter
     * @param columnName
     *            the name of the column to add
     * @param conn
     *            the database connection
     * @param md
     *            the metadata of the database
     * @throws SQLException
     */
    public void alterTable(String tableName, String columnName, Connection conn, DatabaseMetaData md)
        throws SQLException
    {
        // get the table with the given table-name
        ResultSet rs = md.getColumns(null, "PUBLIC", tableName, null);

        // get all column-names of the "tableName"
        String columns = "";
        while (rs.next())
        { // create a list with the current column-names
            columns += rs.getString("COLUMN_NAME");
            columns += "; ";
        }
        // alter the "tableName"-table if it has no column with the given columnName
        if (!columns.contains(columnName))
        {
            Statement stmnt = conn.createStatement();
            stmnt.execute("ALTER TABLE " + tableName + " ADD " + columnName + " VARCHAR");
            Log.info("Altered table " + tableName + ", added column " + columnName);
        }
    }
}
