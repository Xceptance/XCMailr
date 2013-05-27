package xcmailrstarter;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;

import org.h2.tools.RunScript;

public class ScriptRunner {

	public ScriptRunner() {
		Connection conn;
		try {
			// Step 1: Load the JDBC driver.
			Class.forName("org.h2.Driver");
			System.out.println("Driver Loaded.");
			// Step 2: Establish the connection to the database.
			String url = "jdbc:h2:~/xcmailrDB;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";

			conn = DriverManager.getConnection(url, "sa", "");
			System.out.println("Got Connection.");

			RunScript.execute(conn, new FileReader("conf/default-drop.sql"));
			RunScript.execute(conn, new FileReader("conf/default-create.sql"));
			System.out.println("Executed Scripts.");
		} catch (Exception e) {
			System.err
					.println("Got an exception! Connection error or Script execution failed.");
			e.printStackTrace();
			System.exit(0);
		}

	}
}
