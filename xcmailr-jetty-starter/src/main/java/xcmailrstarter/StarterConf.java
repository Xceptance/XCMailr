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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * A configuration class which reads the necessary configuration parameters
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
public class StarterConf
{
    public final String XCM_HOME;

    public final Integer XCM_PORT;

    public final String XCM_DB_URL;

    public final String XCM_DB_USER;

    public final String XCM_DB_PASS;

    public final String XCM_DB_DRIVER;

    public final String XCM_CONTEXT_PATH;

    public final String XCM_HOST;

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
        final Configuration conf = (Configuration) cfg;
        XCM_DB_URL = conf.getString("ebean.datasource.databaseUrl");
        XCM_DB_USER = conf.getString("ebean.datasource.username");
        XCM_DB_PASS = conf.getString("ebean.datasource.password");
        XCM_DB_DRIVER = conf.getString("ebean.datasource.databaseDriver");
        XCM_PORT = conf.getInt("application.port");
        XCM_CONTEXT_PATH = conf.getString("application.basedir");
        XCM_HOST = conf.getString("application.host", "localhost");

    }
}
