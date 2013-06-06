package xcmailrstarter;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

public class StarterConf
{
    public final String XCM_HOME;

    public final Integer XCM_PORT;

    public final String XCM_DB_URL;

    public final String XCM_DB_USER;

    public final String XCM_DB_PASS;

    public final String XCM_DB_DRIVER;
    
    public final String XCM_CONTEXT_PATH;
    
    private Configuration conf;

    public StarterConf(String[] args) throws Exception
    {
            // get the server file-location
            XCM_HOME = System.getProperty("xcmailr.xcmstarter.home"); 
            // get the config file-location
            String confFile = System.getProperty("ninja.external.configuration");
            PropertiesConfiguration cfg = new PropertiesConfiguration();
            cfg.setEncoding("utf-8");
            cfg.setDelimiterParsingDisabled(true);
            String confPath = XCM_HOME + "/" + confFile;

            // try to load the config
            cfg.load(confPath);
            conf = (Configuration) cfg;
            XCM_DB_URL = conf.getString("ebean.datasource.databaseUrl");
            XCM_DB_USER = conf.getString("ebean.datasource.username");
            XCM_DB_PASS = conf.getString("ebean.datasource.password");
            XCM_DB_DRIVER = conf.getString("ebean.datasource.databaseDriver");
            XCM_PORT = conf.getInt("application.port");
            XCM_CONTEXT_PATH = conf.getString("application.basedir");
            
    }
    
}
